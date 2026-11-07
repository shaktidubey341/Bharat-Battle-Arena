package com.bharatbattlearena.game.collision

import android.graphics.RectF
import com.bharatbattlearena.game.map.GameMap
import com.bharatbattlearena.game.player.Fighter
import kotlin.math.hypot

/** Axis-separated AABB collision between fighters and the map's solids. */
object Collision {

    private fun overlaps(l: Float, t: Float, r: Float, b: Float, rc: RectF): Boolean =
        l < rc.right && r > rc.left && t < rc.bottom && b > rc.top

    fun moveFighter(f: Fighter, map: GameMap, dt: Float) {
        val hw = f.width * 0.5f
        val h = f.height
        val prevBottom = f.y
        val prevTop = prevBottom - h
        val solids = map.solids

        // Horizontal
        f.x += f.vx * dt
        for (s in solids) {
            if (s.oneWay) continue
            val rc = s.rect
            if (!overlaps(f.x - hw, f.y - h, f.x + hw, f.y, rc)) continue
            if (f.y - rc.top < 3f) continue // standing on top, not blocked
            if (f.vx > 0f) f.x = rc.left - hw else if (f.vx < 0f) f.x = rc.right + hw
            f.vx = 0f
        }
        if (f.x < hw) {
            f.x = hw
            f.vx = 0f
        }
        if (f.x > map.width - hw) {
            f.x = map.width - hw
            f.vx = 0f
        }

        // Vertical
        f.y += f.vy * dt
        f.onGround = false
        f.onOneWay = false
        for (s in solids) {
            val rc = s.rect
            if (!overlaps(f.x - hw, f.y - h, f.x + hw, f.y, rc)) continue
            if (s.oneWay) {
                if (f.vy >= 0f && prevBottom <= rc.top + 2f && f.dropT <= 0f) {
                    f.y = rc.top
                    f.vy = 0f
                    f.onGround = true
                    f.onOneWay = true
                }
            } else {
                if (f.vy >= 0f && prevBottom <= rc.top + 4f) {
                    f.y = rc.top
                    f.vy = 0f
                    f.onGround = true
                } else if (f.vy < 0f && prevTop >= rc.bottom - 4f) {
                    f.y = rc.bottom + h
                    f.vy = 0f
                }
            }
        }
        if (f.y > map.height) {
            f.y = map.height
            f.vy = 0f
            f.onGround = true
        }
    }

    /** True if the fighter has room to stand up from a crouch. */
    fun canStand(f: Fighter, map: GameMap): Boolean {
        val hw = f.width * 0.5f
        for (s in map.solids) {
            if (s.oneWay) continue
            if (overlaps(f.x - hw, f.y - 92f, f.x + hw, f.y - 62f, s.rect)) return false
        }
        return true
    }

    /** Line of sight test against solid (non one-way) geometry. */
    fun lineClear(map: GameMap, x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        val dx = x2 - x1
        val dy = y2 - y1
        val dist = hypot(dx, dy)
        val steps = (dist / 24f).toInt() + 1
        for (i in 1 until steps) {
            val t = i.toFloat() / steps
            val px = x1 + dx * t
            val py = y1 + dy * t
            for (s in map.solids) {
                if (!s.oneWay && s.rect.contains(px, py)) return false
            }
        }
        return true
    }
}
