package com.bharatbattlearena.game.combat

class Bullet {
    var active = false
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var life = 0f
    var dmg = 0f
    var owner = 0
    var team = 0
    var rocket = false
}

/** type 0 = frag, 1 = flash, 2 = smoke */
class Grenade(val type: Int, var x: Float, var y: Float, var vx: Float, var vy: Float, var fuse: Float, val owner: Int, val team: Int) {
    var rot = 0f
    var alive = true
}

class Mine(val x: Float, val y: Float, val owner: Int, val team: Int) {
    var arm = 1.0f
    var alive = true
    var t = 0f
}

class Smoke(val x: Float, val y: Float) {
    var age = 0f
    val life = 7f
    val r = 140f
}

/** kind: 0 explosion, 1 spark, 2 flash-grenade burst, 3 dust puff, 4 respawn ring, 5 debris puff */
class Fx(val kind: Int, var x: Float, var y: Float, val life: Float, val size: Float) {
    var age = 0f
    var vx = 0f
    var vy = 0f
}

/** type: 0 health, 1 ammo, 2 grenades, 3 mine, 4 weapon */
class Pickup(var x: Float, var y: Float) {
    var type = 0
    var wId = 1
    var active = true
    var respawn = 0f
    var bob = 0f
    var dropped = false
    var life = 20f
}

/** kind: 0 crate, 1 explosive barrel */
class Prop(val kind: Int, val x: Float, val y: Float) {
    var hp = if (kind == 0) 40f else 30f
    var alive = true
    var respawn = 0f
    val halfW: Float get() = if (kind == 0) 28f else 20f
    val height: Float get() = if (kind == 0) 56f else 60f
    var lastOwner = -1
    var lastTeam = -1
}
