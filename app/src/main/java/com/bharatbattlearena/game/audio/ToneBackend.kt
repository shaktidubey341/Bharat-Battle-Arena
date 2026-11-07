package com.bharatbattlearena.game.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Audio backend that needs no sound files: each [Sfx] is a short synthesized tone
 * (adapted from the earlier SoundFx table). Every call is guarded, so devices without
 * audio support simply stay silent. Music is a hook only - see [setMusic].
 */
class ToneBackend : SoundBackend {

    private class Tone(val tone: Int, val ms: Int, val minGapMs: Long)

    private val table: Map<Sfx, Tone> = mapOf(
        Sfx.MENU_CLICK to Tone(ToneGenerator.TONE_PROP_BEEP, 40, 80),
        Sfx.SHOT to Tone(ToneGenerator.TONE_DTMF_1, 25, 70),
        Sfx.RELOAD to Tone(ToneGenerator.TONE_PROP_ACK, 60, 150),
        Sfx.JUMP to Tone(ToneGenerator.TONE_DTMF_3, 20, 150),
        Sfx.LAND to Tone(ToneGenerator.TONE_DTMF_0, 20, 200),
        Sfx.HIT to Tone(ToneGenerator.TONE_DTMF_9, 30, 70),
        Sfx.MELEE to Tone(ToneGenerator.TONE_DTMF_4, 40, 120),
        Sfx.EXPLOSION to Tone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 160, 120),
        Sfx.POWER to Tone(ToneGenerator.TONE_CDMA_CONFIRM, 150, 200),
        Sfx.DEATH to Tone(ToneGenerator.TONE_PROP_NACK, 250, 200),
        Sfx.RESPAWN to Tone(ToneGenerator.TONE_PROP_ACK, 120, 200),
        Sfx.COUNTDOWN to Tone(ToneGenerator.TONE_CDMA_PIP, 120, 300),
        Sfx.VICTORY to Tone(ToneGenerator.TONE_CDMA_CONFIRM, 500, 500),
        Sfx.DEFEAT to Tone(ToneGenerator.TONE_PROP_NACK, 500, 500)
    )

    private var gen: ToneGenerator? = null
    private var level = -1f
    private val last = LongArray(Sfx.values().size)

    /** Music hook: start/stop a MediaPlayer here once music files exist in res/raw. */
    @Suppress("unused")
    fun setMusic(menu: Boolean) {
    }

    fun setVolume(v: Float) {
        if (v == level) return
        level = v
        release()
        if (v > 0.01f) {
            try {
                gen = ToneGenerator(AudioManager.STREAM_MUSIC, (v * 100f).toInt().coerceIn(1, 100))
            } catch (e: Exception) {
                gen = null
            }
        }
    }

    override fun play(sfx: Sfx, volume: Float) {
        val g = gen ?: return
        val t = table[sfx] ?: return
        val now = System.currentTimeMillis()
        if (now - last[sfx.ordinal] < t.minGapMs) return
        last[sfx.ordinal] = now
        try {
            g.startTone(t.tone, t.ms)
        } catch (e: Exception) {
            // never crash on audio
        }
    }

    fun release() {
        try {
            gen?.release()
        } catch (e: Exception) {
            // ignore
        }
        gen = null
    }
}
