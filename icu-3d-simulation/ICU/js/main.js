let canvas, babylonEngine, scene, camera;
let hotspotMeshes = {};
let sounds = {};
let currentScenarioURL = 'scenarios/hypoxia.json';
let customScenarioData = null;

let monitorScreen = null;
let monitorWave = [];
let monitorAlarmActive = false;
let currentMonitorVitals = null;
const SCREEN_CFG = {widthFrac: 0.60, heightFrac: 0.40, faceFlip: false};

const ROOM = {width: 16, depth: 16, height: 5};

window.addEventListener('DOMContentLoaded', async () => {
    canvas = document.getElementById('renderCanvas');
    babylonEngine = new BABYLON.Engine(canvas, true);

    // build the whole 3D scene: room, models, hotspots, monitor
    scene = await createScene();

    // scene is ready, hide the loader and show the start screen
    document.getElementById('loading-screen').classList.add('hidden');
    document.getElementById('start-screen').classList.remove('hidden');

    // keep redrawing every frame
    babylonEngine.runRenderLoop(() => scene.render());
    window.addEventListener('resize', () => babylonEngine.resize());

    // create the EHR panel
    ehr = new EHRPanel();

    setupEventListeners();

    // browsers block autoplay until the first gesture
    const primeAudio = () => {
        if (sounds.alarm) {
            const v = sounds.alarm.volume;
            sounds.alarm.volume = 0;
            sounds.alarm.play().then(() => {
                sounds.alarm.pause();
                sounds.alarm.currentTime = 0;
                sounds.alarm.volume = v;
            }).catch(() => {
                sounds.alarm.volume = v;
            });
        }
        window.removeEventListener('pointerdown', primeAudio);
        window.removeEventListener('keydown', primeAudio);
    };
    window.addEventListener('pointerdown', primeAudio);
    window.addEventListener('keydown', primeAudio);
});

// camera, lights and everything inside the room
async function createScene() {
    const scene = new BABYLON.Scene(babylonEngine);
    scene.clearColor = new BABYLON.Color4(0.05, 0.06, 0.1, 1);

    camera = new BABYLON.UniversalCamera('camera',
        new BABYLON.Vector3(0, 1.65, -6), scene);
    // look around with the mouse
    camera.attachControl(canvas, true);
    // WASD + arrow keys to move
    camera.keysUp = [87, 38];
    camera.keysDown = [83, 40];
    camera.keysLeft = [65, 37];
    camera.keysRight = [68, 39];
    camera.speed = 0.3;
    camera.inertia = 0.82;
    camera.angularSensibility = 3000;
    camera.minZ = 0.1;
    camera.fov = 1.0;

    const halfW = ROOM.width / 2 - 0.5;
    const halfD = ROOM.depth / 2 - 0.5;
    // keep the camera inside the four walls
    scene.onBeforeRenderObservable.add(() => {
        camera.position.x = Math.max(-halfW, Math.min(halfW, camera.position.x));
        camera.position.z = Math.max(-halfD, Math.min(halfD, camera.position.z));
        camera.position.y = Math.max(1.2, Math.min(ROOM.height - 0.3, camera.position.y));
    });

    const hemi = new BABYLON.HemisphericLight('hemi', new BABYLON.Vector3(0, 1, 0), scene);
    hemi.intensity = 0.8;
    hemi.diffuse = new BABYLON.Color3(0.85, 0.9, 1.0);

    const dir = new BABYLON.DirectionalLight('dir', new BABYLON.Vector3(-1, -2, 1), scene);
    dir.position = new BABYLON.Vector3(4, 6, -4);
    dir.intensity = 0.4;

    // floor/walls/ceiling, then models, hotspots, monitor, sounds
    createRoom(scene);
    await loadModels(scene);
    setupHotspots(scene);
    createMonitorScreen(scene);
    loadSounds();

    return scene;
}

