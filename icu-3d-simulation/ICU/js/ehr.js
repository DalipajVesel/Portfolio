class EHRPanel {
    constructor() {
        this.panel = document.getElementById('ehr-panel');
        this.isOpen = false;
        this.activeTab = 'vitals';
        this.formData = {};
        this.currentVitals = null;

        this._setupTabs();
        this._setupClose();
    }

    // clicking a tab switches the visible panel
    _setupTabs() {
        const tabs = document.querySelectorAll('.ehr-tab');
        tabs.forEach(tab => {
            tab.addEventListener('click', () => {
                this.switchTab(tab.dataset.tab);
            });
        });
    }

    _setupClose() {
        const btn = document.getElementById('btnCloseEHR');
        if (btn) btn.addEventListener('click', () => this.close());
    }

    // open the EHR (optionally on a given tab)
    open(tab) {
        if (tab) this.switchTab(tab);
        this.panel?.classList.remove('hidden');
        this.isOpen = true;
    }

    close() {
        this.panel?.classList.add('hidden');
        this.isOpen = false;
    }

    toggle() {
        if (this.isOpen) this.close();
        else this.open();
    }

    // show the chosen tab, refresh the vitals if it is that one
    switchTab(tabId) {
        this.activeTab = tabId;

        document.querySelectorAll('.ehr-tab').forEach(t => {
            t.classList.toggle('active', t.dataset.tab === tabId);
        });
        document.querySelectorAll('.ehr-tab-content').forEach(c => {
            c.classList.remove('active');
        });
        const content = document.getElementById(`tab-${tabId}`);
        if (content) content.classList.add('active');

        if (tabId === 'vitals' && this.currentVitals) {
            this._renderVitalsTable(this.currentVitals);
        }
    }

    // the forms come from ehr_config in the scenario json
    buildForms(ehrConfig) {
        if (!ehrConfig || !ehrConfig.forms) return;

        for (const [formId, config] of Object.entries(ehrConfig.forms)) {
            this._buildForm(formId, config);
        }
    }

    // one form, a textarea for each field and a save button
    _buildForm(formId, config) {
        const container = document.getElementById(`form-${formId}`);
        if (!container) return;

        container.innerHTML = '';

        config.fields.forEach(field => {
            const fieldDiv = document.createElement('div');
            fieldDiv.className = 'ehr-field';

            const label = document.createElement('label');
            label.setAttribute('for', `input-${formId}-${field}`);
            label.textContent = this._formatFieldName(field);

            const textarea = document.createElement('textarea');
            textarea.id = `input-${formId}-${field}`;
            textarea.dataset.formId = formId;
            textarea.dataset.field = field;
            textarea.placeholder = `Εισαγωγή...`;
            textarea.rows = 2;

            if (this.formData[formId]?.[field]) {
                textarea.value = this.formData[formId][field];
            }

            fieldDiv.appendChild(label);
            fieldDiv.appendChild(textarea);
            container.appendChild(fieldDiv);
        });

        const saveBtn = document.createElement('button');
        saveBtn.type = 'button';
        saveBtn.className = 'btn-save';
        saveBtn.textContent = 'Αποθήκευση';
        saveBtn.addEventListener('click', () => this._saveForm(formId, config));
        container.appendChild(saveBtn);
    }

    // collect the fields and hand them to the engine
    _saveForm(formId, config) {
        const data = {};
        let hasData = false;

        config.fields.forEach(field => {
            const input = document.getElementById(`input-${formId}-${field}`);
            if (input) {
                const value = input.value?.trim() || '';
                if (value) {
                    data[field] = value;
                    hasData = true;
                }
            }
        });

        // don't save a completely empty form
        if (!hasData) {
            ui.showToast('warning', 'Συμπληρώστε τουλάχιστον ένα πεδίο');
            return;
        }

        this.formData[formId] = {...this.formData[formId], ...data};
        engine.submitEHRForm(formId, data);

        ui.showToast('success', `Φόρμα αποθηκεύτηκε`);
    }

    // new vitals, keep them and draw the table again
    updateVitals(vitals) {
        if (!vitals) return;
        this.currentVitals = vitals;
        if (this.activeTab === 'vitals') {
            this._renderVitalsTable(vitals);
        }
    }

    // the vitals table and the last readings
    _renderVitalsTable(vitals) {
        const tbody = document.getElementById('ehrVitalsTable');
        if (!tbody) return;

        const labels = {
            hr: {label: 'Καρδιακός Ρυθμός (HR)', unit: 'bpm'},
            spo2: {label: 'Κορεσμός Οξυγόνου (SpO2)', unit: '%'},
            rr: {label: 'Αναπνοές (RR)', unit: '/min'},
            bp: {label: 'Αρτηριακή Πίεση (BP)', unit: 'mmHg'},
            temp: {label: 'Θερμοκρασία', unit: '°C'}
        };

        tbody.innerHTML = '';

        for (const [key, info] of Object.entries(labels)) {
            const value = vitals[key];
            const displayValue = (typeof value === 'number' && key === 'temp')
                ? value.toFixed(1) : value;

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${info.label}</td>
                <td><strong>${displayValue}</strong> ${info.unit}</td>
            `;
            tbody.appendChild(tr);
        }

        const historyEl = document.getElementById('vitalsHistory');
        if (historyEl) {
            historyEl.innerHTML = '';
            // last 5 readings, newest first
            const recent = logger.vitalsHistory.slice(-5).reverse();
            if (recent.length === 0) {
                historyEl.innerHTML = '<p>Δεν υπάρχει ιστορικό.</p>';
            } else {
                recent.forEach(entry => {
                    const div = document.createElement('div');
                    const t = (typeof entry.temp === 'number') ? entry.temp.toFixed(1) : entry.temp;
                    div.innerHTML = `[${entry.elapsed}] HR:${entry.hr} SpO2:${entry.spo2}% RR:${entry.rr} BP:${entry.bp} T:${t}°C`;
                    historyEl.appendChild(div);
                });
            }
        }
    }

    // wipe everything for a new run
    reset() {
        this.formData = {};
        this.currentVitals = null;
        this.activeTab = 'vitals';
        this.close();

        ['form-assessment_form', 'form-intervention_form', 'form-communication_log'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.innerHTML = '';
        });

        const tbody = document.getElementById('ehrVitalsTable');
        if (tbody) tbody.innerHTML = '';

        const history = document.getElementById('vitalsHistory');
        if (history) history.innerHTML = '';

        this.switchTab('vitals');
    }

    // Greek label for each form field
    _formatFieldName(field) {
        const names = {
            observation: 'Παρατηρήσεις',
            skin_color: 'Χρώμα Δέρματος',
            consciousness: 'Επίπεδο Συνείδησης',
            device: 'Συσκευή',
            fiO2_setting: 'Ρύθμιση FiO2',
            flow_rate: 'Ροή',
            recipient: 'Παραλήπτης',
            reason: 'Αιτία',
            outcome: 'Αποτέλεσμα'
        };
        return names[field] || field.replace(/_/g, ' ');
    }
}

let ehr = null;
