# ASSET_MAPPING - which PDF image feeds which system

| PDF page(s) | Output folder | Used by |
|---|---|---|
| 1-5 characters | `assets/characters/char_01..05/{idle,walk,run,jump,punch,kick,block,hit,die,victory}_N.png` | `Assets.character()` -> `WorldRenderer.drawFighter()` (animation chosen from state) |
| 2-5 portraits | `characters/char_0N/portrait.jpg` | Character selection screen (`GameView.drawCharSelect`) |
| 6-14 maps | `assets/maps/*.jpg` | `Maps` (MapData.kt) + `WorldRenderer` (full art, never stretched; platforms measured against it) |
| 15-21 guns | `assets/weapons/*.png` | `Weapons` + drawn in hand, rotated to aim; HUD icon |
| 22 frag grenade | `grenades/explosive/grenade.png` | Grenade projectile + grenade pickup |
| 23 flash grenade | `grenades/flash/{grenade,flash_burst}.png` | Flash grenade + blinding burst |
| 24 smoke grenade | `grenades/smoke/grenade.png` | Smoke grenade (cloud is procedural) |
| 25 mine | `grenades/mine/{mine_placed,mine_armed}.png` | Mine system, mine button |
| 26 flashes | `effects/muzzle_flash/*.png` | Muzzle flash, bullet impact spark |
| 27 bullet | `bullets/bullet_1.png` | Bullet projectile |
| 29 explosion | `effects/explosion/explosion_1..5.png` | Grenade, mine, rocket, barrel explosions |
| 34 crates | `props/crate_{intact,damaged,broken}.png` | Destructible crates |
| 36 barrel | `props/barrel.png` | Explosive barrels |
| 42 planks | `props/plank.png` | Platforms on plaza maps |
| 38, 44 pickups | `pickups/{health,ammo}.png` | Pickup system |
| 53 HUD | `ui/*.png` | `TouchControls` |
