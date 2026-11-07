package com.bharatbattlearena.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import com.bharatbattlearena.game.GameEngine
import com.bharatbattlearena.game.map.GameMap
import com.bharatbattlearena.game.map.SolidKind
import com.bharatbattlearena.game.camera.Camera2D
import com.bharatbattlearena.game.player.FighterRenderer
import com.bharatbattlearena.game.util.mixColor
import com.bharatbattlearena.game.util.shade
import com.bharatbattlearena.game.util.withAlpha
import kotlin.math.floor
import kotlin.math.sin

/** Draws the sky, parallax skyline, map, pickups, fighters, bullets and effects. */
class GameRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val fr = FighterRenderer()
    private var skyShader: LinearGradient? = null
    private var shaderH = 0
    private var shaderKey = 0

    fun draw(c: Canvas, e: GameEngine, w: Int, h: Int) {
        val cam = e.camera
        drawSky(c, e.map, cam, w, h)
        c.save()
        c.scale(cam.scale, cam.scale)
        c.translate(-cam.renderX, -cam.renderY)
        drawSolids(c, e.map, cam)
        drawPickups(c, e)
        drawFighters(c, e, cam)
        drawBullets(c, e)
        e.effects.draw(c, p)
        e.effects.drawTexts(c, p)
        c.restore()
    }

    private fun hash01(i: Int, l: Int): Float {
        var v = i * 374761393 + l * 668265263
        v = (v xor (v ushr 13)) * 1274126177
        v = v xor (v ushr 16)
        return (v and 0xFFFF) / 65535f
    }

    private fun drawSky(c: Canvas, map: GameMap, cam: Camera2D, w: Int, h: Int) {
        val key = map.skyTop * 31 + map.skyBottom
        if (skyShader == null || shaderH != h || shaderKey != key) {
            skyShader = LinearGradient(0f, 0f, 0f, h.toFloat(), map.skyTop, map.skyBottom, Shader.TileMode.CLAMP)
            shaderH = h
            shaderKey = key
        }
        p.style = Paint.Style.FILL
        p.shader = skyShader
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        // Sun
        p.color = withAlpha(Color.rgb(255, 245, 200), 200)
        c.drawCircle(w * 0.78f, h * 0.2f, h * 0.09f, p)

        // Parallax skyline (two layers)
        for (layer in 0..1) {
            val factor = 0.15f + 0.2f * layer
            p.color = mixColor(map.skyBottom, Color.rgb(70, 55, 85), 0.3f + 0.25f * layer)
            val baseY = (map.groundY - cam.renderY) * cam.scale
            val startIdx = floor((cam.renderX * factor) / 200f).toInt() - 1
            val count = (w / cam.scale / 200f).toInt() + 3
            for (i in startIdx..startIdx + count) {
                val bh = (120f + hash01(i, layer) * 260f) * (1f + layer * 0.4f)
                val sx = (i * 200f - cam.renderX * factor) * cam.scale
                c.drawRect(sx, baseY - bh * cam.scale, sx + 190f * cam.scale, baseY + 400f * cam.scale, p)
            }
        }
    }

    private fun drawSolids(c: Canvas, map: GameMap, cam: Camera2D) {
        val l = cam.renderX
        val r = cam.renderX + cam.viewW
        val t = cam.renderY
        val b = cam.renderY + cam.viewH
        p.style = Paint.Style.FILL
        for (s in map.solids) {
            val rc = s.rect
            if (rc.right < l || rc.left > r || rc.bottom < t || rc.top > b) continue
            when (s.kind) {
                SolidKind.GROUND -> {
                    p.color = Color.rgb(62, 56, 62)
                    c.drawRect(rc.left.coerceAtLeast(l - 10f), rc.top, rc.right.coerceAtMost(r + 10f), rc.bottom, p)
                    p.color = Color.rgb(96, 92, 98)
                    c.drawRect(rc.left.coerceAtLeast(l - 10f), rc.top, rc.right.coerceAtMost(r + 10f), rc.top + 14f, p)
                    p.color = Color.rgb(240, 200, 70)
                    var dx = floor(l / 90f) * 90f
                    while (dx < r) {
                        c.drawRect(dx, rc.top + 36f, dx + 44f, rc.top + 42f, p)
                        dx += 90f
                    }
                }
                SolidKind.BUILDING -> drawBuilding(c, map, s.tint, rc, t, b)
                SolidKind.PLATFORM -> {
                    p.color = Color.rgb(120, 82, 52)
                    c.drawRect(rc.left, rc.top, rc.right, rc.bottom, p)
                    p.color = Color.rgb(160, 115, 75)
                    c.drawRect(rc.left, rc.top, rc.right, rc.top + 5f, p)
                }
                SolidKind.BRIDGE -> {
                    p.color = Color.rgb(90, 98, 110)
                    c.drawRect(rc.left, rc.top, rc.right, rc.bottom, p)
                    p.color = Color.rgb(140, 150, 165)
                    c.drawRect(rc.left, rc.top, rc.right, rc.top + 4f, p)
                    p.color = Color.rgb(70, 76, 88)
                    var px = rc.left + 10f
                    while (px < rc.right) {
                        c.drawRect(px, rc.top - 26f, px + 4f, rc.top, p)
                        px += 60f
                    }
                    c.drawRect(rc.left, rc.top - 28f, rc.right, rc.top - 24f, p)
                }
                SolidKind.CRATE -> {
                    p.color = Color.rgb(150, 110, 60)
                    c.drawRect(rc.left, rc.top, rc.right, rc.bottom, p)
                    p.style = Paint.Style.STROKE
                    p.strokeWidth = 4f
                    p.color = Color.rgb(100, 70, 35)
                    c.drawRect(rc.left + 2f, rc.top + 2f, rc.right - 2f, rc.bottom - 2f, p)
                    c.drawLine(rc.left, rc.top, rc.right, rc.bottom, p)
                    c.drawLine(rc.right, rc.top, rc.left, rc.bottom, p)
                    p.style = Paint.Style.FILL
                }
            }
        }
    }

    private fun drawBuilding(c: Canvas, map: GameMap, idx: Int, rc: RectF, viewTop: Float, viewBottom: Float) {
        val base = map.buildingColors[idx.coerceAtLeast(0) % map.buildingColors.size]
        p.color = base
        c.drawRect(rc.left, rc.top, rc.right, rc.bottom, p)
        p.color = shade(base, 0.7f)
        c.drawRect(rc.left - 6f, rc.top - 8f, rc.right + 6f, rc.top, p)
        c.drawRect(rc.right - 14f, rc.top, rc.right, rc.bottom, p)
        // Windows
        var row = 0
        var wy = rc.top + 30f
        while (wy + 34f < rc.bottom - 20f) {
            if (wy > viewBottom) break
            if (wy + 34f >= viewTop) {
                var col = 0
                var wx = rc.left + 24f
                while (wx + 26f < rc.right - 24f) {
                    val lit = hash01(idx * 131 + row * 7 + col, 3) < 0.35f
                    p.color = if (lit) Color.rgb(255, 225, 130) else Color.rgb(60, 70, 90)
                    c.drawRect(wx, wy, wx + 26f, wy + 34f, p)
                    wx += 46f
                    col++
                }
            }
            wy += 60f
            row++
        }
    }

    private fun drawPickups(c: Canvas, e: GameEngine) {
        val cam = e.camera
        p.textAlign = Paint.Align.CENTER
        p.textSize = 18f
        for (pk in e.pickups) {
            if (pk.x < cam.renderX - 60f || pk.x > cam.renderX + cam.viewW + 60f) continue
            val t = pk.type
            val bob = sin(e.time * 3f + pk.x) * 4f
            if (t == null) {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                p.color = withAlpha(Color.WHITE, 70)
                c.drawCircle(pk.x, pk.y - 28f, 14f, p)
                p.style = Paint.Style.FILL
                continue
            }
            p.style = Paint.Style.FILL
            p.color = withAlpha(t.color, 70)
            c.drawCircle(pk.x, pk.y - 30f + bob, 30f, p)
            p.color = t.color
            rect.set(pk.x - 18f, pk.y - 38f + bob, pk.x + 18f, pk.y - 22f + bob)
            c.drawRoundRect(rect, 4f, 4f, p)
            p.color = Color.WHITE
            c.drawText(t.label, pk.x, pk.y - 62f + bob, p)
        }
    }

    private fun drawFighters(c: Canvas, e: GameEngine, cam: Camera2D) {
        for (f in e.fighters) {
            if (f.x < cam.renderX - 120f || f.x > cam.renderX + cam.viewW + 120f) continue
            if (f.y < cam.renderY - 60f || f.y > cam.renderY + cam.viewH + 260f) continue
            val alpha = when {
                !f.alive -> 130
                f.protectT > 0f && ((e.time * 12f).toInt() % 2 == 0) -> 120
                else -> 255
            }
            fr.draw(
                c, p, f.def, f.x, f.y, f.height, f.facing, f.anim, f.animT, f.aimAngle,
                f.weapon.type, f.hitFlash, alpha, f.shieldT > 0f, 1f
            )
            if (!f.alive) continue
            // Name tag + health bar
            p.style = Paint.Style.FILL
            p.textAlign = Paint.Align.CENTER
            p.textSize = 20f
            p.color = if (f === e.player) Color.rgb(255, 220, 90) else Color.WHITE
            c.drawText(if (f === e.player) "YOU" else f.name, f.x, f.y - f.height - 34f, p)
            if (f !== e.player) {
                p.color = Color.argb(160, 0, 0, 0)
                c.drawRect(f.x - 26f, f.y - f.height - 26f, f.x + 26f, f.y - f.height - 20f, p)
                p.color = Color.rgb(90, 220, 110)
                c.drawRect(
                    f.x - 26f, f.y - f.height - 26f,
                    f.x - 26f + 52f * (f.hp / f.def.maxHp).coerceIn(0f, 1f), f.y - f.height - 20f, p
                )
            }
            // Status dots
            var dotX = f.x - 12f
            if (f.slowT > 0f) { p.color = Color.rgb(120, 220, 255); c.drawCircle(dotX, f.y - f.height - 12f, 4f, p); dotX += 12f }
            if (f.burnT > 0f) { p.color = Color.rgb(255, 120, 30); c.drawCircle(dotX, f.y - f.height - 12f, 4f, p); dotX += 12f }
            if (f.vulnT > 0f) { p.color = Color.rgb(190, 120, 255); c.drawCircle(dotX, f.y - f.height - 12f, 4f, p) }
        }
    }

    private fun drawBullets(c: Canvas, e: GameEngine) {
        for (b in e.bullets.items) {
            if (!b.active) continue
            if (b.type.explosiveRadius > 0f) {
                p.style = Paint.Style.FILL
                p.color = Color.rgb(255, 170, 40)
                c.drawCircle(b.x, b.y, 8f, p)
            } else {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 4f
                p.color = Color.rgb(255, 235, 150)
                c.drawLine(b.x, b.y, b.x - b.vx * 0.018f, b.y - b.vy * 0.018f, p)
                p.style = Paint.Style.FILL
            }
        }
    }
}
