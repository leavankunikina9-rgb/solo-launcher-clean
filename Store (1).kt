package com.sovereign.solo

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

object Store {
    // S-Rank Entertainment Hazards (Dopamine Traps)
    val ENT = setOf(
        "com.google.android.youtube", "com.zhiliaoapp.musically", "com.instagram.android",
        "com.netflix.mediaclient", "com.facebook.katana", "com.twitter.android",
        "com.snapchat.android", "com.crunchyroll.crunchyroid", "com.reddit.frontpage",
        "com.dts.freefireth", "com.tencent.ig"
    )

    const val GATE_MIN = 50          // Pomodoro-optimized deep work block
    const val PASS_MIN = 20          // Controlled dopamine window
    const val PASS_COST = 400        // High economic friction for entertainment
    const val GATE_GOLD = 300
    const val GATE_XP = 50
    const val MIN_LEVEL_FOR_PASS = 3 // E-Rank restriction

    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences("monarch_system_v4", Context.MODE_PRIVATE)
    fun now(): Long = System.currentTimeMillis()

    // Player Metrics
    fun xp(c: Context): Int = prefs(c).getInt("xp", 0)
    fun gold(c: Context): Int = prefs(c).getInt("gold", 0)
    fun fatigue(c: Context): Int = prefs(c).getInt("fatigue", 0)
    fun streak(c: Context): Int = prefs(c).getInt("streak", 1)

    // Behavioral Science Stat Scaling (First Principles)
    fun str(c: Context): Int = 10 + (xp(c) / 35)  // Discipline via execution
    fun intStat(c: Context): Int = 10 + (xp(c) / 20) // Knowledge via active recall
    fun foc(c: Context): Int = 10 + (xp(c) / 25)  // Concentration via gate completion
    fun vit(c: Context): Int = (100 - fatigue(c)).coerceAtLeast(10) // Energy preservation

    // Timers
    fun gateEnd(c: Context): Long = prefs(c).getLong("gateEnd", 0)
    fun passEnd(c: Context): Long = prefs(c).getLong("passEnd", 0)
    fun lockEnd(c: Context): Long = prefs(c).getLong("lockEnd", 0)

    fun gateOn(c: Context): Boolean = now() < gateEnd(c)
    fun passOn(c: Context): Boolean = now() < passEnd(c)

    fun level(xp: Int): Int {
        var x = xp; var l = 1
        while (x >= 100 + 25 * (l - 1)) {
            x -= 100 + 25 * (l - 1); l++
        }
        return l
    }

    fun levelXp(xp: Int): Pair<Int, Int> {
        var x = xp; var l = 1
        while (x >= 100 + 25 * (l - 1)) {
            x -= 100 + 25 * (l - 1); l++
        }
        return Pair(x, 100 + 25 * (l - 1))
    }

    fun rank(l: Int): String = when {
        l >= 30 -> "S"
        l >= 20 -> "A"
        l >= 15 -> "B"
        l >= 10 -> "C"
        l >= 5 -> "D"
        else -> "E"
    }

    fun blocked(c: Context, pkg: String): Boolean {
        return pkg in ENT && (gateOn(c) || now() < lockEnd(c) || !passOn(c))
    }

    fun enterGate(c: Context) {
        prefs(c).edit().putLong("gateEnd", now() + GATE_MIN * 60000L).apply()
    }

    fun completeGate(c: Context): Boolean {
        if (gateOn(c) || gateEnd(c) == 0L) return false
        prefs(c).edit()
            .putInt("xp", xp(c) + GATE_XP)
            .putInt("gold", gold(c) + GATE_GOLD)
            .putLong("gateEnd", 0)
            .apply()
        return true
    }

    fun abandonGate(c: Context) {
        val f = (fatigue(c) + 25).coerceAtMost(100)
        val editor = prefs(c).edit().putLong("gateEnd", 0)
        if (f >= 100) {
            editor.putInt("fatigue", 0).putLong("lockEnd", now() + 12 * 3600000L)
        } else {
            editor.putInt("fatigue", f)
        }
        editor.apply()
    }

    fun buyPass(c: Context): String {
        if (gateOn(c)) return "[EDITH] DENIED: GATE IS ACTIVE. FOCUS REQUIRED."
        if (level(xp(c)) < MIN_LEVEL_FOR_PASS) return "[EDITH] RANK TOO LOW. REACH E-3 TO UNLOCK PASS."
        if (gold(c) < PASS_COST) return "[EDITH] INSUFFICIENT GOLD. CLEAR GATES."
        prefs(c).edit()
            .putInt("gold", gold(c) - PASS_COST)
            .putLong("passEnd", now() + PASS_MIN * 60000L)
            .apply()
        return "[EDITH] ENTERTAINMENT PASS GRANTED FOR $PASS_MIN MIN."
    }

    // Luna's AI Checklist & Behavioral Reminders
    val QUESTS = listOf(
        "Mathematics: Logarithms & Progressions Problem Set",
        "Physics: Electromagnetic Induction & Hand-Crank Review",
        "Chemistry / Computer Science: Algorithm Trace & Covalent Bonds"
    )

    private fun todayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun questDone(c: Context, index: Int): Boolean {
        return prefs(c).getString("luna_q_$index", "") == todayKey()
    }

    fun doQuest(c: Context, index: Int) {
        if (!questDone(c, index)) {
            prefs(c).edit()
                .putString("luna_q_$index", todayKey())
                .putInt("xp", xp(c) + 25)
                .putInt("gold", gold(c) + 75)
                .apply()
        }
    }
}
