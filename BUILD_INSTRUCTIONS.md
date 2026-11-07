# Build and push instructions

## Termux: unzip, commit, push
```
unzip BHARAT-BATTLE-ARENA.zip
cd Bharat-Battle-Arena
git init
git add .
git commit -m "Initial Bharat Battle Arena"
git branch -M main
git remote add origin YOUR_GITHUB_REPOSITORY_URL
git push -u origin main
```
Replace `YOUR_GITHUB_REPOSITORY_URL` with your own repository URL. After the push, open the repository's
**Actions** tab; the "Android Build" workflow produces the APK under *Artifacts*.

## Local build
The Gradle wrapper JAR is not included. Either install Gradle 8.7 and run `gradle assembleDebug`, or create the
wrapper once with `gradle wrapper --gradle-version 8.7` and then use `./gradlew assembleDebug`.

Debug APK location: `app/build/outputs/apk/debug/app-debug.apk`

Release (unsigned unless you configure signing): `./gradlew assembleRelease bundleRelease`
-> `app/build/outputs/apk/release/` and `app/build/outputs/bundle/release/`.

## Signing (optional)
Set `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` in the environment, or add repository secrets
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` for the release workflow. Never commit a keystore.
