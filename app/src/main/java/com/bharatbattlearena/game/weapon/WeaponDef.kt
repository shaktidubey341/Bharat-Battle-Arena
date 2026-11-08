package com.bharatbattlearena.game.weapon

class WeaponDef(
    val id: Int, val name: String, val file: String,
    val damage: Float, val delay: Float, val mag: Int, val reserve: Int, val reload: Float,
    val speed: Float, val life: Float, val spread: Float, val pellets: Int,
    val recoil: Float, val length: Float, val rocket: Boolean, val prefRange: Float
) {
    val range: Float get() = speed * life
}

class WeaponState(val def: WeaponDef) {
    var ammo: Int = def.mag
    var reserve: Int = def.reserve
}

object Weapons {
    val PISTOL = WeaponDef(0, "Pistol", "pistol", 14f, 0.26f, 12, 60, 1.0f, 1500f, 0.7f, 0.025f, 1, 40f, 34f, false, 260f)
    val SMG = WeaponDef(1, "SMG", "smg", 8f, 0.08f, 30, 120, 1.5f, 1600f, 0.6f, 0.07f, 1, 25f, 48f, false, 220f)
    val RIFLE = WeaponDef(2, "Assault Rifle", "rifle", 11f, 0.11f, 30, 120, 1.8f, 1800f, 0.8f, 0.035f, 1, 30f, 64f, false, 380f)
    val SHOTGUN = WeaponDef(3, "Shotgun", "shotgun", 9f, 0.8f, 6, 30, 2.0f, 1300f, 0.38f, 0.14f, 7, 160f, 66f, false, 160f)
    val SNIPER = WeaponDef(4, "Sniper", "sniper", 70f, 1.3f, 5, 20, 2.4f, 3400f, 1.0f, 0.004f, 1, 220f, 96f, false, 650f)
    val HEAVY = WeaponDef(5, "Heavy MG", "heavy", 10f, 0.09f, 60, 180, 3.2f, 1700f, 0.8f, 0.09f, 1, 45f, 80f, false, 400f)
    val LAUNCHER = WeaponDef(6, "Rocket Launcher", "launcher", 85f, 1.5f, 1, 4, 2.4f, 950f, 2.5f, 0.01f, 1, 260f, 80f, true, 450f)
    val ALL: Array<WeaponDef> = arrayOf(PISTOL, SMG, RIFLE, SHOTGUN, SNIPER, HEAVY, LAUNCHER)
}
