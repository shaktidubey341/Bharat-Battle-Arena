package com.bharatbattlearena.game.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.bharatbattlearena.game.core.Assets
import com.bharatbattlearena.game.core.Prefs
import com.bharatbattlearena.game.player.Fighter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Left stick: move (push up = jetpack, push down = crouch / drop through platform).
 * Right stick: aim, and fires while pushed. Buttons use the icons from the supplied HUD sheet.
 */
class TouchControls {
    class Btn(val id: Int, val icon: String, val label: String) {
        var cx = 0f
        var cy = 0f
        var r = 0f
        var pointer = -1
        val pressed: Boolean get() = pointer != -1
    }

    companion object {
        const val SHOOT = 0; const val JUMP = 1; const val JET = 2; const val RELOAD = 3
        const val GRENADE = 4; const val SWITCH = 5; const val MELEE = 6; const val MINE = 7
        const val CROUCH = 8; const val GTYPE = 9
    }

    val buttons = arrayOf(
        Btn(SHOOT, "ui/shoot.png", ""), Btn(JUMP, "ui/run.png", ""), Btn(JET, "ui/jet.png", ""),
        Btn(RELOAD, "ui/reload.png", ""), Btn(GRENADE, "ui/grenade.png", ""), Btn(SWITCH, "ui/switch.png", ""),
        Btn(MELEE, "ui/melee.png", ""), Btn(MINE, "grenades/mine/mine_armed.png", "MINE"),
        Btn(CROUCH, "ui/crouch.png", ""), Btn(GTYPE, "", "")
    )
    private var lPtr = -1
    private var rPtr = -1
    private var lcx = 0f; private var lcy = 0f; private var lr = 0f
    private var rcx = 0f; private var rcy = 0f; private var rr = 0f
    private var lx = 0f; private var ly = 0f
    private var rx = 0f; private var ry = 0f
    private var sw = 0
    private var sh = 0
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tp = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dst = RectF()
    private var pendJump = false
    private var pendReload = false
    private var pendGren = false
    private var pendSwitch = false
    private var pendMelee = false
    private var pendMine = false
    private var pendGType = false

    init {
        p.isFilterBitmap = true
        tp.textAlign = Paint.Align.CENTER
        tp.isFakeBoldText = true
    }

    fun layout(w: Int, h: Int) {
        sw = w; sh = h
        val u = h / 1080f
        val s = Prefs.ctrlScale
        lr = 150f * u * s; rr = 150f * u * s
        lcx = 120f * u + lr; lcy = h - 90f * u - lr
        rcx = w - 120f * u - rr; rcy = h - 90f * u - rr
        val br = 58f * u * s
        fun put(id: Int, fromRight: Boolean, x: Float, y: Float, r: Float) {
            val b = buttons[id]
            b.cx = if (fromRight) w - x * u else x * u
            b.cy = h - y * u
            b.r = r
        }
        put(JUMP, true, 560f, 120f, br)
        put(SHOOT, true, 590f, 320f, br)
        put(JET, true, 250f, 560f, br)
        put(RELOAD, true, 430f, 540f, br)
        put(SWITCH, true, 90f, 560f, br * 0.9f)
        put(MELEE, true, 90f, 720f, br * 0.9f)
        put(MINE, true, 250f, 720f, br * 0.9f)
        put(GRENADE, true, 760f, 120f, br)
        put(GTYPE, true, 760f, 290f, br * 0.6f)
        put(CROUCH, false, 120f, 560f, br)
    }

    fun reset() {
        lPtr = -1; rPtr = -1; lx = 0f; ly = 0f; rx = 0f; ry = 0f
        for (b in buttons) b.pointer = -1
    }

    private fun hitBtn(x: Float, y: Float): Btn? {
        for (b in buttons) {
            val dx = x - b.cx
            val dy = y - b.cy
            if (dx * dx + dy * dy <= (b.r * 1.2f) * (b.r * 1.2f)) return b
        }
        return null
    }

    private fun stick(x: Float, y: Float, cx: Float, cy: Float, r: Float, isL: Boolean) {
        var dx = (x - cx) / r
        var dy = (y - cy) / r
        val m = sqrt(dx * dx + dy * dy)
        if (m > 1f) { dx /= m; dy /= m }
        if (isL) { lx = dx; ly = dy } else { rx = dx; ry = dy }
    }

