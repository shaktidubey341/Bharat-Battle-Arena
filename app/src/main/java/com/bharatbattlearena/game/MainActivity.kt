package com.bharatbattlearena.game

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.bharatbattlearena.game.ai.Difficulty
import com.bharatbattlearena.game.audio.Sfx
import com.bharatbattlearena.game.audio.SoundHub
import com.bharatbattlearena.game.audio.ToneBackend
import com.bharatbattlearena.game.map.MapFactory
import com.bharatbattlearena.game.player.Characters
import com.bharatbattlearena.game.ui.CharacterPreviewView
import com.bharatbattlearena.game.ui.MapPreviewView
import com.bharatbattlearena.game.util.Prefs

/**
 * Menu flow (all native Views, no layout XML):
 * Main menu -> Mode -> Character -> Map -> Match settings -> GameView.
 * LAN multiplayer is listed but not implemented yet; it shows an honest notice.
 */
class MainActivity : Activity() {

    private lateinit var prefs: Prefs
    private val tones = ToneBackend()
    private val ui = Handler(Looper.getMainLooper())
    private var gameView: GameView? = null
    private var onMain = true

    private var selChar = 0
    private var selMap = "up"
    private var durationIdx = 1
    private var diffIdx = 1
    private var botIdx = 1
    private val durations = intArrayOf(5, 10, 15)
    private val botCounts = intArrayOf(3, 5, 9)

