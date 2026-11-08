package com.bharatbattlearena.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.bharatbattlearena.game.GameWorld
import com.bharatbattlearena.game.core.Assets
import kotlin.math.max
import kotlin.math.min

class Hud {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val t = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    init {
        t.isFakeBoldText = true
        p.isFilterBitmap = true
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int, al: Paint.Align) {
        t.textAlign = al
        t.textSize = size
        t.color = Color.argb(200, 0, 0, 0)
        c.drawText(s, x + size * 0.06f, y + size * 0.06f, t)
        t.color = col
        c.drawText(s, x, y, t)
    }

    private fun panel(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        r.set(x, y, x + w, y + h)
        p.color = Color.argb(150, 10, 14, 24)
        c.drawRoundRect(r, 10f, 10f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 2f; p.color = Color.argb(200, 90, 130, 200)
        c.drawRoundRect(r, 10f, 10f, p)
        p.style = Paint.Style.FILL
    }

    fun draw(c: Canvas, w: GameWorld, sw: Int, sh: Int) {
        val u = sh / 1080f
        val f = w.player
        // health + fuel
        val x0 = 30f * u
        var y0 = 28f * u
        panel(c, x0, y0, 420f * u, 44f * u)
        p.color = Color.argb(255, 60, 20, 20)
        c.drawRect(x0 + 8f * u, y0 + 8f * u, x0 + 412f * u, y0 + 36f * u, p)
        p.color = Color.rgb(230, 50, 50)
        c.drawRect(x0 + 8f * u, y0 + 8f * u, x0 + 8f * u + 404f * u * (max(0f, f.hp) / 100f), y0 + 36f * u, p)
        text(c, "HP " + max(0, f.hp.toInt()), x0 + 18f * u, y0 + 33f * u, 24f * u, Color.WHITE, Paint.Align.LEFT)
        y0 += 52f * u
        panel(c, x0, y0, 420f * u, 28f * u)
        p.color = Color.rgb(40, 190, 240)
        c.drawRect(x0 + 8f * u, y0 + 6f * u, x0 + 8f * u + 404f * u * (f.fuel / 100f), y0 + 22f * u, p)
        text(c, "JET", x0 + 14f * u, y0 + 23f * u, 16f * u, Color.WHITE, Paint.Align.LEFT)
        y0 += 36f * u
        // weapon / ammo
        panel(c, x0, y0, 420f * u, 96f * u)
        val ws = f.weapon
        if (ws != null) {
            val b = Assets.weapon(ws.def.file)
            if (b != null) {
                val ww = 150f * u
                val hh = min(70f * u, ww * b.height / b.width)
                r.set(x0 + 12f * u, y0 + 48f * u - hh / 2f, x0 + 12f * u + ww, y0 + 48f * u + hh / 2f)
                c.drawBitmap(b, null, r, p)
            }
            text(c, ws.ammo.toString() + " / " + ws.reserve, x0 + 180f * u, y0 + 46f * u, 36f * u, Color.WHITE, Paint.Align.LEFT)
            text(c, ws.def.name + (if (f.reloadT > 0f) "  RELOADING" else ""), x0 + 180f * u, y0 + 80f * u, 22f * u, Color.rgb(255, 220, 120), Paint.Align.LEFT)
        }
        y0 += 104f * u
        val gn = when (f.gType) { 0 -> "FRAG"; 1 -> "FLASH"; else -> "SMOKE" }
        panel(c, x0, y0, 420f * u, 44f * u)
        text(c, "GRENADE " + gn + " x" + f.grenades[f.gType] + "    MINES x" + f.mines, x0 + 14f * u, y0 + 31f * u, 22f * u, Color.WHITE, Paint.Align.LEFT)

        // timer + scores
        val cx = sw / 2f
        val secs = max(0, w.timeLeft.toInt())
        panel(c, cx - 120f * u, 18f * u, 240f * u, 64f * u)
        text(c, (secs / 60).toString() + ":" + (if (secs % 60 < 10) "0" else "") + (secs % 60), cx, 66f * u, 44f * u, Color.WHITE, Paint.Align.CENTER)
        if (w.mode == 1) {
            val a = w.teamKills(0)
            val b = w.teamKills(1)
            r.set(cx - 150f * u, 88f * u, cx, 130f * u)
            p.color = Color.argb(220, 170, 30, 30); c.drawRoundRect(r, 8f, 8f, p)
            r.set(cx, 88f * u, cx + 150f * u, 130f * u)
            p.color = Color.argb(220, 30, 70, 180); c.drawRoundRect(r, 8f, 8f, p)
            text(c, "RED $a", cx - 75f * u, 120f * u, 30f * u, Color.WHITE, Paint.Align.CENTER)
            text(c, "BLUE $b", cx + 75f * u, 120f * u, 30f * u, Color.WHITE, Paint.Align.CENTER)
        } else {
            text(c, "K " + f.kills + "   D " + f.deaths, cx, 118f * u, 30f * u, Color.WHITE, Paint.Align.CENTER)
        }
        // kill feed
        var fy = 160f * u
        for (it in w.feed) {
            text(c, it.text, cx, fy, 24f * u, if (it.mine) Color.rgb(255, 230, 90) else Color.WHITE, Paint.Align.CENTER)
            fy += 30f * u
        }
        // scoreboard (top 4)
        val rank = w.ranking()
        panel(c, sw - 330f * u, 18f * u, 240f * u, (34f + 28f * min(4, rank.size)) * u)
        var sy = 48f * u
        for (i in 0 until min(4, rank.size)) {
            val q = rank[i]
            val col = if (q.id == 0) Color.rgb(255, 230, 90) else Color.WHITE
            val tag = if (w.mode == 1) (if (q.team == 0) "[R] " else "[B] ") else ""
            text(c, tag + q.name, sw - 320f * u, sy, 20f * u, col, Paint.Align.LEFT)
            text(c, q.kills.toString() + "/" + q.deaths, sw - 110f * u, sy, 20f * u, col, Paint.Align.RIGHT)
            sy += 28f * u
        }
        // pause button
        p.color = Color.argb(170, 20, 25, 40)
        c.drawCircle(sw - 50f * u, 50f * u, 32f * u, p)
        p.color = Color.WHITE
        c.drawRect(sw - 60f * u, 36f * u, sw - 52f * u, 64f * u, p)
        c.drawRect(sw - 44f * u, 36f * u, sw - 36f * u, 64f * u, p)

        // respawn ring / countdown / flash
        if (!f.alive && w.phase == 1) {
            val s = max(1, Math.ceil(f.respawnT.toDouble()).toInt())
            p.color = Color.argb(170, 10, 10, 20)
            c.drawCircle(cx, sh * 0.42f, 100f * u, p)
            p.style = Paint.Style.STROKE; p.strokeWidth = 12f * u; p.color = Color.rgb(230, 60, 40)
            r.set(cx - 100f * u, sh * 0.42f - 100f * u, cx + 100f * u, sh * 0.42f + 100f * u)
            c.drawArc(r, -90f, 360f * (f.respawnT / GameWorld.RESPAWN_TIME), false, p)
            p.style = Paint.Style.FILL
            text(c, s.toString(), cx, sh * 0.42f + 28f * u, 90f * u, Color.WHITE, Paint.Align.CENTER)
            text(c, "RESPAWNING IN", cx, sh * 0.42f - 120f * u, 30f * u, Color.WHITE, Paint.Align.CENTER)
        }
        if (w.phase == 0) {
            val n = max(1, Math.ceil(w.countdown.toDouble()).toInt())
            text(c, n.toString(), cx, sh * 0.45f, 200f * u, Color.WHITE, Paint.Align.CENTER)
            text(c, "GET READY", cx, sh * 0.25f, 50f * u, Color.rgb(255, 220, 120), Paint.Align.CENTER)
        }
        if (f.flashed > 0f) {
            p.color = Color.argb((255 * min(1f, f.flashed)).toInt(), 255, 255, 255)
            c.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), p)
        }
    }
}
