# Deploy
```
cd BlindAssist
# one-time: generate and commit the Gradle wrapper (needs Gradle + network; not possible in the authoring sandbox)
gradle wrapper --gradle-version 8.7 && git add gradle gradlew gradlew.bat && git commit -m "add gradle wrapper"
git remote add origin https://github.com/<you>/BlindAssist.git
git push -u origin main
```
Add secrets (GitHub > Settings > Secrets > Actions): KEYSTORE_BASE64 (`base64 -w0 release.jks`), KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
```
git tag v0.2.0 && git push origin v0.1.0
```
Download APK from Releases, then: `adb install -r BlindAssist-release.apk` or sideload. Fetch the detector model for local builds: `tools/fetch_models.sh`.
