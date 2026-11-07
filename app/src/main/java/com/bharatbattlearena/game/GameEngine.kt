package com.bharatbattlearena.game

import com.bharatbattlearena.game.ai.BotBrain
import com.bharatbattlearena.game.ai.Difficulty
import com.bharatbattlearena.game.audio.Sfx
import com.bharatbattlearena.game.audio.SoundHub
import com.bharatbattlearena.game.camera.Camera2D
import com.bharatbattlearena.game.collision.Collision
import com.bharatbattlearena.game.combat.CombatSystem
import com.bharatbattlearena.game.effects.EffectsSystem
import com.bharatbattlearena.game.map.GameMap
import com.bharatbattlearena.game.map.MapFactory
import com.bharatbattlearena.game.map.SpawnPoint
import com.bharatbattlearena.game.player.Characters
import com.bharatbattlearena.game.player.Fighter
import com.bharatbattlearena.game.player.FighterController
import com.bharatbattlearena.game.weapon.BulletPool
import com.bharatbattlearena.game.weapon.PickupState
import com.bharatbattlearena.game.weapon.WeaponState
import com.bharatbattlearena.game.weapon.WeaponType
import java.util.Random
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ceil

enum class Phase { COUNTDOWN, PLAYING, ENDED }

class GameConfig(
    val characterId: Int,
    val mapId: String,
    val durationSec: Int,
    val difficulty: Difficulty,
    val botCount: Int
)

class ResultRow(
    val name: String,
    val kills: Int,
    val deaths: Int,
    val score: Int,
    val isPlayer: Boolean,
    val characterName: String
)

class KillMsg(val text: String, var ttl: Float, val involvesPlayer: Boolean)

/**
 * Owns all match state and runs the simulation one fixed step at a time.
 * Flow: COUNTDOWN (3s) -> PLAYING (timer) -> ENDED (results).
 */
class GameEngine(val config: GameConfig, quality: Int) {

    companion object {
        private val BOT_NAMES = arrayOf("Arjun", "Bhima", "Chetan", "Deepak", "Esha", "Farhan", "Gauri", "Harsh", "Indu")
        private const val WEAPON_RESPAWN = 20f
    }

    val map: GameMap = MapFactory.build(config.mapId)
    val rnd = Random()
    val fighters = ArrayList<Fighter>()
    private val brains = ArrayList<BotBrain>()
    val player: Fighter
    val bullets = BulletPool(384)
    val effects = EffectsSystem(quality)
    val camera = Camera2D(map)
    val combat = CombatSystem(this)
    val pickups = ArrayList<PickupState>()
    val killFeed = ArrayList<KillMsg>()
    val board = ArrayList<Fighter>()

    var phase = Phase.COUNTDOWN
        private set
    var countdown = 3.99f
        private set
    var timeLeft = config.durationSec.toFloat()
        private set
    var time = 0f
        private set
    var results: List<ResultRow> = emptyList()
        private set
    var winnerText = ""
        private set
    var bannerText = ""
        private set
    var mvpName = ""
        private set

    private var boardT = 0f
    private var lastCountdownInt = 4
    private val boardCmp = Comparator<Fighter> { a, b ->
        if (b.kills != a.kills) b.kills - a.kills else a.deaths - b.deaths
    }

    init {
        player = Fighter(0, "You", Characters.byId(config.characterId), false)
        fighters.add(player)
        val count = config.botCount.coerceIn(1, 9)
        for (i in 0 until count) {
            val def = Characters.all[rnd.nextInt(Characters.all.size)]
            val bot = Fighter(i + 1, "Bot " + BOT_NAMES[i % BOT_NAMES.size], def, true)
            bot.extraSpread = config.difficulty.extraSpread
            fighters.add(bot)
            brains.add(BotBrain(bot, config.difficulty, rnd))
        }
        for (s in map.pickups) pickups.add(PickupState(s.x, s.y, randomWeapon()))
        for (f in fighters) respawn(f, true)
        refreshBoard()
    }

    fun addKill(m: KillMsg) {
        killFeed.add(m)
        if (killFeed.size > 5) killFeed.removeAt(0)
    }

    // ------------------------------------------------------------------ update

