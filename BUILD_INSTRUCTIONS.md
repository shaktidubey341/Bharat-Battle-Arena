# Build instructions

## 1. Push to GitHub from Termux
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
Replace `YOUR_GITHUB_REPOSITORY_URL` with your own empty repository URL.

## 2. Get the APK from GitHub Actions
1. Open your repository -> **Actions** tab -> workflow **Android Build** (runs on every push; you can also press *Run workflow*).
2. When it is green, open the run and download the artifact **bharat-battle-arena-debug-apk**.
3. Unzip it and install `app-debug.apk` on your phone (allow "install unknown apps").

The workflow installs Gradle 8.7 itself, so the Gradle wrapper jar is not required on GitHub.

## 3. Build locally (PC with JDK 17 + Android SDK, or Android Studio)
Android Studio: File -> Open -> the `Bharat-Battle-Arena` folder -> Run.
Command line (needs Gradle 8.7 installed):
```
gradle assembleDebug
```
APK output: `app/build/outputs/apk/debug/app-debug.apk`

Optional: create a wrapper once with `gradle wrapper --gradle-version 8.7`, then use `./gradlew assembleDebug`.

## Status
This project was written without access to an Android toolchain, so **it has not been compiled yet**.
The first GitHub Actions run is the first real build. If it reports a compile error, send me the log and I will fix it.
