package com.sovereign.solo

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class BlockActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var timerDisplay: TextView
    private val tickRunnable = object : Runnable {
        override fun run() {
            val g = Store.gateEnd(this@BlockActivity) - Store.now()
            timerDisplay.text = if (g > 0) "FOCUS GATE ACTIVE: %02d:%02d".format(g / 60000, g / 1000 % 60) else "EDITH LOCK: NO PASS DETECTED"
            ui.postDelayed(this, 1000)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val target = intent.getStringExtra("pkg") ?: "TARGET"
        val border = GradientDrawable().apply {
            setColor(Color.parseColor("#0F050A")); setStroke(4, Color.parseColor("#F43F5E")); cornerRadius = 12f
        }
        fun t(s: String, sz: Float, c: Int) = TextView(this).apply {
            text = s; textSize = sz; setTextColor(c); gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); setPadding(0, 12, 0, 12)
        }
        timerDisplay = t("", 16f, Color.parseColor("#FACC15"))
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(40, 40, 40, 40); background = border
            addView(t("EDITH PROTOCOL: ACCESS BLOCKED", 20f, Color.parseColor("#F43F5E")))
            addView(t("HAZARD DETECTED: ${target.uppercase()}", 13f, Color.WHITE))
            addView(timerDisplay)
            addView(t("Luna reminder: Discipline equals freedom. Return to your mission.", 11f, Color.GRAY, false))
            addView(Button(this@BlockActivity).apply {
                text = "RETURN TO MISSION"; setTextColor(Color.WHITE)
                background = GradientDrawable().apply { setColor(Color.parseColor("#E11D48")); cornerRadius = 8f }
                setOnClickListener { finish() }
            })
        }
        setContentView(LinearLayout(this).apply {
            gravity = Gravity.CENTER; setBackgroundColor(Color.parseColor("#030712")); setPadding(24, 24, 24, 24)
            addView(content)
        })
    }
    override fun onResume() { super.onResume(); ui.post(tickRunnable) }
    override fun onPause() { super.onPause(); ui.removeCallbacks(tickRunnable) }
}
