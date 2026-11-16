package com.maverick.unscroll

import android.content.Context
import org.json.JSONObject

data class Quote(val text: String, val who: String)

/** Loads shared/quotes.json from assets and hands out lines in a shuffled cycle (no repeats until the pool is spent). */
object Quotes {
    private var wake: MutableList<String> = mutableListOf()
    private var quotes: MutableList<Quote> = mutableListOf()
    private val wakeQueue = ArrayDeque<String>()
    private val quoteQueue = ArrayDeque<Quote>()

    @Synchronized
    fun load(ctx: Context) {
        if (wake.isNotEmpty()) return
        val raw = ctx.assets.open("quotes.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val w = root.getJSONArray("wake")
        for (i in 0 until w.length()) wake.add(w.getString(i))
        val q = root.getJSONArray("quotes")
        for (i in 0 until q.length()) {
            val o = q.getJSONObject(i)
            quotes.add(Quote(o.getString("text"), o.getString("who")))
        }
        if (wake.isEmpty()) wake.add("Remember who you are.")
        if (quotes.isEmpty()) quotes.add(Quote("Confine yourself to the present.", "Marcus Aurelius"))
    }

    @Synchronized
    fun nextWake(): String {
        if (wakeQueue.isEmpty()) wakeQueue.addAll(wake.shuffled())
        return wakeQueue.removeFirst()
    }

    @Synchronized
    fun nextQuote(): Quote {
        if (quoteQueue.isEmpty()) quoteQueue.addAll(quotes.shuffled())
        return quoteQueue.removeFirst()
    }
}
