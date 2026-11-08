package com.bharatbattlearena.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.bharatbattlearena.game.core.Assets
import com.bharatbattlearena.game.core.Prefs
import com.bharatbattlearena.game.map.Maps
import com.bharatbattlearena.game.ui.Hud
import com.bharatbattlearena.game.ui.TouchControls
import com.bharatbattlearena.game.ui.WorldRenderer
import kotlin.math.max
import kotlin.math.min

class GameView(ctx: Context) : SurfaceView(ctx), SurfaceHolder.Callback, Runnable {
    companion object {
        const val S_MENU = 0; const val S_SETUP = 1; const val S_CHAR = 2; const val S_MAPS = 3
        const val S_HOWTO = 4; const val S_SETTINGS = 5; const val S_LOADING = 6; const val S_GAME = 7
        const val S_PAUSE = 8; const val S_RESULT = 9
        val ACCENT = Color.rgb(255, 150, 40)
    }

    var onExit: (() -> Unit)? = null
    private var screen = S_MENU
    private var flow = false
    private var world: GameWorld? = null
    private val renderer = WorldRenderer()
    private val hud = Hud()
    private val controls = TouchControls()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tpaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    @Volatile private var tapX = -1f
    @Volatile private var tapY = -1f
    @Volatile private var running = false
    private var thread: Thread? = null
    private var surfaceOk = false
    private var sw = 1
    private var sh = 1
    private var clock = 0f
    private var loadFrames = 0
    private var endTimer = 0f
    private var u = 1f

    init {
        holder.addCallback(this)
        paint.isFilterBitmap = true
        tpaint.isFakeBoldText = true
        isFocusable = true
        Assets.init(ctx)
        Prefs.init(ctx)
    }

