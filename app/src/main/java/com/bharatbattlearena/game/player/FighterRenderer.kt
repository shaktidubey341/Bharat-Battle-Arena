package com.bharatbattlearena.game.player

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.bharatbattlearena.game.util.PI_F
import com.bharatbattlearena.game.util.blendWhite
import com.bharatbattlearena.game.util.shade
import com.bharatbattlearena.game.util.withAlpha
import com.bharatbattlearena.game.weapon.WeaponType
import kotlin.math.cos
import kotlin.math.sin

/**
 * Procedural, state-based character drawing (no sprite sheets needed).
 * To use real sprites later, replace the body of [draw] - the engine only calls this.
 */
class FighterRenderer {
    private val rect = RectF()
    private val path = Path()
    private var alpha = 255
    private var flashK = 0f

    private fun col(c: Int): Int = withAlpha(blendWhite(c, flashK), alpha)

    private fun weaponLen(t: WeaponType): Float = when (t) {
        WeaponType.PISTOL -> 16f
        WeaponType.SMG -> 24f
        WeaponType.RIFLE -> 34f
        WeaponType.SHOTGUN -> 36f
        WeaponType.SNIPER -> 46f
        WeaponType.HEAVY -> 42f
        WeaponType.LAUNCHER -> 42f
    }

    fun draw(
        c: Canvas, p: Paint, def: CharacterDef, x: Float, y: Float, h: Float, facing: Int,
        anim: Anim, t: Float, aim: Float, weapon: WeaponType, flash: Float, alpha: Int,
        shield: Boolean, scale: Float
    ) {
        this.alpha = alpha
        this.flashK = if (flash > 0f) (flash / 0.12f).coerceIn(0f, 1f) * 0.8f else 0f
        val crouch = h < 80f
        val dead = anim == Anim.DEATH

        c.save()
        c.translate(x, y)
        c.scale(facing * scale, scale)
        if (dead) {
            c.translate(0f, -12f)
            c.rotate(-82f)
        }

        val bob = if (anim == Anim.IDLE) sin(t * 3f) * 1.5f else 0f
        val hipY = if (crouch) -17f else -h * 0.46f
        val shoulderY = -h + (if (crouch) 28f else 34f) + bob
        val headY = -h + (if (crouch) 13f else 15f) + bob
        val air = anim == Anim.JUMP || anim == Anim.FALL
        val swing = when (anim) {
            Anim.RUN -> sin(t * 14f) * 17f
            Anim.WALK -> sin(t * 8f) * 10f
            else -> 0f
        }
        val prim = col(def.primary)
        val sec = col(def.secondary)
        val skin = col(def.skin)
        val dark = col(shade(def.primary, 0.55f))

        // Power aura
        if (anim == Anim.POWER) {
            p.style = Paint.Style.STROKE
            p.strokeWidth = 4f
            p.color = withAlpha(def.secondary, 160)
            c.drawCircle(0f, -h * 0.5f, h * 0.75f, p)
        }

        // Legs
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeWidth = 10f
        p.color = dark
        if (air) {
            c.drawLine(-5f, hipY, -12f, -12f, p)
            c.drawLine(5f, hipY, 12f, -6f, p)
        } else if (crouch) {
            c.drawLine(-6f, hipY, -16f, 0f, p)
            c.drawLine(6f, hipY, 12f, 0f, p)
        } else {
            c.drawLine(-5f, hipY, -5f + swing, 0f, p)
            c.drawLine(5f, hipY, 5f - swing, 0f, p)
        }

        // Torso
        p.style = Paint.Style.FILL
        p.color = prim
        rect.set(-15f, shoulderY, 15f, hipY + 5f)
        c.drawRoundRect(rect, 7f, 7f, p)
        p.color = sec
        c.drawRect(-15f, shoulderY + 11f, 15f, shoulderY + 17f, p)

        // Head
        p.color = skin
        c.drawCircle(2f, headY, 14f, p)
        p.color = dark
        c.drawCircle(9f, headY - 1f, 2f, p)
        drawHeadgear(c, p, def.headStyle, headY, t, prim, sec, dark)

        // Arm + weapon
        val armAngle = when (anim) {
            Anim.RELOAD -> 50f
            Anim.MELEE -> sin(t * 30f) * 60f - 20f
            Anim.POWER -> -90f
            else -> aim * 180f / PI_F
        }
        c.save()
        c.translate(4f, shoulderY + 4f)
        c.rotate(armAngle)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 7f
        p.color = skin
        c.drawLine(0f, 0f, 14f, 0f, p)
        if (anim != Anim.POWER && anim != Anim.MELEE) {
            p.style = Paint.Style.FILL
            p.color = col(weapon.color)
            rect.set(10f, -3f, 10f + weaponLen(weapon), 4f)
            c.drawRoundRect(rect, 2f, 2f, p)
            p.color = col(Color.rgb(50, 50, 55))
            rect.set(10f, 0f, 10f + weaponLen(weapon) * 0.6f, 4f)
            c.drawRect(rect, p)
        }
        c.restore()

        // Shield bubble
        if (shield) {
            p.style = Paint.Style.FILL
            p.color = withAlpha(def.secondary, 45)
            c.drawCircle(0f, -h * 0.5f, h * 0.72f, p)
            p.style = Paint.Style.STROKE
            p.strokeWidth = 3f
            p.color = withAlpha(def.secondary, 190)
            c.drawCircle(0f, -h * 0.5f, h * 0.72f, p)
        }

        p.style = Paint.Style.FILL
        c.restore()
    }