// builds the floor, the 4 walls and the ceiling
function createRoom(scene) {
    const halfW = ROOM.width / 2;
    const halfD = ROOM.depth / 2;
    const H = ROOM.height;

    const floor = BABYLON.MeshBuilder.CreateGround('floor', {width: ROOM.width, height: ROOM.depth}, scene);
    const floorMat = new BABYLON.StandardMaterial('floorMat', scene);
    try {
        const tex = new BABYLON.Texture('assets/floor_diff.jpg', scene);
        tex.uScale = 5;
        tex.vScale = 5;
        floorMat.diffuseTexture = tex;
    } catch (e) {
        floorMat.diffuseColor = new BABYLON.Color3(0.75, 0.78, 0.8);
    }
    floor.material = floorMat;

    const wallMat = new BABYLON.StandardMaterial('wallMat', scene);
    wallMat.backFaceCulling = false;
    try {
        const tex = new BABYLON.Texture('assets/wall_color.jpg', scene);
        tex.uScale = 4;
        tex.vScale = 2;
        wallMat.diffuseTexture = tex;
    } catch (e) {
        wallMat.diffuseColor = new BABYLON.Color3(0.92, 0.93, 0.95);
    }

    const walls = [
        {pos: [0, H / 2, -halfD], rot: [0, Math.PI, 0], w: ROOM.width},
        {pos: [0, H / 2, halfD], rot: [0, 0, 0], w: ROOM.width},
        {pos: [-halfW, H / 2, 0], rot: [0, -Math.PI / 2, 0], w: ROOM.depth},
        {pos: [halfW, H / 2, 0], rot: [0, Math.PI / 2, 0], w: ROOM.depth}
    ];
    walls.forEach((def, i) => {
        const wall = BABYLON.MeshBuilder.CreatePlane(`wall_${i}`, {
            width: def.w, height: H, sideOrientation: BABYLON.Mesh.DOUBLESIDE
        }, scene);
        wall.position = new BABYLON.Vector3(...def.pos);
        wall.rotation = new BABYLON.Vector3(...def.rot);
        wall.material = wallMat;
    });

    const ceiling = BABYLON.MeshBuilder.CreatePlane('ceiling', {
        width: ROOM.width, height: ROOM.depth,
        sideOrientation: BABYLON.Mesh.DOUBLESIDE
    }, scene);
    ceiling.position.y = H;
    ceiling.rotation.x = Math.PI / 2;
    const ceilMat = new BABYLON.StandardMaterial('ceilMat', scene);
    ceilMat.diffuseColor = new BABYLON.Color3(0.95, 0.95, 0.97);
    ceilMat.backFaceCulling = false;
    ceiling.material = ceilMat;
}

// load each .glb model and drop it in place
async function loadModels(scene) {
    const modelConfigs = [
        {name: 'bed', file: 'bed.glb', pos: [0.1, 0.055, 0], scale: 1.09, rot: [0, 1.558, 0]},
        {name: 'patient_model', file: 'patient_model.glb', pos: [0.2, 1.598, 0.3], scale: 0.013, rot: [0, 0.008, 0]},
        {name: 'monitor', file: 'monitor.glb', pos: [-2.5, 0.9, -0.5], scale: 4.377, rot: [0, 0.524, 0]},
        {
            name: 'ventilator',
            file: 'medical_ventilator.glb',
            pos: [2.661, 0.018, -0.594],
            scale: 1.056,
            rot: [0, -1.642, 0]
        },
        {name: 'ehr_terminal', file: 'ehr_terminal.glb', pos: [-3.5, 0.01, 2.5], scale: 0.226, rot: [0, 1.047, 0]},
        {name: 'call_button', file: 'call_button.glb', pos: [-1.5, 1.7, -1.8], scale: 0.14, rot: [0, 0, 0]}
    ];

    // import the model and keep its meshes (hotspots attach to them)
    for (const cfg of modelConfigs) {
        try {
            const result = await BABYLON.SceneLoader.ImportMeshAsync('', 'assets/', cfg.file, scene);
            const root = result.meshes[0];
            root.name = cfg.name;
            root.position = new BABYLON.Vector3(...cfg.pos);
            root.rotation = new BABYLON.Vector3(...cfg.rot);
            root.scaling = new BABYLON.Vector3(cfg.scale, cfg.scale, cfg.scale);
            hotspotMeshes[cfg.name] = {root, meshes: result.meshes};
        } catch (e) {
            console.warn(`Could not load ${cfg.file}`, e);
        }
    }
}

