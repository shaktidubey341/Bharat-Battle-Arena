package com.bharatbattlearena.game

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.bharatbattlearena.game.ui.GameRenderer
import com.bharatbattlearena.game.ui.HudRenderer
import com.bharatbattlearena.game.ui.TouchControls
import com.bharatbattlearena.game.util.Prefs

/**
 * SurfaceView that owns the game loop thread: fixed 1/60s simulation steps,
 * rendering capped to the preferred FPS (30 or 60).
 */
class GameView(
    context: Context,
    config: GameConfig,
    private val prefs: Prefs,
    private val onExit: () -> Unit,
    private val onRematch: () -> Unit
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private val engine = GameEngine(config, prefs.quality)
    private val controls = TouchControls(prefs)
    private val renderer = GameRenderer()
    private val hud = HudRenderer()

    @Volatile
    private var running = false
    private var thread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
        keepScreenOn = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        running = true
        thread = Thread(this, "bba-loop").also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            thread?.join(1000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        thread = null
        controls.releaseAll(engine.player.input)
    }

    override fun run() {
        val step = 1f / 60f
        var last = System.nanoTime()
        var acc = 0f
        while (running) {
            val frameStart = System.nanoTime()
            var dt = (frameStart - last) / 1_000_000_000f
            last = frameStart
            if (dt > 0.1f) dt = 0.1f
            acc += dt
            try {
                val w = width
                val h = height
                if (w > 0 && h > 0) {
                    controls.layout(w, h)
                    var n = 0
                    while (acc >= step && n < 5) {
                        engine.update(step, w, h)
                        acc -= step
                        n++
                    }
                    if (n == 5) acc = 0f
                    val canvas = holder.lockCanvas()
                    if (canvas != null) {
                        try {
                            renderer.draw(canvas, engine, w, h)
                            hud.draw(canvas, engine, w, h)
                            if (engine.phase == Phase.ENDED) {
                                hud.drawResults(canvas, engine, w, h)
                            } else {
                                controls.draw(canvas, engine.player)
                            }
                        } finally {
                            holder.unlockCanvasAndPost(canvas)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BBA", "frame error", e)
            }
            val targetNs = 1_000_000_000L / prefs.fps.coerceIn(20, 120)
            val spent = System.nanoTime() - frameStart
            if (spent < targetNs) {
                try {
                    Thread.sleep((targetNs - spent) / 1_000_000L)
                } catch (e: InterruptedException) {
                    return
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (engine.phase == Phase.ENDED) {
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                if (hud.rematchRect.contains(event.x, event.y)) {
                    onRematch()
                } else if (hud.menuRect.contains(event.x, event.y)) {
                    onExit()
                }
            }
            return true
        }
        controls.onTouch(event, engine.player.input, engine.camera)
        return true
    }
}
