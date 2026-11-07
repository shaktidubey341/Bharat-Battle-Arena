package com.bharatbattlearena.game.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import com.bharatbattlearena.game.camera.Camera2D
import com.bharatbattlearena.game.player.Fighter
import com.bharatbattlearena.game.player.InputState
import com.bharatbattlearena.game.util.Prefs
import com.bharatbattlearena.game.util.clampF
import kotlin.math.hypot

/**
 * On-screen joystick + buttons with multi-touch. Size, opacity, joystick size and
 * sensitivity come from [Prefs]. Two fingers on empty screen space pinch-zoom the camera.
 * Layout scales with screen height so it adapts to any phone.
 */
class TouchControls(private val prefs: Prefs) {

    companion object {
        const val SHOOT = 0
        const val JUMP = 1
        const val CROUCH = 2
        const val AIM = 3
        const val RELOAD = 4
        const val MELEE = 5
        const val SWITCH = 6
        const val POWER = 7
        const val ZOOM_IN = 8
        const val ZOOM_OUT = 9
        private const val COUNT = 10
        private const val MAX_PTR = 32
        private const val NONE = -100
        private const val ROLE_JOY = -1
        private const val ROLE_FREE = -2
    }

    // anchorRight: true = offset from bottom-right, false = offset from top-left.
    private class Btn(val id: Int, val label: String, val dx: Float, val dy: Float, val r: Float, val anchorRight: Boolean)

    private val btns = arrayOf(
        Btn(SHOOT, "FIRE", 190f, 170f, 88f, true),
        Btn(JUMP, "JUMP", 385f, 95f, 64f, true),
        Btn(CROUCH, "DUCK", 400f, 255f, 50f, true),
        Btn(AIM, "AIM", 215f, 350f, 46f, true),
        Btn(RELOAD, "RLD", 55f, 175f, 40f, true),
        Btn(MELEE, "HIT", 55f, 300f, 40f, true),
        Btn(SWITCH, "SWAP", 55f, 425f, 40f, true),
        Btn(POWER, "POWER", 580f, 110f, 58f, true),
        Btn(ZOOM_IN, "+", 50f, 270f, 28f, false),
        Btn(ZOOM_OUT, "-", 50f, 335f, 28f, false)
    )

    private val cx = FloatArray(COUNT)
    private val cy = FloatArray(COUNT)
    private val rad = FloatArray(COUNT)
    private val down = BooleanArray(COUNT)

    private val role = IntArray(MAX_PTR) { NONE }
    private val px = FloatArray(MAX_PTR)
    private val py = FloatArray(MAX_PTR)

