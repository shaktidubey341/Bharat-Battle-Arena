package com.bharatbattlearena.game.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.bharatbattlearena.game.GameEngine
import com.bharatbattlearena.game.Phase
import kotlin.math.ceil

/** Heads-up display, countdown/respawn banners and the results screen. */
class HudRenderer {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    val rematchRect = RectF()
    val menuRect = RectF()
    private var lastSec = -1
    private var timeStr = "0:00"

    init {
        p.typeface = Typeface.DEFAULT_BOLD
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, align: Paint.Align) {
        p.style = Paint.Style.FILL
        p.textAlign = align
        p.textSize = size
        p.color = color
        c.drawText(s, x, y, p)
    }

    fun draw(c: Canvas, e: GameEngine, w: Int, h: Int) {
        val s = h / 720f
        val pl = e.player
        val wp = pl.weapon

        // Health + armor
        val bx = 24f * s
        val by = 22f * s
        val bw = 260f * s
        p.style = Paint.Style.FILL
        p.color = Color.argb(150, 0, 0, 0)
        rect.set(bx, by, bx + bw, by + 22f * s)
        c.drawRoundRect(rect, 6f * s, 6f * s, p)
        p.color = if (pl.hp > 30f) Color.rgb(90, 220, 110) else Color.rgb(230, 70, 70)
        rect.set(bx, by, bx + bw * (pl.hp / pl.def.maxHp).coerceIn(0f, 1f), by + 22f * s)
        c.drawRoundRect(rect, 6f * s, 6f * s, p)
        text(c, "HP " + pl.hp.toInt(), bx + 8f * s, by + 17f * s, 16f * s, Color.WHITE, Paint.Align.LEFT)
        if (pl.armor > 0f) {
            p.color = Color.argb(150, 0, 0, 0)
            rect.set(bx, by + 28f * s, bx + bw, by + 42f * s)
            c.drawRoundRect(rect, 5f * s, 5f * s, p)
            p.color = Color.rgb(110, 170, 255)
            rect.set(bx, by + 28f * s, bx + bw * (pl.armor / 100f).coerceIn(0f, 1f), by + 42f * s)
            c.drawRoundRect(rect, 5f * s, 5f * s, p)
            text(c, "ARMOR " + pl.armor.toInt(), bx + 8f * s, by + 40f * s, 12f * s, Color.WHITE, Paint.Align.LEFT)
        }
        text(
            c, "K " + pl.kills + "   D " + pl.deaths, bx, by + 68f * s, 20f * s,
            Color.rgb(255, 220, 90), Paint.Align.LEFT
        )

        // Timer
        val sec = e.timeLeft.toInt()
        if (sec != lastSec) {
            lastSec = sec
            timeStr = (sec / 60).toString() + ":" + (if (sec % 60 < 10) "0" else "") + (sec % 60)
        }
        text(c, timeStr, w * 0.5f, 40f * s, 34f * s, Color.WHITE, Paint.Align.CENTER)

        // Ammo / weapon
        val reserve = if (wp.reserve < 0) "\u221E" else wp.reserve.toString()
        val reloading = if (wp.reloadT > 0f) "  RELOADING" else ""
        text(
            c, wp.type.label + "  " + wp.mag + "/" + reserve + reloading, w * 0.5f, h - 24f * s,
            22f * s, wp.type.color, Paint.Align.CENTER
        )
        // Weapon slots
        val slots = pl.weapons.size
        for (i in 0 until slots) {
            p.color = if (i == pl.cur) Color.argb(220, 255, 255, 255) else Color.argb(90, 255, 255, 255)
            val sx = w * 0.5f - (slots * 18f * s) / 2f + i * 18f * s + 9f * s
            c.drawCircle(sx, h - 52f * s, 5f * s, p)
        }

        // Power ready text
        text(
            c, pl.def.powerName + (if (pl.powerCd <= 0f) ": READY" else ": " + ceil(pl.powerCd).toInt() + "s"),
            w * 0.5f, h - 80f * s, 16f * s,
            if (pl.powerCd <= 0f) Color.rgb(140, 255, 160) else Color.LTGRAY, Paint.Align.CENTER
        )

        // Mini scoreboard (top 4)
        val shown = minOf(4, e.board.size)
        for (i in 0 until shown) {
            val f = e.board[i]
            val col = if (f === pl) Color.rgb(255, 220, 90) else Color.WHITE
            text(c, f.name, w - 150f * s, 30f * s + i * 24f * s, 17f * s, col, Paint.Align.RIGHT)
            text(c, f.kills.toString(), w - 24f * s, 30f * s + i * 24f * s, 17f * s, col, Paint.Align.RIGHT)
        }

        // Kill feed
        for (i in 0 until e.killFeed.size) {
            val m = e.killFeed[i]
            text(
                c, m.text, w - 24f * s, 140f * s + i * 22f * s, 15f * s,
                if (m.involvesPlayer) Color.rgb(255, 220, 90) else Color.LTGRAY, Paint.Align.RIGHT
            )
        }

        // Banners
        if (e.phase == Phase.COUNTDOWN) {
            val n = ceil(e.countdown).toInt()
            text(c, if (n > 3) "GET READY" else n.toString(), w * 0.5f, h * 0.45f, 90f * s, Color.WHITE, Paint.Align.CENTER)
        } else if (e.phase == Phase.PLAYING && !pl.alive) {
            text(c, "ELIMINATED", w * 0.5f, h * 0.38f, 48f * s, Color.rgb(255, 110, 110), Paint.Align.CENTER)
            text(
                c, "Respawning in " + ceil(pl.respawnT).toInt(), w * 0.5f, h * 0.38f + 48f * s,
                28f * s, Color.WHITE, Paint.Align.CENTER
            )
        } else if (e.phase == Phase.PLAYING && e.time < 4.5f + 3.99f) {
            if (e.countdown <= 0f && e.timeLeft > e.config.durationSec - 1f) {
                text(c, "FIGHT!", w * 0.5f, h * 0.45f, 90f * s, Color.rgb(255, 170, 40), Paint.Align.CENTER)
            }
        }
    }

