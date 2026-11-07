package com.bharatbattlearena.game.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import com.bharatbattlearena.game.map.GameMap
import com.bharatbattlearena.game.map.MapFactory
import com.bharatbattlearena.game.map.MapInfo
import com.bharatbattlearena.game.map.SolidKind
import com.bharatbattlearena.game.util.shade
import java.util.Random

/** Procedural map thumbnail. Playable maps draw their real layout; others a themed skyline. */
class MapPreviewView(context: Context, private val info: MapInfo) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val map: GameMap? = if (info.available) MapFactory.build(info.id) else null

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(0f, 0f, 0f, h, info.skyTop, info.skyBottom, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        if (map != null) {
            val sx = w / map.width
            val visibleH = 900f
            val sy = h / visibleH
            val offY = map.groundY - visibleH + 100f
            for (s in map.solids) {
                if (s.rect.right < 0f || s.rect.left > map.width) continue
                paint.color = when (s.kind) {
                    SolidKind.GROUND -> Color.rgb(62, 56, 62)
                    SolidKind.BUILDING -> map.buildingColors[s.tint.coerceAtLeast(0) % map.buildingColors.size]
                    SolidKind.PLATFORM -> Color.rgb(120, 82, 52)
                    SolidKind.BRIDGE -> Color.rgb(90, 98, 110)
                    SolidKind.CRATE -> Color.rgb(150, 110, 60)
                }
                canvas.drawRect(
                    s.rect.left * sx, (s.rect.top - offY) * sy,
                    s.rect.right * sx, (s.rect.bottom - offY) * sy, paint
                )
            }
        } else {
            val r = Random(info.id.hashCode().toLong())
            var x = 0f
            val base = shade(info.skyTop, 0.45f)
            while (x < w) {
                val bw = w * (0.06f + r.nextFloat() * 0.07f)
                val bh = h * (0.25f + r.nextFloat() * 0.45f)
                paint.color = base
                canvas.drawRect(x, h - bh, x + bw, h, paint)
                x += bw + 3f
            }
            paint.color = Color.argb(150, 0, 0, 0)
            canvas.drawRect(0f, h * 0.9f, w, h, paint)
        }
    }
}