    private var w = 0
    private var h = 0
    private var joyCx = 0f
    private var joyCy = 0f
    private var joyR = 100f
    private var thumbX = 0f
    private var thumbY = 0f
    private var lastPinch = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        paint.typeface = Typeface.DEFAULT_BOLD
    }

    fun layout(width: Int, height: Int) {
        w = width
        h = height
        val s = height / 720f
        val scale = prefs.btnScale
        for (i in btns.indices) {
            val b = btns[i]
            if (b.anchorRight) {
                cx[b.id] = width - b.dx * s
                cy[b.id] = height - b.dy * s
            } else {
                cx[b.id] = b.dx * s
                cy[b.id] = b.dy * s
            }
            rad[b.id] = b.r * s * (if (b.id >= ZOOM_IN) 1f else scale)
        }
        joyR = 120f * s * prefs.joyScale
        joyCx = 200f * s
        joyCy = height - 190f * s
        if (role.none { it == ROLE_JOY }) {
            thumbX = joyCx
            thumbY = joyCy
        }
    }

    fun onTouch(e: MotionEvent, input: InputState, cam: Camera2D) {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = e.actionIndex
                pressAt(e.getPointerId(i), e.getX(i), e.getY(i), input, cam)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    moveTo(e.getPointerId(i), e.getX(i), e.getY(i), input)
                }
                pinch(cam)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                release(e.getPointerId(e.actionIndex), input)
                lastPinch = 0f
            }
            MotionEvent.ACTION_CANCEL -> releaseAll(input)
        }
    }

    fun releaseAll(input: InputState) {
        for (i in 0 until MAX_PTR) if (role[i] != NONE) release(i, input)
        input.reset()
    }

    private fun pressAt(id: Int, x: Float, y: Float, input: InputState, cam: Camera2D) {
        if (id < 0 || id >= MAX_PTR) return
        for (b in btns) {
            val dx = x - cx[b.id]
            val dy = y - cy[b.id]
            val r = rad[b.id] * 1.2f
            if (dx * dx + dy * dy <= r * r) {
                role[id] = b.id
                setButton(b.id, true, input, cam)
                return
            }
        }
        if (x < w * 0.5f && y > h * 0.3f) {
            role[id] = ROLE_JOY
            thumbX = x
            thumbY = y
            updateJoy(input)
            return
        }
        role[id] = ROLE_FREE
        px[id] = x
        py[id] = y
    }

    private fun moveTo(id: Int, x: Float, y: Float, input: InputState) {
        if (id < 0 || id >= MAX_PTR) return
        when (role[id]) {
            ROLE_JOY -> {
                thumbX = x
                thumbY = y
                updateJoy(input)
            }
            ROLE_FREE -> {
                px[id] = x
                py[id] = y
            }
        }
    }

    private fun release(id: Int, input: InputState) {
        if (id < 0 || id >= MAX_PTR) return
        val r = role[id]
        when {
            r == ROLE_JOY -> {
                thumbX = joyCx
                thumbY = joyCy
                input.moveX = 0f
                input.aimY = 0f
            }
            r >= 0 -> setButton(r, false, input, null)
        }
        role[id] = NONE
    }

    private fun updateJoy(input: InputState) {
        var dx = (thumbX - joyCx) / joyR
        var dy = (thumbY - joyCy) / joyR
        val mag = hypot(dx, dy)
        if (mag > 1f) {
            dx /= mag
            dy /= mag
            // keep the visible thumb on the ring
            thumbX = joyCx + dx * joyR
            thumbY = joyCy + dy * joyR
        }
        input.moveX = if (hypot(dx, dy) < 0.12f) 0f else clampF(dx * prefs.sensitivity, -1f, 1f)
        input.aimY = clampF(dy, -1f, 1f)
    }

    private fun setButton(id: Int, isDown: Boolean, input: InputState, cam: Camera2D?) {
        down[id] = isDown
        when (id) {
            SHOOT -> input.shoot = isDown
            JUMP -> if (isDown) input.jumpPressed = true
            CROUCH -> input.crouch = isDown
            AIM -> input.aim = isDown
            RELOAD -> if (isDown) input.reloadPressed = true
            MELEE -> if (isDown) input.meleePressed = true
            SWITCH -> if (isDown) input.switchPressed = true
            POWER -> if (isDown) input.powerPressed = true
            ZOOM_IN -> if (isDown) cam?.zoomBy(1.15f)
            ZOOM_OUT -> if (isDown) cam?.zoomBy(1f / 1.15f)
        }
    }

    private fun pinch(cam: Camera2D) {
        var a = -1
        var b = -1
        for (i in 0 until MAX_PTR) {
            if (role[i] == ROLE_FREE) {
                if (a < 0) a = i else if (b < 0) b = i
            }
        }
        if (a >= 0 && b >= 0) {
            val d = hypot(px[a] - px[b], py[a] - py[b])
            if (lastPinch > 10f && d > 10f) cam.zoomBy(d / lastPinch)
            lastPinch = d
        } else {
            lastPinch = 0f
        }
    }

    fun draw(c: Canvas, player: Fighter) {
        val s = h / 720f
        val a = (prefs.opacity * 255f).toInt().coerceIn(40, 255)

        // Joystick
        paint.style = Paint.Style.FILL
        paint.color = Color4.white(a / 4)
        c.drawCircle(joyCx, joyCy, joyR, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * s
        paint.color = Color4.white(a)
        c.drawCircle(joyCx, joyCy, joyR, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color4.white(a)
        c.drawCircle(thumbX, thumbY, joyR * 0.38f, paint)

        // Buttons
        paint.textAlign = Paint.Align.CENTER
        for (b in btns) {
            val x = cx[b.id]
            val y = cy[b.id]
            val r = rad[b.id]
            paint.style = Paint.Style.FILL
            paint.color = if (down[b.id]) Color4.white(a) else Color4.white(a / 4)
            c.drawCircle(x, y, r, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f * s
            paint.color = Color4.white(a)
            c.drawCircle(x, y, r, paint)

            if (b.id == POWER && player.powerCd > 0f) {
                val frac = player.powerCd / player.def.powerCooldown
                paint.style = Paint.Style.FILL
                paint.color = Color4.black(150)
                rect.set(x - r, y - r, x + r, y + r)
                c.drawArc(rect, -90f, 360f * frac, true, paint)
            }

            paint.style = Paint.Style.FILL
            paint.color = if (down[b.id]) Color4.black(255) else Color4.white(255)
            paint.textSize = (if (b.id >= ZOOM_IN) 30f else 20f) * s
            val label = if (b.id == POWER && player.powerCd > 0f) player.powerCd.toInt().plus(1).toString() else b.label
            c.drawText(label, x, y + paint.textSize * 0.35f, paint)
        }
    }

    /** Tiny colour helpers kept local to avoid allocating Color objects. */
    private object Color4 {
        fun white(a: Int): Int = (a.coerceIn(0, 255) shl 24) or 0x00FFFFFF
        fun black(a: Int): Int = (a.coerceIn(0, 255) shl 24)
    }
}
