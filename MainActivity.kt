package com.sovereign.solo

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Locale

class MainActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private var timerText: TextView? = null
    private var lunaPromptText: TextView? = null
    private var activeTab = 0 // 0: HUB, 1: STATUS, 2: LUNA (AI), 3: GATES, 4: APPS
    private var containerLayout: LinearLayout? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            updateTimer()
            ui.postDelayed(this, 1000)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        buildHUD()
    }

    override fun onResume() {
        super.onResume()
        buildHUD()
        ui.post(timerRunnable)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(timerRunnable)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intercepted: System Launcher Standard
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun makeBoxDrawable(borderColor: String = "#00F0FF", bgColor: String = "#0A0F1D"): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(bgColor))
            setStroke(dp(1), Color.parseColor(borderColor))
            cornerRadius = dp(6).toFloat()
        }
    }

    private fun tv(t: String, size: Float, color: Int = Color.parseColor("#00F0FF"), bold: Boolean = true) = TextView(this).apply {
        text = t
        textSize = size
        setTextColor(color)
        typeface = Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun hudButton(text: String, active: Boolean = true, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isEnabled = active
        setTextColor(if (active) Color.WHITE else Color.GRAY)
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        background = makeBoxDrawable(if (active) "#00F0FF" else "#334155", if (active) "#101D36" else "#0F172A")
        setOnClickListener { onClick() }
    }

    private fun updateTimer() {
        val tvw = timerText ?: return
        val g = Store.gateEnd(this) - Store.now()
        val pz = Store.passEnd(this) - Store.now()
        tvw.text = when {
            g > 0 -> "[ DUNGEON GATE ACTIVE · FOCUS REMAINING: ${fmt(g)} ]"
            Store.gateEnd(this) > 0 -> "[ GATE CLEARED · CLAIM REWARD WITH EDITH ]"
            pz > 0 -> "[ PASS ACTIVE · TIME REMAINING: ${fmt(pz)} ]"
            else -> "[ SYSTEM STATUS: OPTIMAL / HAZARDS LOCKED ]"
        }
    }

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return "%02d:%02d".format(s / 60, s % 60)
    }

    private fun buildHUD() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EE030712")) // Sci-fi glass overlay
            setPadding(dp(12), dp(28), dp(12), dp(12))
        }

        // Header Panel (EDITH & LUNA Integration)
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = makeBoxDrawable("#00F0FF", "#140A1F2D")
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        topRow.addView(tv("EDITH / LUNA SYSTEM HUD v4.0", 12f, Color.parseColor("#00F0FF")), LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        
        val aiBtn = hudButton("🎙 LUNA AI", true) { launchLunaVoice() }.apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(30))
            textSize = 9f
        }
        topRow.addView(aiBtn)
        header.addView(topRow)

        val xp = Store.xp(this)
        val lv = Store.level(xp)
        val (cur, need) = Store.levelXp(xp)

        header.addView(tv("HUNTER: LEON  |  LEVEL %02d  [ RANK %s ]".format(lv, Store.rank(lv)), 15f, Color.parseColor("#C084FC")))
        header.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = need; progress = cur
            setPadding(0, dp(4), 0, dp(4))
        })
        header.addView(tv("XP: $cur/$need  |  GOLD: ${Store.gold(this)}  |  FATIGUE: ${Store.fatigue(this)}%", 10f, Color.parseColor("#94A3B8")))

        timerText = tv("", 11f, Color.parseColor("#FACC15")).also { header.addView(it) }
        lunaPromptText = tv("[LUNA]: Focus block is optimal. Complete your daily checklist.", 10f, Color.parseColor("#38BDF8")).also { header.addView(it) }
        updateTimer()
        root.addView(header)

        // Favorite Apps Quick Dock (Orbit Sci-Fi Style)
        renderFavoriteDock(root)

        // Navigation Tabs
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(8))
        }
        val tabs = listOf("HUB", "STATUS", "LUNA", "GATES", "APPS")
        tabs.forEachIndexed { idx, label ->
            val tabBtn = Button(this).apply {
                text = label
                textSize = 9f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setTextColor(if (activeTab == idx) Color.BLACK else Color.parseColor("#00F0FF"))
                background = makeBoxDrawable(if (activeTab == idx) "#00F0FF" else "#1E293B", if (activeTab == idx) "#00F0FF" else "#0F172A")
                layoutParams = LinearLayout.LayoutParams(0, dp(32), 1f).apply { setMargins(dp(1), 0, dp(1), 0) }
                setOnClickListener { activeTab = idx; buildHUD() }
            }
            nav.addView(tabBtn)
        }
        root.addView(nav)

        // Content Area Container
        containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = makeBoxDrawable("#1E293B", "#10080D1A")
        }

        when (activeTab) {
            0 -> renderHubTab(containerLayout!!)
            1 -> renderStatusTab(containerLayout!!)
            2 -> renderLunaTab(containerLayout!!)
            3 -> renderGatesTab(containerLayout!!)
            4 -> renderInventoryTab(containerLayout!!)
        }

        val scroll = ScrollView(this).apply {
            addView(containerLayout)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(scroll)
        setContentView(root)
    }

    private fun renderFavoriteDock(root: LinearLayout) {
        val dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
        }
        val favs = listOf(
            Pair("PHONE", "com.android.dialer"),
            Pair("CHROME", "com.android.chrome"),
            Pair("NOTES", "com.google.android.keep"),
            Pair("SETTINGS", "com.android.settings")
        )
        favs.forEach { (name, pkg) ->
            val btn = TextView(this).apply {
                text = name; textSize = 9f
                setTextColor(Color.parseColor("#00F0FF"))
                gravity = Gravity.CENTER
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                background = makeBoxDrawable("#00F0FF", "#100F172A")
                setPadding(dp(4), dp(6), dp(4), dp(6))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins(dp(2), 0, dp(2), 0) }
                setOnClickListener { launchApp(name, pkg) }
            }
            dock.addView(btn)
        }
        root.addView(dock)
    }

    private fun renderHubTab(panel: LinearLayout) {
        panel.addView(tv("ORBIT HUD & SYSTEM CONTROLS", 13f, Color.WHITE))
        panel.addView(tv("EDITH PROTOCOL: Environmental and security telemetry active.", 10f, Color.GRAY))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        panel.addView(hudButton("SET SYSTEM WALLPAPER") {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Select Monarch Background"))
        })
        panel.addView(Space(this, null).apply { minimumHeight = dp(8) })
        panel.addView(hudButton("ACCESSIBILITY SHIELD CONFIG") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
    }

    private fun renderStatusTab(panel: LinearLayout) {
        panel.addView(tv("COGNITIVE ATTRIBUTE MATRIX", 13f, Color.WHITE))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        panel.addView(tv("STR (DISCIPLINE) : ${Store.str(this)}", 12f, Color.parseColor("#00F0FF")))
        panel.addView(tv("INT (KNOWLEDGE)  : ${Store.intStat(this)}", 12f, Color.parseColor("#00F0FF")))
        panel.addView(tv("FOC (FOCUS ACC)  : ${Store.foc(this)}", 12f, Color.parseColor("#00F0FF")))
        panel.addView(tv("VIT (ENERGY)     : ${Store.vit(this)}", 12f, Color.parseColor("#00F0FF")))
    }

    private fun renderLunaTab(panel: LinearLayout) {
        panel.addView(tv("LUNA AI ASSISTANT & CHECKLIST", 13f, Color.WHITE))
        panel.addView(tv("Luna: 'Execute your missions with zero cognitive friction, Leon.'", 10f, Color.parseColor("#38BDF8")))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        Store.QUESTS.forEachIndexed { i, q ->
            val done = Store.questDone(this, i)
            val cb = CheckBox(this).apply {
                text = q; textSize = 11f
                setTextColor(if (done) Color.GRAY else Color.parseColor("#9BE7FF"))
                isChecked = done; isEnabled = !done
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        Store.doQuest(this@MainActivity, i)
                        Toast.makeText(this@MainActivity, "[LUNA]: Mission verified. +25 XP | +75 Gold", Toast.LENGTH_SHORT).show()
                        buildHUD()
                    }
                }
            }
            panel.addView(cb)
        }
    }

    private fun renderGatesTab(panel: LinearLayout) {
        panel.addView(tv("DEEP WORK FOCUS GATES", 13f, Color.WHITE))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        val gOn = Store.gateOn(this)
        val done = !gOn && Store.gateEnd(this) > 0

        panel.addView(hudButton("ENTER FOCUS GATE (${Store.GATE_MIN} MIN)", !gOn && !done) {
            Store.enterGate(this); buildHUD()
        })
        panel.addView(Space(this, null).apply { minimumHeight = dp(6) })
        panel.addView(hudButton("CLAIM GATE REWARD", done) {
            if (Store.completeGate(this)) Toast.makeText(this, "[EDITH]: +${Store.GATE_XP} XP | +${Store.GATE_GOLD} Gold", Toast.LENGTH_SHORT).show()
            buildHUD()
        })
        panel.addView(Space(this, null).apply { minimumHeight = dp(6) })
        panel.addView(hudButton("ABANDON GATE (FATIGUE +25)", gOn) {
            Store.abandonGate(this); Toast.makeText(this, "[EDITH]: Gate aborted. Fatigue penalty applied.", Toast.LENGTH_SHORT).show()
            buildHUD()
        })
        panel.addView(Space(this, null).apply { minimumHeight = dp(12) })
        panel.addView(hudButton("ACQUIRE ENTERTAINMENT PASS (${Store.PASS_MIN} MIN)") {
            Toast.makeText(this, Store.buyPass(this), Toast.LENGTH_SHORT).show(); buildHUD()
        })
    }

    private fun renderInventoryTab(panel: LinearLayout) {
        panel.addView(tv("SYSTEM APPLICATION INVENTORY", 13f, Color.WHITE))
        panel.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)).apply { setMargins(0, dp(4), 0, dp(8)) }; setBackgroundColor(Color.parseColor("#334155")) })

        val search = EditText(this).apply {
            hint = "Search tactical applications..."
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 11f
            background = makeBoxDrawable("#334155", "#0F172A")
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val apps = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { Pair(it.loadLabel(packageManager).toString(), it.activityInfo.packageName) }.sortedBy { it.first.lowercase() }

        fun updateList(query: String) {
            listLayout.removeAllViews()
            apps.filter { it.first.contains(query, true) && it.second != packageName }.forEach { (name, pkg) ->
                val ent = pkg in Store.ENT
                val item = tv((if (ent) "☠ [HAZARD] " else "⚡ ") + name, 12f, if (ent) Color.parseColor("#F43F5E") else Color.parseColor("#00F0FF")).apply {
                    setPadding(0, dp(6), 0, dp(6))
                    setOnClickListener { launchApp(name, pkg) }
                }
                listLayout.addView(item)
            }
        }

        updateList("")
        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateList(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        panel.addView(search)
        panel.addView(Space(this, null).apply { minimumHeight = dp(6) })
        panel.addView(listLayout)
    }

    private fun launchLunaVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "LUNA LISTENING FOR CHECKLIST UPDATE...")
        }
        try {
            startActivityForResult(intent, 999)
        } catch (e: Exception) {
            Toast.makeText(this, "[LUNA]: Speech module offline.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchApp(name: String, pkg: String) {
        if (Store.blocked(this, pkg)) {
            startActivity(Intent(this, BlockActivity::class.java).putExtra("pkg", name))
            return
        }
        packageManager.getLaunchIntentForPackage(pkg)?.let { startActivity(it) }
    }
}
