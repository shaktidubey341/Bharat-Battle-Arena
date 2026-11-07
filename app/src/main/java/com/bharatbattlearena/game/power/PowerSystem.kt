package com.bharatbattlearena.game.power

import android.graphics.Color
import com.bharatbattlearena.game.GameEngine
import com.bharatbattlearena.game.audio.Sfx
import com.bharatbattlearena.game.audio.SoundHub
import com.bharatbattlearena.game.combat.HitZone
import com.bharatbattlearena.game.player.Fighter
import com.bharatbattlearena.game.player.PowerType
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Activates each character's unique power. Used by both humans and bots. */
object PowerSystem {

    fun activate(e: GameEngine, f: Fighter) {
        f.powerCd = f.def.powerCooldown
        f.powerAnimT = 0.4f
        val cx = f.x
        val cy = f.y - f.height * 0.5f
        SoundHub.play(Sfx.POWER)
        when (f.def.power) {
            PowerType.SOLAR_SHIELD -> {
                f.shieldT = 4.5f
                f.shieldReduce = 0.65f
                e.effects.ring(cx, cy, 30f, 120f, 0.5f, Color.rgb(255, 200, 60))
                e.effects.burst(cx, cy, 16, 160f, 0.6f, 5f, Color.rgb(255, 210, 90), 0f)
            }
            PowerType.TACTICAL_SHIELD -> {
                f.armor = min(f.armor + 60f, 100f)
                f.shieldT = 3f
                f.shieldReduce = 0.4f
                e.effects.ring(cx, cy, 30f, 110f, 0.5f, Color.rgb(120, 180, 255))
            }
            PowerType.PUSH_WAVE -> {
                e.effects.ring(cx, cy, 20f, 280f, 0.4f, Color.rgb(200, 240, 255))
                e.effects.ring(cx, cy, 10f, 200f, 0.3f, Color.WHITE)
                forEnemies(e, f, 280f) { o, d ->
                    val dir = if (o.x >= f.x) 1f else -1f
                    o.vx += dir * 900f
                    o.vy -= 300f
                    o.stunT = max(o.stunT, 0.4f)
                    e.combat.damage(o, 10f, f, HitZone.BODY, false)
                    e.effects.impact(o.x, o.y - o.height * 0.5f, Color.rgb(200, 240, 255))
                }
            }
            PowerType.FIRE_BURST -> {
                e.effects.explosion(cx, cy, 230f)
                forEnemies(e, f, 230f) { o, d ->
                    val dir = if (o.x >= f.x) 1f else -1f
                    o.vx += dir * 400f
                    o.vy -= 200f
                    o.burnT = 3f
                    o.burnOwner = f
                    e.combat.damage(o, 40f * f.def.damageDealtMul * (1f - 0.5f * d / 230f), f, HitZone.BODY, false)
                }
            }
            PowerType.KNOWLEDGE_DEBUFF -> {
                e.effects.ring(cx, cy, 20f, 380f, 0.5f, Color.rgb(190, 120, 255))
                forEnemies(e, f, 380f) { o, d ->
                    o.penaltyT = max(o.penaltyT, 5f)
                    o.vulnT = max(o.vulnT, 5f)
                    e.effects.ring(o.x, o.y - o.height * 0.5f, 10f, 40f, 0.5f, Color.rgb(190, 120, 255))
                }
            }
            PowerType.ICE_SLOW -> {
                e.effects.ring(cx, cy, 20f, 380f, 0.5f, Color.rgb(150, 230, 255))
                e.effects.burst(cx, cy, 20, 300f, 0.7f, 5f, Color.rgb(210, 245, 255), 0f)
                forEnemies(e, f, 380f) { o, d ->
                    e.combat.applySlow(o, 4f, 0.45f)
                    e.effects.ring(o.x, o.y - o.height * 0.5f, 10f, 40f, 0.5f, Color.rgb(150, 230, 255))
                }
            }
        }
        if (f === e.player) e.camera.shake(8f)
    }

    private inline fun forEnemies(e: GameEngine, f: Fighter, radius: Float, action: (Fighter, Float) -> Unit) {
        for (i in 0 until e.fighters.size) {
            val o = e.fighters[i]
            if (o === f || !o.alive) continue
            val d = hypot(o.x - f.x, (o.y - o.height * 0.5f) - (f.y - f.height * 0.5f))
            if (d <= radius) action(o, d)
        }
    }
}
