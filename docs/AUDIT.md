# AUDIT (Stage 0) — BlindAssist_Android.zip

Tags: **VERIFIED** = test/CI output exists; **UNVERIFIED** = not tested; **ASSUMED** = reasoned, not checked.
Audit method: every file read by hand. No compiler or Gradle was available in the audit sandbox (no network, no kotlinc/gradle), so **nothing below was compiled — NOT COMPILED LOCALLY**.

## Facts detected
| Item | Value |
|---|---|
| Package | `com.kaleem.blindassist` |
| AGP / Kotlin / Gradle | 8.5.2 / 1.9.24 / 8.7 (wrapper properties only) |
| compile/target/min SDK | 34 / 34 / 26 |
| CameraX / MediaPipe | 1.3.4 / tasks-vision 0.10.14 |
| Source | 11 Kotlin files, ~55 KB |
| Missing | `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, version catalog, tests, CI, LICENSE, app icon, model asset, proguard rules |
| Model | Not shipped; `app/build.gradle.kts` downloaded it at build time (breaks offline/CI builds) |

## Compile risks (all UNVERIFIED — could not compile)
1. `AlertEngine.update`: `compareBy({ -it.first.ordinal }, { it.second.ttc ?: 99.0 }, ...)` has no explicit type argument; Kotlin type inference may fail. **Fixed in stage-1** by `compareBy<Pair<Level, Track>>(...)`.
2. `kotlin.collections.ArrayDeque.removeFirst()` (Tracker): lint `NewApi` warning on newer JDK/Android API combos. Low risk.
3. `AssistService` uses `@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)` — needs `androidx.annotation` (transitive via core-ktx; ASSUMED OK).
4. `Detector.kt` and manifest lack an app icon; lint may flag `MissingApplicationIcon` as warning (not error).
5. `Track.nextId` is a global mutable companion var (not thread-safe, non-deterministic across tests).
6. Lint default `abortOnError=true`; unknown how many existing warnings are promoted. CI is the verifier.

## Logic bugs
1. **Dead code paths**: `Cfg.NAV` lists `backpack`, `stop sign`, `parking meter`, `tv`, but none has an entry in `Cfg.DIMS`/`GROUNDED`, so `estimateRange` returns `null` and they are silently dropped. Fixed: removed from NAV.
2. **Ground hazards unreachable**: `GROUND_HAZARDS` (stairs, pothole…) can never be produced by the COCO model; the `classify` branch is dead until a custom model exists (Stage 2).
3. **FOV mismatch (ASSUMED)**: `readHfov` uses the full sensor short side, but the analysis stream is 640x480 (4:3); if the sensor is 16:9 the stream is cropped and the real HFOV is smaller → distance bias. UNVERIFIED; measure with the tape benchmark.
4. **TTS language fallback missing**: `Speech` calls `tts.language = loc` and ignores the result. If the Hindi/Telugu voice is not installed the user gets silence or garbled English. Safety bug. Fixed in stage-1 (spoken English warning once).
5. **Model missing = app refuses to start** (`no_model` + `stopSelf`). Requirement says degrade gracefully. Fixed in stage-1 (limited mode).
6. Wake lock `acquire(4h)` expires silently; a 5th hour of use loses the lock. Not fixed yet (Stage 8).
7. Frame pipeline allocates a rotated Bitmap per frame (GC pressure, latency). Not fixed (Stage 1 logging will quantify).
8. Pitch sign convention assumed (`asin(gz/|g|)`, positive = camera looks down). ASSUMED correct for TYPE_GRAVITY; verify on device.
9. No roll correction, no step/sway compensation (known).
10. Alerts are muted for the first 10 s after start (`armedAt`) — by design, but camera failure in that window is also silent.

## Safety risks
- The code never speaks "clear"/"safe"; `Avoidance` internal state is named `"clear"` but never spoken. A unit test now enforces that no spoken string says "path is clear"/"safe to walk" (stage-1).
- COCO-only detector: no stairs, drops, potholes, glass, poles, overhead hazards. Cane required. Documented in README/SAFETY_STATEMENT.
- Distances are monocular estimates from assumed object sizes; **no accuracy has been measured** (UNVERIFIED).
- Screen-off camera access on Android 14+/OEM ROMs untested (UNVERIFIED) — see docs/OEM_SCREEN_OFF.md.
- Hindi/Telugu strings machine-drafted: **NEEDS NATIVE REVIEW**.

## Per-file notes
- `Cfg.kt` — tunables, mutable singleton; fine for now. NAV bug above.
- `Geometry.kt` — pinhole + ground-plane projection; no roll term.
- `Detector.kt` — hard-wired MediaPipe asset `efficientdet_lite0.tflite`; throws if absent.
- `Sensors.kt` — low-pass only when falling back to accelerometer; `upsideDown()` threshold gy<-3.
- `RangeFusion.kt` — inverse-variance fusion of width/height/ground cues, chi² consistency, fail-safe = nearest. Reasonable; untested.
- `Tracker.kt` — IoU matching, 1-D Kalman on range, looming TTC from log-size regression. Untested.
- `Avoidance.kt` — 1-D corridor interval merge; veer/blocked. Untested.
- `AlertEngine.kt` — hysteresis, edge-triggered speech, beeps, vibration. Untested.
- `Audio.kt` — stereo earcons via AudioTrack, priority TTS queue. TTS language fallback bug.
- `AssistService.kt` — foreground camera service, watchdog, MediaSession button. No thermal/battery handling.
- `MainActivity.kt` — programmatic UI, one big button; no spoken onboarding safety text beyond service start.
- `I18n.kt` — en/hi/te strings; Hindi/Telugu unreviewed.

## Self-review addendum (manual, no compiler available — NOT a substitute for CI)
Re-read every edited Kotlin file and cross-checked the unit-test assumptions against `Cfg`/`AlertEngine`/`Avoidance`/`I18n` constants by hand (e.g. corridor half-width at 2 m = 0.70 m, vehicle scale 2.0, step string). Fixed:
- `tts?.language = ...` replaced by `setLanguage(...)` (setter returns Int; synthetic-property assignment may not compile).
- `@Suppress` on an `else` expression replaced by a function-level annotation.
- Crash handler installed once per process (was chained on every service start, leaking the service).
- Speech key collision (`"lost"` used by both camera-lost and the voice "I'm lost" reply) -> `"lostmsg"`.
Remaining known compile/test risks (UNVERIFIED): TFLite/ML Kit dependency resolution and API signatures (`Tensor.numBytes`, `quantizationParams`), `MediaSession.Callback.onSkipToNext`, lint errors in the debug manifest overlay, numeric tolerances in tests that depend on Kalman convergence (`TrackerTest.approachingObjectGetsFiniteTtc`, `ReplayTest`).
