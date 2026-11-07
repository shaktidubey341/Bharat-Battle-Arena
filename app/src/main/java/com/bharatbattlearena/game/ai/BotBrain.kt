package com.bharatbattlearena.game.ai

import com.bharatbattlearena.game.GameEngine
import com.bharatbattlearena.game.Phase
import com.bharatbattlearena.game.collision.Collision
import com.bharatbattlearena.game.player.Fighter
import com.bharatbattlearena.game.player.PowerType
import com.bharatbattlearena.game.util.clampF
import com.bharatbattlearena.game.weapon.PickupState
import com.bharatbattlearena.game.weapon.WeaponType
import java.util.Random
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * Finite-state bot. It "thinks" a few times per second (reaction time scales with
 * difficulty), then each frame turns the current state into an InputState - exactly
 * the same input path a human uses.
 *
 * Stage 1 limits: no real pathfinding. Bots climb by jumping when blocked or when
 * the goal is above them. Grenade/mine states arrive with those systems in Stage 2.
 */
class BotBrain(private val f: Fighter, private val diff: Difficulty, private val rnd: Random) {

    enum class State {
        IDLE, PATROL, SEARCH_WEAPON, CHASE, AIM, SHOOT, RELOAD, MELEE,
        USE_POWER, TAKE_COVER, RETREAT, JUMP, RESPAWN
    }

    var state = State.PATROL
        private set

    private var thinkT = rnd.nextFloat() * 0.3f
    private var enemy: Fighter? = null
    private var lastKnownX = 0f
    private var lastKnownY = 0f
    private var lastKnownT = 0f
    private var sightT = 0f
    private var goalX = f.x
    private var jumpCd = 0f
    private var stuckT = 0f
    private var lastX = f.x
    private var strafeDir = 1f
    private var strafeT = 0f
    private var aimNoise = 0f
    private var burstT = 0f
    private var burstOn = true
    private var searchT = 0f
    private var noPickupT = 0f

    fun update(e: GameEngine, dt: Float) {
        val inp = f.input
        if (!f.alive) {
            state = State.RESPAWN
            inp.reset()
            sightT = 0f
            enemy = null
            return
        }
        if (e.phase != Phase.PLAYING) {
            inp.reset()
            return
        }
        if (state == State.RESPAWN) state = State.PATROL
        thinkT -= dt
        jumpCd -= dt
        lastKnownT -= dt
        strafeT -= dt
        burstT -= dt
        noPickupT -= dt
        if (thinkT <= 0f) {
            thinkT = diff.reaction * (0.7f + rnd.nextFloat() * 0.6f)
            think(e)
        }
        act(e, dt)
    }

    private fun think(e: GameEngine) {
        val prev = enemy
        var best: Fighter? = null
        var bd = 1400f
        val cx = f.x
        val cy = f.y - f.height * 0.6f
        for (i in 0 until e.fighters.size) {
            val o = e.fighters[i]
            if (o === f || !o.alive) continue
            val d = hypot(o.x - f.x, o.y - f.y)
            if (d < bd && Collision.lineClear(e.map, cx, cy, o.x, o.y - o.height * 0.6f)) {
                bd = d
                best = o
            }
        }
        enemy = best
        aimNoise = (rnd.nextFloat() - 0.5f) * 2f * diff.aimError

        // Stuck detection
        if (abs(f.x - lastX) < 8f && abs(f.input.moveX) > 0.3f) stuckT += diff.reaction else stuckT = 0f
        lastX = f.x

        if (best == null) {
            sightT = 0f
        } else {
            if (best !== prev) sightT = 0f
            lastKnownX = best.x
            lastKnownY = best.y
            lastKnownT = 4f
        }

        val hasGoodWeapon = f.weapons.size > 1
        val wantPickup = noPickupT <= 0f && !hasGoodWeapon && nearestPickup(e) != null

        if (best != null) {
            val dist = bd
            chooseWeapon(dist)
            val cw = f.weapon
            state = when {
                f.hp < 30f && dist < 800f -> State.RETREAT
                dist < 90f -> State.MELEE
                f.powerCd <= 0f && wantsPower(dist) -> State.USE_POWER
                (cw.mag <= 0 && cw.reserve != 0) || cw.reloadT > 0f -> State.RELOAD
                wantPickup && dist > 650f -> State.SEARCH_WEAPON
                dist > cw.type.range * 0.8f -> State.CHASE
                else -> State.SHOOT
            }
        } else if (lastKnownT > 0f) {
            state = State.CHASE
        } else if (wantPickup) {
            state = State.SEARCH_WEAPON
        } else if (state != State.PATROL) {
            state = State.PATROL
        }
    }

