package com.bharatbattlearena.game.util

const val PI_F = 3.1415927f

fun clampF(v: Float, lo: Float, hi: Float): Float = if (v < lo) lo else if (v > hi) hi else v

/** Moves [cur] toward [target] by at most [delta]. */
fun approach(cur: Float, target: Float, delta: Float): Float =
    if (cur < target) minOf(cur + delta, target) else maxOf(cur - delta, target)

fun withAlpha(color: Int, alpha: Int): Int =
    (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

/** Blend [color] toward white by [t] (0..1). */
fun blendWhite(color: Int, t: Float): Int {
    val k = clampF(t, 0f, 1f)
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val nr = (r + (255 - r) * k).toInt()
    val ng = (g + (255 - g) * k).toInt()
    val nb = (b + (255 - b) * k).toInt()
    return (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
}

/** Multiply the RGB channels of [color] by [f]. */
fun shade(color: Int, f: Float): Int {
    val r = (((color shr 16) and 0xFF) * f).toInt().coerceIn(0, 255)
    val g = (((color shr 8) and 0xFF) * f).toInt().coerceIn(0, 255)
    val b = ((color and 0xFF) * f).toInt().coerceIn(0, 255)
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}

/** Linear blend of two opaque colours. */
fun mixColor(a: Int, b: Int, t: Float): Int {
    val k = clampF(t, 0f, 1f)
    val r = (((a shr 16) and 0xFF) * (1 - k) + ((b shr 16) and 0xFF) * k).toInt()
    val g = (((a shr 8) and 0xFF) * (1 - k) + ((b shr 8) and 0xFF) * k).toInt()
    val bl = ((a and 0xFF) * (1 - k) + (b and 0xFF) * k).toInt()
    return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
}
