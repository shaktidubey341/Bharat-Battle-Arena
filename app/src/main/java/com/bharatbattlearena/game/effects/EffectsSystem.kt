package com.bharatbattlearena.game.effects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.bharatbattlearena.game.util.withAlpha
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pooled procedural particles (dots, rings, sparks, smoke) and floating damage text.
 * Everything is stylised and non-graphic: sparks, impact rings, dust, energy bursts.
 */
class EffectsSystem(var quality: Int) {

    private class P {
        var active = false
        var type = 0
        var x = 0f
        var y = 0f
        var vx = 0f
        var vy = 0f
        var life = 0f
        var max = 1f
        var size = 4f
        var endSize = 4f
        var color = 0
        var grav = 0f
    }

    private class T {
        var active = false
        var x = 0f
        var y = 0f
        var life = 0f
        var text = ""
        var color = 0
    }

    companion object {
        const val DOT = 0
        const val RING = 1
        const val SPARK = 2
        const val SMOKE = 3
        private const val TAU = 6.2831855f
    }

    private val ps = Array(600) { P() }
    private val ts = Array(24) { T() }
    private var cursor = 0
    private var tCursor = 0
    private val r = Random()

    private fun count(n: Int): Int {
        val m = when (quality) {
            0 -> 0.4f
            1 -> 0.7f
            else -> 1f
        }
        return maxOf(1, (n * m).toInt())
    }

    fun spawn(
        type: Int, x: Float, y: Float, vx: Float, vy: Float, life: Float,
        size: Float, endSize: Float, color: Int, grav: Float = 0f
    ) {
        val p = ps[cursor]
        cursor = (cursor + 1) % ps.size
        p.active = true
        p.type = type
        p.x = x
        p.y = y
        p.vx = vx
        p.vy = vy
        p.life = life
        p.max = life
        p.size = size
        p.endSize = endSize
        p.color = color
        p.grav = grav
    }

    fun burst(x: Float, y: Float, n: Int, speed: Float, life: Float, size: Float, color: Int, grav: Float) {
        repeat(count(n)) {
            val a = r.nextFloat() * TAU
            val s = speed * (0.3f + 0.7f * r.nextFloat())
            spawn(DOT, x, y, cos(a) * s, sin(a) * s, life * (0.6f + 0.4f * r.nextFloat()), size, 0f, color, grav)
        }
    }

    fun sparks(x: Float, y: Float, n: Int, color: Int) {
        repeat(count(n)) {
            val a = r.nextFloat() * TAU
            val s = 200f + r.nextFloat() * 400f
            spawn(SPARK, x, y, cos(a) * s, sin(a) * s, 0.25f, 2.5f, 1f, color, 500f)
        }
    }

    fun ring(x: Float, y: Float, r0: Float, r1: Float, life: Float, color: Int) {
        spawn(RING, x, y, 0f, 0f, life, r0, r1, color)
    }

    fun muzzle(x: Float, y: Float, dir: Int) {
        spawn(DOT, x, y, dir * 60f, 0f, 0.07f, 13f, 4f, Color.rgb(255, 230, 120))
        sparks(x, y, 3, Color.rgb(255, 210, 90))
    }

    fun impact(x: Float, y: Float, color: Int) {
        ring(x, y, 3f, 16f, 0.2f, color)
        sparks(x, y, 4, color)
    }

    fun trail(x: Float, y: Float, color: Int) {
        spawn(DOT, x, y, 0f, 0f, 0.12f, 3f, 1f, color)
    }

    fun dust(x: Float, y: Float, n: Int) {
        repeat(count(n)) {
            spawn(
                SMOKE, x + (r.nextFloat() - 0.5f) * 30f, y,
                (r.nextFloat() - 0.5f) * 120f, -r.nextFloat() * 60f,
                0.5f, 6f, 18f, Color.rgb(190, 170, 140)
            )
        }
    }

    fun explosion(x: Float, y: Float, radius: Float) {
        ring(x, y, 10f, radius, 0.35f, Color.rgb(255, 170, 60))
        spawn(DOT, x, y, 0f, 0f, 0.25f, radius * 0.55f, radius * 0.15f, Color.rgb(255, 230, 120))
        burst(x, y, 22, radius * 2.2f, 0.5f, 9f, Color.rgb(255, 130, 40), 250f)
        repeat(count(6)) {
            spawn(
                SMOKE, x + (r.nextFloat() - 0.5f) * radius, y + (r.nextFloat() - 0.5f) * radius * 0.5f,
                (r.nextFloat() - 0.5f) * 80f, -40f - r.nextFloat() * 60f,
                0.9f, 14f, radius * 0.35f, Color.rgb(90, 80, 80)
            )
        }
    }

    fun fire(x: Float, y: Float) {
        spawn(DOT, x + (r.nextFloat() - 0.5f) * 20f, y, (r.nextFloat() - 0.5f) * 40f, -90f, 0.4f, 7f, 1f, Color.rgb(255, 140, 30))
    }

    fun respawn(x: Float, y: Float, color: Int) {
        ring(x, y, 10f, 90f, 0.5f, color)
        burst(x, y - 40f, 18, 200f, 0.6f, 5f, color, -100f)
    }

    fun text(x: Float, y: Float, s: String, color: Int) {
        val t = ts[tCursor]
        tCursor = (tCursor + 1) % ts.size
        t.active = true
        t.x = x
        t.y = y
        t.life = 0.8f
        t.text = s
        t.color = color
    }

    fun update(dt: Float) {
        for (p in ps) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }
            p.vy += p.grav * dt
            p.x += p.vx * dt
            p.y += p.vy * dt
            if (p.type == SMOKE) {
                p.vx *= (1f - 2f * dt)
                p.vy *= (1f - 1.5f * dt)
            }
        }
        for (t in ts) {
            if (!t.active) continue
            t.life -= dt
            if (t.life <= 0f) t.active = false
            t.y -= 70f * dt
        }
    }

    fun draw(c: Canvas, p: Paint) {
        for (pt in ps) {
            if (!pt.active) continue
            val k = pt.life / pt.max
            val a = (255f * k).toInt()
            val size = pt.endSize + (pt.size - pt.endSize) * k
            when (pt.type) {
                DOT -> {
                    p.style = Paint.Style.FILL
                    p.color = withAlpha(pt.color, a)
                    c.drawCircle(pt.x, pt.y, size, p)
                }
                SMOKE -> {
                    p.style = Paint.Style.FILL
                    p.color = withAlpha(pt.color, a / 2)
                    c.drawCircle(pt.x, pt.y, size, p)
                }
                RING -> {
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = 4f
                    p.color = withAlpha(pt.color, a)
                    c.drawCircle(pt.x, pt.y, size, p)
                }
                SPARK -> {
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = size
                    p.color = withAlpha(pt.color, a)
                    c.drawLine(pt.x, pt.y, pt.x - pt.vx * 0.03f, pt.y - pt.vy * 0.03f, p)
                }
            }
        }
        p.style = Paint.Style.FILL
    }

    fun drawTexts(c: Canvas, p: Paint) {
        p.style = Paint.Style.FILL
        p.textAlign = Paint.Align.CENTER
        p.textSize = 26f
        for (t in ts) {
            if (!t.active) continue
            p.color = withAlpha(t.color, (255f * (t.life / 0.8f)).toInt())
            c.drawText(t.text, t.x, t.y, p)
        }
    }
}
