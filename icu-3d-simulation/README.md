# ICU 3D Simulation

A 3D intensive care room in the browser for clinical training. You walk
around, look at the patient and the monitor, take decisions with a time
limit, fill the EHR panel and get a score at the end. It is made with
Babylon.js and plain JavaScript.

The clinical part is data: a scenario is a JSON file with message, decision,
gate and end nodes, and the vitals and the alarms run on their own. Two
scenarios are included, hypoxaemia and asthma.

Human-Computer Interaction course.

    python server.py

Then open http://localhost:3000. The models load over HTTP.

The 3D models are from Sketchfab under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/):
[Hospital Bed](https://sketchfab.com/3d-models/hospital-bed-0e974ea6eb9a4c069c56f152ae5162a4)
by Ansh_Singla, [Patient](https://sketchfab.com/3d-models/patient-00b483f284a542899b94e99831f1ad1c)
by edouard77, [Medical Ventilator](https://sketchfab.com/3d-models/medical-ventilator-a03a99fab9314aab96fd41ec69acf1a3)
by lazarys, [LAPTOP](https://sketchfab.com/3d-models/laptop-dc3daa4c867a4582b6aaeeb484ca7bf4)
by VISIONNEW and [Call Button](https://sketchfab.com/3d-models/call-button-58894fc6e2b04fe9b282c3de816b0263)
by Sergey Burov. The monitor model, [patient monitor](https://sketchfab.com/3d-models/patient-monitor-81cfd6dd75a5401885614100e855bc93)
by inth04, is not in the repo because its license does not allow it. Download it
and save it as assets/monitor.glb, without it the vitals monitor is not shown.
