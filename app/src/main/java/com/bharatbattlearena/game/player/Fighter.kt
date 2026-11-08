package com.bharatbattlearena.game.player

import com.bharatbattlearena.game.weapon.WeaponState

class Fighter(val id: Int, val name: String, val charIdx: Int, var team: Int, val isBot: Boolean) {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var face = 1
    var aim = 0f
    var aimActive = false
    var hp = 100f
    var alive = false
    var respawnT = 0f
    var invuln = 0f
    var onGround = false
    var platSolid = true
    var fuel = 100f
    var jetting = false
    var dropT = 0f
    var crouch = false
    val weapons = arrayOfNulls<WeaponState>(2)
    var cur = 0
    val grenades = intArrayOf(2, 1, 1)
    var gType = 0
    var mines = 1
    var kills = 0
    var deaths = 0
    var fireCd = 0f
    var reloadT = 0f
    var meleeT = 0f
    var meleeCd = 0f
    var hitT = 0f
    var flashed = 0f
    var muzzleT = 0f
    var dieT = 0f
    var animT = 0f
    var lastHit = -1
    var lastHitAge = 99f

    // input (set by touch controls or the bot brain)
    var moveX = 0f
    var wantJump = false
    var wantJet = false
    var wantDown = false
    var wantFire = false
    var wantReload = false
    var wantGrenade = false
    var wantMine = false
    var wantMelee = false
    var wantSwitch = false

    val weapon: WeaponState? get() = weapons[cur]
}