    fun update(dt: Float, sw: Int, sh: Int) {
        time += dt
        when (phase) {
            Phase.COUNTDOWN -> {
                countdown -= dt
                val ci = ceil(countdown).toInt()
                if (ci != lastCountdownInt && ci > 0) {
                    lastCountdownInt = ci
                    SoundHub.play(Sfx.COUNTDOWN)
                }
                if (countdown <= 0f) phase = Phase.PLAYING
            }
            Phase.PLAYING -> {
                timeLeft -= dt
                if (timeLeft <= 0f) {
                    timeLeft = 0f
                    endMatch()
                }
            }
            Phase.ENDED -> {}
        }

        if (phase != Phase.ENDED) {
            for (i in 0 until brains.size) brains[i].update(this, dt)
            for (i in 0 until fighters.size) {
                val f = fighters[i]
                if (!f.isBot && f.alive) updateHumanAim(f)
                FighterController.update(this, f, dt)
            }
            updateBullets(dt)
            updatePickups(dt)
        }

        effects.update(dt)
        var i = 0
        while (i < killFeed.size) {
            val m = killFeed[i]
            m.ttl -= dt
            if (m.ttl <= 0f) killFeed.removeAt(i) else i++
        }
        boardT -= dt
        if (boardT <= 0f) {
            boardT = 0.5f
            refreshBoard()
        }
        camera.update(dt, player.x, player.y - player.height * 0.5f, player.facing, sw, sh)
    }

    private fun refreshBoard() {
        board.clear()
        board.addAll(fighters)
        board.sortWith(boardCmp)
    }

    /**
     * Touch aim assist: aim at the nearest enemy in front within a cone; otherwise
     * aim straight, or up/down with the stick while the Aim button is held.
     */
    private fun updateHumanAim(f: Fighter) {
        val range = f.weapon.type.range
        var bestAng = 0f
        var bestD = Float.MAX_VALUE
        var found = false
        val my = f.y - f.height * 0.58f
        for (i in 0 until fighters.size) {
            val o = fighters[i]
            if (o === f || !o.alive) continue
            val dx = o.x - f.x
            if (abs(dx) > range || abs(dx) < 20f) continue
            if ((dx > 0f) != (f.facing > 0)) continue
            val dy = (o.y - o.height * 0.55f) - my
            val ang = atan2(dy, abs(dx))
            if (abs(ang) > 0.7f) continue
            val d = hypot(dx, dy)
            if (d < bestD) {
                bestD = d
                bestAng = ang
                found = true
            }
        }
        f.aimAngle = if (found) bestAng else if (f.input.aim) f.input.aimY * 0.8f else 0f
    }

    private fun updateBullets(dt: Float) {
        val solids = map.solids
        for (b in bullets.items) {
            if (!b.active) continue
            val speed = hypot(b.vx, b.vy)
            val steps = (speed * dt / 10f).toInt() + 1
            val sx = b.vx * dt / steps
            val sy = b.vy * dt / steps
            var done = false
            var s = 0
            while (s < steps && !done) {
                s++
                b.x += sx
                b.y += sy
                if (b.x < 0f || b.x > map.width || b.y < 0f || b.y > map.height) {
                    b.active = false
                    done = true
                    break
                }
                for (sol in solids) {
                    if (!sol.oneWay && sol.rect.contains(b.x, b.y)) {
                        done = true
                        break
                    }
                }
                if (done) {
                    detonate(b.type, b.x, b.y, b.damage, b.owner)
                    if (b.type.explosiveRadius <= 0f) effects.impact(b.x, b.y, 0xFFCCCCCC.toInt())
                    b.active = false
                    break
                }
                for (i in 0 until fighters.size) {
                    val f = fighters[i]
                    if (!f.alive || f === b.owner || f.protectT > 0f) continue
                    if (b.x > f.x - f.width * 0.5f - 4f && b.x < f.x + f.width * 0.5f + 4f &&
                        b.y > f.y - f.height - 4f && b.y < f.y
                    ) {
                        if (b.type.explosiveRadius > 0f) {
                            detonate(b.type, b.x, b.y, b.damage, b.owner)
                        } else {
                            combat.bulletHit(b, f)
                        }
                        b.active = false
                        done = true
                        break
                    }
                }
            }
            if (b.active) {
                b.life -= dt
                if (b.life <= 0f) {
                    detonate(b.type, b.x, b.y, b.damage, b.owner)
                    b.active = false
                } else {
                    effects.trail(b.x, b.y, b.type.color)
                }
            }
        }
    }

    private fun detonate(type: WeaponType, x: Float, y: Float, damage: Float, owner: Fighter?) {
        if (type.explosiveRadius > 0f) combat.explode(x, y, type.explosiveRadius, damage, owner)
    }

