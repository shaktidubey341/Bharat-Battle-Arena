# BHARAT BATTLE ARENA

A native Android (Kotlin) 2D side-view arena shooter with touch controls, procedural graphics and
original fictional fighters. No game engine, no third-party libraries, no image assets required.

> **Build status:** this project was written without access to a Kotlin compiler. It has passed static
> checks (balanced brackets, imports, cross-file references, XML/YAML syntax) but **has not been compiled**.
> Run the GitHub Actions workflow (or `gradle assembleDebug`) and report any compiler errors.
> See `PROGRESS.md` for exactly what is and is not implemented.

## What works in this version (single player)
- Menu flow: Main menu -> Mode -> Character -> Map -> Match settings (5/10/15 min, difficulty, bot count) -> match
- 6 original fighters, each with a unique power and a small passive (see in-game character screen)
- One large scrolling map (Uttar Pradesh, 9600 units wide, multiple floors, bridges, stairs, crates)
- Movement: walk/run, jump, crouch, drop-through platforms, coyote time
- 7 weapons (pistol, SMG, rifle, shotgun, sniper, heavy, energy launcher) with pickups that respawn
- Hit zones (head/body/arm/leg effects), melee, knockback, shields, status effects
- AI bots (Easy/Normal/Hard) using the same stats as the player
- 5-second automatic respawn, unlimited, safe spawn selection, synchronized match timer, results screen with MVP
- Camera: smooth follow, zoom buttons, pinch zoom, shake, map clamping
- Settings: button size/opacity, joystick size/sensitivity, volume, graphics quality, 30/60 FPS (saved locally)

## Not implemented yet
LAN multiplayer (host/join/lobby/discovery), team battle, grenades, mines, maps 2-5, real audio/music files,
draggable button layout. The LAN menu entries show a notice instead of failing silently.

## Tech
Kotlin 1.9.24, Android Gradle Plugin 8.5.2, Gradle 8.7, JDK 17, compile/target SDK 34, min SDK 21.
Rendering: `SurfaceView` + `Canvas`. Fixed 60 Hz simulation, render capped at 30/60 FPS.

## Project structure
```
app/src/main/java/com/bharatbattlearena/game/
  MainActivity.kt      menus (native Views)       GameView.kt     game loop thread + touch routing
  GameEngine.kt        match state / rules        ai/             BotBrain, Difficulty
  player/              Fighter, controller, characters, procedural renderer
  weapon/ combat/ power/ collision/ map/ camera/ effects/ audio/ ui/ util/
```

## Build (Android Studio)
Open the folder, let Gradle sync, press Run. Or: `gradle assembleDebug`
(APK: `app/build/outputs/apk/debug/app-debug.apk`).

## GitHub Actions
Push to GitHub; `.github/workflows/android-build.yml` builds a debug APK and uploads it as an artifact.
`android-release.yml` builds release APK + AAB on `v*` tags (unsigned unless you add signing secrets).

## Controls
Left stick move - JUMP - DUCK (DUCK+JUMP drops through thin platforms) - FIRE (auto-aims at enemies in front) -
AIM (steadier shots; stick aims up/down) - RLD reload - HIT melee - SWAP weapon - POWER ability - +/- zoom.

## Termux
See `BUILD_INSTRUCTIONS.md`. Building the APK inside Termux is often unreliable; pushing to GitHub and using
Actions is the recommended route.

## Troubleshooting
- *Compiler error*: copy the first error line from the Actions log and report it.
- *`./gradlew` fails*: the wrapper JAR is not included; run `gradle wrapper --gradle-version 8.7` once, or use `gradle`.
- *No sound*: sounds are short synthesized tones; raise Sound volume in Settings.
