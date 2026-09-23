class ScenarioEngine {
    constructor() {
        this.scenario = null;
        this.state = null;
        this.currentNode = null;
        this.decisionPath = [];
        this.timeoutInterval = null;
        this.ehrData = {};
        this.activeGlobalRules = new Set();
        this.onNodeEnter = null;
        this.onVitalsChange = null;
        this.onToast = null;
        this.onGate = null;
        this.onEnd = null;
        this.onTimeout = null;
    }

    // fetch a scenario JSON from a URL
    async loadFromURL(url) {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`Αποτυχία φόρτωσης σεναρίου: ${response.status}`);
        this.scenario = await response.json();
        return this.scenario;
    }

    // or use a scenario object we already have (uploaded file)
    loadFromObject(obj) {
        this.scenario = obj;
        return this.scenario;
    }

    // reset state to the scenario's initial_state and start a fresh log
    init() {
        if (!this.scenario) throw new Error('Δεν έχει φορτωθεί σενάριο');

        // deep copy so we never touch the original scenario
        this.state = JSON.parse(JSON.stringify(this.scenario.initial_state));
        this.decisionPath = [];
        this.ehrData = {};
        this.currentNode = null;
        this.activeGlobalRules = new Set();
        this.clearTimeout();

        if (this.scenario.ehr_config && this.scenario.ehr_config.forms) {
            for (const formId of Object.keys(this.scenario.ehr_config.forms)) {
                this.ehrData[formId] = {};
            }
        }

        logger.init();
        logger.logVitals({...this.state.vitals});
    }

    // begin at the first node
    start() {
        if (!this.scenario || !this.scenario.nodes || this.scenario.nodes.length === 0) {
            throw new Error('Σενάριο χωρίς κόμβους');
        }
        this.processNode(this.scenario.nodes[0].id);
    }

    // find a node by id
    getNodeById(id) {
        return this.scenario.nodes.find(n => n.id === id);
    }

    // log it, apply the effects, check the rules and go on by node type
    processNode(nodeId) {
        this.clearTimeout();
        const node = this.getNodeById(nodeId);
        if (!node) {
            console.error('Node not found:', nodeId);
            return;
        }

        this.currentNode = node;
        // remember every node we enter, for the debrief path
        this.decisionPath.push({
            nodeId: node.id,
            type: node.type,
            text: node.text,
            timestamp: new Date().toISOString(),
            elapsed: logger.getElapsed()
        });

        logger.log('NODE_ENTER', {
            message: `Είσοδος σε κόμβο: ${node.id} (${node.type})`,
            nodeId: node.id,
            text: node.text
        });

        if (node.effects) {
            this.applyEffects(node.effects);
        }

        this.evaluateGlobalRules();

        switch (node.type) {
            case 'message':
                if (this.onNodeEnter) this.onNodeEnter(node);
                break;
            case 'decision':
                if (this.onNodeEnter) this.onNodeEnter(node);

                if (node.timeout) {
                    this.startTimeout(node);
                }
                break;
            case 'gate':
                this.handleGate(node);
                break;
            case 'end':
                this.handleEnd(node);
                break;
            default:
                console.warn('Unknown node type:', node.type);
        }
    }

    // the user picked an option on a decision node
    selectOption(optionId) {
        if (!this.currentNode || this.currentNode.type !== 'decision') return;

        this.clearTimeout();
        const option = this.currentNode.options.find(o => o.id === optionId);
        if (!option) return;

        logger.log('OPTION_SELECTED', {
            message: `Επιλογή: "${option.label}"`,
            nodeId: this.currentNode.id,
            optionId: option.id,
            label: option.label
        });

        const lastPath = this.decisionPath[this.decisionPath.length - 1];
        if (lastPath) lastPath.selectedOption = option.label;

        // note which hotspot the option is tied to
        if (option.target_hotspot) {
            logger.log('HOTSPOT_INTERACTION', {
                message: `Αλληλεπίδραση με hotspot: ${option.target_hotspot}`,
                hotspot: option.target_hotspot
            });
        }

        if (option.effects) {
            this.applyEffects(option.effects);
        }

        if (option.next_node_id) {
            setTimeout(() => this.processNode(option.next_node_id), 500);
        }
    }

    // 'Continue' on a message node goes to the next node
    continueFromMessage() {
        if (!this.currentNode || this.currentNode.type !== 'message') return;
        if (this.currentNode.next_node_id) {
            this.processNode(this.currentNode.next_node_id);
        }
    }

    // apply effects: score, flags, vitals, toast
    applyEffects(effects) {

        if (effects.score_delta !== undefined) {
            this.state.current_score += effects.score_delta;
        }

        if (effects.state_update) {
            for (const [key, value] of Object.entries(effects.state_update)) {
                this.setNestedValue(this.state, key, value);
            }
        }

        // change vitals, log it, refresh UI, re-check rules
        if (effects.vitals_update) {
            const oldVitals = {...this.state.vitals};
            for (const [key, value] of Object.entries(effects.vitals_update)) {
                this.state.vitals[key] = value;
            }
            logger.log('VITALS_CHANGE', {
                message: `Αλλαγή ζωτικών: ${JSON.stringify(effects.vitals_update)}`,
                old: oldVitals,
                new: {...this.state.vitals}
            });
            logger.logVitals({...this.state.vitals});
            if (this.onVitalsChange) this.onVitalsChange(this.state.vitals);

            this.evaluateGlobalRules();
        }

        if (effects.toast) {
            const toast = typeof effects.toast === 'string'
                ? {style: 'info', message: effects.toast}
                : effects.toast;
            if (this.onToast) this.onToast(toast.style, toast.message);
        }
    }

    // sets a value by path, e.g. 'vitals.spo2'
    setNestedValue(obj, path, value) {
        const keys = path.split('.');
        let current = obj;
        for (let i = 0; i < keys.length - 1; i++) {
            if (current[keys[i]] === undefined) current[keys[i]] = {};
            current = current[keys[i]];
        }
        current[keys[keys.length - 1]] = value;
    }

    // reads a value by path
    getNestedValue(obj, path) {
        const keys = path.split('.');
        let current = obj;
        for (const key of keys) {
            if (current === undefined || current === null) return undefined;
            current = current[key];
        }
        return current;
    }

    // the toast shows only when a rule turns on
    evaluateGlobalRules() {
        if (!this.scenario.rules || !this.scenario.rules.global_rules) return;

        const currentlyActiveRules = new Set();

        this.scenario.rules.global_rules.forEach((rule, index) => {
            const ruleKey = rule.id || `global_rule_${index}`;
            const isActive = this.evaluateCondition(rule.condition);

            if (!isActive) return;

            currentlyActiveRules.add(ruleKey);

            // already on, don't show the toast again
            if (this.activeGlobalRules.has(ruleKey)) {
                return;
            }

            for (const effect of rule.effects) {
                // visual alarm 
                if (effect.type === 'ui_toast' && this.onToast) {
                    this.onToast(effect.style, effect.message);
                }
            }
        });

        this.activeGlobalRules = currentlyActiveRules;
    }

    // alarm if flagged, or if SpO2 < 90
    isMonitorAlertActive() {
        if (!this.state) return false;

        if (this.state.ui && this.state.ui.monitor_alert) return true;
        return this.state.vitals.spo2 < 90;
    }

    // check a condition (lt/gt/eq... or plain equality)
    evaluateCondition(condition) {
        for (const [path, check] of Object.entries(condition)) {
            const value = this.getNestedValue(this.state, path);
            if (typeof check === 'object') {
                if (check.lt !== undefined && !(value < check.lt)) return false;
                if (check.gt !== undefined && !(value > check.gt)) return false;
                if (check.eq !== undefined && !(value === check.eq)) return false;
                if (check.gte !== undefined && !(value >= check.gte)) return false;
                if (check.lte !== undefined && !(value <= check.lte)) return false;
            } else {
                if (value !== check) return false;
            }
        }
        return true;
    }

    // countdown for timed decisions; on expiry apply the penalty
    startTimeout(node) {
        const timeout = node.timeout;
        const totalMs = timeout.seconds * 1000;
        const startTime = Date.now();

        if (this.onTimeout) this.onTimeout('start', timeout.seconds);

        this.timeoutInterval = setInterval(() => {
            const elapsed = Date.now() - startTime;
            const remaining = Math.max(0, totalMs - elapsed);
            const pct = remaining / totalMs;

            if (this.onTimeout) this.onTimeout('tick', remaining / 1000, pct);

            if (remaining <= 0) {
                this.clearTimeout();
                logger.log('TIMEOUT', {
                    message: `Timeout στον κόμβο ${node.id} (${timeout.seconds}s)`,
                    nodeId: node.id
                });
                if (timeout.on_timeout_effects) {
                    this.applyEffects(timeout.on_timeout_effects);
                }
                if (this.onTimeout) this.onTimeout('expired');
                if (timeout.next_node_id) {
                    setTimeout(() => this.processNode(timeout.next_node_id), 1000);
                }
            }
        }, 100);
    }

    clearTimeout() {
        if (this.timeoutInterval) {
            clearInterval(this.timeoutInterval);
            this.timeoutInterval = null;
        }
    }

    // a gate: let the UI block progress until docs are filled
    handleGate(node) {
        logger.log('GATE_CHECK', {message: `Gate: ${node.text}`, nodeId: node.id});
        if (this.onGate) this.onGate(node);
    }

    checkGateRequirements(node) {
        return this.getMissingGateRequirements(node).length === 0;
    }

    // which required EHR fields are still empty
    getMissingGateRequirements(node) {
        const missing = [];
        if (!node.gate_requirements || !node.gate_requirements.required_forms) return missing;
        for (const req of node.gate_requirements.required_forms) {
            const formData = this.ehrData[req.form_id] || {};
            for (const field of req.fields) {
                if (!formData[field] || formData[field].toString().trim() === '') {
                    missing.push({form_id: req.form_id, field});
                }
            }
        }
        return missing;
    }

    // gate passed: apply its effects and move on
    passGate(node) {
        if (node.effects_on_pass) {
            this.applyEffects(node.effects_on_pass);
        }
        if (node.feedback_success) {
            if (this.onToast) this.onToast('success', node.feedback_success);
        }
        if (node.next_node_id) {
            setTimeout(() => this.processNode(node.next_node_id), 500);
        }
    }

    // store what was typed into an EHR form
    submitEHRForm(formId, data) {
        this.ehrData[formId] = {...this.ehrData[formId], ...data};
        logger.log('EHR_SUBMIT', {
            message: `EHR υποβολή: ${formId}`,
            formId,
            data
        });
    }

    // scenario is over, send the results to the debrief
    handleEnd(node) {
        this.clearTimeout();
        if (this.onEnd) this.onEnd(node, {
            score: this.state.current_score,
            flags: this.state.flags,
            decisionPath: this.decisionPath,
            ehrData: this.ehrData,
            vitalsHistory: logger.vitalsHistory,
            totalTime: logger.getElapsed()
        });
    }

    // current vitals snapshot
    getVitals() {
        return {...this.state.vitals};
    }
}

const engine = new ScenarioEngine();
