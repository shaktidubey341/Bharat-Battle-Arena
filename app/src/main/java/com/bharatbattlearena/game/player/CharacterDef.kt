package com.bharatbattlearena.game.player

import android.graphics.Color

enum class PowerType { SOLAR_SHIELD, TACTICAL_SHIELD, PUSH_WAVE, FIRE_BURST, KNOWLEDGE_DEBUFF, ICE_SLOW }

/**
 * An original fictional fighter. All six share the same base health; each trades a
 * small passive bonus and a unique power so none is strictly stronger.
 */
class CharacterDef(
    val id: Int,
    val name: String,
    val title: String,
    val primary: Int,
    val secondary: Int,
    val skin: Int,
    val headStyle: Int,
    val maxHp: Float,
    val jumpMul: Float,
    val power: PowerType,
    val powerName: String,
    val powerDesc: String,
    val powerCooldown: Float,
    val passiveName: String,
    val passiveDesc: String,
    val regen: Float = 0f,
    val damageTakenMul: Float = 1f,
    val damageDealtMul: Float = 1f,
    val reloadMul: Float = 1f,
    val speedMul: Float = 1f
)

object Characters {
    val all: List<CharacterDef> = listOf(
        CharacterDef(
            0, "Surya Veer", "Sun Guardian",
            Color.rgb(255, 140, 30), Color.rgb(255, 245, 220), Color.rgb(222, 170, 125), 0,
            100f, 1f,
            PowerType.SOLAR_SHIELD, "Solar Energy Shield",
            "A blazing shield absorbs 65% of damage for 4.5s.", 20f,
            "Sun Warmth", "Regenerates 1.5 HP/s after 5s without damage.",
            regen = 1.5f
        ),
        CharacterDef(
            1, "Rakshak Dev", "Iron Defender",
            Color.rgb(40, 70, 120), Color.rgb(200, 205, 215), Color.rgb(205, 150, 110), 1,
            100f, 1f,
            PowerType.TACTICAL_SHIELD, "Tactical Defense Shield",
            "Gain 60 armor and 40% damage reduction for 3s.", 22f,
            "Iron Guard", "Takes 8% less damage.",
            damageTakenMul = 0.92f
        ),
        CharacterDef(
            2, "Vayu Raj", "Wind Runner",
            Color.rgb(110, 195, 230), Color.rgb(255, 255, 255), Color.rgb(230, 185, 140), 2,
            100f, 1.10f,
            PowerType.PUSH_WAVE, "Energy Push Wave",
            "Shockwave knocks back and stuns nearby enemies.", 14f,
            "Gale Step", "Jumps 10% higher."
        ),
        CharacterDef(
            3, "Agni Singh", "Flame Striker",
            Color.rgb(200, 40, 30), Color.rgb(255, 205, 80), Color.rgb(200, 140, 100), 3,
            100f, 1f,
            PowerType.FIRE_BURST, "Fire Burst",
            "Energy blast damages and ignites enemies nearby.", 18f,
            "Hot Blood", "Deals 6% more damage.",
            damageDealtMul = 1.06f
        ),
        CharacterDef(
            4, "Gyan Mitra", "Mind Tactician",
            Color.rgb(95, 60, 150), Color.rgb(240, 200, 60), Color.rgb(215, 165, 125), 4,
            100f, 1f,
            PowerType.KNOWLEDGE_DEBUFF, "Knowledge Debuff",
            "Nearby enemies fumble their weapons and take +25% damage for 5s.", 20f,
            "Quick Learner", "Reloads 20% faster.",
            reloadMul = 0.8f
        ),
        CharacterDef(
            5, "Himani Rao", "Frost Warden",
            Color.rgb(150, 225, 240), Color.rgb(60, 100, 170), Color.rgb(235, 190, 160), 5,
            100f, 1f,
            PowerType.ICE_SLOW, "Ice Wind Slow",
            "A freezing wind slows nearby enemies by 55% for 4s.", 16f,
            "Frost Stride", "Moves 7% faster.",
            speedMul = 1.07f
        )
    )

    fun byId(id: Int): CharacterDef = all[id.coerceIn(0, all.size - 1)]
}
