package com.bharatbattlearena.game.player

import com.bharatbattlearena.game.weapon.WeaponState
import com.bharatbattlearena.game.weapon.WeaponType

enum class Anim { IDLE, WALK, RUN, JUMP, FALL, CROUCH, AIM, SHOOT, RELOAD, MELEE, DASH, POWER, HIT, DEATH, RESPAWN }

/**
 * One input frame for a fighter. Written by touch controls (human) or BotBrain (AI),
 * read by FighterController. "Pressed" flags are one-shot edges consumed by the controller.
 */
class InputState {
    @Volatile var moveX = 0f
    @Volatile var aimY = 0f
    @Volatile var shoot = false
    @Volatile var aim = false
    @Volatile var crouch = false
    @Volatile var jumpPressed = false
    @Volatile var reloadPressed = false
    @Volatile var meleePressed = false
    @Volatile var switchPressed = false
    @Volatile var powerPressed = false

    fun reset() {
        moveX = 0f
        aimY = 0f
        shoot = false
        aim = false
        crouch = false
        jumpPressed = false
        reloadPressed = false
        meleePressed = false
        switchPressed = false
        powerPressed = false
    }
}

/** Shared state for humans and bots alike. Bots get no hidden stat advantages. */
class Fighter(val id: Int, val name: String, val def: CharacterDef, val isBot: Boolean) {
    // Position: x = horizontal centre, y = feet.
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var facing = 1
    var onGround = false
    var onOneWay = false
    var crouching = false

    val width = 50f
    val height: Float get() = if (crouching) 62f else 92f

    var hp = def.maxHp
    var armor = 0f
    var alive = false
    var respawnT = 0f
    var kills = 0
    var deaths = 0

    val input = InputState()
    val weapons = ArrayList<WeaponState>(3)
    var cur = 0
    val weapon: WeaponState get() = weapons[cur]
    var aimAngle = 0f
    var extraSpread = 0f

    // Cooldowns / timers (seconds)
    var shootCd = 0f
    var meleeCd = 0f
    var powerCd = 0f
    var slowT = 0f
    var penaltyT = 0f
    var vulnT = 0f
    var burnT = 0f
    var stunT = 0f
    var shieldT = 0f
    var protectT = 0f
    var hitFlash = 0f
    var hitAnimT = 0f
    var meleeAnimT = 0f
    var shootAnimT = 0f
    var powerAnimT = 0f
    var landT = 0f
    var sinceHurt = 99f
    var jumpBuf = 0f
    var coyote = 0f
    var dropT = 0f

    var slowMul = 1f
    var shieldReduce = 0f
    var burnOwner: Fighter? = null

    var anim = Anim.IDLE
    var animT = 0f

    init {
        equipDefault()
    }

    fun equipDefault() {
        weapons.clear()
        weapons.add(WeaponState(WeaponType.PISTOL))
        cur = 0
    }

    fun clearStatus() {
        shootCd = 0f
        meleeCd = 0f
        powerCd = 0f
        slowT = 0f
        penaltyT = 0f
        vulnT = 0f
        burnT = 0f
        stunT = 0f
        shieldT = 0f
        protectT = 0f
        hitFlash = 0f
        hitAnimT = 0f
        meleeAnimT = 0f
        shootAnimT = 0f
        powerAnimT = 0f
        sinceHurt = 99f
        slowMul = 1f
        shieldReduce = 0f
        burnOwner = null
        armor = 0f
    }

    private fun dec(v: Float, dt: Float): Float = if (v > 0f) maxOf(0f, v - dt) else 0f

    fun tick(dt: Float) {
        shootCd = dec(shootCd, dt)
        meleeCd = dec(meleeCd, dt)
        powerCd = dec(powerCd, dt)
        slowT = dec(slowT, dt)
        if (slowT <= 0f) slowMul = 1f
        penaltyT = dec(penaltyT, dt)
        vulnT = dec(vulnT, dt)
        burnT = dec(burnT, dt)
        stunT = dec(stunT, dt)
        shieldT = dec(shieldT, dt)
        protectT = dec(protectT, dt)
        hitFlash = dec(hitFlash, dt)
        hitAnimT = dec(hitAnimT, dt)
        meleeAnimT = dec(meleeAnimT, dt)
        shootAnimT = dec(shootAnimT, dt)
        powerAnimT = dec(powerAnimT, dt)
        landT = dec(landT, dt)
        dropT = dec(dropT, dt)
        sinceHurt += dt
        animT += dt
    }
}
