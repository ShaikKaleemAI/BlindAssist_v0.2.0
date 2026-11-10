# Scorecard (updated each stage). Scores 1-10; no 9/10 without field data.
| Area | Score | Evidence | Tag |
|---|---|---|---|
| Innovation | 4 | Pocket-mode design + range fusion + TTC are in the original code; nothing new measured | ASSUMED |
| Accuracy | 2 | No tape-measure data, no hazard model, COCO only | UNVERIFIED |
| Speed | ? | Metrics CSV logger exists (docs/ARCHITECTURE.md); no device run | UNVERIFIED |
| Safety | 3 | Cane-first wording, never "clear", limited-mode warning, unit-test guard; no field test | UNVERIFIED |
| Assistant quality | 1 | Only earphone-button "what's around me"; no voice, OCR, navigation | VERIFIED (by absence) |

## Targets
| Target | Status | Procedure |
|---|---|---|
| Distance error <10% at 1-5 m | UNVERIFIED | tools/benchmark/analyze_distance.py with tape-measure CSV |
| Hazard recall >90%, <1 false alarm / 5 min | UNVERIFIED | ml/eval.py on site-held-out test set + 30-min walk logs |
| Latency <150 ms, 15+ fps | UNVERIFIED | metrics.csv from app (internal filesDir); true photon-to-alert needs 240 fps camera test |
| 2 h without overheating | UNVERIFIED | 2 h walk with metrics.csv temp column |
| Build green | NOT COMPILED LOCALLY | CI run of android-ci.yml |

## Stage status (all code NOT COMPILED LOCALLY; Kotlin tests written but never run)
- Stage 0: done. Stage 1: done in code + replay/ablation (tape, JVM runner, debug-only video->tape receiver).
- Stage 2 data: open-data pipeline built and its merge/split/report logic unit-tested on MOCK data (15 Python tests PASSED locally, tooling only): ml/open_data/{sources.yaml,fetch_open_data.py,build_dataset.py}, ml/make_report.py, ml/run_all.sh, ml/train_colab.ipynb. Downloads/training/eval NOT run (no network/GPU here); no real dataset size or accuracy exists yet.
- Stage 2: runtime hazard detector implemented (`TfliteHazardDetector`: TFLite INT8 YOLO, CPU default, NNAPI opt-in, `YoloDecoder` + NMS with JVM tests, labels file, hazard classes ranged via ground cue, hi/te labels NEEDS NATIVE REVIEW). No model is trained or shipped, so detection of stairs/potholes/drains/etc. is UNVERIFIED and currently inactive: without `hazard_yolo_int8.tflite` + `hazard_labels.txt` the app uses COCO or limited mode. Stretch-resize vs training letterbox is a known accuracy risk. GPU delegate NOT implemented.
- Stage 3: free-space/drop-off logic on relative inverse depth (ground-plane fit) + `DepthEstimator` interface, synthetic tests only. NO depth model, NOT wired. Depth is relative, never metric.
- Stage 4: step detector, orientation/roll, sway estimator, height-from-walk math: library + tests; NOT wired (roll not applied, no calibration UI).
- Stage 5: wake-word gate + command parser + dispatcher wired into the service (tests not run). NO speech engine (`VoiceEngine` = `NullVoice`), so voice does not work yet; plan in docs/VOICE_PLAN.md. Unsupported commands reply "not available yet".
- Stage 6: waypoint navigator, direct-bearing fallback, OCR post-processing (unwired); ML Kit OCR (Latin only) wired to earphone "next" button = "read this" (UNVERIFIED on device; MediaSession key mapping differs per headset). No GPS source, no maps/routing, no VLM.
- Stage 7: fall detector + 15 s cancel countdown wired (accelerometer listener -> spoken warning -> repeating LOCAL alarm; NO message or call is sent; earphone button cancels; thresholds ASSUMED, false-alarm/miss rates UNVERIFIED). Adaptive verbosity, place store, daily summary: logic + tests, NOT wired. No face memory, no caregiver link / SOS delivery (needs permissions + opt-in), no persistence/UI.
- Stage 8: thermal frame-skip + hot warning; wake-lock refresh every 3 h; last-gasp vibration on crash; "stopped unexpectedly last time" notice on next launch; battery-settings button; per-brand hints (Samsung/Xiaomi/Oppo/Vivo). NOT done: auto-restart after crash (Android 12+ blocks camera foreground-service restarts from background), actual OEM testing. All UNVERIFIED on real devices.
- Stage 9: Python evidence tools (5 tests PASSED locally: tooling only), paper/slides/protocol with PENDING FIELD DATA.
