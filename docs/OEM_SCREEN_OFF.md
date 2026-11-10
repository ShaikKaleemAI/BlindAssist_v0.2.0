# Screen-off camera survival (all UNVERIFIED — no real devices were available)
Samsung (One UI), Xiaomi (MIUI/HyperOS), Oppo (ColorOS), Vivo (FuntouchOS/OriginOS): may kill foreground services or revoke camera when the screen is off, and "battery optimization/auto-start" toggles differ. Android 14+ camera FGS rules apply.
Test procedure: start app, lock screen, walk 30 min, check metrics.csv for frame gaps >1.5 s and spoken "camera lost". Record result per phone here. Battery-optimization exemption handling is NOT implemented yet (Stage 8).

## What the app does (implemented, UNVERIFIED)
Shows a brand hint on the main screen, a battery-settings button, refreshes the wake lock every 3 h, vibrates on an uncaught crash, and reports an unexpected stop at next launch. It cannot restart itself after a crash (Android 12+ restrictions).
