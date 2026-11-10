# Remaining work (as of the last commit) — nothing here is done unless marked
Legend: [YOU] only you can do it · [DECIDE] needs your decision · [ME] I can build it once unblocked

## 0. First, before anything else
- [YOU] Push the repo (docs/DEPLOY.md), run `gradle wrapper --gradle-version 8.7`, commit the wrapper, read the first CI result.
- [ME] Fix every compile/test/lint failure from that CI log. The code has never been compiled.

## 1. Blocked on a decision
| Item | Decision needed | Then I can build |
|---|---|---|
| Speech recognition (Stage 5) | Engine: Vosk or Whisper-tiny? Accept microphone permission + `camera|microphone` foreground type + large models fetched by script? | `VoiceEngine` implementation, model fetch + pinned hashes, permission flow (docs/VOICE_PLAN.md) |
| GPS navigation (Stage 6) | Accept location permission + `location` foreground type? Navigation is only reachable by voice today, so it depends on the row above | location source, saved-places UI, route recording, progress announcements (navigator logic already exists) |
| Face memory (Stage 7) | Do you want biometric data stored at all? If yes: consent screen, per-person delete, on-device only | embedding model integration + consent UI |
| Caregiver link / SOS delivery (Stage 7) | SMS (needs SEND_SMS) or an optional online channel (breaks the no-network default unless opt-in)? | opt-in SMS sender; today the fall alarm is local only |
| Scene description | Accept a large on-device VLM download? | optional model fetch + fallback text |

## 2. Needs data, a model or hardware [YOU]
- Run the open-data pipeline once on a GPU (Colab: ml/train_colab.ipynb, ~no data from you needed; optional free Roboflow key), commit docs/generated + the model it copies into assets/models. Then (for any 9+ claim) collect field video: ml/dataset/COLLECTION_PLAN.md Stage B.
- Export a depth model (Depth Anything V2 small / MiDaS) to INT8 TFLite, then I wire `DepthEstimator` + `FreeSpace` into the pipeline.
- Tape-measure distance benchmark; 30-minute false-alarm walks; 2-hour thermal walk; screen-off tests on Samsung/Xiaomi/Oppo/Vivo.
- Native-speaker review of every Hindi and Telugu string (labels, alerts, wake phrases, commands).
- Keystore + GitHub secrets for signed releases.
- User study with blind participants and an O&M instructor (docs/USER_STUDY_PROTOCOL.md). Never rely on the app near traffic before this.

## 3. Wiring I deliberately held back [ME, after CI is green]
- Roll correction (axis sign unverified), sway into range uncertainty, camera-height calibration walk + UI.
- Adaptive verbosity into the alert path; place store persistence; daily summary.
- Replace stretch-resize with letterbox in the hazard detector to match training.
- GPU delegate (NNAPI is opt-in and off by default).

## 4. Targets still UNVERIFIED
Distance error <10%, hazard recall >90% with <1 false alarm/5 min, latency <150 ms, 15+ fps, 2 h without overheating. See docs/SCORECARD.md for the measurement procedure of each.