const HOTSPOT_LABELS = {
    'hs_monitor': 'Μόνιτορ Ζωτικών (Monitor)',
    'hs_patient': 'Ασθενής / Κλίνη (Patient)',
    'hs_ventilator': 'Αναπνευστήρας (Ventilator)',
    'hs_ehr': 'Τερματικό EHR (e-Health)',
    'hs_call': 'Κουμπί Κλήσης (Call Button)'
};

// tooltip label (prefer the one from the scenario JSON)
function getHotspotLabel(hotspotId) {
    const fromScenario = engine?.scenario?.hotspots?.find(h => h.id === hotspotId)?.label;
    return fromScenario || HOTSPOT_LABELS[hotspotId] || hotspotId;
}

function showHotspotTooltip(hotspotId) {
    const tip = document.getElementById('hotspot-tooltip');
    if (!tip) return;
    tip.textContent = getHotspotLabel(hotspotId);
    tip.classList.remove('hidden');
}

function hideHotspotTooltip() {
    document.getElementById('hotspot-tooltip')?.classList.add('hidden');
}

function moveHotspotTooltip(e) {
    const tip = document.getElementById('hotspot-tooltip');
    if (!tip || tip.classList.contains('hidden')) return;
    tip.style.left = (e.clientX + 14) + 'px';
    tip.style.top = (e.clientY + 14) + 'px';
}

// make the 5 objects clickable and show a tooltip on hover
function setupHotspots(scene) {
    window.addEventListener('pointermove', moveHotspotTooltip);

    const hotspotMap = {
        'monitor': 'hs_monitor',
        'bed': 'hs_patient',
        'patient_model': 'hs_patient',
        'ventilator': 'hs_ventilator',
        'ehr_terminal': 'hs_ehr',
        'call_button': 'hs_call'
    };

    // click and hover on every mesh of each object
    for (const [meshName, hotspotId] of Object.entries(hotspotMap)) {
        const meshData = hotspotMeshes[meshName];
        if (!meshData) continue;

        for (const mesh of meshData.meshes) {
            if (!mesh.actionManager) {
                mesh.actionManager = new BABYLON.ActionManager(scene);
            }
            mesh.actionManager.registerAction(
                new BABYLON.ExecuteCodeAction(BABYLON.ActionManager.OnPickTrigger, () => {
                    onHotspotClick(hotspotId);
                })
            );
            mesh.actionManager.registerAction(
                new BABYLON.ExecuteCodeAction(BABYLON.ActionManager.OnPointerOverTrigger, () => {
                    canvas.style.cursor = 'pointer';
                    showHotspotTooltip(hotspotId);
                })
            );
            mesh.actionManager.registerAction(
                new BABYLON.ExecuteCodeAction(BABYLON.ActionManager.OnPointerOutTrigger, () => {
                    canvas.style.cursor = 'default';
                    hideHotspotTooltip();
                })
            );
        }
    }
}

// what clicking an object in the room does
function onHotspotClick(hotspotId) {
    logger.log('HOTSPOT_INTERACTION', {
        message: `Click hotspot: ${hotspotId}`,
        hotspot: hotspotId
    });

    switch (hotspotId) {
        case 'hs_ehr':
            if (ehr) ehr.open();
            break;
        case 'hs_monitor':
            if (ehr) ehr.open('vitals');
            break;
        case 'hs_call':
            ui.showToast('info', 'Κουμπί κλήσης ενεργοποιήθηκε');
            break;
        case 'hs_ventilator':
            ui.showToast('info', 'Αναπνευστήρας');
            break;
        case 'hs_patient':
            ui.showToast('info', 'Αξιολόγηση ασθενούς');
            break;
    }
}

