package com.bharatbattlearena.game.camera

import com.bharatbattlearena.game.map.GameMap
import com.bharatbattlearena.game.util.clampF
import java.util.Random
import kotlin.math.exp
import kotlin.math.max

/**
 * Smooth follow camera with zoom limits, world clamping and shake.
 * World units -> pixels: screenPx = (world - render) * scale.
 */
class Camera2D(private val map: GameMap) {
    var x = 0f
    var y = 0f
    var renderX = 0f
    var renderY = 0f
    var zoom = 1f
    var scale = 1f
    var viewW = 1f
    var viewH = 1f
    val minZoom = 0.7f
    val maxZoom = 1.6f

    private var shakeMag = 0f
    private val rnd = Random()
    private var ready = false

    fun zoomBy(factor: Float) {
        zoom = clampF(zoom * factor, minZoom, maxZoom)
    }

    fun shake(mag: Float) {
        if (mag > shakeMag) shakeMag = mag
    }

    fun update(dt: Float, tx: Float, ty: Float, facing: Int, sw: Int, sh: Int) {
        // The visible height is 900 world units at zoom 1; never zoom out past the map.
        val minScale = max(sw / map.width, sh / map.height)
        scale = max(sh / 900f * zoom, minScale)
        viewW = sw / scale
        viewH = sh / scale

        val maxX = max(0f, map.width - viewW)
        val maxY = max(0f, map.height - viewH)
        val desiredX = clampF(tx + facing * 80f - viewW * 0.5f, 0f, maxX)
        val desiredY = clampF(ty - viewH * 0.55f, 0f, maxY)

        if (!ready) {
            x = desiredX
            y = desiredY
            ready = true
        } else {
            val k = 1f - exp(-8f * dt)
            x += (desiredX - x) * k
            y += (desiredY - y) * k
        }
        x = clampF(x, 0f, maxX)
        y = clampF(y, 0f, maxY)

        shakeMag *= exp(-7f * dt)
        if (shakeMag < 0.3f) shakeMag = 0f
        val sx = (rnd.nextFloat() - 0.5f) * 2f * shakeMag
        val sy = (rnd.nextFloat() - 0.5f) * 2f * shakeMag
        renderX = clampF(x + sx, 0f, maxX)
        renderY = clampF(y + sy, 0f, maxY)
    }
}