    // ------------------------------------------------------------------ lifecycle
    override fun surfaceCreated(h: SurfaceHolder) { surfaceOk = true; startLoop() }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
        sw = w; sh = hh; u = hh / 1080f
        controls.layout(w, hh)
    }
    override fun surfaceDestroyed(h: SurfaceHolder) { surfaceOk = false; stopLoop() }

    fun startLoop() {
        if (running || !surfaceOk) return
        running = true
        val t = Thread(this, "bba-loop")
        thread = t
        t.start()
    }

    fun stopLoop() {
        running = false
        try { thread?.join(500) } catch (e: InterruptedException) { }
        thread = null
        Prefs.save()
        if (screen == S_GAME) screen = S_PAUSE
    }

    fun onBack() {
        when (screen) {
            S_MENU -> onExit?.invoke()
            S_GAME -> { controls.reset(); screen = S_PAUSE }
            S_PAUSE -> screen = S_GAME
            S_SETUP -> screen = S_MENU
            S_CHAR -> screen = if (flow) S_SETUP else S_MENU
            S_MAPS -> screen = if (flow) S_CHAR else S_MENU
            S_LOADING -> { }
            else -> screen = S_MENU
        }
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val start = System.nanoTime()
            var dt = (start - last) / 1e9f
            last = start
            if (dt > 0.05f) dt = 0.05f
            val c: Canvas? = try { holder.lockCanvas() } catch (e: Exception) { null }
            if (c == null) { try { Thread.sleep(16) } catch (e: InterruptedException) { }; continue }
            try {
                step(dt)
                render(c)
            } catch (e: Exception) {
                // never crash the render thread on a bad frame
            } finally {
                try { holder.unlockCanvasAndPost(c) } catch (e: Exception) { }
            }
            val target = if (Prefs.fps30) 33L else 16L
            val spent = (System.nanoTime() - start) / 1000000L
            if (spent < target) { try { Thread.sleep(target - spent) } catch (e: InterruptedException) { } }
        }
    }

    // ------------------------------------------------------------------ input
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (screen == S_GAME) {
            val a = e.actionMasked
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_POINTER_DOWN) {
                val i = e.actionIndex
                val dx = e.getX(i) - (sw - 50f * u)
                val dy = e.getY(i) - 50f * u
                if (dx * dx + dy * dy < (45f * u) * (45f * u)) { controls.reset(); screen = S_PAUSE; return true }
            }
            controls.onTouch(e)
            return true
        }
        if (e.actionMasked == MotionEvent.ACTION_UP) { tapX = e.x; tapY = e.y }
        return true
    }

    // ------------------------------------------------------------------ update
    private fun step(dt: Float) {
        clock += dt
        if (screen == S_LOADING) {
            loadFrames++
            if (loadFrames > 2) startMatch()
            return
        }
        if (screen == S_GAME) {
            val w = world ?: return
            controls.apply(w.player)
            w.update(dt)
            renderer.updateCamera(w, sw, sh, dt, Prefs.zoom)
            if (w.phase == 2) {
                endTimer += dt
                if (endTimer > 2.5f) { controls.reset(); screen = S_RESULT }
            }
        }
    }

    private fun startMatch() {
        Prefs.save()
        val m = Maps.all[Prefs.map.coerceIn(0, Maps.all.size - 1)]
        val size = Prefs.size
        val w = GameWorld(m, Prefs.mode, Prefs.diff, size, Prefs.minutes * 60f, Prefs.character)
        for (f in w.fighters) Assets.character(f.charIdx)
        for (wd in com.bharatbattlearena.game.weapon.Weapons.ALL) Assets.weapon(wd.file)
        renderer.loadMap(m)
        controls.layout(sw, sh)
        controls.reset()
        world = w
        endTimer = 0f
        renderer.updateCamera(w, sw, sh, 0.5f, Prefs.zoom)
        screen = S_GAME
    }

    // ------------------------------------------------------------------ drawing helpers
    private fun txt(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int, al: Paint.Align = Paint.Align.CENTER) {
        tpaint.textAlign = al
        tpaint.textSize = size
        tpaint.color = Color.argb(190, 0, 0, 0)
        c.drawText(s, x + size * 0.05f, y + size * 0.05f, tpaint)
        tpaint.color = col
        c.drawText(s, x, y, tpaint)
    }

    private fun box(c: Canvas, x: Float, y: Float, w: Float, h: Float, fill: Int, stroke: Int, sw2: Float = 3f) {
        rect.set(x, y, x + w, y + h)
        paint.style = Paint.Style.FILL
        paint.color = fill
        c.drawRoundRect(rect, 14f * u, 14f * u, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = sw2 * u
        paint.color = stroke
        c.drawRoundRect(rect, 14f * u, 14f * u, paint)
        paint.style = Paint.Style.FILL
    }

    private fun tapped(x: Float, y: Float, w: Float, h: Float): Boolean {
        val tx = tapX
        val ty = tapY
        if (tx >= x && tx <= x + w && ty >= y && ty <= y + h) { tapX = -1f; tapY = -1f; return true }
        return false
    }

    private fun btn(c: Canvas, x: Float, y: Float, w: Float, h: Float, label: String, sel: Boolean = false, accent: Int = ACCENT): Boolean {
        box(c, x, y, w, h, if (sel) Color.argb(235, 120, 50, 10) else Color.argb(220, 14, 20, 34), if (sel) Color.rgb(255, 220, 90) else accent)
        txt(c, label, x + w / 2f, y + h / 2f + h * 0.16f, h * 0.42f, Color.WHITE)
        return tapped(x, y, w, h)
    }

    private fun drawCover(c: Canvas, b: Bitmap?) {
        if (b == null) { c.drawColor(Color.rgb(12, 16, 30)); return }
        val sa = sw.toFloat() / sh
        val ba = b.width.toFloat() / b.height
        val src = android.graphics.Rect()
        if (ba > sa) { val ww = (b.height * sa).toInt(); src.set((b.width - ww) / 2, 0, (b.width + ww) / 2, b.height) }
        else { val hh = (b.width / sa).toInt(); src.set(0, (b.height - hh) / 2, b.width, (b.height + hh) / 2) }
        rect.set(0f, 0f, sw.toFloat(), sh.toFloat())
        c.drawBitmap(b, src, rect, paint)
    }

    private fun backdrop(c: Canvas, title: String) {
        val m = Maps.all[Prefs.map.coerceIn(0, Maps.all.size - 1)]
        drawCover(c, Assets.thumb(m.file, 2))
        paint.color = Color.argb(165, 6, 8, 18)
        c.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), paint)
        txt(c, title, sw / 2f, 96f * u, 70f * u, Color.WHITE)
    }

    private fun drawChar(c: Canvas, idx: Int, cx: Float, bottom: Float, h: Float) {
        val cs = Assets.character(idx)
        val a = cs.anims["idle"] ?: return
        if (a.isEmpty()) return
        val b = a[(clock * 5f).toInt() % a.size]
        val ww = h * b.width / b.height
        rect.set(cx - ww / 2f, bottom - h, cx + ww / 2f, bottom)
        c.drawBitmap(b, null, rect, paint)
    }

    // ------------------------------------------------------------------ render
    private fun render(c: Canvas) {
        val seenX = tapX
        val seenY = tapY
        c.drawColor(Color.BLACK)
        when (screen) {
            S_MENU -> drawMenu(c)
            S_SETUP -> drawSetup(c)
            S_CHAR -> drawCharSelect(c)
            S_MAPS -> drawMaps(c)
            S_HOWTO -> drawHowTo(c)
            S_SETTINGS -> drawSettings(c)
            S_LOADING -> { backdrop(c, ""); txt(c, "LOADING...", sw / 2f, sh / 2f, 80f * u, Color.WHITE) }
            S_GAME -> drawGame(c)
            S_PAUSE -> { drawGame(c); drawPause(c) }
            S_RESULT -> drawResult(c)
        }
        if (tapX == seenX && tapY == seenY) { tapX = -1f; tapY = -1f }
    }

    private fun drawGame(c: Canvas) {
        val w = world ?: return
        renderer.draw(c, w, sw, sh)
        hud.draw(c, w, sw, sh)
        controls.draw(c, w.player)
        if (w.phase == 2) {
            paint.color = Color.argb(120, 0, 0, 0)
            c.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), paint)
            txt(c, "TIME UP", sw / 2f, sh * 0.5f, 120f * u, Color.WHITE)
        }
    }

    private fun drawMenu(c: Canvas) {
        backdrop(c, "")
        txt(c, "BHARAT", 70f * u, 200f * u, 110f * u, Color.rgb(255, 160, 40), Paint.Align.LEFT)
        txt(c, "BATTLE ARENA", 70f * u, 300f * u, 90f * u, Color.WHITE, Paint.Align.LEFT)
        drawChar(c, Prefs.character, sw * 0.76f, sh - 70f * u, 720f * u)
        var y = 360f * u
        val bw = 560f * u
        val bh = 88f * u
        val x = 70f * u
        if (btn(c, x, y, bw, bh, "PLAY")) { flow = true; screen = S_SETUP }
        y += bh + 16f * u
        if (btn(c, x, y, bw, bh, "CHARACTER")) { flow = false; screen = S_CHAR }
        y += bh + 16f * u
        if (btn(c, x, y, bw, bh, "MAPS")) { flow = false; screen = S_MAPS }
        y += bh + 16f * u
        if (btn(c, x, y, bw, bh, "SETTINGS")) screen = S_SETTINGS
        y += bh + 16f * u
        if (btn(c, x, y, bw / 2f - 8f * u, bh, "HOW TO PLAY")) screen = S_HOWTO
        if (btn(c, x + bw / 2f + 8f * u, y, bw / 2f - 8f * u, bh, "EXIT", false, Color.rgb(230, 60, 60))) onExit?.invoke()
        txt(c, "LAN multiplayer, audio and more maps: coming soon", 70f * u, sh - 30f * u, 24f * u, Color.argb(200, 200, 210, 230), Paint.Align.LEFT)
    }

    private fun optRow(c: Canvas, y: Float, label: String, opts: Array<String>, cur: Int): Int {
        txt(c, label, 140f * u, y + 62f * u, 40f * u, Color.WHITE, Paint.Align.LEFT)
        val bw = 330f * u
        var res = -1
        for (i in opts.indices) {
            if (btn(c, 640f * u + i * (bw + 18f * u), y, bw, 90f * u, opts[i], i == cur)) res = i
        }
        return res
    }

    private fun drawSetup(c: Canvas) {
        backdrop(c, "MATCH SETUP")
        var r = optRow(c, 190f * u, "MODE", arrayOf("FREE FOR ALL", "TEAM BATTLE"), Prefs.mode)
        if (r >= 0) Prefs.mode = r
        r = optRow(c, 320f * u, "DIFFICULTY", arrayOf("EASY", "NORMAL", "HARD"), Prefs.diff)
        if (r >= 0) Prefs.diff = r
        val sizes = intArrayOf(4, 6, 8)
        val labels = if (Prefs.mode == 1) arrayOf("2 v 2", "3 v 3", "4 v 4") else arrayOf("4 FIGHTERS", "6 FIGHTERS", "8 FIGHTERS")
        var cur = sizes.indexOf(Prefs.size)
        if (cur < 0) cur = 1
        r = optRow(c, 450f * u, "FIGHTERS", labels, cur)
        if (r >= 0) Prefs.size = sizes[r]
        val mins = intArrayOf(3, 5, 10)
        var mc = mins.indexOf(Prefs.minutes)
        if (mc < 0) mc = 1
        r = optRow(c, 580f * u, "MATCH TIME", arrayOf("3 MIN", "5 MIN", "10 MIN"), mc)
        if (r >= 0) Prefs.minutes = mins[r]
        txt(c, "You fight AI bots that use the same weapons, health and damage as you.", sw / 2f, 760f * u, 30f * u, Color.argb(220, 220, 230, 255))
        if (btn(c, 100f * u, sh - 150f * u, 380f * u, 100f * u, "BACK")) { Prefs.save(); screen = S_MENU }
        if (btn(c, sw - 580f * u, sh - 150f * u, 480f * u, 100f * u, "NEXT: CHARACTER")) { Prefs.save(); screen = S_CHAR }
    }

    private fun drawCharSelect(c: Canvas) {
        backdrop(c, "CHOOSE YOUR FIGHTER")
        val n = Assets.CHAR_NAMES.size
        val gap = 24f * u
        val cw = min(380f * u, (sw - 160f * u - gap * (n - 1)) / n)
        val ch = 600f * u
        val x0 = (sw - (cw * n + gap * (n - 1))) / 2f
        val y0 = 150f * u
        for (i in 0 until n) {
            val x = x0 + i * (cw + gap)
            val sel = i == Prefs.character
            box(c, x, y0, cw, ch, Color.argb(235, 16, 22, 38), if (sel) Color.rgb(255, 225, 90) else Color.rgb(90, 110, 160), if (sel) 7f else 3f)
            val cs = Assets.character(i)
            val pb = cs.portrait
            if (pb != null) {
                val s = min((cw - 20f * u) / pb.width, (ch - 90f * u) / pb.height)
                val pw = pb.width * s
                val ph = pb.height * s
                rect.set(x + (cw - pw) / 2f, y0 + 10f * u, x + (cw + pw) / 2f, y0 + 10f * u + ph)
                c.drawBitmap(pb, null, rect, paint)
            }
            if (pb == null) drawChar(c, i, x + cw / 2f, y0 + ch - 70f * u, ch - 130f * u)
            txt(c, cs.name, x + cw / 2f, y0 + ch - 24f * u, 34f * u, if (sel) Color.rgb(255, 225, 90) else Color.WHITE)
            if (tapped(x, y0, cw, ch)) Prefs.character = i
        }
        // animated preview of the selected fighter
        drawChar(c, Prefs.character, sw - 150f * u, sh - 170f * u, 230f * u)
        txt(c, "All fighters have identical stats: HP 100  |  Jetpack 100  |  Speed 300 (fair play)", 100f * u, sh - 210f * u, 30f * u, Color.argb(230, 220, 230, 255), Paint.Align.LEFT)
        if (btn(c, 100f * u, sh - 150f * u, 380f * u, 100f * u, "BACK")) { Prefs.save(); screen = if (flow) S_SETUP else S_MENU }
        if (btn(c, sw - 780f * u, sh - 150f * u, 480f * u, 100f * u, if (flow) "NEXT: MAP" else "CONFIRM")) {
            Prefs.save(); screen = if (flow) S_MAPS else S_MENU
        }
    }

    private fun drawMaps(c: Canvas) {
        backdrop(c, "SELECT MAP")
        val maps = Maps.all
        val cols = 4
        val gap = 20f * u
        val cw = (sw - 160f * u - gap * (cols - 1)) / cols
        val chh = cw * 0.40f
        val x0 = 80f * u
        val y0 = 140f * u
        for (i in maps.indices) {
            val col = i % cols
            val row = i / cols
            val x = x0 + col * (cw + gap)
            val y = y0 + row * (chh + 70f * u)
            val sel = i == Prefs.map
            box(c, x - 6f * u, y - 6f * u, cw + 12f * u, chh + 62f * u, Color.argb(235, 16, 22, 38), if (sel) Color.rgb(255, 225, 90) else Color.rgb(90, 110, 160), if (sel) 7f else 3f)
            val b = Assets.thumb(maps[i].file, 4)
            if (b != null) {
                rect.set(x, y, x + cw, y + chh)
                c.drawBitmap(b, null, rect, paint)
            }
            txt(c, maps[i].name, x + cw / 2f, y + chh + 40f * u, 30f * u, if (sel) Color.rgb(255, 225, 90) else Color.WHITE)
            if (tapped(x, y, cw, chh + 50f * u)) Prefs.map = i
        }
        val m = maps[Prefs.map.coerceIn(0, maps.size - 1)]
        val py = y0 + 2 * (chh + 70f * u) + 4f * u
        box(c, x0, py, sw - 160f * u, 130f * u, Color.argb(225, 12, 16, 28), Color.rgb(90, 110, 160))
        txt(c, m.name + "   |   " + m.sizeText + " units   |   " + m.difficultyText, x0 + 24f * u, py + 52f * u, 34f * u, Color.WHITE, Paint.Align.LEFT)
        txt(c, m.desc, x0 + 24f * u, py + 102f * u, 28f * u, Color.argb(230, 210, 220, 255), Paint.Align.LEFT)
        if (btn(c, 100f * u, sh - 130f * u, 380f * u, 90f * u, "BACK")) { Prefs.save(); screen = if (flow) S_CHAR else S_MENU }
        if (btn(c, sw - 640f * u, sh - 130f * u, 540f * u, 90f * u, if (flow) "START MATCH" else "CONFIRM")) {
            Prefs.save()
            if (flow) { loadFrames = 0; screen = S_LOADING } else screen = S_MENU
        }
    }

    private fun drawHowTo(c: Canvas) {
        backdrop(c, "HOW TO PLAY")
        val lines = arrayOf(
            "LEFT STICK: move.  Push UP = jetpack, push DOWN = crouch / drop through planks.",
            "RIGHT STICK: aim your weapon. Hold it pushed to fire. SHOOT button also fires.",
            "RUN icon = jump.  JET = hold to fly (fuel recharges on the ground).",
            "RELOAD, SWITCH (2 weapon slots), GRENADE (tap FRAG/FLASH/SMOKE to change type), MINE, MELEE.",
            "Pick up glowing items: health, ammo, grenades, mines and weapons. Crates and barrels can be destroyed.",
            "When you die you respawn automatically after exactly 5 seconds at a safe spawn point.",
            "Free For All: most kills wins.  Team Battle: Red vs Blue, highest team kills wins. Tie = Draw.",
            "Falling into the river kills you. Use the bridge or your jetpack!"
        )
        var y = 190f * u
        for (l in lines) { txt(c, l, 110f * u, y, 33f * u, Color.WHITE, Paint.Align.LEFT); y += 70f * u }
        if (btn(c, 100f * u, sh - 150f * u, 380f * u, 100f * u, "BACK")) screen = S_MENU
    }

    private fun stepper(c: Canvas, y: Float, label: String, value: String): Int {
        txt(c, label, 140f * u, y + 62f * u, 40f * u, Color.WHITE, Paint.Align.LEFT)
        var r = 0
        if (btn(c, 800f * u, y, 110f * u, 90f * u, "-")) r = -1
        txt(c, value, 1080f * u, y + 64f * u, 42f * u, Color.WHITE)
        if (btn(c, 1250f * u, y, 110f * u, 90f * u, "+")) r = 1
        return r
    }

    private fun drawSettings(c: Canvas) {
        backdrop(c, "SETTINGS")
        var r = stepper(c, 190f * u, "CONTROL SIZE", (Prefs.ctrlScale * 100).toInt().toString() + "%")
        if (r != 0) { Prefs.ctrlScale = (Prefs.ctrlScale + 0.1f * r).coerceIn(0.7f, 1.4f); controls.layout(sw, sh) }
        r = stepper(c, 320f * u, "CONTROL OPACITY", (Prefs.ctrlAlpha * 100).toInt().toString() + "%")
        if (r != 0) Prefs.ctrlAlpha = (Prefs.ctrlAlpha + 0.1f * r).coerceIn(0.3f, 1f)
        r = stepper(c, 450f * u, "CAMERA ZOOM", (Prefs.zoom * 100).toInt().toString() + "%")
        if (r != 0) Prefs.zoom = (Prefs.zoom + 0.1f * r).coerceIn(0.7f, 1.5f)
        txt(c, "FRAME RATE", 140f * u, 642f * u, 40f * u, Color.WHITE, Paint.Align.LEFT)
        if (btn(c, 800f * u, 580f * u, 280f * u, 90f * u, "30 FPS", Prefs.fps30)) Prefs.fps30 = true
        if (btn(c, 1100f * u, 580f * u, 280f * u, 90f * u, "60 FPS", !Prefs.fps30)) Prefs.fps30 = false
        if (btn(c, 100f * u, sh - 150f * u, 380f * u, 100f * u, "BACK")) { Prefs.save(); screen = S_MENU }
    }

    private fun drawPause(c: Canvas) {
        paint.color = Color.argb(170, 0, 0, 0)
        c.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), paint)
        txt(c, "PAUSED", sw / 2f, 240f * u, 100f * u, Color.WHITE)
        val bw = 640f * u
        val bh = 100f * u
        val x = (sw - bw) / 2f
        if (btn(c, x, 330f * u, bw, bh, "RESUME")) screen = S_GAME
        if (btn(c, x, 450f * u, bw, bh, "RESTART MATCH")) { loadFrames = 0; screen = S_LOADING }
        if (btn(c, x, 570f * u, bw, bh, "MAIN MENU", false, Color.rgb(230, 60, 60))) { world = null; flow = false; screen = S_MENU }
    }

    private fun drawResult(c: Canvas) {
        val w = world
        backdrop(c, "MATCH RESULT")
        if (w == null) { screen = S_MENU; return }
        val won = w.playerWon()
        txt(c, w.winnerText, sw / 2f, 190f * u, 80f * u, if (won) Color.rgb(255, 225, 90) else Color.WHITE)
        txt(c, if (w.winnerText == "DRAW") "DRAW" else if (won) "VICTORY" else "DEFEAT", sw / 2f, 255f * u, 44f * u, if (won) Color.rgb(120, 255, 140) else Color.rgb(255, 130, 120))
        val x0 = sw / 2f - 560f * u
        var y = 330f * u
        box(c, x0 - 20f * u, y - 56f * u, 1160f * u, (66f + 54f * w.fighters.size) * u, Color.argb(225, 12, 16, 28), Color.rgb(90, 110, 160))
        txt(c, "#", x0, y, 30f * u, Color.rgb(255, 190, 90), Paint.Align.LEFT)
        txt(c, "PLAYER", x0 + 70f * u, y, 30f * u, Color.rgb(255, 190, 90), Paint.Align.LEFT)
        txt(c, "TEAM", x0 + 560f * u, y, 30f * u, Color.rgb(255, 190, 90), Paint.Align.LEFT)
        txt(c, "KILLS", x0 + 800f * u, y, 30f * u, Color.rgb(255, 190, 90), Paint.Align.LEFT)
        txt(c, "DEATHS", x0 + 960f * u, y, 30f * u, Color.rgb(255, 190, 90), Paint.Align.LEFT)
        y += 54f * u
        val rank = w.ranking()
        for (i in rank.indices) {
            val f = rank[i]
            val col = if (f.id == 0) Color.rgb(255, 230, 90) else Color.WHITE
            txt(c, (i + 1).toString(), x0, y, 30f * u, col, Paint.Align.LEFT)
            txt(c, f.name + (if (i == 0) "   (MVP)" else ""), x0 + 70f * u, y, 30f * u, col, Paint.Align.LEFT)
            txt(c, if (w.mode == 1) (if (f.team == 0) "RED" else "BLUE") else "-", x0 + 560f * u, y, 30f * u, col, Paint.Align.LEFT)
            txt(c, f.kills.toString(), x0 + 800f * u, y, 30f * u, col, Paint.Align.LEFT)
            txt(c, f.deaths.toString(), x0 + 960f * u, y, 30f * u, col, Paint.Align.LEFT)
            y += 54f * u
        }
        val by = sh - 150f * u
        val bw = 470f * u
        if (btn(c, 100f * u, by, bw, 100f * u, "REMATCH")) { loadFrames = 0; screen = S_LOADING }
        if (btn(c, sw / 2f - bw / 2f, by, bw, 100f * u, "MATCH SETUP")) { flow = true; screen = S_SETUP }
        if (btn(c, sw - 100f * u - bw, by, bw, 100f * u, "MAIN MENU")) { world = null; flow = false; screen = S_MENU }
    }
}
