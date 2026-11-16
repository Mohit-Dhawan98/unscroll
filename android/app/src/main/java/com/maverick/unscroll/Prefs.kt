package com.maverick.unscroll

import android.content.Context
import android.content.SharedPreferences

class Prefs(ctx: Context) {
    private val p: SharedPreferences = ctx.getSharedPreferences("unscroll", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = p.getBoolean("enabled", true)
        set(v) = p.edit().putBoolean("enabled", v).apply()

    /** Also fire when the bottom-nav "Shorts"/"Reels" tab is the selected one. */
    var tabFallback: Boolean
        get() = p.getBoolean("tab", true)
        set(v) = p.edit().putBoolean("tab", v).apply()

    /** true = GLOBAL_ACTION_HOME, false = GLOBAL_ACTION_BACK */
    var goHome: Boolean
        get() = p.getBoolean("home", true)
        set(v) = p.edit().putBoolean("home", v).apply()

    var overlaySeconds: Int
        get() = p.getInt("seconds", 5)
        set(v) = p.edit().putInt("seconds", v).apply()

    var heuristic: Boolean
        get() = p.getBoolean("heuristic", true)
        set(v) = p.edit().putBoolean("heuristic", v).apply()

    /** Comma-separated id substrings the user added from the debug dump. */
    var extraYt: String
        get() = p.getString("extraYt", "") ?: ""
        set(v) = p.edit().putString("extraYt", v).apply()
    var extraIg: String
        get() = p.getString("extraIg", "") ?: ""
        set(v) = p.edit().putString("extraIg", v).apply()

    fun options() = Detector.Options(
        tabFallback = tabFallback,
        heuristic = heuristic,
        extraYt = split(extraYt),
        extraIg = split(extraIg),
    )
    private fun split(s: String) = s.split(',', '\n', ' ').map { it.trim() }.filter { it.isNotEmpty() }

    var debug: Boolean
        get() = p.getBoolean("debug", false)
        set(v) = p.edit().putBoolean("debug", v).apply()

    var catches: Int
        get() = p.getInt("catches", 0)
        set(v) = p.edit().putInt("catches", v).apply()

    var lastReason: String
        get() = p.getString("lastReason", "") ?: ""
        set(v) = p.edit().putString("lastReason", v).apply()
}
