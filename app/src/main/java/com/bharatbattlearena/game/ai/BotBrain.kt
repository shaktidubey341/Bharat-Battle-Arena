package com.bharatbattlearena.game.ai

import com.bharatbattlearena.game.GameWorld
import com.bharatbattlearena.game.combat.Pickup
import com.bharatbattlearena.game.player.Fighter
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Bots drive exactly the same Fighter input fields as the human player and use the same
 * weapons, health and damage rules. Only reaction time, aim error and decisions differ.
 */
class BotBrain(val f: Fighter, val diff: Int) {
    private var target: Fighter? = null
    private var seek: Pickup? = null
    private var retargetT = 0f
    private var reactT = 0f
    private var strafeT = 0f
    private var strafeDir = 1
    private var errT = 0f
    private var errA = 0f
    private var gCd = 5f
    private var mCd = 10f
    private var swCd = 0f
    private var dropCd = 0f
    private var burstT = 0f
    private var burstOn = true
    private var patrolX = -1f
    private var patrolT = 0f
    private val rnd = java.util.Random()
    private val react = floatArrayOf(0.55f, 0.28f, 0.10f)[diff]
    private val aimErr = floatArrayOf(0.22f, 0.11f, 0.045f)[diff]
    private val fireTol = floatArrayOf(0.22f, 0.13f, 0.07f)[diff]
    private val turn = floatArrayOf(6f, 10f, 18f)[diff]
    private val gCool = floatArrayOf(8f, 5.5f, 3.5f)[diff]

    private fun norm(a: Float): Float {
        var x = a
        while (x > PI) x -= (2 * PI).toFloat()
        while (x < -PI) x += (2 * PI).toFloat()
        return x
    }

    fun update(dt: Float, w: GameWorld) {
        if (!f.alive) {
            target = null
            return
        }
        f.moveX = 0f
        f.wantFire = false
        f.wantDown = false
        f.wantJet = false
        gCd -= dt; mCd -= dt; swCd -= dt; dropCd -= dt
        retargetT -= dt; strafeT -= dt; errT -= dt; patrolT -= dt; reactT -= dt
        val map = w.map

        // recover from falling into the river / below the ground
        if (!f.onGround && f.vy > 150f && f.y > map.h * 0.86f && f.fuel > 5f) f.wantJet = true

        if (f.flashed > 0.4f) {          // blinded: stumble around
            if (strafeT <= 0f) { strafeT = 0.5f; strafeDir = if (rnd.nextBoolean()) 1 else -1 }
            f.moveX = strafeDir * 0.5f
            f.aimActive = false
            return
        }

        if (retargetT <= 0f) {
            retargetT = 0.5f
            val old = target
            target = pickTarget(w)
            if (target != null && old == null) reactT = react
            seek = pickPickup(w)
        }
        val t = target
        val sp = seek
        if (t == null) {
            f.aimActive = false
            if (sp != null) {
                goTo(sp.x, sp.y, w)
            } else {
                if (patrolX < 0f || patrolT <= 0f) {
                    patrolX = 100f + rnd.nextFloat() * (map.w - 200f)
                    patrolT = 4f + rnd.nextFloat() * 3f
                }
                goTo(patrolX, f.y, w)
            }
            val cw = f.weapon
            if (cw != null && cw.ammo < cw.def.mag / 2 && cw.reserve > 0) f.wantReload = true
            return
        }

        val hx = f.x
        val hy = f.y - 50f
        val tx = t.x
        val ty = t.y - 45f
        val dx = tx - hx
        val dy = ty - hy
        val dist = sqrt(dx * dx + dy * dy)
        val wp = f.weapon

        // weapon selection
        if (wp != null && swCd <= 0f && diff >= 1) {
            swCd = 1.5f
            val o = 1 - f.cur
            val ow = f.weapons[o]
            if (ow != null && (ow.ammo > 0 || ow.reserve > 0)) {
                val sc = abs(dist - wp.def.prefRange) + (if (wp.ammo == 0 && wp.reserve == 0) 9999f else 0f)
                val so = abs(dist - ow.def.prefRange)
                if (so + 150f < sc) f.wantSwitch = true
            }
        }
        if (wp != null && wp.ammo == 0) {
            if (wp.reserve > 0) f.wantReload = true else if (f.weapons[1 - f.cur] != null) f.wantSwitch = true
        }

        // movement
        val pref = wp?.def?.prefRange ?: 250f
        if (sp != null && f.hp < 45f) {
            goTo(sp.x, sp.y, w)
        } else if (dist > pref + 60f) {
            f.moveX = if (dx > 0) 1f else -1f
        } else if (dist < pref - 80f) {
            f.moveX = if (dx > 0) -1f else 1f
        } else {
            if (strafeT <= 0f) { strafeT = 0.6f + rnd.nextFloat(); strafeDir = if (rnd.nextBoolean()) 1 else -1 }
            f.moveX = strafeDir * 0.6f
        }
        if (dy < -80f && abs(dx) < 500f) {
            if (f.onGround && rnd.nextFloat() < 0.5f) f.wantJump = true
            f.wantJet = true
        } else if (dy > 90f && f.onGround && !f.platSolid && dropCd <= 0f) {
            dropCd = 1.5f
            f.wantDown = true
        }

        // aim
        var lead = 0f
        if (wp != null) lead = (dist / wp.def.speed)
        val ax = tx + t.vx * lead - hx
        val ay = ty + t.vy * lead * 0.5f - hy
        val ang = atan2(ay, ax)
        if (errT <= 0f) { errT = 0.35f; errA = (rnd.nextFloat() - 0.5f) * 2f * aimErr * (1f + dist / 600f) }
        val want = ang + errA
        var d = norm(want - f.aim)
        val k = (turn * dt).coerceAtMost(1f)
        f.aim = norm(f.aim + d * k)
        f.aimActive = true

        // fire
        val blocked = w.inSmoke(f.x, f.y - 50f) || w.inSmoke(t.x, t.y - 50f)
        val maxR = if (wp != null) wp.def.range * 0.9f else 400f
        burstT -= dt
        if (burstT <= 0f) {
            burstOn = if (diff == 0) !burstOn else if (diff == 1) (rnd.nextFloat() < 0.8f) else true
            burstT = if (burstOn) 0.7f else 0.45f
        }
        d = norm(ang - f.aim)
        if (reactT <= 0f && !blocked && dist < maxR && burstOn && abs(d) < fireTol + 0.04f) f.wantFire = true

        // grenades
        if (gCd <= 0f && !blocked && dist in 190f..480f && rnd.nextFloat() < 0.02f) {
            gCd = gCool
            var type = 0
            if (rnd.nextFloat() < 0.2f && f.grenades[1] > 0) type = 1
            else if (rnd.nextFloat() < 0.15f && f.grenades[2] > 0) type = 2
            if (f.grenades[type] <= 0) type = 0
            if (f.grenades[type] > 0) {
                f.gType = type
                f.aim = (ang - 0.30f - 0.25f * (dist / 480f))
                f.wantGrenade = true
            }
        }
        // mines
        if (mCd <= 0f && f.onGround && f.mines > 0 && dist > 260f && rnd.nextFloat() < 0.01f) {
            mCd = 14f
            f.wantMine = true
        }
        // melee
        if (dist < 70f && abs(dy) < 50f) f.wantMelee = true
    }

