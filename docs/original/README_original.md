# Blind Assist — Android (pocket mode)

Phone in pocket, back camera peeking out, walk normally. Everything runs ON THE PHONE (no laptop, no Wi-Fi):
camera → EfficientDet-Lite0 (MediaPipe) → range fusion (width + height + ground-plane) → tracker (Kalman + looming TTC)
→ alerts → speech (Android TTS: English/Hindi/Telugu) + stereo beeps + vibration.
Core logic is a Kotlin port of Blind Assist v11 (range_fusion, tracker, avoidance, alert_engine, i18n).

## Build
1. Android Studio (Koala+), open this folder, let Gradle sync (needs internet once; the model is downloaded to
   `app/src/main/assets/efficientdet_lite0.tflite` automatically. Offline? drop that file there yourself).
2. Plug in the phone (USB debugging) → Run. Android 8.0+.

## Use
- Set **camera height** (cm, from ground to the camera in your pocket) and language. Tap START.
- You get 10 s to place the phone: upright, camera at the top edge out of the pocket, lens unobstructed.
- Earphone button (single press): "what is around me". Notification "Stop" button, or reopen app → STOP.
- Use bone-conduction / open-ear headphones so beeps are stereo and you still hear traffic.
- Phone's own speaker works too, but pocket muffles it, and panning needs headphones.

## Pocket specifics already handled
Tilt from the gravity sensor (live ground-plane correction), auto 180° flip if phone is upside-down, real FOV read from
camera, "camera covered" and "phone not upright" warnings, camera-stall watchdog, foreground service + wake lock.

## Not verified (written without an Android SDK; NOT compiled or run)
Build errors are possible; real accuracy, pocket sway, battery/heat, screen-off camera behaviour per phone brand,
Hindi/Telugu wording and TTS voice availability (install the voices in Android settings).
Known limits: COCO model has no stairs / potholes / drains / glass (use your cane); head-height hazards are not seen
from pocket height; roll (sideways tilt) is not corrected; never says the path is "clear".