    private fun updatePickups(dt: Float) {
        for (i in 0 until pickups.size) {
            val p = pickups[i]
            val t = p.type
            if (t == null) {
                p.timer -= dt
                if (p.timer <= 0f) p.type = randomWeapon()
                continue
            }
            for (j in 0 until fighters.size) {
                val f = fighters[j]
                if (!f.alive) continue
                if (abs(f.x - p.x) < 40f && abs(f.y - p.y) < 60f && giveWeapon(f, t)) {
                    effects.ring(p.x, p.y - 30f, 8f, 50f, 0.3f, t.color)
                    p.type = null
                    p.timer = WEAPON_RESPAWN
                    break
                }
            }
        }
    }

    /** Returns false when the fighter gains nothing (already stocked up). */
    private fun giveWeapon(f: Fighter, type: WeaponType): Boolean {
        for (w in f.weapons) {
            if (w.type == type) {
                val maxReserve = type.reserveMags * type.mag
                if (w.reserve >= maxReserve) return false
                w.reserve = min(w.reserve + type.mag * 2, maxReserve)
                return true
            }
        }
        val ws = WeaponState(type)
        if (f.weapons.size < 3) {
            f.weapons.add(ws)
            f.cur = f.weapons.size - 1
        } else {
            val idx = if (f.cur == 0) 2 else f.cur
            f.weapons[idx] = ws
            f.cur = idx
        }
        return true
    }

    /** Weighted random pick: powerful weapons are rarer. */
    private fun randomWeapon(): WeaponType {
        var total = 0
        for (t in WeaponType.values()) total += t.rarity
        var roll = rnd.nextInt(max(1, total))
        for (t in WeaponType.values()) {
            if (t.rarity <= 0) continue
            roll -= t.rarity
            if (roll < 0) return t
        }
        return WeaponType.SMG
    }

    // ----------------------------------------------------------------- respawn

    /** Picks the candidate spawn that is farthest from living fighters. */
    private fun pickSpawn(f: Fighter): SpawnPoint {
        var best = map.spawns[0]
        var bestScore = -1f
        repeat(10) {
            val sp = map.spawns[rnd.nextInt(map.spawns.size)]
            var minD = 100000f
            for (i in 0 until fighters.size) {
                val o = fighters[i]
                if (o === f || !o.alive) continue
                minD = min(minD, hypot(o.x - sp.x, o.y - sp.y))
            }
            val score = minD + rnd.nextFloat() * 100f
            if (score > bestScore) {
                bestScore = score
                best = sp
            }
        }
        return best
    }

    fun respawn(f: Fighter, initial: Boolean) {
        val sp = pickSpawn(f)
        f.x = sp.x
        f.y = sp.y
        f.vx = 0f
        f.vy = 0f
        f.hp = f.def.maxHp
        f.alive = true
        f.respawnT = 0f
        f.crouching = false
        f.clearStatus()
        f.equipDefault()
        f.protectT = 2f
        f.facing = if (sp.x < map.width * 0.5f) 1 else -1
        f.aimAngle = 0f
        effects.respawn(f.x, f.y - 40f, f.def.primary)
        if (!initial) SoundHub.play(Sfx.RESPAWN)
        if (f === player) camera.shake(3f)
    }

    // ------------------------------------------------------------------ result

    private fun endMatch() {
        phase = Phase.ENDED
        for (f in fighters) f.input.reset()
        val rows = ArrayList<ResultRow>()
        for (f in fighters) {
            rows.add(ResultRow(f.name, f.kills, f.deaths, f.kills * 100 - f.deaths * 20, !f.isBot, f.def.name))
        }
        rows.sortWith(compareByDescending<ResultRow> { it.kills }.thenBy { it.deaths })
        results = rows
        val top = rows[0]
        val draw = rows.size > 1 && rows[1].kills == top.kills
        var mvp = rows[0]
        for (r in rows) if (r.score > mvp.score) mvp = r
        mvpName = mvp.name
        if (draw) {
            winnerText = "DRAW"
            bannerText = "DRAW"
            SoundHub.play(Sfx.DEFEAT)
        } else {
            winnerText = "WINNER: " + top.name
            bannerText = if (top.isPlayer) "VICTORY" else "DEFEAT"
            SoundHub.play(if (top.isPlayer) Sfx.VICTORY else Sfx.DEFEAT)
        }
    }
}
