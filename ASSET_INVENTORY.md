# ASSET_INVENTORY - game.pdf (54 pages, all inspected)

Every page is ONE flat JPEG with no alpha channel. The "transparent" checkerboards on pages 15-52 are painted pixels,
so sprites were cut out with colour-based background removal. Originals are kept untouched in `source_pdf_pages/page_NN.jpg`.

| Pages | Category | Content | Type | Used in game as |
|---|---|---|---|---|
| 1-5 | Characters | 5 fighters: idle, walk, run, jump, punch, kick, block, hit, die, victory (+ special-attack/power-up FX, not used) | Real sprite sheets | Fighter animation (`assets/characters/char_0N/`), portraits from pages 2-5 |
| 6-8 | Maps | UP, Varanasi, Delhi scenic backdrops with flat floor | Single image | "Plaza" maps (UP Plaza, Varanasi Ghat, Delhi India Gate) |
| 9, 12 | Map | Bardha (UP) platform arena (2 near-identical variants) | Single image | Map "Bardha (UP)" (page 9) |
| 10, 11 | Maps | Varanasi arena - sunset / night | Single image | Maps "Varanasi Sunset", "Varanasi Night" |
| 13, 14 | Maps | New Delhi arena, Delhi Red Fort arena | Single image | Maps "New Delhi", "Delhi Red Fort" |
| 15-21 | Guns | Pistol, SMG, Rifle, Shotgun, Sniper, Heavy MG, Rocket launcher (8 frames each) | Sheets, fake checkerboard | `assets/weapons/*.png` (first frame used) |
| 22-24 | Grenades | Frag, Flash, Smoke grenades | Sheets | `assets/grenades/{explosive,flash,smoke}` |
| 25 | Mine | Mine states | Sheet | `assets/grenades/mine` |
| 26 | Effects | Muzzle flash / sparks | Sheet | `effects/muzzle_flash` |
| 27 | Bullet | Bullet with trail | Sheet, black bg | `assets/bullets` |
| 28-29 | Effects | Explosions | Sheets | `effects/explosion` (5 frames) |
| 30-33 | Effects | Fire (orange, blue), green smoke, grey smoke | Sheets | Not used yet (procedural smoke is used) |
| 34-37, 41-43 | Props | Crates, barrels, walls, wooden planks, debris | Sheets | crate, barrel, plank used; rest upcoming |
| 38-40, 44-52 | Pickups | Health, magazines, ammo boxes, weapon/grenade/mine pickups | Sheets | health + ammo used; rest upcoming |
| 53 | UI | HUD parts, joysticks, round buttons | Sheet | on-screen buttons + stick base (`assets/ui`) |
| 54 | UI | Menu / lobby mockups | Mockup | layout reference only |

## Gaps vs. the original brief
- Only **5** characters exist in the PDF (brief asked for 6). Sheets are martial-arts style (no gun-holding poses).
- **No Mumbai or Bihar map** in the PDF. Delivered 8 maps from the PDF instead (5 arenas + 3 plazas).
- No audio files in the PDF.
- Pages 2, 4, 5 look like realistic likenesses with team-style jerseys: review before publishing the game.

## Extraction result
- Characters: 5 fighters, 36-47 frames each, 10 animation sequences each (50 total).
- Weapons: 7. Bullets: 4 frames. Explosion: 5 frames. Muzzle/spark: 3. Grenades: 3 + flash burst. Mines: 2.
- Props: 3 crate states, barrel, plank. Pickups: health, ammo. UI: 8 buttons + stick base.
