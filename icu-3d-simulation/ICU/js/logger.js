class EventLogger {

    constructor() {
        this.entries = [];
        this.vitalsHistory = [];
        this.startTime = Date.now();
    }

    init() {
        this.entries = [];
        this.vitalsHistory = [];
        this.startTime = Date.now();
    }

    // record one event with a timestamp
    log(type, data) {
        this.entries.push({
            timestamp: new Date().toISOString(),
            elapsed: this.getElapsed(),
            type: type,
            data: data
        });
    }

    // keep the full history of vitals readings
    logVitals(vitals) {
        this.vitalsHistory.push({
            timestamp: new Date().toISOString(),
            elapsed: this.getElapsed(),
            ...vitals
        });
    }

    // mm:ss elapsed since the run started
    getElapsed() {
        const sec = Math.floor((Date.now() - this.startTime) / 1000);
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }

    // trigger a browser download of JSON
    download(filename, data) {
        const blob = new Blob([JSON.stringify(data, null, 2)], {type: 'application/json'});
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
    }

    // dump the whole log to icu_log.json
    exportJSON() {
        this.download('icu_log.json', {
            events: this.entries,
            vitalsHistory: this.vitalsHistory,
            exportedAt: new Date().toISOString()
        });
    }

    // reset the log
    clear() {
        this.init();
    }
}

const logger = new EventLogger();
