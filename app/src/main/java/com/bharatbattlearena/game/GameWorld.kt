package com.bharatbattlearena.game

import com.bharatbattlearena.game.ai.BotBrain
import com.bharatbattlearena.game.combat.Bullet
import com.bharatbattlearena.game.combat.Fx
import com.bharatbattlearena.game.combat.Grenade
import com.bharatbattlearena.game.combat.Mine
import com.bharatbattlearena.game.combat.Pickup
import com.bharatbattlearena.game.combat.Prop
import com.bharatbattlearena.game.combat.Smoke
import com.bharatbattlearena.game.map.MapDef
import com.bharatbattlearena.game.map.Pt
import com.bharatbattlearena.game.player.Fighter
import com.bharatbattlearena.game.weapon.WeaponState
import com.bharatbattlearena.game.weapon.Weapons
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class FeedItem(val text: String, var t: Float, val mine: Boolean)

class GameWorld(
    val map: MapDef,
    val mode: Int,          // 0 free for all, 1 team battle
    val diff: Int,
    val size: Int,
    val duration: Float,
    val playerChar: Int
) {
    companion object {
        const val GRAV = 1700f
        const val RUN = 300f
        const val JUMP_V = 700f
        const val JET_ACC = 3300f
        const val JET_MAX = 470f
        const val HAND_H = 52f
        const val BODY_H = 88f
        const val RESPAWN_TIME = 5f
    }

    val rnd = java.util.Random()
    val fighters = ArrayList<Fighter>()
    val bots = ArrayList<BotBrain>()
    val bullets = ArrayList<Bullet>()
    val grenades = ArrayList<Grenade>()
    val mines = ArrayList<Mine>()
    val smokes = ArrayList<Smoke>()
    val fx = ArrayList<Fx>()
    val pickups = ArrayList<Pickup>()
    val props = ArrayList<Prop>()
    val feed = ArrayList<FeedItem>()
    var phase = 0                 // 0 countdown, 1 playing, 2 finished
    var countdown = 3.99f
    var timeLeft = duration
    var shake = 0f
    var winnerText = ""
    val player: Fighter

    init {
        for (i in 0 until 220) bullets.add(Bullet())
        val names = arrayOf("Rana", "Arjun", "Veer", "Shera", "Bhima", "Karan", "Jai", "Dhruv", "Aman")
        for (i in 0 until size) {
            val isBot = i != 0
            val team = if (mode == 1) i % 2 else i
            val ch = if (i == 0) playerChar else rnd.nextInt(5)
            val f = Fighter(i, if (i == 0) "YOU" else names[(i - 1) % names.size], ch, team, isBot)
            fighters.add(f)
            if (isBot) bots.add(BotBrain(f, diff))
        }
        player = fighters[0]
        for (c in map.crates) props.add(Prop(0, c.x, c.y))
        for (b in map.barrels) props.add(Prop(1, b.x, b.y))
        for (s in map.pickupSpots) {
            val p = Pickup(s.x, s.y)
            rollPickup(p)
            pickups.add(p)
        }
        for (f in fighters) spawn(f)
        for (f in fighters) f.invuln = 0.5f
    }

    // ------------------------------------------------------------------ scoring helpers
    fun teamKills(t: Int): Int {
        var s = 0
        for (f in fighters) if (f.team == t) s += f.kills
        return s
    }

    fun inSmoke(x: Float, y: Float): Boolean {
        for (s in smokes) {
            val dx = x - s.x
            val dy = y - s.y
            if (dx * dx + dy * dy < s.r * s.r) return true
        }
        return false
    }

    private fun addFeed(text: String, mine: Boolean) {
        feed.add(FeedItem(text, 5f, mine))
        if (feed.size > 5) feed.removeAt(0)
    }

    // ------------------------------------------------------------------ main update
    fun update(dt: Float) {
        shake = max(0f, shake - dt * 1.8f)
        for (i in feed.indices) feed[i].t -= dt
        feed.removeAll { it.t <= 0f }
        if (phase == 0) {
            countdown -= dt
            for (f in fighters) {
                f.moveX = 0f; f.wantFire = false; f.wantJump = false; f.wantJet = false
                stepFighter(f, dt)
            }
            if (countdown <= 0f) phase = 1
            return
        }
        if (phase == 2) {
            updateEffects(dt)
            return
        }
        timeLeft -= dt
        for (b in bots) b.update(dt, this)
        for (f in fighters) stepFighter(f, dt)
        updateBullets(dt)
        updateGrenades(dt)
        updateMines(dt)
        updatePickups(dt)
        updateProps(dt)
        updateEffects(dt)
        if (timeLeft <= 0f) finish()
    }

    private fun finish() {
        phase = 2
        timeLeft = 0f
        if (mode == 1) {
            val a = teamKills(0)
            val b = teamKills(1)
            winnerText = if (a > b) "RED TEAM WINS" else if (b > a) "BLUE TEAM WINS" else "DRAW"
        } else {
            var mk = -1
            for (f in fighters) if (f.kills > mk) mk = f.kills
            val tops = fighters.filter { it.kills == mk }
            winnerText = if (tops.size > 1) "DRAW" else tops[0].name + " WINS"
        }
        for (f in fighters) { f.wantFire = false; f.moveX = 0f }
    }

    fun ranking(): List<Fighter> = fighters.sortedWith(compareByDescending<Fighter> { it.kills }.thenBy { it.deaths })

    fun playerWon(): Boolean {
        if (winnerText == "DRAW") return false
        return if (mode == 1) {
            (player.team == 0 && winnerText.startsWith("RED")) || (player.team == 1 && winnerText.startsWith("BLUE"))
        } else winnerText == "YOU WINS"
    }

    // ------------------------------------------------------------------ fighters
    private fun approach(v: Float, target: Float, d: Float): Float =
        if (v < target) min(v + d, target) else max(v - d, target)

    fun spawn(f: Fighter) {
        var best: Pt? = null
        var bestD = -1f
        for (s in map.spawns) {
            if (mode == 1) {
                val left = s.x < map.w * 0.5f
                if ((f.team == 0) != left) continue
            }
            var md = 1e9f
            for (o in fighters) {
                if (o === f || !o.alive) continue
                if (mode == 1 && o.team == f.team) continue
                val dx = o.x - s.x
                val dy = o.y - s.y
                val d = sqrt(dx * dx + dy * dy)
                if (d < md) md = d
            }
            md += rnd.nextFloat() * 120f
            if (md > bestD) { bestD = md; best = s }
        }
        val sp = best ?: map.spawns[rnd.nextInt(map.spawns.size)]
        f.x = sp.x
        f.y = sp.y - 2f
        f.vx = 0f; f.vy = 0f
        f.alive = true
        f.hp = 100f
        f.fuel = 100f
        f.invuln = 2f
        f.reloadT = 0f; f.fireCd = 0f; f.meleeT = 0f; f.hitT = 0f; f.flashed = 0f
        f.weapons[0] = WeaponState(Weapons.SMG)
        f.weapons[1] = WeaponState(Weapons.PISTOL)
        f.cur = 0
        f.grenades[0] = 2; f.grenades[1] = 1; f.grenades[2] = 1
        f.gType = 0
        f.mines = 1
        f.onGround = false
        f.lastHitAge = 99f
        f.face = if (sp.x < map.w * 0.5f) 1 else -1
        f.aim = if (f.face > 0) 0f else Math.PI.toFloat()
        fx.add(Fx(4, f.x, f.y - 40f, 0.8f, 110f))
    }

    private fun stepFighter(f: Fighter, dt: Float) {
        if (!f.alive) {
            f.dieT += dt
            f.respawnT -= dt
            if (f.respawnT <= 0f && phase == 1) spawn(f)
            return
        }
        f.animT += dt
        f.fireCd -= dt; f.hitT -= dt; f.meleeT -= dt; f.meleeCd -= dt
        f.invuln -= dt; f.flashed -= dt; f.muzzleT -= dt; f.dropT -= dt
        f.lastHitAge += dt
        if (f.reloadT > 0f) {
            f.reloadT -= dt
            if (f.reloadT <= 0f) finishReload(f)
        }
        if (f.wantSwitch) {
            f.wantSwitch = false
            val o = 1 - f.cur
            if (f.weapons[o] != null) { f.cur = o; f.reloadT = 0f; f.fireCd = 0.2f }
        }
        if (f.wantReload) { f.wantReload = false; startReload(f) }
        if (f.wantGrenade) { f.wantGrenade = false; throwGrenade(f) }
        if (f.wantMine) { f.wantMine = false; placeMine(f) }
        if (f.wantMelee) { f.wantMelee = false; melee(f) }

        // crouch (solid ground) or drop through (one-way platform)
        f.crouch = false
        if (f.wantDown && f.onGround) {
            if (f.platSolid) f.crouch = true
            else if (f.dropT <= 0f) { f.dropT = 0.3f; f.onGround = false; f.y += 4f }
        }
        val tgt = f.moveX * (if (f.crouch) 140f else RUN)
        val acc = if (f.onGround) 2600f else 1500f
        f.vx = approach(f.vx, tgt, acc * dt)
        if (f.moveX > 0.2f) f.face = 1 else if (f.moveX < -0.2f) f.face = -1
        if (f.aimActive) f.face = if (cos(f.aim) >= 0f) 1 else -1

        if (f.wantJump) {
            f.wantJump = false
            if (f.onGround) {
                f.vy = -JUMP_V
                f.onGround = false
                dust(f.x, f.y)
            }
        }
        f.jetting = false
        if (f.wantJet && f.fuel > 0f) {
            f.vy -= JET_ACC * dt
            if (f.vy < -JET_MAX) f.vy = -JET_MAX
            f.fuel -= 38f * dt
            f.jetting = true
            f.onGround = false
        } else if (f.onGround) {
            f.fuel = min(100f, f.fuel + 40f * dt)
        } else {
            f.fuel = min(100f, f.fuel + 6f * dt)
        }
        f.vy += GRAV * dt
        if (f.vy > 1100f) f.vy = 1100f

        val prevY = f.y
        f.x += f.vx * dt
        f.y += f.vy * dt
        if (f.x < 40f) { f.x = 40f; f.vx = 0f }
        if (f.x > map.w - 40f) { f.x = map.w - 40f; f.vx = 0f }
        if (f.y < 95f) { f.y = 95f; if (f.vy < 0f) f.vy = 0f }

        val was = f.onGround
        f.onGround = false
        if (f.vy >= 0f) {
            var by = 1e9f
            var solid = true
            for (p in map.platforms) {
                if (f.dropT > 0f && !p.solid) continue
                if (f.x >= p.x0 - 6f && f.x <= p.x1 + 6f && prevY <= p.y + 2f && f.y >= p.y && p.y < by) {
                    by = p.y
                    solid = p.solid
                }
            }
            if (by < 1e8f) {
                if (f.vy > 450f && !was) dust(f.x, by)
                f.y = by
                f.vy = 0f
                f.onGround = true
                f.platSolid = solid
            }
        }
        if (f.y > map.h + 40f) {
            kill(f, if (f.lastHitAge < 4f) f.lastHit else -1, true)
            return
        }
        if (f.wantFire) tryFire(f)
    }

    private fun dust(x: Float, y: Float) {
        for (i in 0 until 3) {
            val d = Fx(3, x + (rnd.nextFloat() - 0.5f) * 30f, y - 4f, 0.45f, 38f)
            d.vx = (rnd.nextFloat() - 0.5f) * 120f
            d.vy = -20f - rnd.nextFloat() * 30f
            fx.add(d)
        }
    }

    // ------------------------------------------------------------------ weapons
    private fun startReload(f: Fighter) {
        val w = f.weapon ?: return
        if (f.reloadT > 0f || w.ammo >= w.def.mag || w.reserve <= 0) return
        f.reloadT = w.def.reload
    }

    private fun finishReload(f: Fighter) {
        val w = f.weapon ?: return
        val take = min(w.def.mag - w.ammo, w.reserve)
        w.ammo += take
        w.reserve -= take
    }

    private fun tryFire(f: Fighter) {
        val w = f.weapon ?: return
        if (f.reloadT > 0f || f.fireCd > 0f || f.meleeT > 0f) return
        if (w.ammo <= 0) { startReload(f); return }
        val d = w.def
        w.ammo--
        f.fireCd = d.delay
        f.muzzleT = 0.07f
        val hx = f.x + f.face * 5f
        val hy = f.y - HAND_H
        val mx = hx + cos(f.aim) * d.length * 0.8f
        val my = hy + sin(f.aim) * d.length * 0.8f
        for (i in 0 until d.pellets) {
            val a = f.aim + (rnd.nextFloat() - 0.5f) * 2f * d.spread
            for (b in bullets) {
                if (b.active) continue
                b.active = true
                b.x = mx; b.y = my
                b.vx = cos(a) * d.speed; b.vy = sin(a) * d.speed
                b.life = d.life; b.dmg = d.damage
                b.owner = f.id; b.team = f.team; b.rocket = d.rocket
                break
            }
        }
        f.vx -= cos(f.aim) * d.recoil
        if (!f.onGround) f.vy -= sin(f.aim) * d.recoil * 0.5f
        if (f.id == 0) shake = max(shake, if (d.recoil > 100f) 0.35f else 0.06f)
        if (w.ammo == 0) startReload(f)
    }

    private fun melee(f: Fighter) {
        if (f.meleeCd > 0f) return
        f.meleeT = 0.35f
        f.meleeCd = 0.6f
        f.reloadT = 0f
        for (e in fighters) {
            if (e === f || !e.alive || e.invuln > 0f) continue
            if (mode == 1 && e.team == f.team) continue
            val rel = (e.x - f.x) * f.face
            if (abs(e.y - f.y) < 60f && rel > -10f && rel < 78f) {
                damage(e, 28f, f.id)
                e.vx += f.face * 380f
                e.vy -= 140f
                e.onGround = false
                fx.add(Fx(1, e.x, e.y - 55f, 0.25f, 60f))
            }
        }
        for (p in props) {
            if (!p.alive) continue
            val rel = (p.x - f.x) * f.face
            if (abs(p.y - f.y) < 60f && rel > -10f && rel < 78f) damageProp(p, 28f, f.id, f.team)
        }
    }

    private fun throwGrenade(f: Fighter) {
        if (f.grenades[f.gType] <= 0) {
            var found = -1
            for (i in 0..2) if (f.grenades[i] > 0) { found = i; break }
            if (found < 0) return
            f.gType = found
        }
        f.grenades[f.gType]--
        val sp = 720f
        val g = Grenade(
            f.gType, f.x + f.face * 10f, f.y - HAND_H,
            cos(f.aim) * sp + f.vx * 0.5f, sin(f.aim) * sp - 120f + f.vy * 0.3f, 2.3f, f.id, f.team
        )
        grenades.add(g)
    }

    private fun placeMine(f: Fighter) {
        if (f.mines <= 0 || !f.onGround) return
        f.mines--
        mines.add(Mine(f.x, f.y, f.id, f.team))
    }

    // ------------------------------------------------------------------ damage
    fun damage(f: Fighter, amt: Float, by: Int) {
        if (!f.alive || f.invuln > 0f) return
        f.hp -= amt
        f.hitT = 0.25f
        f.lastHit = by
        f.lastHitAge = 0f
        if (f.id == 0) shake = max(shake, 0.25f)
        if (f.hp <= 0f) kill(f, by, false)
    }

    private fun kill(f: Fighter, by: Int, fell: Boolean) {
        if (!f.alive) return
        f.alive = false
        f.hp = 0f
        f.deaths++
        f.dieT = 0f
        f.respawnT = RESPAWN_TIME
        f.reloadT = 0f
        f.jetting = false
        f.wantFire = false
        var killer: Fighter? = null
        if (by >= 0 && by < fighters.size) killer = fighters[by]
        if (killer != null && killer.id != f.id && !(mode == 1 && killer.team == f.team)) killer.kills++
        val txt = if (killer != null && killer.id != f.id) killer.name + "  >  " + f.name
        else if (fell) f.name + " fell into the river" else f.name + " blew up"
        addFeed(txt, killer != null && killer.id == 0 || f.id == 0)
        val w = f.weapons[f.cur]
        if (w != null && w.def.id != 0 && !fell) {
            val p = Pickup(f.x, f.y)
            p.type = 4; p.wId = w.def.id; p.dropped = true; p.life = 20f
            pickups.add(p)
        }
        if (f.id == 0) shake = max(shake, 0.5f)
    }

    fun explode(x: Float, y: Float, r: Float, dmg: Float, owner: Int, team: Int) {
        fx.add(Fx(0, x, y, 0.6f, r * 2.3f))
        val pl = player
        val pdx = pl.x - x
        val pdy = pl.y - y
        val pd = sqrt(pdx * pdx + pdy * pdy)
        if (pd < 900f) shake = max(shake, 0.7f * (1f - pd / 900f) + 0.1f)
        for (f in fighters) {
            if (!f.alive) continue
            val dx = f.x - x
            val dy = (f.y - 45f) - y
            val d = sqrt(dx * dx + dy * dy)
            if (d < r + 20f) {
                if (mode == 1 && f.team == team && f.id != owner) continue
                var a = dmg * (1f - 0.75f * min(1f, d / r))
                if (f.id == owner) a *= 0.5f
                val k = max(0f, 1f - d / r)
                val inv = 1f / max(d, 1f)
                f.vx += dx * inv * 520f * k
                f.vy += dy * inv * 560f * k - 120f * k
                f.onGround = false
                damage(f, a, owner)
            }
        }
        for (p in props) {
            if (!p.alive) continue
            val dx = p.x - x
            val dy = (p.y - 30f) - y
            if (sqrt(dx * dx + dy * dy) < r) damageProp(p, dmg, owner, team)
        }
        for (m in mines) {
            if (!m.alive) continue
            val dx = m.x - x
            val dy = m.y - y
            if (sqrt(dx * dx + dy * dy) < r * 0.8f) {
                m.alive = false
                explode(m.x, m.y - 10f, 150f, 100f, m.owner, m.team)
            }
        }
    }

    private fun damageProp(p: Prop, amt: Float, owner: Int, team: Int) {
        if (!p.alive) return
        p.hp -= amt
        p.lastOwner = owner
        p.lastTeam = team
        if (p.hp <= 0f) {
            p.alive = false
            p.respawn = 25f
            if (p.kind == 1) {
                explode(p.x, p.y - 30f, 150f, 70f, owner, team)
            } else {
                for (i in 0 until 5) {
                    val d = Fx(5, p.x, p.y - 28f, 0.6f, 36f)
                    d.vx = (rnd.nextFloat() - 0.5f) * 300f
                    d.vy = -rnd.nextFloat() * 240f
                    fx.add(d)
                }
                if (rnd.nextFloat() < 0.4f) {
                    val pk = Pickup(p.x, p.y)
                    rollPickup(pk)
                    pk.dropped = true
                    pk.life = 20f
                    pickups.add(pk)
                }
            }
        }
    }

    // ------------------------------------------------------------------ projectiles
    private fun updateBullets(dt: Float) {
        for (b in bullets) {
            if (!b.active) continue
            val dist = sqrt(b.vx * b.vx + b.vy * b.vy) * dt
            val steps = max(1, (dist / 16f).toInt() + 1)
            val sx = b.vx * dt / steps
            val sy = b.vy * dt / steps
            var done = false
            var s = 0
            while (!done && s < steps) {
                s++
                val px = b.x
                val py = b.y
                b.x += sx
                b.y += sy
                if (sy > 0f) {
                    val t = map.platformHitT(px, py, b.x, b.y)
                    if (t >= 0f) {
                        b.x = px + (b.x - px) * t
                        b.y = py + (b.y - py) * t
                        hitSomething(b, null)
                        done = true
                        continue
                    }
                }
                if (b.x < -50f || b.x > map.w + 50f || b.y > map.h + 100f || b.y < -400f) {
                    b.active = false
                    done = true
                    continue
                }
                for (f in fighters) {
                    if (!f.alive || f.id == b.owner || f.invuln > 0f) continue
                    if (mode == 1 && f.team == b.team) continue
                    if (abs(b.x - f.x) < 19f && b.y > f.y - BODY_H && b.y < f.y) {
                        hitSomething(b, f)
                        done = true
                        break
                    }
                }
                if (done) continue
                for (p in props) {
                    if (!p.alive) continue
                    if (abs(b.x - p.x) < p.halfW && b.y > p.y - p.height && b.y < p.y) {
                        if (!b.rocket) damageProp(p, b.dmg, b.owner, b.team)
                        hitSomething(b, null)
                        done = true
                        break
                    }
                }
            }
            if (b.active) {
                b.life -= dt
                if (b.life <= 0f) {
                    if (b.rocket) explode(b.x, b.y, 150f, b.dmg, b.owner, b.team)
                    b.active = false
                }
            }
        }
    }

    private fun hitSomething(b: Bullet, f: Fighter?) {
        if (b.rocket) {
            explode(b.x, b.y, 150f, b.dmg, b.owner, b.team)
        } else {
            if (f != null) {
                damage(f, b.dmg, b.owner)
                f.vx += b.vx * 0.02f
            }
            fx.add(Fx(1, b.x, b.y, 0.2f, 46f))
        }
        b.active = false
    }

    private fun updateGrenades(dt: Float) {
        for (g in grenades) {
            if (!g.alive) continue
            g.vy += 1500f * dt
            val px = g.x
            val py = g.y
            g.x += g.vx * dt
            g.y += g.vy * dt
            g.rot += g.vx * dt * 0.02f
            if (g.vy > 0f) {
                val t = map.platformHitT(px, py, g.x, g.y)
                if (t >= 0f) {
                    g.y = py + (g.y - py) * t
                    g.vy = -g.vy * 0.4f
                    g.vx *= 0.7f
                    if (abs(g.vy) < 70f) { g.vy = 0f; g.vx *= 0.9f }
                }
            }
            if (g.x < 20f) { g.x = 20f; g.vx = -g.vx * 0.6f }
            if (g.x > map.w - 20f) { g.x = map.w - 20f; g.vx = -g.vx * 0.6f }
            g.fuse -= dt
            if (g.y > map.h + 120f) g.alive = false
            if (g.fuse <= 0f && g.alive) {
                g.alive = false
                detonate(g)
            }
        }
        grenades.removeAll { !it.alive }
    }

    private fun detonate(g: Grenade) {
        when (g.type) {
            0 -> explode(g.x, g.y - 8f, 170f, 90f, g.owner, g.team)
            1 -> {
                fx.add(Fx(2, g.x, g.y - 10f, 0.7f, 360f))
                for (f in fighters) {
                    if (!f.alive) continue
                    val dx = f.x - g.x
                    val dy = f.y - 45f - g.y
                    val d = sqrt(dx * dx + dy * dy)
                    if (d < 420f) f.flashed = max(f.flashed, 0.6f + 2.6f * (1f - d / 420f))
                }
            }
            else -> smokes.add(Smoke(g.x, g.y - 40f))
        }
    }

    private fun updateMines(dt: Float) {
        for (m in mines) {
            if (!m.alive) continue
            m.t += dt
            if (m.arm > 0f) { m.arm -= dt; continue }
            for (f in fighters) {
                if (!f.alive || f.id == m.owner || f.invuln > 0f) continue
                if (mode == 1 && f.team == m.team) continue
                if (abs(f.x - m.x) < 40f && f.y > m.y - 90f && f.y < m.y + 16f) {
                    m.alive = false
                    explode(m.x, m.y - 10f, 150f, 100f, m.owner, m.team)
                    break
                }
            }
        }
        mines.removeAll { !it.alive }
    }

    // ------------------------------------------------------------------ pickups and props
    fun rollPickup(p: Pickup) {
        val r = rnd.nextFloat()
        p.dropped = false
        p.active = true
        if (r < 0.35f) { p.type = 4; p.wId = 2 + rnd.nextInt(5) }       // rifle..launcher
        else if (r < 0.55f) p.type = 0
        else if (r < 0.75f) p.type = 1
        else if (r < 0.90f) p.type = 2
        else p.type = 3
    }

    private fun updatePickups(dt: Float) {
        for (p in pickups) {
            p.bob += dt
            if (p.dropped) {
                p.life -= dt
                if (p.life <= 0f) { p.active = false; continue }
            } else if (!p.active) {
                p.respawn -= dt
                if (p.respawn <= 0f) rollPickup(p)
                continue
            }
            if (!p.active) continue
            for (f in fighters) {
                if (!f.alive) continue
                if (abs(f.x - p.x) < 42f && abs((f.y - 40f) - p.y + 20f) < 70f) {
                    if (collect(f, p)) {
                        p.active = false
                        p.respawn = 14f
                        break
                    }
                }
            }
        }
        pickups.removeAll { it.dropped && !it.active }
    }

    private fun collect(f: Fighter, p: Pickup): Boolean {
        when (p.type) {
            0 -> { if (f.hp >= 100f) return false; f.hp = min(100f, f.hp + 50f) }
            1 -> {
                var any = false
                for (w in f.weapons) if (w != null && w.reserve < w.def.reserve) { w.reserve = w.def.reserve; any = true }
                if (!any) return false
            }
            2 -> {
                if (f.grenades[0] >= 5 && f.grenades[1] >= 3 && f.grenades[2] >= 3) return false
                f.grenades[0] = min(5, f.grenades[0] + 2)
                f.grenades[1] = min(3, f.grenades[1] + 1)
                f.grenades[2] = min(3, f.grenades[2] + 1)
            }
            3 -> { if (f.mines >= 4) return false; f.mines = min(4, f.mines + 2) }
            else -> {
                val def = Weapons.ALL[p.wId]
                var same = -1
                for (i in 0..1) { val w = f.weapons[i]; if (w != null && w.def.id == def.id) same = i }
                if (same >= 0) {
                    val w = f.weapons[same]!!
                    if (w.reserve >= def.reserve) return false
                    w.reserve = def.reserve
                } else if (f.weapons[1] == null) {
                    f.weapons[1] = WeaponState(def)
                } else {
                    f.weapons[f.cur] = WeaponState(def)
                    f.reloadT = 0f
                }
            }
        }
        return true
    }

    private fun updateProps(dt: Float) {
        for (p in props) {
            if (!p.alive) {
                p.respawn -= dt
                if (p.respawn <= 0f) { p.alive = true; p.hp = if (p.kind == 0) 40f else 30f }
            }
        }
    }

    private fun updateEffects(dt: Float) {
        for (e in fx) {
            e.age += dt
            e.x += e.vx * dt
            e.y += e.vy * dt
        }
        fx.removeAll { it.age >= it.life }
        for (s in smokes) s.age += dt
        smokes.removeAll { it.age >= it.life }
    }
}
