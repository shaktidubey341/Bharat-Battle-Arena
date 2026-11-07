package com.bharatbattlearena.game.weapon

import com.bharatbattlearena.game.player.Fighter

class Bullet {
    var active = false
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var damage = 0f
    var life = 0f
    var owner: Fighter? = null
    var type: WeaponType = WeaponType.PISTOL
}

/** Fixed-size pool so firing never allocates. Oldest bullet is recycled when full. */
class BulletPool(size: Int) {
    val items: Array<Bullet> = Array(size) { Bullet() }
    private var cursor = 0

    fun spawn(
        owner: Fighter, x: Float, y: Float, vx: Float, vy: Float,
        damage: Float, life: Float, type: WeaponType
    ) {
        var b: Bullet? = null
        for (i in items.indices) {
            val c = items[(cursor + i) % items.size]
            if (!c.active) {
                b = c
                cursor = (cursor + i + 1) % items.size
                break
            }
        }
        if (b == null) {
            b = items[cursor]
            cursor = (cursor + 1) % items.size
        }
        b.active = true
        b.owner = owner
        b.x = x
        b.y = y
        b.vx = vx
        b.vy = vy
        b.damage = damage
        b.life = life
        b.type = type
    }
}

/** A weapon pickup location. [type] is null while on cooldown. */
class PickupState(val x: Float, val y: Float, var type: WeaponType?) {
    var timer = 0f
}
