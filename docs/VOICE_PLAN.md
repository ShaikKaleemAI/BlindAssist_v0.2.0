# Voice plan (Stage 5) — status: wake word + parser + dispatcher done and unit-tested; NO speech engine shipped
Why not shipped: it needs your decision. Android's built-in recogniser is not guaranteed offline (breaks the offline promise); Vosk/Whisper-tiny need large language models and extra permissions.
## To add Vosk (UNVERIFIED — I could not download or test it)
1. Gradle: `implementation("com.alphacephei:vosk-android:<latest>")` (check alphacephei.com/vosk for the current version and its JNA requirement).
2. Models: small en-in, hi and te models from alphacephei.com/vosk/models (verify names, sizes, licences). Do NOT commit them; extend tools/fetch_models.sh with pinned SHA256.
3. Manifest: `RECORD_AUDIO`, `FOREGROUND_SERVICE_MICROPHONE`; start the service with foregroundServiceType `camera|microphone`; request the mic permission in MainActivity.
4. Implement `VoiceEngine` (Voice.kt) around `SpeechService`; the service already routes transcripts through `WakeWord` -> `VoiceCommands` -> `CommandDispatcher`.
5. Measure: wake-word false accepts per hour while walking in traffic, command recall in noise, Indian-accent WER. All PENDING FIELD DATA.
## Commands today (after wake phrase "hey assist")
Working: what's ahead, read this, stop, quiet/detailed mode, I'm lost (spoken guidance only). Reply "not available yet": describe the scene, find <object>, take me to <place>, call my family.
