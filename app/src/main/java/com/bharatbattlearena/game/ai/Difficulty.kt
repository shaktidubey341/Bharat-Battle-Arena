package com.bharatbattlearena.game.ai

/**
 * Bot tuning. These only change reaction time and aim quality - bots use the
 * same health, damage and speed as the player (no hidden advantages).
 */
enum class Difficulty(
    val label: String,
    val reaction: Float,
    val firstShotDelay: Float,
    val aimError: Float,
    val extraSpread: Float
) {
    EASY("Easy", 0.45f, 0.70f, 0.12f, 3.5f),
    NORMAL("Normal", 0.25f, 0.35f, 0.06f, 1.5f),
    HARD("Hard", 0.12f, 0.15f, 0.02f, 0f)
}