    fun onTouch(e: MotionEvent) {
        val a = e.actionMasked
        val idx = e.actionIndex
        when (a) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> down(e.getPointerId(idx), e.getX(idx), e.getY(idx))
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val id = e.getPointerId(i)
                    if (id == lPtr) stick(e.getX(i), e.getY(i), lcx, lcy, lr, true)
                    else if (id == rPtr) stick(e.getX(i), e.getY(i), rcx, rcy, rr, false)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> up(e.getPointerId(idx))
            MotionEvent.ACTION_CANCEL -> reset()
        }
    }

    private fun down(id: Int, x: Float, y: Float) {
        val b = hitBtn(x, y)
        if (b != null) {
            b.pointer = id
            when (b.id) {
                JUMP -> pendJump = true
                RELOAD -> pendReload = true
                GRENADE -> pendGren = true
                SWITCH -> pendSwitch = true
                MELEE -> pendMelee = true
                MINE -> pendMine = true
                GTYPE -> pendGType = true
            }
            return
        }
        if (x < sw * 0.5f) {
            if (lPtr == -1) { lPtr = id; stick(x, y, lcx, lcy, lr, true) }
        } else if (rPtr == -1) { rPtr = id; stick(x, y, rcx, rcy, rr, false) }
    }

    private fun up(id: Int) {
        if (id == lPtr) { lPtr = -1; lx = 0f; ly = 0f }
        if (id == rPtr) { rPtr = -1; rx = 0f; ry = 0f }
        for (b in buttons) if (b.pointer == id) b.pointer = -1
    }

    /** Push the current control state into the player's fighter. */
    fun apply(f: Fighter) {
        f.moveX = if (lx > 0.2f || lx < -0.2f) lx else 0f
        f.wantJet = ly < -0.55f || buttons[JET].pressed
        f.wantDown = ly > 0.6f || buttons[CROUCH].pressed
        val rmag = sqrt(rx * rx + ry * ry)
        if (rPtr != -1 && rmag > 0.12f) {
            f.aim = atan2(ry, rx)
            f.aimActive = true
        } else {
            f.aimActive = false
            f.aim = if (f.face >= 0) 0f else PI.toFloat()
        }
        f.wantFire = buttons[SHOOT].pressed || (rPtr != -1 && rmag > 0.4f)
        if (pendJump) { f.wantJump = true; pendJump = false }
        if (pendReload) { f.wantReload = true; pendReload = false }
        if (pendGren) { f.wantGrenade = true; pendGren = false }
        if (pendSwitch) { f.wantSwitch = true; pendSwitch = false }
        if (pendMelee) { f.wantMelee = true; pendMelee = false }
        if (pendMine) { f.wantMine = true; pendMine = false }
        if (pendGType) { f.gType = (f.gType + 1) % 3; pendGType = false }
    }

    fun draw(c: Canvas, f: Fighter) {
        val a = (Prefs.ctrlAlpha * 255).toInt().coerceIn(40, 255)
        drawStick(c, lcx, lcy, lr, lx, ly, a)
        drawStick(c, rcx, rcy, rr, rx, ry, a)
        for (b in buttons) {
            if (b.id == GTYPE) {
                p.color = Color.argb(a, 20, 30, 50)
                c.drawCircle(b.cx, b.cy, b.r, p)
                p.style = Paint.Style.STROKE; p.strokeWidth = 3f; p.color = Color.argb(a, 160, 200, 255)
                c.drawCircle(b.cx, b.cy, b.r, p)
                p.style = Paint.Style.FILL
                tp.color = Color.WHITE
                tp.textSize = b.r * 0.55f
                val nm = when (f.gType) { 0 -> "FRAG"; 1 -> "FLASH"; else -> "SMOKE" }
                c.drawText(nm, b.cx, b.cy + b.r * 0.2f, tp)
                continue
            }
            val r = if (b.pressed) b.r * 0.92f else b.r
            if (b.id == MINE) {
                p.color = Color.argb(a, 20, 30, 40)
                c.drawCircle(b.cx, b.cy, b.r, p)
            }
            val bmp: Bitmap? = Assets.bmp(b.icon)
            if (bmp != null) {
                if (b.id == MINE) dst.set(b.cx - r * 0.8f, b.cy - r * 0.45f, b.cx + r * 0.8f, b.cy + r * 0.45f)
                else dst.set(b.cx - r, b.cy - r, b.cx + r, b.cy + r)
                p.alpha = if (b.pressed) 255 else a
                c.drawBitmap(bmp, null, dst, p)
                p.alpha = 255
            }
        }
    }

    private fun drawStick(c: Canvas, cx: Float, cy: Float, r: Float, vx: Float, vy: Float, a: Int) {
        val base = Assets.bmp("ui/stick_base.png")
        if (base != null) {
            dst.set(cx - r, cy - r, cx + r, cy + r)
            p.alpha = a
            c.drawBitmap(base, null, dst, p)
            p.alpha = 255
        }
        p.color = Color.argb(min255(a + 40), 30, 30, 36)
        c.drawCircle(cx + vx * r * 0.55f, cy + vy * r * 0.55f, r * 0.38f, p)
        p.style = Paint.Style.STROKE; p.strokeWidth = 4f; p.color = Color.argb(a, 200, 220, 255)
        c.drawCircle(cx + vx * r * 0.55f, cy + vy * r * 0.55f, r * 0.38f, p)
        p.style = Paint.Style.FILL
    }

    private fun min255(v: Int): Int = if (v > 255) 255 else v
}
