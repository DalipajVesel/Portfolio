const ui = (() => {

    // number keys pick an option
    document.addEventListener('keydown', (e) => {
        if (e.key < '1' || e.key > '9') return;
        const tag = document.activeElement?.tagName;
        if (tag === 'TEXTAREA' || tag === 'INPUT' || tag === 'SELECT') return;
        const panel = document.getElementById('narrative-panel');
        if (!panel || panel.classList.contains('hidden')) return;
        const buttons = panel.querySelectorAll('.option-btn');
        const idx = parseInt(e.key, 10) - 1;
        if (buttons[idx]) {
            e.preventDefault();
            buttons[idx].click();
        }
    });

    // show or hide a screen by id
    function showScreen(id) {
        document.getElementById(id)?.classList.remove('hidden');
    }

    function hideScreen(id) {
        document.getElementById(id)?.classList.add('hidden');
    }

    // hide start + debrief
    function hideAllScreens() {
        ['start-screen', 'debrief-screen'].forEach(id => hideScreen(id));
    }

    // the in-game HUD (vitals bar + buttons)
    function showHUD() {
        document.getElementById('hud')?.classList.remove('hidden');
    }

    function hideHUD() {
        document.getElementById('hud')?.classList.add('hidden');
        hideNarrative();
        hideGate();
        hideTimeout();
    }

    // refresh the vitals shown at the top
    function updateVitalsHUD(vitals) {
        if (!vitals) return;
        setVitalValue('valHR', vitals.hr);
        setVitalValue('valSpO2', vitals.spo2);
        setVitalValue('valRR', vitals.rr);
        setVitalValue('valBP', vitals.bp);
        setVitalValue('valTemp', typeof vitals.temp === 'number' ? vitals.temp.toFixed(1) : vitals.temp);
    }

    function setVitalValue(id, value) {
        const el = document.getElementById(id);
        if (el) el.textContent = value ?? '--';
    }

    // show a message or a decision in the narrative panel
    function showNarrative(node) {
        const panel = document.getElementById('narrative-panel');
        const textEl = document.getElementById('narrativeText');
        const optionsEl = document.getElementById('narrativeOptions');
        const continueBtn = document.getElementById('btnNarrativeContinue');

        if (!panel || !textEl || !optionsEl) return;

        optionsEl.innerHTML = '';
        continueBtn.style.display = 'none';
        hideTimeout();

        textEl.textContent = node.text || '';

        switch (node.type) {
            // message, only a Continue button
            case 'message':
                continueBtn.style.display = 'block';
                continueBtn.onclick = () => {
                    hideNarrative();
                    engine.continueFromMessage();
                };
                break;

            // decision, one button for each option
            case 'decision':
                if (node.options) {
                    node.options.forEach((option, index) => {
                        const btn = document.createElement('button');
                        btn.className = 'option-btn';
                        btn.innerHTML = `
                            <span class="option-key">${index + 1}</span>
                            <span>${option.label}</span>
                        `;
                        btn.addEventListener('click', () => {
                            hideNarrative();
                            engine.selectOption(option.id);
                        });
                        optionsEl.appendChild(btn);
                    });
                }
                break;
        }

        panel.classList.remove('hidden');
    }

    function hideNarrative() {
        const panel = document.getElementById('narrative-panel');
        if (panel) panel.classList.add('hidden');
        hideTimeout();
    }

    // the countdown bar under a timed decision
    function showTimeout(seconds) {
        const container = document.getElementById('narrativeTimeout');
        const fill = document.getElementById('timeoutBarFill');
        const label = document.getElementById('timeoutLabel');

        if (!container) return;
        container.classList.remove('hidden');
        if (fill) fill.style.width = '100%';
        if (label) label.textContent = `${seconds}s`;
    }

    // shrink the bar; turn it red near the end
    function updateTimeout(remainingSeconds, pct) {
        const fill = document.getElementById('timeoutBarFill');
        const label = document.getElementById('timeoutLabel');

        if (fill) fill.style.width = (pct * 100) + '%';
        if (label) label.textContent = `${Math.ceil(remainingSeconds)}s`;

        if (fill && pct < 0.3) {
            fill.style.background = 'var(--accent-red)';
        } else if (fill) {
            fill.style.background = 'var(--accent-orange)';
        }
    }

    function hideTimeout() {
        const container = document.getElementById('narrativeTimeout');
        if (container) container.classList.add('hidden');
    }

    function gateFormTitle(formId) {
        return engine.scenario?.ehr_config?.forms?.[formId]?.title || formId;
    }

    function gateFieldLabel(field) {
        return (typeof ehr !== 'undefined' && ehr) ? ehr._formatFieldName(field) : field;
    }

    // readable list of the missing EHR fields
    function describeMissing(node) {
        return engine.getMissingGateRequirements(node)
            .map(m => `${gateFormTitle(m.form_id)} - ${gateFieldLabel(m.field)}`)
            .join(', ');
    }

    // a documentation gate blocks the flow until the required EHR fields are filled in
    function showGate(node) {
        hideNarrative();

        const overlay = document.getElementById('gate-overlay');
        const title = document.getElementById('gateTitle');
        const description = document.getElementById('gateDescription');
        const blockedMsg = document.getElementById('gateBlockedMsg');

        if (!overlay) return;

        if (title) title.textContent = node.text || 'Documentation Gate';
        if (description) {
            const required = describeMissing(node);
            description.textContent = (node.description || '')
                + (required ? `  Απαιτούνται: ${required}.` : '');
        }
        if (blockedMsg) blockedMsg.textContent = node.feedback_blocked || '';

        const btnOpen = document.getElementById('btnOpenEHRFromGate');
        const btnCheck = document.getElementById('btnCheckGate');

        if (btnOpen) {
            btnOpen.onclick = () => {
                if (typeof ehr !== 'undefined' && ehr) {
                    const firstRequired = node.gate_requirements?.required_forms?.[0];
                    if (firstRequired) {
                        ehr.open(formIdToTab(firstRequired.form_id));
                    } else {
                        ehr.open();
                    }
                }
            };
        }

        if (btnCheck) {
            // 'Check' passes if filled, otherwise it shows what is missing
            btnCheck.onclick = () => {
                const passed = engine.checkGateRequirements(node);
                if (passed) {
                    hideGate();
                    engine.passGate(node);
                } else {
                    const miss = describeMissing(node);
                    showToast('warning', (node.feedback_blocked || 'Ελλιπής τεκμηρίωση')
                        + (miss ? ` Λείπει: ${miss}` : ''));
                    if (blockedMsg) {
                        blockedMsg.textContent = miss ? `Λείπει ακόμη: ${miss}` : (node.feedback_blocked || '');
                        blockedMsg.style.animation = 'none';
                        blockedMsg.offsetHeight;
                        blockedMsg.style.animation = 'vitalPulse 0.5s ease 2';
                    }
                }
            };
        }

        overlay.classList.remove('hidden');
    }

    function hideGate() {
        const overlay = document.getElementById('gate-overlay');
        if (overlay) overlay.classList.add('hidden');
    }

    function formIdToTab(formId) {
        const map = {
            'assessment_form': 'assessment',
            'intervention_form': 'intervention',
            'communication_log': 'communication'
        };
        return map[formId] || 'vitals';
    }

    // pop a message that fades after 5s
    function showToast(style, message) {
        const container = document.getElementById('toast-container');
        if (!container) return;

        const toast = document.createElement('div');
        toast.className = `toast toast-${style || 'info'}`;
        toast.textContent = message;
        container.appendChild(toast);

        setTimeout(() => {
            if (toast.parentNode) toast.parentNode.removeChild(toast);
        }, 5000);
    }

    // open/close the action log panel
    function toggleLog() {
        const panel = document.getElementById('log-panel');
        if (!panel) return;
        const willOpen = panel.classList.contains('hidden');
        panel.classList.toggle('hidden');
        if (willOpen) renderLog();
    }

    function closeLog() {
        const panel = document.getElementById('log-panel');
        if (panel) panel.classList.add('hidden');
    }

    // draw every logged event
    function renderLog() {
        const body = document.getElementById('logBody');
        if (!body) return;
        body.innerHTML = '';
        if (!logger.entries || logger.entries.length === 0) {
            body.innerHTML = '<p style="color: var(--text-muted); text-align: center; padding: 20px;">Δεν υπάρχουν καταγραφές.</p>';
            return;
        }
        logger.entries.forEach(entry => {
            const div = document.createElement('div');
            div.className = 'log-entry';
            const t = entry.elapsed || (entry.timestamp || '').slice(11, 19);
            const msg = (entry.data && entry.data.message) ? entry.data.message : '';
            // escape <, > and & so the log text is not read as HTML
            const safeMsg = msg.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
            div.innerHTML = `
                <div>
                    <span class="log-time">[${t}]</span>
                    <span class="log-type log-type-${entry.type}">${entry.type}</span>
                </div>
                <div class="log-msg">${safeMsg}</div>
            `;
            body.appendChild(div);
        });
        body.scrollTop = body.scrollHeight;
    }

    return {
        showScreen,
        hideAllScreens,
        showHUD,
        hideHUD,
        updateVitalsHUD,
        showNarrative,
        hideNarrative,
        showTimeout,
        updateTimeout,
        hideTimeout,
        showGate,
        hideGate,
        showToast,
        toggleLog,
        closeLog
    };
})();