    private val orange = Color.rgb(255, 140, 30)
    private val panel = Color.rgb(34, 38, 74)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        selChar = prefs.character.coerceIn(0, Characters.all.size - 1)
        selMap = if (MapFactory.isAvailable(prefs.mapId)) prefs.mapId else "up"
        durationIdx = durations.indexOf(prefs.durationMin).coerceAtLeast(0)
        diffIdx = prefs.difficulty.coerceIn(0, Difficulty.values().size - 1)
        botIdx = botCounts.indexOf(prefs.bots).let { if (it < 0) 1 else it }
        applyAudio()
        SoundHub.backend = tones
        immersive()
        showMainMenu()
    }

    private fun applyAudio() {
        tones.setVolume(prefs.sfxVolume)
        SoundHub.volume = prefs.sfxVolume
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) immersive()
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        if (gameView != null || !onMain) showMainMenu() else super.onBackPressed()
    }

    override fun onDestroy() {
        tones.release()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- helpers

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun bg(color: Int, radius: Int, stroke: Int = 0, strokeColor: Int = 0): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(radius).toFloat()
        if (stroke > 0) d.setStroke(dp(stroke), strokeColor)
        return d
    }

    private fun label(s: String, sp: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = sp
        t.setTextColor(color)
        if (bold) t.typeface = Typeface.DEFAULT_BOLD
        return t
    }

    private fun button(s: String, enabled: Boolean = true, onClick: () -> Unit): Button {
        val b = Button(this)
        b.text = s
        b.setAllCaps(false)
        b.setTextColor(Color.WHITE)
        b.textSize = 16f
        b.background = bg(if (enabled) orange else Color.rgb(70, 72, 96), 12)
        b.setPadding(dp(18), dp(8), dp(18), dp(8))
        b.setOnClickListener {
            SoundHub.play(Sfx.MENU_CLICK)
            onClick()
        }
        return b
    }

    private fun lp(w: Int, h: Int, margin: Int = 6): LinearLayout.LayoutParams {
        val p = LinearLayout.LayoutParams(w, h)
        p.setMargins(dp(margin), dp(margin), dp(margin), dp(margin))
        return p
    }

    private fun screen(): LinearLayout {
        val l = LinearLayout(this)
        l.orientation = LinearLayout.VERTICAL
        l.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.rgb(18, 20, 48), Color.rgb(70, 32, 52))
        )
        l.setPadding(dp(24), dp(12), dp(24), dp(12))
        l.gravity = Gravity.CENTER_HORIZONTAL
        return l
    }

    private fun show(v: View, main: Boolean) {
        gameView = null
        onMain = main
        setContentView(v)
    }

    private fun notice(title: String, msg: String) {
        AlertDialog.Builder(this).setTitle(title).setMessage(msg).setPositiveButton("OK", null).show()
    }

    private fun bottomBar(back: () -> Unit, nextLabel: String?, next: () -> Unit): LinearLayout {
        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.HORIZONTAL
        bar.gravity = Gravity.CENTER
        bar.addView(button("Back") { back() }, lp(dp(150), ViewGroup.LayoutParams.WRAP_CONTENT))
        if (nextLabel != null) {
            bar.addView(button(nextLabel) { next() }, lp(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        return bar
    }

    private fun chipRow(name: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(label(name, 16f, Color.LTGRAY), lp(dp(160), ViewGroup.LayoutParams.WRAP_CONTENT))
        val chips = ArrayList<Button>()
        fun restyle(sel: Int) {
            for (i in chips.indices) chips[i].background = bg(if (i == sel) orange else Color.rgb(60, 64, 100), 10)
        }
        for (i in options.indices) {
            val b = Button(this)
            b.text = options[i]
            b.setAllCaps(false)
            b.setTextColor(Color.WHITE)
            b.setOnClickListener {
                SoundHub.play(Sfx.MENU_CLICK)
                restyle(i)
                onSelect(i)
            }
            chips.add(b)
            row.addView(b, lp(dp(110), ViewGroup.LayoutParams.WRAP_CONTENT, 4))
        }
        restyle(selected)
        return row
    }

    private fun seekRow(name: String, min: Float, max: Float, value: Float, onChange: (Float) -> Unit): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.addView(label(name, 15f), lp(dp(190), ViewGroup.LayoutParams.WRAP_CONTENT))
        val sb = SeekBar(this)
        sb.max = 100
        sb.progress = (((value - min) / (max - min)) * 100f).toInt().coerceIn(0, 100)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                if (fromUser) onChange(min + (max - min) * p / 100f)
            }

            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
        row.addView(sb, lp(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT))
        return row
    }

    // ------------------------------------------------------------------- screens

    private fun showMainMenu() {
        val root = screen()
        root.addView(label("BHARAT BATTLE ARENA", 30f, orange, true))
        root.addView(label("Fast 2D arena combat with original fictional fighters", 13f, Color.LTGRAY))

        val items = listOf<Pair<String, () -> Unit>>(
            "Play" to { showModeSelect() },
            "Single Player" to { showCharacterSelect(true) },
            "LAN Multiplayer" to { showLanNotice() },
            "Character" to { showCharacterSelect(false) },
            "Maps" to { showMapSelect(false) },
            "Settings" to { showSettings() },
            "How To Play" to { showHowTo() },
            "Exit" to { finish() }
        )
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        list.gravity = Gravity.CENTER_HORIZONTAL
        var i = 0
        while (i < items.size) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            for (j in i until minOf(i + 2, items.size)) {
                val item = items[j]
                row.addView(button(item.first) { item.second() }, lp(dp(230), ViewGroup.LayoutParams.WRAP_CONTENT))
            }
            list.addView(row)
            i += 2
        }
        val sv = ScrollView(this)
        sv.addView(list)
        root.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        show(root, true)
    }

    private fun showLanNotice() {
        notice(
            "LAN Multiplayer",
            "LAN host/join is not implemented in this build yet.\n\nSingle Player (up to 9 bots) is fully playable."
        )
    }

    private fun showModeSelect() {
        val root = screen()
        root.addView(label("Select Mode", 26f, Color.WHITE, true))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(button("Single Player (vs bots)") { showCharacterSelect(true) }, lp(dp(360), ViewGroup.LayoutParams.WRAP_CONTENT))
        for (n in listOf("Free For All LAN", "Team Battle LAN", "Mixed Match (LAN + bots)")) {
            col.addView(button("$n  -  not available yet", false) { showLanNotice() }, lp(dp(360), ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        root.addView(col, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomBar({ showMainMenu() }, null, {}))
        show(root, false)
    }

    private fun showCharacterSelect(flow: Boolean) {
        val root = screen()
        root.addView(label("Choose Your Fighter", 24f, Color.WHITE, true))
        val hs = HorizontalScrollView(this)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val cards = ArrayList<LinearLayout>()
        fun refresh() {
            for (i in cards.indices) {
                cards[i].background = if (i == selChar) bg(panel, 14, 3, orange) else bg(panel, 14, 1, Color.rgb(80, 84, 130))
            }
        }
        for (def in Characters.all) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setPadding(dp(10), dp(8), dp(10), dp(8))
            card.addView(CharacterPreviewView(this, def), LinearLayout.LayoutParams(dp(200), dp(140)))
            card.addView(label(def.name, 18f, Color.WHITE, true))
            card.addView(label(def.title, 12f, Color.LTGRAY))
            card.addView(
                label(
                    "HP " + def.maxHp.toInt() + "   Speed " + (def.speedMul * 100).toInt() + "%   Jump " +
                            (def.jumpMul * 100).toInt() + "%", 12f, Color.rgb(160, 220, 255)
                )
            )
            card.addView(label("Power: " + def.powerName + " (" + def.powerCooldown.toInt() + "s)", 13f, orange, true))
            val d1 = label(def.powerDesc, 12f)
            d1.setWidth(dp(200))
            card.addView(d1)
            card.addView(label("Passive: " + def.passiveName, 13f, Color.rgb(140, 255, 160), true))
            val d2 = label(def.passiveDesc, 12f)
            d2.setWidth(dp(200))
            card.addView(d2)
            card.setOnClickListener {
                SoundHub.play(Sfx.MENU_CLICK)
                selChar = def.id
                prefs.character = def.id
                refresh()
            }
            cards.add(card)
            row.addView(card, lp(dp(220), ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        refresh()
        hs.addView(row)
        root.addView(hs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomBar({ showMainMenu() }, if (flow) "Next: Map" else "Done") {
            if (flow) showMapSelect(true) else showMainMenu()
        })
        show(root, false)
    }

    private fun showMapSelect(flow: Boolean) {
        val root = screen()
        root.addView(label("Choose Map", 24f, Color.WHITE, true))
        val hs = HorizontalScrollView(this)
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val cards = ArrayList<LinearLayout>()
        fun refresh() {
            for (i in cards.indices) {
                val sel = MapFactory.infos[i].id == selMap
                cards[i].background = if (sel) bg(panel, 14, 3, orange) else bg(panel, 14, 1, Color.rgb(80, 84, 130))
            }
        }
        for (info in MapFactory.infos) {
            val card = LinearLayout(this)
            card.orientation = LinearLayout.VERTICAL
            card.setPadding(dp(10), dp(8), dp(10), dp(8))
            card.addView(MapPreviewView(this, info), LinearLayout.LayoutParams(dp(240), dp(120)))
            card.addView(label(info.name, 18f, if (info.available) Color.WHITE else Color.GRAY, true))
            val d = label(info.description, 12f, Color.LTGRAY)
            d.setWidth(dp(240))
            card.addView(d)
            card.addView(label(info.sizeText + "  |  " + info.difficultyText, 12f, Color.rgb(160, 220, 255)))
            if (!info.available) card.addView(label("Coming soon", 13f, orange, true))
            card.setOnClickListener {
                SoundHub.play(Sfx.MENU_CLICK)
                if (info.available) {
                    selMap = info.id
                    prefs.mapId = info.id
                    refresh()
                } else {
                    Toast.makeText(this, info.name + " is not available yet", Toast.LENGTH_SHORT).show()
                }
            }
            cards.add(card)
            row.addView(card, lp(dp(260), ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        refresh()
        hs.addView(row)
        root.addView(hs, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomBar({ if (flow) showCharacterSelect(true) else showMainMenu() }, if (flow) "Next: Match Settings" else "Done") {
            if (flow) showMatchSettings() else showMainMenu()
        })
        show(root, false)
    }

    private fun showMatchSettings() {
        val root = screen()
        root.addView(label("Match Settings", 24f, Color.WHITE, true))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(chipRow("Match time", durations.map { "$it min" }, durationIdx) { durationIdx = it })
        col.addView(chipRow("Bot difficulty", Difficulty.values().map { it.label }, diffIdx) { diffIdx = it })
        col.addView(chipRow("Bots", botCounts.map { "$it" }, botIdx) { botIdx = it })
        col.addView(label("Most kills wins. You respawn 5 seconds after dying, unlimited times.", 13f, Color.LTGRAY))
        root.addView(col, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomBar({ showMapSelect(true) }, "START MATCH") {
            prefs.durationMin = durations[durationIdx]
            prefs.difficulty = diffIdx
            prefs.bots = botCounts[botIdx]
            startGame()
        })
        show(root, false)
    }

    private fun showSettings() {
        val root = screen()
        root.addView(label("Settings", 24f, Color.WHITE, true))
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.addView(seekRow("Button size", 0.6f, 1.6f, prefs.btnScale) { prefs.btnScale = it })
        col.addView(seekRow("Button opacity", 0.2f, 1f, prefs.opacity) { prefs.opacity = it })
        col.addView(seekRow("Joystick size", 0.7f, 1.5f, prefs.joyScale) { prefs.joyScale = it })
        col.addView(seekRow("Joystick sensitivity", 0.6f, 1.6f, prefs.sensitivity) { prefs.sensitivity = it })
        col.addView(seekRow("Sound volume", 0f, 1f, prefs.sfxVolume) {
            prefs.sfxVolume = it
            applyAudio()
        })
        col.addView(seekRow("Music volume", 0f, 1f, prefs.musicVolume) { prefs.musicVolume = it })
        col.addView(chipRow("Graphics", listOf("Low", "Medium", "High"), prefs.quality.coerceIn(0, 2)) { prefs.quality = it })
        col.addView(chipRow("Frame rate", listOf("30 FPS", "60 FPS"), if (prefs.fps <= 30) 0 else 1) {
            prefs.fps = if (it == 0) 30 else 60
        })
        col.addView(label("Music files are not bundled yet, so the music volume has no effect.", 12f, Color.LTGRAY))
        val sv = ScrollView(this)
        sv.addView(col)
        root.addView(sv, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(bottomBar({ showMainMenu() }, null, {}))
        show(root, false)
    }

    private fun showHowTo() {
        notice(
            "How To Play",
            "Goal: get the most kills before the timer ends. You respawn 5 seconds after you die.\n\n" +
                    "Left stick: move. JUMP, DUCK (DUCK + JUMP drops through thin platforms).\n" +
                    "FIRE shoots; you auto-aim at enemies in front of you. Hold AIM for steadier shots " +
                    "(then the stick aims up/down).\n" +
                    "RLD reloads, HIT is a melee strike, SWAP changes weapon, POWER uses your " +
                    "character's special ability.\n" +
                    "Walk over glowing boxes to pick up weapons. Headshots do extra damage; leg hits " +
                    "slow, arm hits spoil aim.\n" +
                    "+ / - or a two-finger pinch zooms the camera."
        )
    }

    // ----------------------------------------------------------------------- game

    private fun startGame() {
        val cfg = GameConfig(
            selChar, selMap, durations[durationIdx] * 60,
            Difficulty.values()[diffIdx], botCounts[botIdx]
        )
        launch(cfg)
    }

    private fun launch(cfg: GameConfig) {
        val gv = GameView(
            this, cfg, prefs,
            { ui.post { showMainMenu() } },
            { ui.post { launch(cfg) } }
        )
        gameView = gv
        onMain = false
        setContentView(gv)
    }
}
