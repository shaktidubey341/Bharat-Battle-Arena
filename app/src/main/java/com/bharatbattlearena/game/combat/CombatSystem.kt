package com.bharatbattlearena.game.combat

import android.graphics.Color
import com.bharatbattlearena.game.GameEngine
import com.bharatbattlearena.game.KillMsg
import com.bharatbattlearena.game.Phase
import com.bharatbattlearena.game.audio.Sfx
import com.bharatbattlearena.game.audio.SoundHub
import com.bharatbattlearena.game.player.Fighter
import com.bharatbattlearena.game.weapon.Bullet
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Damage, hit zones, explosions, melee and kills. Effects are stylised
 * (flashes, sparks, rings) - there is no gore.
 */
class CombatSystem(private val e: GameEngine) {

    companion object {
        const val RESPAWN_SECONDS = 5f
    }

    fun zoneFor(f: Fighter, bx: Float, by: Float): HitZone {
        val rel = (by - (f.y - f.height)) / f.height
        return when {
            rel < 0.22f -> HitZone.HEAD
            rel > 0.68f -> HitZone.LEG
            abs(bx - f.x) > f.width * 0.28f -> HitZone.ARM
            else -> HitZone.BODY
        }
    }

    fun applySlow(f: Fighter, duration: Float, mul: Float) {
        f.slowMul = if (f.slowT <= 0f) mul else min(f.slowMul, mul)
        f.slowT = max(f.slowT, duration)
    }

    fun bulletHit(b: Bullet, f: Fighter) {
        val zone = zoneFor(f, b.x, b.y)
        var dmg = b.damage
        var color = Color.rgb(255, 230, 150)
        when (zone) {
            HitZone.HEAD -> {
                dmg *= 1.8f
                color = Color.rgb(255, 90, 90)
            }
            HitZone.ARM -> {
                dmg *= 0.85f
                f.penaltyT = max(f.penaltyT, 3f)
            }
            HitZone.LEG -> {
                dmg *= 0.8f
                applySlow(f, 2.5f, 0.6f)
            }
            HitZone.BODY -> {}
        }
        e.effects.impact(b.x, b.y, color)
        damage(f, dmg, b.owner, zone, false)
    }

    /** Core damage pipeline: passive -> vulnerability -> shield -> armor -> health. */
    fun damage(target: Fighter, amount: Float, attacker: Fighter?, zone: HitZone, silent: Boolean) {
        if (!target.alive || target.protectT > 0f || e.phase != Phase.PLAYING) return
        var d = amount * target.def.damageTakenMul
        if (target.vulnT > 0f) d *= 1.25f
        if (target.shieldT > 0f) d *= (1f - target.shieldReduce)
        if (target.armor > 0f) {
            val absorbed = min(target.armor, d * 0.5f)
            target.armor -= absorbed
            d -= absorbed
        }
        target.hp -= d
        target.sinceHurt = 0f
        if (!silent) {
            target.hitFlash = 0.12f
            target.hitAnimT = 0.15f
            val head = zone == HitZone.HEAD
            e.effects.text(
                target.x, target.y - target.height - 10f, d.toInt().toString(),
                if (head) Color.rgb(255, 90, 90) else Color.WHITE
            )
            if (attacker === e.player || target === e.player) SoundHub.play(Sfx.HIT)
            if (target === e.player) e.camera.shake(6f)
        }
        if (target.hp <= 0f) kill(target, attacker)
    }

    private fun kill(v: Fighter, attacker: Fighter?) {
        v.alive = false
        v.hp = 0f
        v.deaths++
        v.respawnT = RESPAWN_SECONDS
        v.crouching = false
        v.vy = -450f
        v.vx = -v.facing * 150f
        v.shieldT = 0f
        v.burnT = 0f
        v.stunT = 0f
        if (attacker != null && attacker !== v) attacker.kills++
        e.effects.burst(v.x, v.y - 45f, 16, 260f, 0.5f, 5f, v.def.primary, 300f)
        e.effects.ring(v.x, v.y - 45f, 10f, 70f, 0.35f, Color.WHITE)
        val text = if (attacker != null && attacker !== v) attacker.name + " > " + v.name else v.name + " fell"
        e.addKill(KillMsg(text, 4f, attacker === e.player || v === e.player))
        SoundHub.play(Sfx.DEATH)
        if (v === e.player) e.camera.shake(14f)
    }

    /** Radial explosion with distance falloff and knockback. Owner takes half damage. */
    fun explode(x: Float, y: Float, radius: Float, dmg: Float, owner: Fighter?) {
        e.effects.explosion(x, y, radius)
        SoundHub.play(Sfx.EXPLOSION)
        val shakeDist = hypot(e.player.x - x, e.player.y - y)
        if (shakeDist < 900f) e.camera.shake(16f * (1f - shakeDist / 900f) + 4f)
        for (i in 0 until e.fighters.size) {
            val f = e.fighters[i]
            if (!f.alive) continue
            val cx = f.x
            val cy = f.y - f.height * 0.5f
            val d = hypot(cx - x, cy - y)
            if (d > radius) continue
            val fall = 1f - 0.7f * (d / radius)
            var amount = dmg * fall
            if (f === owner) amount *= 0.5f
            val dirX = if (d < 1f) 0f else (cx - x) / d
            val dirY = if (d < 1f) -1f else (cy - y) / d
            f.vx += dirX * 750f * fall
            f.vy += dirY * 600f * fall - 150f * fall
            damage(f, amount, owner, HitZone.BODY, false)
        }
    }

    fun melee(f: Fighter) {
        f.meleeCd = 0.55f
        f.meleeAnimT = 0.25f
        SoundHub.play(Sfx.MELEE)
        val range = 85f
        for (i in 0 until e.fighters.size) {
            val o = e.fighters[i]
            if (o === f || !o.alive) continue
            val dx = (o.x - f.x) * f.facing
            if (dx < -10f || dx > range) continue
            if (abs(o.y - f.y) > 70f) continue
            e.effects.impact(o.x - f.facing * 15f, o.y - o.height * 0.55f, Color.rgb(255, 255, 255))
            o.vx += f.facing * 520f
            o.vy -= 220f
            damage(o, 24f * f.def.damageDealtMul, f, HitZone.BODY, false)
        }
    }
}