    private fun wantsPower(dist: Float): Boolean = when (f.def.power) {
        PowerType.SOLAR_SHIELD, PowerType.TACTICAL_SHIELD -> dist < 700f && (f.hp < 70f || dist < 300f)
        PowerType.PUSH_WAVE -> dist < 240f
        PowerType.FIRE_BURST -> dist < 210f
        PowerType.KNOWLEDGE_DEBUFF, PowerType.ICE_SLOW -> dist < 350f
    }

    /** Easy bots only swap when empty; Normal/Hard pick by rough DPS and range. */
    private fun chooseWeapon(dist: Float) {
        val cur = f.weapon
        if (diff == Difficulty.EASY && cur.hasAmmo) return
        var bestIdx = f.cur
        var bestScore = -1f
        for (i in 0 until f.weapons.size) {
            val w = f.weapons[i]
            if (!w.hasAmmo) continue
            val t = w.type
            var score = t.damage * t.pellets / t.interval
            if (dist > t.range) score *= 0.2f
            if (t == WeaponType.SNIPER && dist < 500f) score *= 0.3f
            if (t == WeaponType.SHOTGUN && dist > 450f) score *= 0.2f
            if (score > bestScore) {
                bestScore = score
                bestIdx = i
            }
        }
        if (bestIdx != f.cur && bestIdx < f.weapons.size) {
            f.weapon.reloadT = 0f
            f.cur = bestIdx
        }
    }

    private fun nearestPickup(e: GameEngine): PickupState? {
        var best: PickupState? = null
        var bd = 1800f
        for (i in 0 until e.pickups.size) {
            val p = e.pickups[i]
            val t = p.type ?: continue
            var owned = false
            for (j in 0 until f.weapons.size) if (f.weapons[j].type == t) owned = true
            if (owned) continue
            if (p.y < f.y - 200f) continue // too high to reach without pathfinding
            val d = abs(p.x - f.x) + abs(p.y - f.y) * 0.5f
            if (d < bd) {
                bd = d
                best = p
            }
        }
        return best
    }

    private fun act(e: GameEngine, dt: Float) {
        val inp = f.input
        inp.shoot = false
        inp.aim = false
        inp.crouch = false
        inp.moveX = 0f
        val en = enemy

        when (state) {
            State.PATROL -> patrol(e)
            State.SEARCH_WEAPON -> {
                searchT += dt
                val p = nearestPickup(e)
                if (p == null || searchT > 8f) {
                    if (searchT > 8f) noPickupT = 12f
                    searchT = 0f
                    state = State.PATROL
                } else {
                    moveTo(p.x, p.y, 1f)
                }
            }
            State.CHASE -> {
                if (en != null) {
                    moveTo(en.x, en.y, 1f)
                    aimAndFire(en, dt, true)
                } else {
                    moveTo(lastKnownX, lastKnownY, 1f)
                    if (abs(f.x - lastKnownX) < 80f) lastKnownT = 0f
                }
            }
            State.AIM, State.SHOOT -> {
                if (en != null) combatMove(en, dt) else state = State.PATROL
            }
            State.RELOAD -> {
                if (f.weapon.reloadT <= 0f) inp.reloadPressed = true
                if (en != null) {
                    face(en)
                    val dist = abs(en.x - f.x)
                    if (dist < 500f) inp.moveX = -f.facing * 0.8f else inp.crouch = true
                    if (dist < 300f) inp.moveX = -f.facing * 1f
                }
            }
            State.MELEE -> {
                if (en != null) {
                    face(en)
                    inp.meleePressed = true
                }
            }
            State.USE_POWER -> {
                inp.powerPressed = true
                state = if (en != null) State.SHOOT else State.PATROL
            }
            State.RETREAT -> {
                if (en != null) {
                    face(en)
                    inp.moveX = -f.facing * 1f
                    if (f.onGround && jumpCd <= 0f && rnd.nextFloat() < 0.04f) {
                        inp.jumpPressed = true
                        jumpCd = 0.8f
                    }
                    if (abs(en.x - f.x) < 500f) aimAndFire(en, dt, false)
                } else {
                    state = State.PATROL
                }
            }
            State.TAKE_COVER -> inp.crouch = true
            State.IDLE, State.JUMP, State.RESPAWN -> {}
        }
    }

