package com.bharatbattlearena.game.util

import android.content.Context
import android.content.SharedPreferences

/** Thin typed wrapper over SharedPreferences. All values have safe defaults. */
class Prefs(context: Context) {
    private val sp: SharedPreferences =
        context.applicationContext.getSharedPreferences("bba_prefs", Context.MODE_PRIVATE)

    private fun getI(k: String, d: Int) = sp.getInt(k, d)
    private fun putI(k: String, v: Int) = sp.edit().putInt(k, v).apply()
    private fun getF(k: String, d: Float) = sp.getFloat(k, d)
    private fun putF(k: String, v: Float) = sp.edit().putFloat(k, v).apply()

    var character: Int
        get() = getI("character", 0)
        set(v) = putI("character", v)
    var mapId: String
        get() = sp.getString("mapId", "up") ?: "up"
        set(v) = sp.edit().putString("mapId", v).apply()
    var durationMin: Int
        get() = getI("durationMin", 10)
        set(v) = putI("durationMin", v)
    var difficulty: Int
        get() = getI("difficulty", 1)
        set(v) = putI("difficulty", v)
    var bots: Int
        get() = getI("bots", 5)
        set(v) = putI("bots", v)

    var btnScale: Float
        get() = getF("btnScale", 1f)
        set(v) = putF("btnScale", v)
    var opacity: Float
        get() = getF("opacity", 0.6f)
        set(v) = putF("opacity", v)
    var joyScale: Float
        get() = getF("joyScale", 1f)
        set(v) = putF("joyScale", v)
    var sensitivity: Float
        get() = getF("sensitivity", 1.1f)
        set(v) = putF("sensitivity", v)

    var sfxVolume: Float
        get() = getF("sfxVolume", 0.8f)
        set(v) = putF("sfxVolume", v)
    var musicVolume: Float
        get() = getF("musicVolume", 0.6f)
        set(v) = putF("musicVolume", v)

    /** 0 = low, 1 = medium, 2 = high (particle density). */
    var quality: Int
        get() = getI("quality", 2)
        set(v) = putI("quality", v)
    var fps: Int
        get() = getI("fps", 60)
        set(v) = putI("fps", v)
}
