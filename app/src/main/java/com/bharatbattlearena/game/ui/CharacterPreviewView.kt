package com.bharatbattlearena.game.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import com.bharatbattlearena.game.player.Anim
import com.bharatbattlearena.game.player.CharacterDef
import com.bharatbattlearena.game.player.FighterRenderer
import com.bharatbattlearena.game.util.withAlpha
import com.bharatbattlearena.game.weapon.WeaponType

/** Animated idle preview of a character, drawn with the same renderer used in matches. */
class CharacterPreviewView(context: Context, private val def: CharacterDef) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val renderer = FighterRenderer()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.color = withAlpha(def.primary, 60)
        canvas.drawCircle(w / 2f, h * 0.55f, h * 0.42f, paint)
        paint.color = withAlpha(Color.WHITE, 40)
        canvas.drawRect(w * 0.15f, h * 0.9f, w * 0.85f, h * 0.92f, paint)
        val t = SystemClock.uptimeMillis() / 1000f
        renderer.draw(
            canvas, paint, def, w / 2f - 8f, h * 0.9f, 92f, 1, Anim.IDLE, t, 0f,
            WeaponType.RIFLE, 0f, 255, false, h / 130f
        )
        postInvalidateOnAnimation()
    }
}