// load the looping alarm sound
function loadSounds() {
    try {
        sounds.alarm = new Audio('assets/alarm.mp3');
        sounds.alarm.loop = true;
        sounds.alarm.volume = 0.5;
    } catch (e) {
        console.warn('Alarm sound not loaded');
    }
}

let alarmPlaying = false;

// start/stop the alarm without restarting it each time
function setAlarm(active) {
    if (!sounds.alarm) return;
    if (active && !alarmPlaying) {
        sounds.alarm.play().catch(() => {
        });
        alarmPlaying = true;
    } else if (!active && alarmPlaying) {
        sounds.alarm.pause();
        sounds.alarm.currentTime = 0;
        alarmPlaying = false;
    }
}

// draws the live vitals onto the monitor's screen
function createMonitorScreen(scene) {
    const monitor = hotspotMeshes['monitor'];
    if (!monitor || !monitor.root) return;

    let center, size;
    try {
        let min = null, max = null;
        for (const mesh of (monitor.meshes || [])) {
            if (!mesh.getBoundingInfo) continue;
            mesh.computeWorldMatrix(true);
            const bb = mesh.getBoundingInfo().boundingBox;
            if (!min) {
                min = bb.minimumWorld.clone();
                max = bb.maximumWorld.clone();
            } else {
                min = BABYLON.Vector3.Minimize(min, bb.minimumWorld);
                max = BABYLON.Vector3.Maximize(max, bb.maximumWorld);
            }
        }
        if (!min) throw new Error('no geometry');
        center = min.add(max).scale(0.5);
        size = max.subtract(min);
    } catch (e) {
        center = monitor.root.position.clone();
        size = new BABYLON.Vector3(1.2, 1.0, 0.6);
    }

    let normal = new BABYLON.Vector3(-center.x, 0, -center.z);
    if (normal.lengthSquared() < 1e-4) normal = new BABYLON.Vector3(0, 0, 1);
    normal.normalize();
    if (SCREEN_CFG.faceFlip) normal.scaleInPlace(-1);

    const faceWidth = Math.abs(normal.z) * size.x + Math.abs(normal.x) * size.z;
    const w = Math.max(0.25, faceWidth * SCREEN_CFG.widthFrac);
    const h = Math.max(0.18, size.y * SCREEN_CFG.heightFrac);

    const plane = BABYLON.MeshBuilder.CreatePlane('monitorScreen', {width: w, height: h}, scene);

    // placement tuned by hand
    plane.position = new BABYLON.Vector3(-2.278, 1.168, -0.11);
    plane.rotation = new BABYLON.Vector3(0, -2.607, 0);
    plane.scaling = new BABYLON.Vector3(1.608, 1.695, 1);
    plane.isPickable = false;

    // a canvas texture we repaint every frame
    const tex = new BABYLON.DynamicTexture('monitorTex', {width: 512, height: 320}, scene, false);
    const mat = new BABYLON.StandardMaterial('monitorScreenMat', scene);
    mat.diffuseTexture = tex;
    mat.emissiveTexture = tex;
    mat.disableLighting = true;
    mat.backFaceCulling = false;
    plane.material = mat;

    monitorScreen = {tex: tex, ctx: tex.getContext(), W: 512, H: 320};
    monitorWave = new Array(monitorScreen.W).fill(0);

    let phase = 0;
    // each frame: add an ECG sample and redraw
    scene.onBeforeRenderObservable.add(function () {
        if (!monitorScreen) return;
        const hr = (currentMonitorVitals && Number(currentMonitorVitals.hr)) || 80;
        phase += hr / 3600;
        monitorWave.push(ecgSample(phase));
        monitorWave.shift();
        drawMonitorScreen();
    });

    drawMonitorScreen();
}

// one ECG beat over phase 0..1
function ecgSample(t) {
    const x = ((t % 1) + 1) % 1;
    if (x < 0.10) return Math.sin((x / 0.10) * Math.PI) * 0.15;
    if (x < 0.18) return 0;
    if (x < 0.22) return -0.20;
    if (x < 0.27) return 1.00;
    if (x < 0.32) return -0.35;
    if (x < 0.55) return Math.sin(((x - 0.32) / 0.23) * Math.PI) * 0.25;
    return 0;
}

