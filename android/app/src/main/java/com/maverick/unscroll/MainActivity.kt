package com.maverick.unscroll

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        findViewById<Button>(R.id.btnEnable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "Find \"Unscroll\" (Samsung: under Installed apps) and turn it on", Toast.LENGTH_LONG).show()
        }

        bind(R.id.swEnabled, prefs.enabled) { prefs.enabled = it }
        bind(R.id.swTab, prefs.tabFallback) { prefs.tabFallback = it }
        bind(R.id.swHome, prefs.goHome) { prefs.goHome = it }
        bind(R.id.swHeur, prefs.heuristic) { prefs.heuristic = it }
        bind(R.id.swDebug, prefs.debug) { prefs.debug = it }
        bindText(R.id.extraYt, prefs.extraYt) { prefs.extraYt = it }
        bindText(R.id.extraIg, prefs.extraIg) { prefs.extraIg = it }

        val secLabel = findViewById<TextView>(R.id.secLabel)
        val seek = findViewById<SeekBar>(R.id.seekSeconds)
        seek.progress = prefs.overlaySeconds - 1
        secLabel.text = "Reminder stays on screen: ${prefs.overlaySeconds} s"
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                prefs.overlaySeconds = p + 1
                secLabel.text = "Reminder stays on screen: ${p + 1} s"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            val s = WatchService.instance
            if (s == null) Toast.makeText(this, "Turn the accessibility service on first", Toast.LENGTH_SHORT).show()
            else s.showOverlay()
        }
    }

    override fun onResume() {
        super.onResume()
        val running = serviceEnabled()
        findViewById<TextView>(R.id.status).text =
            if (running) "Status: watching YouTube + Instagram" else "Status: accessibility service is OFF — tap the button below"
        findViewById<TextView>(R.id.stats).text =
            "Caught you ${prefs.catches} time(s)" + if (prefs.lastReason.isNotEmpty()) "\nLast: ${prefs.lastReason}" else ""
    }

    private fun bind(id: Int, initial: Boolean, onChange: (Boolean) -> Unit) {
        val sw = findViewById<Switch>(id)
        sw.isChecked = initial
        sw.setOnCheckedChangeListener { _, v -> onChange(v) }
    }

    private fun bindText(id: Int, initial: String, onChange: (String) -> Unit) {
        val et = findViewById<EditText>(id)
        et.setText(initial)
        et.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) { onChange(s.toString()) }
            override fun beforeTextChanged(s: CharSequence, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence, a: Int, b: Int, c: Int) {}
        })
    }

    private fun serviceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }
}
