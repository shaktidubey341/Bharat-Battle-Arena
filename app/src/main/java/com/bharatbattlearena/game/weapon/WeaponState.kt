package com.bharatbattlearena.game.weapon

/** A weapon instance carried by a fighter (ammo + reload progress). */
class WeaponState(val type: WeaponType) {
    var mag: Int = type.mag
    /** Spare ammo; -1 means infinite. */
    var reserve: Int = if (type.reserveMags < 0) -1 else type.reserveMags * type.mag
    var reloadT: Float = 0f

    val hasAmmo: Boolean get() = mag > 0 || reserve != 0

    fun canReload(): Boolean = reloadT <= 0f && mag < type.mag && reserve != 0

    fun finishReload() {
        val need = type.mag - mag
        if (reserve < 0) {
            mag = type.mag
        } else {
            val take = minOf(need, reserve)
            mag += take
            reserve -= take
        }
    }
}