// paints the grid, the ECG line and the HR/SpO2/RR numbers
function drawMonitorScreen() {
    const m = monitorScreen;
    if (!m) return;
    const ctx = m.ctx, W = m.W, H = m.H;
    const v = currentMonitorVitals || {hr: '--', spo2: '--', rr: '--'};
    const alarm = monitorAlarmActive;

    ctx.fillStyle = '#04121a';
    ctx.fillRect(0, 0, W, H);

    ctx.strokeStyle = 'rgba(45,120,95,0.22)';
    ctx.lineWidth = 1;
    for (let gx = 0; gx < W; gx += 26) {
        ctx.beginPath();
        ctx.moveTo(gx, 0);
        ctx.lineTo(gx, H);
        ctx.stroke();
    }
    for (let gy = 0; gy < H; gy += 26) {
        ctx.beginPath();
        ctx.moveTo(0, gy);
        ctx.lineTo(W, gy);
        ctx.stroke();
    }

    ctx.strokeStyle = alarm ? '#ff3b3b' : '#39ff5e';
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    const midY = H * 0.40, amp = H * 0.26;
    for (let i = 0; i < monitorWave.length; i++) {
        const px = i * (W / monitorWave.length);
        const py = midY - monitorWave[i] * amp;
        if (i === 0) ctx.moveTo(px, py); else ctx.lineTo(px, py);
    }
    ctx.stroke();

    ctx.textAlign = 'left';
    ctx.textBaseline = 'alphabetic';
    ctx.fillStyle = '#5fe3c0';
    ctx.font = 'bold 18px monospace';
    ctx.fillText('SpO2', 14, H - 64);
    ctx.fillStyle = alarm ? '#ff5555' : '#39ff5e';
    ctx.font = 'bold 56px monospace';
    ctx.fillText(String(v.spo2), 12, H - 14);

    ctx.textAlign = 'right';
    ctx.fillStyle = '#ff8a8a';
    ctx.font = 'bold 16px monospace';
    ctx.fillText('HR', W - 14, 30);
    ctx.fillStyle = '#ff6b6b';
    ctx.font = 'bold 44px monospace';
    ctx.fillText(String(v.hr), W - 14, 74);

    ctx.fillStyle = '#ffd479';
    ctx.font = 'bold 26px monospace';
    ctx.fillText('RR ' + v.rr, W - 14, H - 16);
    ctx.textAlign = 'left';

    // red border and SpO2 LOW text while the alarm is on
    if (alarm) {
        const pulse = 0.45 + 0.55 * Math.abs(Math.sin(Date.now() / 280));
        ctx.strokeStyle = 'rgba(255,40,40,' + pulse.toFixed(3) + ')';
        ctx.lineWidth = 10;
        ctx.strokeRect(5, 5, W - 10, H - 10);
        ctx.fillStyle = 'rgba(255,40,40,' + (0.55 + 0.4 * pulse).toFixed(3) + ')';
        ctx.fillRect(0, 0, W, 28);
        ctx.fillStyle = '#ffffff';
        ctx.font = 'bold 17px monospace';
        ctx.textAlign = 'center';
        ctx.fillText('SpO2 LOW', W / 2, 20);
        ctx.textAlign = 'left';
    }

    m.tex.update();
}

// ask the engine if the alarm is on, then set the sound and the screen
function syncMonitorAlertFeedback() {
    const alertActive = engine && typeof engine.isMonitorAlertActive === 'function'
        ? engine.isMonitorAlertActive()
        : false;
    monitorAlarmActive = alertActive;
    setAlarm(alertActive);
}

