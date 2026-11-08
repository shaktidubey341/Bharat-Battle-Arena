package com.bharatbattlearena.game.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.bharatbattlearena.game.GameWorld
import com.bharatbattlearena.game.core.Assets
import com.bharatbattlearena.game.map.MapDef
import com.bharatbattlearena.game.player.Fighter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Draws the scrolling world (map art, props, fighters, projectiles, effects) with a smooth follow camera. */
class WorldRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()
    private val rnd = java.util.Random()
    var camX = 0f
    var camY = 0f
    var scale = 1f
    private var mapBmp: Bitmap? = null
    private var mapFile = ""
    private var camInit = false
    private var clock = 0f

    init {
        p.isFilterBitmap = true
        tp.textAlign = Paint.Align.CENTER
        tp.isFakeBoldText = true
    }

    fun loadMap(m: MapDef) {
        if (mapFile != m.file) {
            Assets.release(mapFile)
            mapBmp = Assets.bmp(m.file)
            mapFile = m.file
        }
        camInit = false
    }

    fun updateCamera(w: GameWorld, sw: Int, sh: Int, dt: Float, zoom: Float) {
        clock += dt
        val viewH = 600f / zoom
        scale = sh / viewH
        val viewW = sw / scale
        val f = w.player
        var tx = f.x + (if (f.aimActive) cos(f.aim) * 80f else f.face * 40f)
        var ty = f.y - 70f
        if (viewW < w.map.w) tx = tx.coerceIn(viewW / 2f, w.map.w - viewW / 2f) else tx = w.map.w / 2f
        if (viewH < w.map.h) ty = ty.coerceIn(viewH / 2f, w.map.h - viewH / 2f) else ty = w.map.h / 2f
        if (!camInit) { camX = tx; camY = ty; camInit = true }
        val k = min(1f, 7f * dt)
        camX += (tx - camX) * k
        camY += (ty - camY) * k
    }

    private fun bottomCenter(c: Canvas, b: Bitmap?, x: Float, y: Float, h: Float, alpha: Int = 255) {
        if (b == null) return
        val w = h * b.width / b.height
        dst.set(x - w / 2f, y - h, x + w / 2f, y)
        p.alpha = alpha
        c.drawBitmap(b, null, dst, p)
        p.alpha = 255
    }

    private fun centered(c: Canvas, b: Bitmap?, x: Float, y: Float, size: Float, alpha: Int = 255) {
        if (b == null) return
        val h = size
        val w = size * b.width / b.height
        dst.set(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f)
        p.alpha = alpha
        c.drawBitmap(b, null, dst, p)
        p.alpha = 255
    }

    fun draw(c: Canvas, w: GameWorld, sw: Int, sh: Int) {
        val map = w.map
        c.save()
        c.translate(sw / 2f, sh / 2f)
        c.scale(scale, scale)
        val sx = (rnd.nextFloat() - 0.5f) * 28f * w.shake
        val sy = (rnd.nextFloat() - 0.5f) * 28f * w.shake
        c.translate(-camX + sx, -camY + sy)

        // map art (always the full original image, never stretched beyond its own proportions)
        val mb = mapBmp
        if (mb != null) {
            dst.set(0f, 0f, map.w, map.h)
            c.drawBitmap(mb, null, dst, p)
        } else {
            p.color = Color.rgb(30, 40, 70)
            c.drawRect(0f, 0f, map.w, map.h, p)
        }
        // visible planks on plaza maps
        val plank = Assets.bmp("props/plank.png")
        for (pl in map.platforms) {
            if (!pl.visible) continue
            val ww = pl.x1 - pl.x0
            dst.set(pl.x0, pl.y - 10f, pl.x1, pl.y - 10f + min(64f, ww * 0.22f))
            if (plank != null) c.drawBitmap(plank, null, dst, p)
        }

        drawProps(c, w)
        drawPickups(c, w)
        for (m in w.mines) {
            bottomCenter(c, Assets.bmp(if (m.arm > 0f) "grenades/mine/mine_placed.png" else "grenades/mine/mine_armed.png"), m.x, m.y + 4f, 26f)
            if (m.arm <= 0f && ((m.t * 3f).toInt() % 2 == 0)) {
                p.color = Color.argb(200, 255, 40, 40)
                c.drawCircle(m.x, m.y - 14f, 4f, p)
            }
        }
        for (s in w.smokes) drawSmoke(c, s.x, s.y, s.r, s.age, s.life)
        for (f in w.fighters) if (!f.alive) drawFighter(c, f, w)
        for (f in w.fighters) if (f.alive && f.id != 0) drawFighter(c, f, w)
        if (w.player.alive) drawFighter(c, w.player, w)
        for (g in w.grenades) {
            val b = Assets.bmp(when (g.type) { 0 -> "grenades/explosive/grenade.png"; 1 -> "grenades/flash/grenade.png"; else -> "grenades/smoke/grenade.png" })
            if (b != null) {
                c.save()
                c.translate(g.x, g.y)
                c.rotate(g.rot * 57.3f)
                centered(c, b, 0f, 0f, 34f)
                c.restore()
            }
        }
        for (b in w.bullets) if (b.active) drawBullet(c, b.x, b.y, b.vx, b.vy, b.rocket)
        for (e in w.fx) drawFx(c, e)
        c.restore()
    }

    private fun drawProps(c: Canvas, w: GameWorld) {
        for (pr in w.props) {
            if (!pr.alive) continue
            if (pr.kind == 0) {
                val r = pr.hp / 40f
                val nm = if (r > 0.66f) "props/crate_intact.png" else if (r > 0.33f) "props/crate_damaged.png" else "props/crate_broken.png"
                bottomCenter(c, Assets.bmp(nm), pr.x, pr.y + 3f, 58f)
            } else {
                bottomCenter(c, Assets.bmp("props/barrel.png"), pr.x, pr.y + 6f, 70f)
            }
        }
    }

    private fun drawPickups(c: Canvas, w: GameWorld) {
        for (pk in w.pickups) {
            if (!pk.active) continue
            val y = pk.y - 36f + sin(pk.bob * 3f) * 6f
            val col = when (pk.type) { 0 -> Color.argb(110, 255, 70, 70); 1 -> Color.argb(110, 255, 220, 60); 2 -> Color.argb(110, 90, 255, 90); 3 -> Color.argb(110, 255, 140, 40); else -> Color.argb(110, 80, 200, 255) }
            p.color = col
            c.drawCircle(pk.x, y, 34f + sin(pk.bob * 4f) * 3f, p)
            when (pk.type) {
                0 -> centered(c, Assets.bmp("pickups/health.png"), pk.x, y, 44f)
                1 -> centered(c, Assets.bmp("pickups/ammo.png"), pk.x, y, 34f)
                2 -> centered(c, Assets.bmp("grenades/explosive/grenade.png"), pk.x, y, 42f)
                3 -> centered(c, Assets.bmp("grenades/mine/mine_armed.png"), pk.x, y, 26f)
                else -> {
                    val wd = com.bharatbattlearena.game.weapon.Weapons.ALL[pk.wId]
                    val b = Assets.weapon(wd.file)
                    if (b != null) {
                        val ww = min(70f, wd.length * 0.95f)
                        centered(c, b, pk.x, y, ww * b.height / b.width)
                    }
                }
            }
        }
    }

    private fun drawSmoke(c: Canvas, x: Float, y: Float, r: Float, age: Float, life: Float) {
        val grow = min(1f, age / 0.8f)
        val fade = if (age > life - 1.5f) max(0f, (life - age) / 1.5f) else 1f
        for (i in 0 until 9) {
            val a = i * 0.7f + age * 0.15f
            val ox = cos(a) * r * 0.45f * grow
            val oy = sin(a * 1.3f) * r * 0.35f * grow - 10f
            p.color = Color.argb((120 * fade).toInt(), 150 + (i % 3) * 12, 165 + (i % 3) * 10, 150)
            c.drawCircle(x + ox, y + oy, r * (0.40f + 0.05f * (i % 3)) * grow, p)
        }
    }

    private fun drawBullet(c: Canvas, x: Float, y: Float, vx: Float, vy: Float, rocket: Boolean) {
        val ang = atan2(vy, vx)
        c.save()
        c.translate(x, y)
        c.rotate(ang * 57.2958f)
        if (rocket) {
            p.color = Color.argb(180, 255, 150, 40)
            c.drawCircle(-22f, 0f, 9f, p)
            c.drawCircle(-34f, 0f, 6f, p)
            p.color = Color.rgb(90, 95, 100)
            c.drawRect(-18f, -4f, 8f, 4f, p)
            p.color = Color.rgb(220, 40, 40)
            c.drawRect(8f, -4f, 16f, 4f, p)
        } else {
            val b = Assets.bmp("bullets/bullet_1.png")
            if (b != null) {
                val len = 74f
                val hh = len * b.height / b.width
                dst.set(-len * 0.8f, -hh / 2f, len * 0.2f, hh / 2f)
                c.drawBitmap(b, null, dst, p)
            }
        }
        c.restore()
    }

    private fun drawFx(c: Canvas, e: com.bharatbattlearena.game.combat.Fx) {
        val t = e.age / e.life
        when (e.kind) {
            0 -> {
                val i = min(4, (t * 5f).toInt()) + 1
                centered(c, Assets.bmp("effects/explosion/explosion_$i.png"), e.x, e.y, e.size, (255 * (1f - max(0f, t - 0.7f) / 0.3f)).toInt())
            }
            1 -> centered(c, Assets.bmp("effects/muzzle_flash/spark_burst.png"), e.x, e.y, e.size * (0.6f + 0.6f * t), (255 * (1f - t)).toInt())
            2 -> centered(c, Assets.bmp("grenades/flash/flash_burst.png"), e.x, e.y, e.size * (0.4f + 0.8f * t), (255 * (1f - t)).toInt())
            3 -> {
                p.color = Color.argb((120 * (1f - t)).toInt(), 200, 180, 140)
                c.drawCircle(e.x, e.y, e.size * (0.25f + 0.5f * t), p)
            }
            4 -> {
                p.style = Paint.Style.STROKE
                p.strokeWidth = 5f
                p.color = Color.argb((220 * (1f - t)).toInt(), 90, 220, 255)
                c.drawCircle(e.x, e.y, e.size * (0.2f + 0.5f * t), p)
                p.style = Paint.Style.FILL
            }
            else -> {
                p.color = Color.argb((220 * (1f - t)).toInt(), 150, 100, 50)
                c.drawRect(e.x - 5f, e.y - 5f, e.x + 5f, e.y + 5f, p)
            }
        }
    }

    private fun pickFrame(f: Fighter, anims: HashMap<String, Array<Bitmap>>): Bitmap? {
        fun arr(n: String): Array<Bitmap>? = anims[n]
        if (!f.alive) {
            val a = arr("die") ?: return null
            return a[min(a.size - 1, (f.dieT * 7f).toInt())]
        }
        if (f.hitT > 0f) {
            val a = arr("hit")
            if (a != null && a.isNotEmpty()) return a[((0.25f - f.hitT) * 12f).toInt().coerceIn(0, a.size - 1)]
        }
        if (f.meleeT > 0f) {
            val a = arr("punch")
            if (a != null && a.isNotEmpty()) return a[(((0.35f - f.meleeT) / 0.35f) * a.size).toInt().coerceIn(0, a.size - 1)]
        }
        if (f.crouch) {
            val a = arr("jump")
            if (a != null && a.isNotEmpty()) return a[0]
        }
        if (!f.onGround) {
            val a = arr("jump")
            if (a != null && a.size > 2) {
                val i = 1 + (((f.vy + 500f) / 1000f) * (a.size - 2)).toInt().coerceIn(0, a.size - 2)
                return a[i]
            }
        }
        val spd = Math.abs(f.vx)
        if (spd > 180f) { val a = arr("run"); if (a != null && a.isNotEmpty()) return a[(f.animT * 12f).toInt() % a.size] }
        if (spd > 25f) { val a = arr("walk"); if (a != null && a.isNotEmpty()) return a[(f.animT * 9f).toInt() % a.size] }
        val a = arr("idle") ?: return null
        return a[(f.animT * 5f).toInt() % a.size]
    }

    private fun drawFighter(c: Canvas, f: Fighter, w: GameWorld) {
        val cs = Assets.character(f.charIdx)
        var alpha = 255
        if (!f.alive) {
            alpha = (255 * max(0f, 1f - (f.dieT - 3.2f) / 1.2f)).toInt()
            if (alpha <= 0) return
        } else if (f.invuln > 0f && ((f.invuln * 10f).toInt() % 2 == 0)) alpha = 110
        val bmp = pickFrame(f, cs.anims)
        // jetpack flame
        if (f.alive && f.jetting) {
            p.color = Color.argb(230, 255, 170, 40)
            val r = 7f + rnd.nextFloat() * 5f
            c.drawCircle(f.x - f.face * 12f, f.y - 2f, r, p)
            p.color = Color.argb(230, 255, 240, 120)
            c.drawCircle(f.x - f.face * 12f, f.y - 4f, r * 0.5f, p)
        }
        if (bmp != null) {
            val s = cs.scale
            val bw = bmp.width * s
            val bh = bmp.height * s
            c.save()
            c.translate(f.x, f.y)
            c.scale(f.face.toFloat(), 1f)
            dst.set(-bw / 2f, -bh, bw / 2f, 0f)
            p.alpha = alpha
            c.drawBitmap(bmp, null, dst, p)
            p.alpha = 255
            c.restore()
        }
        if (!f.alive) return
        // weapon in hand, rotated to the aim direction
        val ws = f.weapon
        if (ws != null && f.meleeT <= 0f && f.hitT <= 0f) {
            val wb = Assets.weapon(ws.def.file)
            if (wb != null) {
                val len = ws.def.length
                val hh = len * wb.height / wb.width
                c.save()
                c.translate(f.x + f.face * 5f, f.y - GameWorld.HAND_H + (if (f.crouch) 14f else 0f))
                var ang = f.aim * 57.2958f
                if (f.reloadT > 0f) ang += sin(f.animT * 14f) * 10f - 18f * f.face
                c.rotate(ang)
                if (f.face < 0) c.scale(1f, -1f)
                val kick = if (f.muzzleT > 0f) 5f else 0f
                dst.set(-len * 0.28f - kick, -hh * 0.6f, len * 0.72f - kick, hh * 0.4f)
                p.alpha = alpha
                c.drawBitmap(wb, null, dst, p)
                p.alpha = 255
                if (f.muzzleT > 0f) {
                    val fl = Assets.bmp(if (ws.def.id == 4 || ws.def.id == 3) "effects/muzzle_flash/muzzle_flash_big.png" else "effects/muzzle_flash/muzzle_flash_small.png")
                    if (fl != null) {
                        val fs = 38f + len * 0.2f
                        dst.set(len * 0.70f, -fs / 2f, len * 0.70f + fs, fs / 2f)
                        c.drawBitmap(fl, null, dst, p)
                    }
                }
                c.restore()
            }
        }
        // name tag + health
        val top = f.y - 108f
        val tcol = if (w.mode == 1) (if (f.team == 0) Color.rgb(255, 90, 90) else Color.rgb(110, 160, 255)) else if (f.id == 0) Color.YELLOW else Color.WHITE
        tp.color = Color.BLACK
        tp.textSize = 15f
        c.drawText(f.name, f.x + 1f, top - 5f, tp)
        tp.color = tcol
        c.drawText(f.name, f.x, top - 6f, tp)
        p.color = Color.argb(160, 0, 0, 0)
        c.drawRect(f.x - 22f, top, f.x + 22f, top + 6f, p)
        p.color = if (f.hp > 50f) Color.rgb(80, 220, 80) else if (f.hp > 25f) Color.rgb(240, 200, 40) else Color.rgb(240, 60, 60)
        c.drawRect(f.x - 21f, top + 1f, f.x - 21f + 42f * (f.hp / 100f), top + 5f, p)
        if (f.reloadT > 0f && ws != null) {
            p.color = Color.argb(200, 255, 255, 255)
            c.drawRect(f.x - 21f, top + 8f, f.x - 21f + 42f * (1f - f.reloadT / ws.def.reload), top + 11f, p)
        }
    }
}
