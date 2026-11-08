package com.bharatbattlearena.game.map

/** One walkable surface. Players land on top of it from above (one-way). solid = cannot drop through. */
class Platform(val x0: Float, val x1: Float, val y: Float, val visible: Boolean = false, val solid: Boolean = false)

class Pt(val x: Float, val y: Float)

class MapDef(
    val id: Int,
    val name: String,
    val desc: String,
    val file: String,
    val w: Float,
    val h: Float,
    val flat: Boolean,
    val platforms: Array<Platform>,
    val spawns: Array<Pt>,
    val pickupSpots: Array<Pt>,
    val crates: Array<Pt>,
    val barrels: Array<Pt>
) {
    val sizeText: String get() = "${w.toInt()} x ${h.toInt()}"
    val difficultyText: String get() = if (flat) "Easy - open plaza" else "Medium - multi-level arena"

    /**
     * Returns t in 0..1 where a segment moving downward crosses a platform top, or -1 when it does not.
     */
    fun platformHitT(x0: Float, y0: Float, x1: Float, y1: Float): Float {
        if (y1 <= y0) return -1f
        var bestT = 2f
        for (p in platforms) {
            if (p.y >= y0 && p.y <= y1) {
                val t = (p.y - y0) / (y1 - y0)
                val x = x0 + (x1 - x0) * t
                if (x >= p.x0 && x <= p.x1 && t < bestT) bestT = t
            }
        }
        return if (bestT <= 1f) bestT else -1f
    }
}

object Maps {
    val all: Array<MapDef> by lazy { build() }

    private fun pts(w: Float, h: Float, vararg uv: Float): Array<Pt> =
        Array(uv.size / 2) { Pt(uv[it * 2] * w, uv[it * 2 + 1] * h) }

    private fun arena(id: Int, name: String, desc: String, file: String, topV: Float): MapDef {
        val w = 2500f
        val h = 1000f
        // x0, x1, y (normalized to the artwork) - measured against the supplied map art
        val raw = arrayOf(
            floatArrayOf(0f, 0.41f, 0.835f),      // left ground
            floatArrayOf(0.62f, 1f, 0.835f),      // right ground
            floatArrayOf(0.43f, 0.58f, 0.805f),   // river bridge
            floatArrayOf(0.22f, 0.30f, 0.78f),    // left stair ledge
            floatArrayOf(0.70f, 0.78f, 0.78f),    // right stair ledge
            floatArrayOf(0f, 0.27f, 0.605f),      // left tier 2
            floatArrayOf(0.73f, 1f, 0.605f),      // right tier 2
            floatArrayOf(0f, 0.28f, 0.40f),       // left upper
            floatArrayOf(0.72f, 1f, 0.40f),       // right upper
            floatArrayOf(0.33f, 0.42f, 0.50f),    // left floating island
            floatArrayOf(0.60f, 0.69f, 0.50f),    // right floating island
            floatArrayOf(0.43f, 0.59f, 0.60f),    // centre lower island
            floatArrayOf(0.40f, 0.60f, topV)      // top centre island
        )
        val solidIdx = setOf(0, 1, 2)
        val plats = Array(raw.size) { Platform(raw[it][0] * w, raw[it][1] * w, raw[it][2] * h, false, solidIdx.contains(it)) }
        val spawns = pts(w, h,
            0.12f, 0.835f, 0.30f, 0.835f, 0.70f, 0.835f, 0.88f, 0.835f,
            0.10f, 0.605f, 0.22f, 0.605f, 0.78f, 0.605f, 0.90f, 0.605f,
            0.08f, 0.40f, 0.22f, 0.40f, 0.78f, 0.40f, 0.92f, 0.40f,
            0.50f, topV, 0.50f, 0.60f, 0.37f, 0.50f, 0.65f, 0.50f)
        val pick = pts(w, h,
            0.20f, 0.835f, 0.80f, 0.835f, 0.50f, 0.805f, 0.15f, 0.605f, 0.85f, 0.605f,
            0.15f, 0.40f, 0.85f, 0.40f, 0.50f, topV, 0.50f, 0.60f, 0.37f, 0.50f, 0.65f, 0.50f)
        val crates = pts(w, h, 0.06f, 0.835f, 0.35f, 0.835f, 0.65f, 0.835f, 0.94f, 0.835f)
        val barrels = pts(w, h, 0.25f, 0.835f, 0.75f, 0.835f)
        return MapDef(id, name, desc, file, w, h, false, plats, spawns, pick, crates, barrels)
    }

    private fun plaza(id: Int, name: String, desc: String, file: String): MapDef {
        val w = 1777f
        val h = 1000f
        val fl = 0.78f
        val plats = arrayOf(
            Platform(0f, w, fl * h, false, true),
            Platform(0.10f * w, 0.30f * w, 0.57f * h, true, false),
            Platform(0.70f * w, 0.90f * w, 0.57f * h, true, false),
            Platform(0.38f * w, 0.62f * w, 0.40f * h, true, false)
        )
        val spawns = pts(w, h,
            0.15f, fl, 0.33f, fl, 0.85f, fl, 0.67f, fl, 0.50f, fl,
            0.20f, 0.57f, 0.80f, 0.57f, 0.50f, 0.40f)
        val pick = pts(w, h, 0.22f, fl, 0.78f, fl, 0.20f, 0.57f, 0.80f, 0.57f, 0.50f, 0.40f, 0.50f, fl)
        val crates = pts(w, h, 0.07f, fl, 0.93f, fl)
        val barrels = pts(w, h, 0.40f, fl, 0.60f, fl)
        return MapDef(id, name, desc, file, w, h, true, plats, spawns, pick, crates, barrels)
    }

    private fun build(): Array<MapDef> = arrayOf(
        arena(0, "Bardha (UP)", "Village hill-fort arena with a river bridge, towers and a temple.", "maps/bardha.jpg", 0.325f),
        arena(1, "Varanasi Sunset", "Ghats at sunset. Floating islands over the Ganga.", "maps/varanasi_sunset.jpg", 0.33f),
        arena(2, "Varanasi Night", "Moonlit Kashi with stairs, temples and a bridge.", "maps/varanasi_night.jpg", 0.30f),
        arena(3, "New Delhi", "Monuments, metro and a river crossing.", "maps/new_delhi.jpg", 0.325f),
        arena(4, "Delhi Red Fort", "Red Fort walls, rail line and a steel bridge.", "maps/red_fort.jpg", 0.325f),
        plaza(5, "UP Plaza", "Lucknow-style plaza. Wide flat arena with planks.", "maps/up_plaza.jpg"),
        plaza(6, "Varanasi Ghat", "Sunset ghat promenade. Fast duels.", "maps/varanasi_ghat.jpg"),
        plaza(7, "Delhi India Gate", "Kartavya Path plaza. Open fight.", "maps/india_gate.jpg")
    )
}