    private fun goTo(px: Float, py: Float, w: GameWorld) {
        val dx = px - f.x
        if (abs(dx) > 25f) f.moveX = if (dx > 0) 1f else -1f
        if (py < f.y - 70f) {
            if (f.onGround) f.wantJump = true
            f.wantJet = true
        } else if (py > f.y + 80f && f.onGround && !f.platSolid && dropCd <= 0f) {
            dropCd = 1.5f
            f.wantDown = true
        }
    }

    private fun pickTarget(w: GameWorld): Fighter? {
        var best: Fighter? = null
        var bd = 1e9f
        for (o in w.fighters) {
            if (o === f || !o.alive) continue
            if (w.mode == 1 && o.team == f.team) continue
            val dx = o.x - f.x
            val dy = o.y - f.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < bd) { bd = d; best = o }
        }
        if (best != null && (w.inSmoke(f.x, f.y - 50f) || w.inSmoke(best.x, best.y - 50f)) && diff < 2) return null
        return best
    }

    private fun pickPickup(w: GameWorld): Pickup? {
        val wp = f.weapon
        val lowAmmo = wp == null || (wp.ammo + wp.reserve) < wp.def.mag / 2
        val weakGun = wp != null && (wp.def.id == 0 || wp.def.id == 1)
        var best: Pickup? = null
        var bd = 1e9f
        for (p in w.pickups) {
            if (!p.active) continue
            val ok = (p.type == 0 && f.hp < 70f) || ((p.type == 1) && lowAmmo) ||
                (p.type == 4 && (lowAmmo || (weakGun && diff >= 1 && p.wId >= 2)))
            if (!ok) continue
            val dx = p.x - f.x
            val dy = p.y - f.y
            val d = sqrt(dx * dx + dy * dy)
            if (d < 750f && d < bd) { bd = d; best = p }
        }
        return best
    }
}
