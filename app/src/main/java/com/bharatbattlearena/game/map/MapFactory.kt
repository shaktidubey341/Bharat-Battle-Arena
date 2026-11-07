package com.bharatbattlearena.game.map

import android.graphics.Color
import android.graphics.RectF
import java.util.Random

/**
 * Builds maps procedurally from a fixed seed, so layouts are identical every run
 * (important later for LAN: host and clients can build the same map from its id).
 *
 * Stage 1 ships Uttar Pradesh only. The other four are listed so the map select
 * screen is complete, but are marked unavailable until Stage 2.
 */
object MapFactory {

    val infos: List<MapInfo> = listOf(
        MapInfo(
            "up", "Uttar Pradesh",
            "Rooftop lanes, fire-escape stairs and tight alleys of a fictional Indian town.",
            "Large - 9600 x 1500", "Medium",
            Color.rgb(255, 170, 90), Color.rgb(255, 228, 175), true
        ),
        MapInfo(
            "delhi", "Delhi",
            "Glass towers, wide roads and elevated metro-style lines.",
            "Large", "Hard",
            Color.rgb(90, 140, 220), Color.rgb(200, 225, 250), false
        ),
        MapInfo(
            "mumbai", "Mumbai",
            "Dense city blocks, narrow streets and a fictional waterfront.",
            "Large", "Hard",
            Color.rgb(70, 110, 150), Color.rgb(240, 190, 150), false
        ),
        MapInfo(
            "bihar", "Bihar",
            "Village rooftops, open fields, trees and dirt roads.",
            "Large", "Easy",
            Color.rgb(120, 190, 120), Color.rgb(250, 235, 170), false
        ),
        MapInfo(
            "varanasi", "Varanasi",
            "A fictional riverside of ghats, stairs, narrow lanes and rooftops.",
            "Large", "Medium",
            Color.rgb(200, 120, 90), Color.rgb(255, 215, 160), false
        )
    )

    fun isAvailable(id: String): Boolean = infos.firstOrNull { it.id == id }?.available == true

    fun build(id: String): GameMap = buildUttarPradesh()

    private fun buildUttarPradesh(): GameMap {
        val rnd = Random(2024L)
        val worldW = 9600f
        val groundY = 1300f
        val worldH = 1500f
        val step = 160f // vertical distance between floors; a jump clears ~226

        val solids = ArrayList<Solid>()
        val spawns = ArrayList<SpawnPoint>()
        val pickups = ArrayList<PickupSpot>()

        // Road + invisible side walls
        solids.add(Solid(RectF(0f, groundY, worldW, worldH), false, SolidKind.GROUND))
        solids.add(Solid(RectF(-200f, 0f, 0f, worldH), false, SolidKind.BUILDING))
        solids.add(Solid(RectF(worldW, 0f, worldW + 200f, worldH), false, SolidKind.BUILDING))

        // Starting plaza
        spawns.add(SpawnPoint(130f, groundY))
        spawns.add(SpawnPoint(300f, groundY))
        pickups.add(PickupSpot(220f, groundY))
        solids.add(Solid(RectF(380f, groundY - 60f, 440f, groundY), false, SolidKind.CRATE))

        var prevRight = 440f
        var prevLevel = 1
        var idx = 0
        while (prevRight + 220f + 480f < worldW - 1000f) {
            val gap = 220f + rnd.nextInt(3) * 40f
            val x = prevRight + gap
            val bw = 320f + rnd.nextInt(5) * 40f
            val level = if (idx > 0 && rnd.nextFloat() < 0.4f) prevLevel else 1 + rnd.nextInt(3)
            val roofY = groundY - level * step

            // Building block
            solids.add(Solid(RectF(x, roofY, x + bw, groundY), false, SolidKind.BUILDING, idx))

            // Fire-escape style stair platforms in the alley on the building's left
            for (i in 1 until level) {
                val py = groundY - i * step
                solids.add(Solid(RectF(x - 120f, py, x - 8f, py + 14f), true, SolidKind.PLATFORM))
            }

            // Rooftop bridge when neighbouring roofs are the same height
            if (idx > 0 && prevLevel == level) {
                solids.add(Solid(RectF(prevRight, roofY, x, roofY + 12f), true, SolidKind.BRIDGE))
            }

            // Street clutter: a crate near the left side of the alley
            if (rnd.nextFloat() < 0.6f) {
                solids.add(
                    Solid(RectF(prevRight + 30f, groundY - 60f, prevRight + 90f, groundY), false, SolidKind.CRATE)
                )
            }

            // Floating rooftop platform (water tank / billboard)
            if (level < 3 && rnd.nextFloat() < 0.6f) {
                solids.add(
                    Solid(RectF(x + 80f, roofY - step, x + bw - 80f, roofY - step + 14f), true, SolidKind.PLATFORM)
                )
                pickups.add(PickupSpot(x + bw * 0.5f, roofY - step))
            }

            // Spawns and pickups
            spawns.add(SpawnPoint(x + bw * 0.3f, roofY))
            spawns.add(SpawnPoint(x + bw * 0.7f, roofY))
            spawns.add(SpawnPoint(prevRight + gap * 0.5f, groundY))
            pickups.add(PickupSpot(x + bw * 0.5f, roofY))
            if (rnd.nextFloat() < 0.5f) pickups.add(PickupSpot(prevRight + gap * 0.5f, groundY))

            prevRight = x + bw
            prevLevel = level
            idx++
        }

        // Elevated roadway near the end of the map
        val ex = worldW - 700f
        solids.add(Solid(RectF(ex - 130f, groundY - step, ex - 10f, groundY - step + 14f), true, SolidKind.PLATFORM))
        solids.add(Solid(RectF(ex, groundY - 2 * step, worldW - 120f, groundY - 2 * step + 16f), true, SolidKind.BRIDGE))
        spawns.add(SpawnPoint(ex + 250f, groundY - 2 * step))
        spawns.add(SpawnPoint(ex + 500f, groundY - 2 * step))
        pickups.add(PickupSpot(ex + 380f, groundY - 2 * step))

        // Final open ground
        spawns.add(SpawnPoint(prevRight + 300f, groundY))
        spawns.add(SpawnPoint(worldW - 200f, groundY))
        pickups.add(PickupSpot(prevRight + 450f, groundY))
        solids.add(Solid(RectF(prevRight + 600f, groundY - 60f, prevRight + 660f, groundY), false, SolidKind.CRATE))

        val palette = intArrayOf(
            Color.rgb(196, 130, 100), Color.rgb(214, 172, 112), Color.rgb(156, 112, 102),
            Color.rgb(184, 152, 132), Color.rgb(140, 160, 170)
        )
        return GameMap(
            "up", "Uttar Pradesh", worldW, worldH, groundY,
            Color.rgb(255, 170, 90), Color.rgb(255, 228, 175),
            palette, Color.rgb(255, 140, 30),
            solids.toTypedArray(), spawns.toTypedArray(), pickups.toTypedArray()
        )
    }
}
