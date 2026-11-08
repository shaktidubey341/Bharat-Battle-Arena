# Bharat Battle Arena

A native Android (Kotlin, Canvas/SurfaceView, no game engine) 2D Mini-Militia-style arena shooter built entirely from the artwork in `game.pdf`.

## Features (v0.1)
- 5 playable fighters from the PDF, animated from the real sprite sheets (idle, walk, run, jump, punch, hit, die).
- 8 scrolling maps from the PDF (5 platform arenas: Bardha UP, Varanasi sunset/night, New Delhi, Red Fort; 3 plazas: UP, Varanasi Ghat, India Gate). Collision platforms were measured against the artwork.
- 7 weapons from the PDF: Pistol, SMG, Assault Rifle, Shotgun, Sniper, Heavy MG, Rocket Launcher (2 weapon slots, reload, recoil, spread, pickups, drops on death).
- Frag, Flash and Smoke grenades, land mines, melee, jetpack with fuel, destructible crates, explosive barrels, health/ammo/grenade/mine/weapon pickups.
- AI bots (Easy / Normal / Hard) using the same weapons, health and damage as the player.
- Modes: Free For All and Team Battle (Red vs Blue). 4 / 6 / 8 fighters, 3 / 5 / 10 minute matches.
- 5-second auto respawn at a safe spawn point, scoreboard, MVP, winner / draw result screen, rematch.
- Touch controls (two sticks + buttons from the PDF HUD sheet), size / opacity / zoom / 30-60 FPS settings, saved preferences.

## Controls
Left stick: move (up = jetpack, down = crouch / drop through planks). Right stick: aim and fire. Buttons: shoot, jump, jet, reload, grenade (tap FRAG/FLASH/SMOKE to change), switch weapon, melee, mine, crouch.

## Upcoming features (not in this version)
LAN multiplayer (host/join/lobby), audio, Mumbai and Bihar maps (not in the PDF), more weapon/pickup/prop art already in the PDF (fire, magazines, walls, debris), pinch-to-zoom, button drag-to-reposition, special attacks (intentionally excluded).

## Tech
Kotlin 1.9, AGP 8.5.2, Gradle 8.7, minSdk 24, no third-party libraries. Code lives in `app/src/main/java/com/bharatbattlearena/game/` (`ai`, `combat`, `core`, `map`, `player`, `ui`, `weapon`).

## Troubleshooting
- Build fails on GitHub: open the failed step log and send it to the developer.
- Black screen on start: make sure the APK was built from the full ZIP (assets folder is ~13 MB).
- Slow phone: Settings -> FRAME RATE -> 30 FPS.
See BUILD_INSTRUCTIONS.md, ASSET_INVENTORY.md and ASSET_MAPPING.md.
