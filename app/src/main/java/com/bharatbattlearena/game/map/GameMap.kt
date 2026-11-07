package com.bharatbattlearena.game.map

import android.graphics.RectF

enum class SolidKind { GROUND, BUILDING, PLATFORM, BRIDGE, CRATE }

/** A collidable rectangle. One-way solids can be jumped up through and dropped through. */
class Solid(val rect: RectF, val oneWay: Boolean, val kind: SolidKind, val tint: Int = 0)

/** [y] is the foot position (standing surface). */
class SpawnPoint(val x: Float, val y: Float)

class PickupSpot(val x: Float, val y: Float)

class GameMap(
    val id: String,
    val name: String,
    val width: Float,
    val height: Float,
    val groundY: Float,
    val skyTop: Int,
    val skyBottom: Int,
    val buildingColors: IntArray,
    val accent: Int,
    val solids: Array<Solid>,
    val spawns: Array<SpawnPoint>,
    val pickups: Array<PickupSpot>
)

class MapInfo(
    val id: String,
    val name: String,
    val description: String,
    val sizeText: String,
    val difficultyText: String,
    val skyTop: Int,
    val skyBottom: Int,
    val available: Boolean
)
