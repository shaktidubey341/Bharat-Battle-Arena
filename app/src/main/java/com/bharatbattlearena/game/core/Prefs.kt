package com.bharatbattlearena.game.core

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    private var sp: SharedPreferences? = null
    var character = 0
    var map = 0
    var mode = 0          // 0 free for all, 1 team battle
    var diff = 1          // 0 easy 1 normal 2 hard
    var size = 6          // fighters in match (incl. player)
    var minutes = 5
    var ctrlScale = 1.0f
    var ctrlAlpha = 0.75f
    var zoom = 1.0f
    var fps30 = false

    fun init(ctx: Context) {
        val p = ctx.getSharedPreferences("bharat_battle_arena", Context.MODE_PRIVATE)
        sp = p
        character = p.getInt("character", 0)
        map = p.getInt("map", 0)
        mode = p.getInt("mode", 0)
        diff = p.getInt("diff", 1)
        size = p.getInt("size", 6)
        minutes = p.getInt("minutes", 5)
        ctrlScale = p.getFloat("ctrlScale", 1.0f)
        ctrlAlpha = p.getFloat("ctrlAlpha", 0.75f)
        zoom = p.getFloat("zoom", 1.0f)
        fps30 = p.getBoolean("fps30", false)
    }

    fun save() {
        val p = sp ?: return
        p.edit()
            .putInt("character", character).putInt("map", map).putInt("mode", mode)
            .putInt("diff", diff).putInt("size", size).putInt("minutes", minutes)
            .putFloat("ctrlScale", ctrlScale).putFloat("ctrlAlpha", ctrlAlpha)
            .putFloat("zoom", zoom).putBoolean("fps30", fps30)
            .apply()
    }
}
