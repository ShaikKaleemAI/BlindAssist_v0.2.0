# Architecture
camera (CameraX 640x480 RGBA) -> rotate (gravity/upside-down) -> `Detector` (MediaPipe EfficientDet-Lite0 | `NullDetector` limited mode)
-> `estimateRange` (width/height/ground cues, inverse-variance fusion) -> `Tracker` (IoU + 1-D Kalman + looming TTC)
-> `AlertEngine` + `Avoidance` -> `Speech` (priority TTS) / `Earcons` (panned beeps) / vibration. `MetricsLogger` writes metrics.csv to app-private storage.
Pure-Kotlin logic (everything except Detector, Audio, Sensors, AssistService, MainActivity) is JVM-unit-tested.
## Replay + ablation (implemented, UNVERIFIED: not compiled or run)
- Tape = CSV of detector outputs per frame (`Tape.HEADER` in Replay.kt); optional `true_z` column for tape-measure ground truth.
- Phone side (debug builds only): `ReplayReceiver` converts a walk video to a tape with the real detector (adb commands in its header comment).
- PC side: `ReplayRunner` runs the real Pipeline from a tape in plain JVM; `Cfg.use*` switches disable one module at a time.
- Ablation: `BA_TAPE=/abs/tape.csv python tools/ablation/ablate.py tools/ablation/run_gradle.sh`.
- Limits: `speech_alerts_per_min` counts all spoken alerts, not verified false alarms; false alarms need labelled tapes.
