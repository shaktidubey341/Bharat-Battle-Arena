package com.bharatbattlearena.game.audio

enum class Sfx {
    MENU_CLICK, SHOT, RELOAD, JUMP, LAND, HIT, MELEE, EXPLOSION,
    POWER, DEATH, RESPAWN, COUNTDOWN, VICTORY, DEFEAT
}

/** Implement this to plug real audio (SoundPool, MediaPlayer...) into the game. */
interface SoundBackend {
    fun play(sfx: Sfx, volume: Float)
}

/**
 * Central sound dispatch. Stage 1 ships no audio files, so [backend] is null and
 * every call is a safe no-op. Drop in a backend later without touching gameplay code.
 */
object SoundHub {
    @Volatile
    var backend: SoundBackend? = null

    @Volatile
    var volume: Float = 0.8f

    fun play(sfx: Sfx) {
        try {
            backend?.play(sfx, volume)
        } catch (e: Exception) {
            // Audio must never crash the game.
        }
    }
}
