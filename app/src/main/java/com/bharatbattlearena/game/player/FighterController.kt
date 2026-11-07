package com.bharatbattlearena.game.player

import com.bharatbattlearena.game.GameEngine
import com.bharatbattlearena.game.Phase
import com.bharatbattlearena.game.audio.Sfx
import com.bharatbattlearena.game.audio.SoundHub
import com.bharatbattlearena.game.collision.Collision
import com.bharatbattlearena.game.combat.HitZone
import com.bharatbattlearena.game.power.PowerSystem
import com.bharatbattlearena.game.util.PI_F
import com.bharatbattlearena.game.util.approach
import com.bharatbattlearena.game.weapon.WeaponType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin

/**
 * Per-frame update for one fighter: movement, jumping, crouching, shooting,
 * reloading, melee, powers, respawn timer and animation state.
 */
object FighterController {
    private const val GRAVITY = 2300f
    private const val MAX_FALL = 1700f
    private const val RUN_SPEED = 330f
    private const val JUMP_SPEED = 1020f
    private const val ACCEL = 2600f
    private const val DECEL = 3200f
    private const val DEG = PI_F / 180f

    fun update(e: GameEngine, f: Fighter, dt: Float) {
        val map = e.map
        val canAct = e.phase == Phase.PLAYING
        f.tick(dt)

        if (!f.alive) {
            f.anim = Anim.DEATH
            f.vy = min(f.vy + GRAVITY * dt, MAX_FALL)
            f.vx = approach(f.vx, 0f, 1500f * dt)
            Collision.moveFighter(f, map, dt)
            if (canAct) {
                f.respawnT -= dt
                if (f.respawnT <= 0f) e.respawn(f, false)
            }
            return
        }

        val inp = f.input
        if (f.cur >= f.weapons.size) f.cur = 0

        // Status effects
        if (canAct && f.burnT > 0f) {
            e.combat.damage(f, 5f * dt, f.burnOwner, HitZone.BODY, true)
            if (e.rnd.nextFloat() < 0.25f) e.effects.fire(f.x, f.y - f.height * 0.6f)
            if (!f.alive) return
        }
        if (canAct && f.def.regen > 0f && f.sinceHurt > 5f && f.hp < f.def.maxHp) {
            f.hp = min(f.def.maxHp, f.hp + f.def.regen * dt)
        }

        val stunned = f.stunT > 0f
        val live = canAct && !stunned
        val mx = if (live) inp.moveX else 0f

        // Crouch
        if (live && inp.crouch) f.crouching = true
        else if (f.crouching && Collision.canStand(f, map)) f.crouching = false

        // Jump (buffered + coyote time). Crouch + jump drops through one-way platforms.
        val jumpReq = inp.jumpPressed
        inp.jumpPressed = false
        if (jumpReq && live) f.jumpBuf = 0.12f
        f.jumpBuf -= dt
        if (f.onGround) f.coyote = 0.1f else f.coyote -= dt
        if (f.jumpBuf > 0f && f.coyote > 0f && live) {
            if (f.crouching && f.onOneWay) {
                f.dropT = 0.25f
                f.vy = 200f
            } else {
                f.vy = -JUMP_SPEED * f.def.jumpMul
                e.effects.dust(f.x, f.y, 4)
                SoundHub.play(Sfx.JUMP)
            }
            f.jumpBuf = 0f
            f.coyote = 0f
            f.onGround = false
        }

        // Horizontal movement
        var maxSpeed = RUN_SPEED * f.def.speedMul
        val mag = abs(mx)
        if (mag < 0.55f) maxSpeed *= 0.5f // walk
        if (f.crouching) maxSpeed *= 0.45f
        if (f.slowT > 0f) maxSpeed *= f.slowMul
        maxSpeed *= f.weapon.type.moveMul
        if (live && inp.aim) maxSpeed *= 0.75f
        val target = if (mag > 0.08f) sign(mx) * maxSpeed else 0f
        val acc = (if (target == 0f) DECEL else ACCEL) * (if (f.onGround) 1f else 0.6f)
        f.vx = approach(f.vx, target, acc * dt)
        if (!f.isBot && mag > 0.15f && !inp.aim) f.facing = if (mx > 0f) 1 else -1

        // Gravity + collision
        val wasGround = f.onGround
        val prevVy = f.vy
        f.vy = min(f.vy + GRAVITY * dt, MAX_FALL)
        Collision.moveFighter(f, map, dt)
        if (!wasGround && f.onGround && prevVy > 700f) {
            e.effects.dust(f.x, f.y, 5)
            f.landT = 0.15f
            SoundHub.play(Sfx.LAND)
        }

        // Weapons and actions
        val w = f.weapon
        if (w.reloadT > 0f) {
            w.reloadT -= dt
            if (w.reloadT <= 0f) {
                w.reloadT = 0f
                w.finishReload()
            }
        }
        if (inp.reloadPressed) {
            inp.reloadPressed = false
            if (live) startReload(f)
        }
        if (inp.switchPressed) {
            inp.switchPressed = false
            if (live && f.weapons.size > 1) {
                w.reloadT = 0f
                f.cur = (f.cur + 1) % f.weapons.size
            }
        }
        if (inp.meleePressed) {
            inp.meleePressed = false
            if (live && f.meleeCd <= 0f) e.combat.melee(f)
        }
        if (inp.powerPressed) {
            inp.powerPressed = false
            if (live && f.powerCd <= 0f) PowerSystem.activate(e, f)
        }
        if (live && inp.shoot) tryShoot(e, f)

        updateAnim(f)
    }

