package com.bharatbattlearena.game.core

import android.content.Context
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class CharSprites(val name: String, val anims: HashMap<String, Array<Bitmap>>, val portrait: Bitmap?, val scale: Float)

object Assets {
    private var am: AssetManager? = null
    private val cache = HashMap<String, Bitmap?>()
    private val chars = HashMap<Int, CharSprites>()
    val CHAR_NAMES = arrayOf("Agni Monk", "Tiranga Veer", "Kisan Veer", "Blue Striker", "Golden Smasher")
    const val TARGET_H = 92f

    fun init(ctx: Context) {
        am = ctx.applicationContext.assets
    }

    fun bmp(path: String): Bitmap? {
        if (cache.containsKey(path)) return cache[path]
        var b: Bitmap? = null
        try {
            val a = am
            if (a != null) {
                val s = a.open(path)
                b = BitmapFactory.decodeStream(s)
                s.close()
            }
        } catch (e: Exception) {
            b = null
        }
        cache[path] = b
        return b
    }

    /** Down-sampled image for menu thumbnails (keeps memory low). */
    fun thumb(path: String, sample: Int): Bitmap? {
        val key = "$path@$sample"
        if (cache.containsKey(key)) return cache[key]
        var b: Bitmap? = null
        try {
            val a = am
            if (a != null) {
                val o = BitmapFactory.Options()
                o.inSampleSize = sample
                val s = a.open(path)
                b = BitmapFactory.decodeStream(s, null, o)
                s.close()
            }
        } catch (e: Exception) {
            b = null
        }
        cache[key] = b
        return b
    }

    fun release(path: String) {
        cache.remove(path)
    }

    fun character(idx: Int): CharSprites {
        val have = chars[idx]
        if (have != null) return have
        val dir = "characters/char_%02d".format(idx + 1)
        var names: Array<String> = emptyArray()
        try {
            val l = am?.list(dir)
            if (l != null) names = l
        } catch (e: Exception) {
        }
        val groups = HashMap<String, ArrayList<Pair<Int, Bitmap>>>()
        for (n in names) {
            if (!n.endsWith(".png")) continue
            val base = n.removeSuffix(".png")
            val us = base.lastIndexOf('_')
            if (us < 0) continue
            val act = base.substring(0, us)
            val k = base.substring(us + 1).toIntOrNull() ?: continue
            val b = bmp("$dir/$n") ?: continue
            groups.getOrPut(act) { ArrayList() }.add(Pair(k, b))
        }
        val anims = HashMap<String, Array<Bitmap>>()
        for ((act, list) in groups) {
            list.sortBy { it.first }
            anims[act] = Array(list.size) { list[it].second }
        }
        val idle = anims["idle"]
        val scale = if (idle != null && idle.isNotEmpty()) TARGET_H / idle[0].height else 0.6f
        val cs = CharSprites(CHAR_NAMES[idx % CHAR_NAMES.size], anims, bmp("$dir/portrait.jpg"), scale)
        chars[idx] = cs
        return cs
    }

    fun weapon(file: String): Bitmap? = bmp("weapons/$file.png")
}
