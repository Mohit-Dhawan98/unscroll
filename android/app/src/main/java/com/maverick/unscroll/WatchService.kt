package com.maverick.unscroll

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView

class WatchService : AccessibilityService() {

    companion object {
        private const val TAG = Detector.TAG
        /** Live instance so the settings screen can trigger a preview. */
        @Volatile var instance: WatchService? = null

        private const val CHECK_INTERVAL_MS = 400L   // don't walk the tree more often than this
        private const val COOLDOWN_MS = 2500L        // after firing, ignore events while the Home transition settles
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefs: Prefs
    private var lastCheck = 0L
    private var lastFire = 0L
    private var overlay: View? = null
    private var countdown: Runnable? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        prefs = Prefs(this)
        Quotes.load(this)
        instance = this
        Log.i(TAG, "service connected")
    }

    override fun onDestroy() {
        instance = null
        removeOverlay()
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!prefs.enabled) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in Detector.PACKAGES) return

        val now = SystemClock.uptimeMillis()
        if (now - lastCheck < CHECK_INTERVAL_MS) return
        if (now - lastFire < COOLDOWN_MS) return
        lastCheck = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return   // stale root from another app
        if (prefs.debug) Detector.dump(root)

        val reason = Detector.detect(root, pkg, prefs.options()) ?: return
        lastFire = now
        prefs.catches = prefs.catches + 1
        prefs.lastReason = "$pkg $reason"
        Log.i(TAG, "caught: $pkg $reason")
        fire()
    }

    private fun fire() {
        showOverlay()
        performGlobalAction(if (prefs.goHome) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)
    }

    /** Full-screen reminder. TYPE_ACCESSIBILITY_OVERLAY needs no "draw over other apps" permission. */
    fun showOverlay() {
        removeOverlay()
        val themed = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        val v = LayoutInflater.from(themed).inflate(R.layout.overlay, null)
        val q = Quotes.nextQuote()
        v.findViewById<TextView>(R.id.wake).text = Quotes.nextWake()
        v.findViewById<TextView>(R.id.quote).text = q.text
        v.findViewById<TextView>(R.id.who).text = "— ${q.who}"
        val count = v.findViewById<TextView>(R.id.count)

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        )
        try {
            (getSystemService(WINDOW_SERVICE) as WindowManager).addView(v, lp)
            overlay = v
        } catch (e: Exception) {
            Log.w(TAG, "overlay failed", e)
            return
        }

        var remaining = prefs.overlaySeconds.coerceIn(1, 60)
        count.text = remaining.toString()
        val tick = object : Runnable {
            override fun run() {
                remaining--
                if (remaining <= 0) { removeOverlay(); return }
                count.text = remaining.toString()
                handler.postDelayed(this, 1000)
            }
        }
        countdown = tick
        handler.postDelayed(tick, 1000)
    }

    private fun removeOverlay() {
        countdown?.let { handler.removeCallbacks(it) }
        countdown = null
        overlay?.let {
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (_: Exception) {}
        }
        overlay = null
    }
}