    fun drawResults(c: Canvas, e: GameEngine, w: Int, h: Int) {
        val s = h / 720f
        p.style = Paint.Style.FILL
        p.color = Color.argb(215, 8, 10, 28)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)

        val banner = e.bannerText
        val bannerColor = when (banner) {
            "VICTORY" -> Color.rgb(120, 255, 150)
            "DEFEAT" -> Color.rgb(255, 110, 110)
            else -> Color.rgb(255, 220, 90)
        }
        text(c, banner, w * 0.5f, 90f * s, 64f * s, bannerColor, Paint.Align.CENTER)
        text(c, e.winnerText + "     MVP: " + e.mvpName, w * 0.5f, 130f * s, 22f * s, Color.WHITE, Paint.Align.CENTER)

        // Table
        val left = w * 0.5f - 330f * s
        val top = 175f * s
        text(c, "#", left, top, 18f * s, Color.LTGRAY, Paint.Align.LEFT)
        text(c, "PLAYER", left + 50f * s, top, 18f * s, Color.LTGRAY, Paint.Align.LEFT)
        text(c, "FIGHTER", left + 250f * s, top, 18f * s, Color.LTGRAY, Paint.Align.LEFT)
        text(c, "K", left + 500f * s, top, 18f * s, Color.LTGRAY, Paint.Align.RIGHT)
        text(c, "D", left + 560f * s, top, 18f * s, Color.LTGRAY, Paint.Align.RIGHT)
        text(c, "SCORE", left + 660f * s, top, 18f * s, Color.LTGRAY, Paint.Align.RIGHT)
        val rows = e.results
        val rowH = minOf(30f * s, (h - top - 150f * s) / maxOf(1, rows.size))
        for (i in rows.indices) {
            val r = rows[i]
            val y = top + (i + 1) * rowH
            val col = if (r.isPlayer) Color.rgb(255, 220, 90) else Color.WHITE
            val ts = minOf(20f * s, rowH * 0.7f)
            text(c, (i + 1).toString(), left, y, ts, col, Paint.Align.LEFT)
            text(c, r.name, left + 50f * s, y, ts, col, Paint.Align.LEFT)
            text(c, r.characterName, left + 250f * s, y, ts, col, Paint.Align.LEFT)
            text(c, r.kills.toString(), left + 500f * s, y, ts, col, Paint.Align.RIGHT)
            text(c, r.deaths.toString(), left + 560f * s, y, ts, col, Paint.Align.RIGHT)
            text(c, r.score.toString(), left + 660f * s, y, ts, col, Paint.Align.RIGHT)
        }

        // Buttons
        val by = h - 100f * s
        rematchRect.set(w * 0.5f - 270f * s, by, w * 0.5f - 20f * s, by + 64f * s)
        menuRect.set(w * 0.5f + 20f * s, by, w * 0.5f + 270f * s, by + 64f * s)
        p.color = Color.rgb(255, 140, 30)
        c.drawRoundRect(rematchRect, 14f * s, 14f * s, p)
        p.color = Color.rgb(70, 76, 110)
        c.drawRoundRect(menuRect, 14f * s, 14f * s, p)
        text(c, "REMATCH", rematchRect.centerX(), rematchRect.centerY() + 9f * s, 26f * s, Color.WHITE, Paint.Align.CENTER)
        text(c, "MAIN MENU", menuRect.centerX(), menuRect.centerY() + 9f * s, 26f * s, Color.WHITE, Paint.Align.CENTER)
    }
}