    private fun face(en: Fighter) {
        f.facing = if (en.x >= f.x) 1 else -1
    }

    private fun aimAndFire(en: Fighter, dt: Float, whileMoving: Boolean) {
        val dx = en.x - f.x
        val dy = (en.y - en.height * 0.55f) - (f.y - f.height * 0.58f)
        val ang = atan2(dy, abs(dx))
        f.aimAngle = clampF(ang + aimNoise, -1f, 1f)
        sightT += dt
        val dist = hypot(dx, dy)
        val facingEnemy = (dx >= 0f && f.facing == 1) || (dx < 0f && f.facing == -1)
        if (facingEnemy && sightT >= diff.firstShotDelay && abs(ang) < 0.9f && dist < f.weapon.type.range) {
            if (diff == Difficulty.EASY) {
                if (burstT <= 0f) {
                    burstOn = !burstOn
                    burstT = if (burstOn) 0.6f else 0.9f
                }
                f.input.shoot = burstOn
            } else {
                f.input.shoot = true
            }
        }
        if (whileMoving) f.input.aim = false
    }

    private fun combatMove(en: Fighter, dt: Float) {
        face(en)
        aimAndFire(en, dt, false)
        val dist = hypot(en.x - f.x, en.y - f.y)
        val range = f.weapon.type.range
        val ideal = range * 0.45f
        var mv = 0f
        if (dist > ideal * 1.4f) {
            mv = f.facing * 0.8f
        } else if (dist < ideal * 0.6f) {
            mv = -f.facing * 0.8f
        } else if (diff != Difficulty.EASY) {
            if (strafeT <= 0f) {
                strafeDir = if (rnd.nextBoolean()) 1f else -1f
                strafeT = 0.8f + rnd.nextFloat() * 1.2f
            }
            mv = strafeDir * 0.5f
        }
        f.input.moveX = mv
        f.input.aim = diff == Difficulty.HARD && dist > 500f
        maybeJump(en.y, en.x - f.x)
    }

    private fun patrol(e: GameEngine) {
        if (abs(f.x - goalX) < 60f || stuckT > 1.5f) {
            goalX = 150f + rnd.nextFloat() * (e.map.width - 300f)
            stuckT = 0f
        }
        moveTo(goalX, f.y, 0.6f)
    }

    private fun moveTo(tx: Float, ty: Float, speed: Float) {
        val dx = tx - f.x
        if (abs(dx) > 25f) {
            f.input.moveX = if (dx > 0f) speed else -speed
            f.facing = if (dx > 0f) 1 else -1
        }
        maybeJump(ty, dx)
    }

    private fun maybeJump(ty: Float, dx: Float) {
        val inp = f.input
        if (jumpCd > 0f || !f.onGround) return
        val blocked = abs(f.vx) < 15f && abs(inp.moveX) > 0.3f && f.animT > 0.5f
        val above = ty < f.y - 100f && abs(dx) < 320f
        if (blocked || above) {
            inp.jumpPressed = true
            jumpCd = 0.6f + rnd.nextFloat() * 0.4f
            state = if (state == State.PATROL || state == State.CHASE || state == State.SEARCH_WEAPON) state else State.JUMP
        } else if (ty > f.y + 120f && f.onOneWay && abs(dx) < 200f) {
            inp.crouch = true
            inp.jumpPressed = true
            jumpCd = 0.8f
        }
    }
}
