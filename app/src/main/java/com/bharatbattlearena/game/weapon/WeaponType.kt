package com.bharatbattlearena.game.weapon

/**
 * Static weapon stats.
 * damage: per bullet/pellet. interval: seconds between shots. spread: degrees.
 * reserveMags: spare magazines when picked up (-1 = infinite).
 * rarity: relative spawn weight at pickup points (0 = never spawns).
 */
enum class WeaponType(
    val label: String,
    val damage: Float,
    val interval: Float,
    val mag: Int,
    val reload: Float,
    val range: Float,
    val spread: Float,
    val recoil: Float,
    val pellets: Int,
    val bulletSpeed: Float,
    val explosiveRadius: Float,
    val reserveMags: Int,
    val moveMul: Float,
    val rarity: Int,
    val color: Int
) {
    PISTOL("Pistol", 18f, 0.28f, 12, 1.0f, 900f, 1.5f, 0.4f, 1, 2400f, 0f, -1, 1.00f, 0, 0xFFDDDDDD.toInt()),
    SMG("SMG", 9f, 0.075f, 30, 1.6f, 700f, 4.5f, 0.5f, 1, 2200f, 0f, 3, 1.00f, 22, 0xFF7FD6FF.toInt()),
    RIFLE("Assault Rifle", 14f, 0.11f, 30, 1.9f, 1100f, 2.5f, 0.8f, 1, 2600f, 0f, 3, 0.95f, 24, 0xFF9BE37A.toInt()),
    SHOTGUN("Shotgun", 8f, 0.85f, 6, 2.4f, 450f, 9f, 2.0f, 8, 2000f, 0f, 3, 0.95f, 18, 0xFFFFB86B.toInt()),
    SNIPER("Sniper", 70f, 1.25f, 5, 2.6f, 2400f, 0.3f, 2.5f, 1, 4200f, 0f, 3, 0.90f, 10, 0xFFE59BFF.toInt()),
    HEAVY("Heavy Gun", 8f, 0.055f, 100, 3.6f, 900f, 6f, 0.4f, 1, 2300f, 0f, 1, 0.72f, 8, 0xFFFF7A7A.toInt()),
    LAUNCHER("Energy Launcher", 70f, 1.5f, 4, 3.0f, 1600f, 0.5f, 3.0f, 1, 1000f, 170f, 2, 0.90f, 7, 0xFFFFD84A.toInt())
}
