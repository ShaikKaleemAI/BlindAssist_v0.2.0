# Changelog
## 0.2.0 (unreleased)
- Repo restructure, Gradle KTS + version catalog, limited-mode fallback, TTS voice warning, metrics CSV, unit tests, CI.
- Motion helpers (steps, orientation, sway, height calibration) as tested library; thermal throttling + hot warning.
- Personal-assistant logic: fall detector, SOS countdown, adaptive verbosity, place store, daily summary (unwired).
- Navigation (waypoint routes, off-route) and OCR post-processing logic (unwired).
- Hazard model runtime (TFLite YOLO) + decoder, depth free-space logic, ML Kit OCR on earphone next, OEM hints, crash/wake-lock handling.
- Fall alarm (local), wake-word gate, voice-engine plug-in point, spoken "not available" replies.
- Open-data pipeline (fetch, merge, train, eval, generated reports, Colab notebook); stairs unified into one class.
