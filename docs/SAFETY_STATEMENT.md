# Safety statement
1. **Blind Assist supplements a white cane or guide dog. It never replaces one.** (Also spoken at start-up.)
2. The app NEVER says "path is clear" or "safe to walk". Silence or "nothing detected" does **not** mean safe. [VERIFIED by unit test `noSpokenStringClaimsClearOrSafe` once CI is green; currently UNVERIFIED — tests never run]
3. Not detected today: stairs, drops, potholes, open drains, glass, poles, overhead hazards, kerbs (COCO model only). [VERIFIED by code audit]
4. Distances are monocular estimates; accuracy is **UNVERIFIED** (benchmark in tools/benchmark pending).
5. Fail-safe: model missing -> spoken "limited mode" warning + vibration, repeated each minute [implemented, UNVERIFIED on device]; camera stall -> spoken warning [implemented, UNVERIFIED].
6. Hindi/Telugu wording is machine-drafted: **NEEDS NATIVE REVIEW**.
7. Not a medical device, not certified, no field trials done. Do not rely on it for road crossing or traffic.
8. Privacy: no network permission requested; no frames leave the phone. [VERIFIED: manifest has no INTERNET permission]