    private fun startReload(f: Fighter) {
        val w = f.weapon
        if (w.canReload()) {
            w.reloadT = w.type.reload * f.def.reloadMul
            SoundHub.play(Sfx.RELOAD)
        }
    }

    private fun tryShoot(e: GameEngine, f: Fighter) {
        val w = f.weapon
        if (w.reloadT > 0f || f.shootCd > 0f) return
        if (w.mag <= 0) {
            startReload(f)
            return
        }
        val t = w.type
        w.mag--
        var cd = t.interval
        if (f.penaltyT > 0f) cd *= 1.35f
        f.shootCd = cd

        var spread = t.spread + f.extraSpread
        if (f.penaltyT > 0f) spread += 3f
        if (f.input.aim) spread *= 0.55f
        if (abs(f.vx) > 120f || !f.onGround) spread *= 1.3f

        val base = if (f.facing > 0) f.aimAngle else PI_F - f.aimAngle
        val mxp = f.x + f.facing * 34f
        val myp = f.y - f.height * 0.58f
        val sp = t.bulletSpeed
        for (i in 0 until t.pellets) {
            val ang = base + (e.rnd.nextFloat() * 2f - 1f) * spread * DEG
            e.bullets.spawn(
                f, mxp, myp, cos(ang) * sp, sin(ang) * sp,
                t.damage * f.def.damageDealtMul, t.range / sp, t
            )
        }
        f.vx -= f.facing * t.recoil * 25f
        f.shootAnimT = 0.15f
        e.effects.muzzle(mxp, myp, f.facing)
        if (!f.isBot) e.camera.shake(t.recoil * 1.5f)
        SoundHub.play(Sfx.SHOT)

        if (w.mag <= 0) {
            if (w.reserve == 0 && t != WeaponType.PISTOL) {
                f.weapons.remove(w)
                f.cur = 0
            } else {
                startReload(f)
            }
        }
    }

    private fun updateAnim(f: Fighter) {
        val w = f.weapon
        f.anim = when {
            f.hitAnimT > 0f -> Anim.HIT
            f.powerAnimT > 0f -> Anim.POWER
            f.meleeAnimT > 0f -> Anim.MELEE
            w.reloadT > 0f -> Anim.RELOAD
            f.shootAnimT > 0f -> Anim.SHOOT
            !f.onGround && f.vy < 0f -> Anim.JUMP
            !f.onGround -> Anim.FALL
            f.crouching -> Anim.CROUCH
            f.input.aim -> Anim.AIM
            abs(f.vx) > 200f -> Anim.RUN
            abs(f.vx) > 20f -> Anim.WALK
            else -> Anim.IDLE
        }
    }
}