// the engine events go to the 3D scene and the UI
function wireEngineCallbacks() {
    engine.onNodeEnter = (node) => {
        ui.showNarrative(node);
        syncMonitorAlertFeedback();
    };

    engine.onVitalsChange = (vitals) => {
        currentMonitorVitals = vitals;
        ui.updateVitalsHUD(vitals);
        if (ehr) ehr.updateVitals(vitals);
        syncMonitorAlertFeedback();
    };

    engine.onToast = (style, message) => {
        ui.showToast(style, message);
    };

    engine.onGate = (node) => {
        ui.showGate(node);
    };

    engine.onEnd = (node, results) => {
        setAlarm(false);
        debrief.show(node, results);
    };

    engine.onTimeout = (type, value, pct) => {
        switch (type) {
            case 'start':
                ui.showTimeout(value);
                break;
            case 'tick':
                ui.updateTimeout(value, pct);
                break;
            case 'expired':
                ui.hideTimeout();
                ui.hideNarrative();
                break;
        }
    };
}

// hook up all the buttons
function setupEventListeners() {
    document.getElementById('btnStartScenario').addEventListener('click', startScenario);

    // load a custom scenario from a .json the user picks
    document.getElementById('scenarioFileInput').addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (ev) => {
            try {
                customScenarioData = JSON.parse(ev.target.result);
                document.getElementById('uploadedFileName').textContent = `${file.name}`;
                document.querySelectorAll('.scenario-card').forEach(c => c.classList.remove('active'));
            } catch (err) {
                ui.showToast('danger', 'Σφάλμα ανάγνωσης JSON');
            }
        };
        reader.readAsText(file);
    });

    document.querySelectorAll('.scenario-card').forEach(card => {
        card.addEventListener('click', () => {
            document.querySelectorAll('.scenario-card').forEach(c => c.classList.remove('active'));
            card.classList.add('active');
            currentScenarioURL = card.dataset.scenario;
            customScenarioData = null;
            document.getElementById('uploadedFileName').textContent = '';
        });
    });

    document.getElementById('btnEHR').addEventListener('click', () => {
        ui.closeLog();
        if (ehr) ehr.toggle();
    });

    document.getElementById('btnLog').addEventListener('click', () => {
        if (ehr) ehr.close();
        ui.toggleLog();
    });

    document.getElementById('btnCloseLog').addEventListener('click', () => ui.closeLog());

    document.getElementById('btnExportLog').addEventListener('click', () => logger.exportJSON());

    document.getElementById('btnReset').addEventListener('click', () => {
        if (confirm('Θέλεις σίγουρα να κάνεις επανεκκίνηση;')) {
            resetAll();
        }
    });

    document.getElementById('btnDebriefRestart').addEventListener('click', () => {
        debrief.hide();
        resetAll();
        startScenario();
    });

    document.getElementById('btnDebriefMenu').addEventListener('click', () => {
        debrief.hide();
        resetAll();
    });
}

// load the scenario, reset state, show the HUD and begin
async function startScenario() {
    try {
        if (customScenarioData) {
            engine.loadFromObject(customScenarioData);
        } else {
            await engine.loadFromURL(currentScenarioURL);
        }

        if (!ehr) ehr = new EHRPanel();
        ehr.reset();
        ehr.buildForms(engine.scenario?.ehr_config);

        engine.init();
        wireEngineCallbacks();

        currentMonitorVitals = engine.getVitals();
        ui.updateVitalsHUD(engine.getVitals());
        ehr.updateVitals(engine.getVitals());
        syncMonitorAlertFeedback();

        ui.hideAllScreens();
        debrief.hide();
        ui.showHUD();

        camera.position = new BABYLON.Vector3(2.248, 2.58, 5.801);
        camera.rotation = new BABYLON.Vector3(0.315, 3.602, 0);

        engine.start();

    } catch (err) {
        console.error('Error starting scenario:', err);
        ui.showToast('danger', `Σφάλμα: ${err.message}`);
    }
}

// stop everything and go back to the start screen
function resetAll() {
    engine.clearTimeout();
    setAlarm(false);
    monitorAlarmActive = false;
    currentMonitorVitals = null;
    ui.hideNarrative();
    ui.hideGate();
    ui.hideHUD();
    ui.closeLog();
    if (ehr) ehr.reset();
    logger.clear();

    ui.showScreen('start-screen');
}
