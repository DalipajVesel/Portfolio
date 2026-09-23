const debrief = (() => {

    // open the debrief: score, path, documentation
    function show(node, results) {
        const screen = document.getElementById('debrief-screen');
        if (!screen) return;

        ui.hideHUD();
        ui.hideNarrative();
        ui.hideGate();
        if (ehr) ehr.close();
        ui.closeLog();

        _renderScore(results);
        _renderDecisionPath(results);
        _renderDocumentation(results);
        _setupExport(results);

        screen.classList.remove('hidden');
    }

    function hide() {
        const screen = document.getElementById('debrief-screen');
        if (screen) screen.classList.add('hidden');
    }

    // the big score number
    function _renderScore(results) {
        const scoreEl = document.getElementById('debriefScore');
        if (scoreEl) scoreEl.textContent = results.score;
    }

    // list every node the user went through
    function _renderDecisionPath(results) {
        const container = document.getElementById('debriefPath');
        if (!container) return;

        container.innerHTML = '';

        if (!results.decisionPath || results.decisionPath.length === 0) {
            container.innerHTML = '<p>Δεν καταγράφηκαν αποφάσεις.</p>';
            return;
        }

        results.decisionPath.forEach((step, index) => {
            const div = document.createElement('div');
            div.className = 'path-step';

            const option = step.selectedOption
                ? `<br><span class="path-label">Επιλογή: ${step.selectedOption}</span>`
                : '';

            div.innerHTML = `
                <div class="path-info">
                    <strong>${index + 1}. ${_truncate(step.text, 80)}</strong>
                    <span class="path-label">${step.type.toUpperCase()} - ${step.elapsed}</span>
                    ${option}
                </div>
            `;
            container.appendChild(div);
        });
    }

    // green or red checklist from the scenario flags
    function _renderDocumentation(results) {
        const container = document.getElementById('debriefDocs');
        if (!container) return;

        container.innerHTML = '';

        const flags = results.flags || {};
        const docItems = [
            {label: 'Κλινική Αξιολόγηση', flag: flags.assessment_complete},
            {label: 'Ρύθμιση Οξυγόνου', flag: flags.oxygen_adjusted},
            {label: 'Τεκμηρίωση Gate #1', flag: flags.documentation_1_complete},
            {label: 'Κλιμάκωση', flag: flags.escalation_complete},
            {label: 'Τεκμηρίωση Gate #2', flag: flags.documentation_2_complete}
        ];

        docItems.forEach(item => {
            const div = document.createElement('div');
            div.className = `doc-item ${item.flag ? 'completed' : 'missed'}`;
            div.innerHTML = `
                <span>${item.flag ? 'Ναι' : 'Όχι'}</span>
                <span>${item.label}</span>
            `;
            container.appendChild(div);
        });
    }

    function _setupExport(results) {
        const btn = document.getElementById('btnDebriefExport');
        if (btn) {
            btn.onclick = () => _exportReport(results);
        }
    }

    // full report with the scenario, the score, the path, the EHR data and the log
    function _exportReport(results) {
        const report = {
            scenario: engine.scenario?.scenario_meta || {},
            score: results.score,
            flags: results.flags,
            decisionPath: results.decisionPath,
            ehrData: results.ehrData,
            eventLog: logger.entries,
            exportedAt: new Date().toISOString()
        };

        logger.download(`debrief_report_${new Date().toISOString().slice(0, 10)}.json`, report);
        ui.showToast('success', 'Η αναφορά εξήχθη');
    }

    // shorten long text with ...
    function _truncate(str, maxLen) {
        if (!str) return '';
        return str.length > maxLen ? str.substring(0, maxLen) + '...' : str;
    }

    return {show, hide};
})();