    private fun drawHeadgear(
        c: Canvas, p: Paint, style: Int, hy: Float, t: Float, prim: Int, sec: Int, dark: Int
    ) {
        p.style = Paint.Style.FILL
        when (style) {
            0 -> { // sun crown + headband
                p.color = sec
                c.drawRect(-12f, hy - 9f, 16f, hy - 5f, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 3f
                p.strokeCap = Paint.Cap.ROUND
                p.color = col(Color.rgb(255, 200, 60))
                for (i in -2..2) {
                    val a = (-90f + i * 28f) * PI_F / 180f
                    c.drawLine(2f + cos(a) * 16f, hy + sin(a) * 16f, 2f + cos(a) * 25f, hy + sin(a) * 25f, p)
                }
                p.style = Paint.Style.FILL
            }
            1 -> { // helmet with visor
                p.color = dark
                rect.set(-12f, hy - 14f, 16f, hy + 14f)
                c.drawArc(rect, 180f, 180f, true, p)
                p.color = sec
                c.drawRect(2f, hy - 4f, 16f, hy + 1f, p)
            }
            2 -> { // spiky hair + trailing scarf
                p.color = dark
                path.reset()
                path.moveTo(-10f, hy - 10f)
                path.lineTo(-6f, hy - 23f)
                path.lineTo(-2f, hy - 11f)
                path.lineTo(2f, hy - 25f)
                path.lineTo(7f, hy - 11f)
                path.lineTo(12f, hy - 20f)
                path.lineTo(13f, hy - 8f)
                path.close()
                c.drawPath(path, p)
                p.color = sec
                c.drawRect(-14f, hy + 10f, 16f, hy + 16f, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 5f
                c.drawLine(-14f, hy + 13f, -30f, hy + 13f + sin(t * 8f) * 4f, p)
                p.style = Paint.Style.FILL
            }
            3 -> { // flame hair
                p.color = col(Color.rgb(255, 120, 20))
                path.reset()
                path.moveTo(-11f, hy - 8f)
                path.lineTo(-8f, hy - 24f)
                path.lineTo(-3f, hy - 12f)
                path.lineTo(2f, hy - 28f)
                path.lineTo(7f, hy - 12f)
                path.lineTo(12f, hy - 22f)
                path.lineTo(13f, hy - 8f)
                path.close()
                c.drawPath(path, p)
                p.color = col(Color.rgb(255, 220, 90))
                c.drawCircle(2f, hy - 14f, 4f, p)
            }
            4 -> { // hair cap + glasses
                p.color = col(Color.rgb(50, 40, 40))
                rect.set(-12f, hy - 14f, 16f, hy + 14f)
                c.drawArc(rect, 200f, 140f, true, p)
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                p.color = dark
                c.drawCircle(8f, hy - 1f, 5f, p)
                c.drawLine(-4f, hy - 1f, 3f, hy - 1f, p)
                p.style = Paint.Style.FILL
            }
            else -> { // woolly cap
                p.color = prim
                rect.set(-13f, hy - 17f, 17f, hy - 3f)
                c.drawRoundRect(rect, 6f, 6f, p)
                p.color = sec
                c.drawRect(-13f, hy - 7f, 17f, hy - 3f, p)
                c.drawCircle(2f, hy - 20f, 4f, p)
            }
        }
    }
}
