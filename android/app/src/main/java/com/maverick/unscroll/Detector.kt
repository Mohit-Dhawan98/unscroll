package com.maverick.unscroll

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Decides whether the current window is the Shorts / Reels player.
 * One bounded depth-first walk of the view tree, matching view-ID substrings
 * (resource IDs shift between app versions, substrings survive longer).
 * Returns a short reason string on a hit, null otherwise.
 *
 * YouTube:   any node whose id contains a Shorts-player marker (reel_progress_bar is the
 *            most stable; it exists only in the player, not in the home-feed Shorts shelf).
 * Instagram: the bottom-nav "clips_tab" is selected, OR a "clips_viewer" node exists while
 *            "feed_tab" is absent (bottom bar hidden = fullscreen reel from feed/explore/profile).
 *            Instagram uses clips_viewer for every video, so clips_viewer alone is not enough.
 * Fallback (optional): bottom-nav tab whose content-description is "Shorts"/"Reels" and selected.
 */
object Detector {
    const val TAG = "Unscroll"

    val PACKAGES = setOf(
        "com.google.android.youtube",
        "app.revanced.android.youtube",
        "com.instagram.android",
    )

    private val YT_PLAYER = listOf(
        "reel_progress_bar",
        "reel_player_page_container",
        "reel_watch_player",
        "reel_player_underlay",
        "shorts_player_container",
        "shorts_video_title",
    )

    private const val MAX_NODES = 600

    /** Accessibility labels of the right-rail action buttons in every short-form player. */
    private val RAIL_WORDS = listOf("like", "comment", "share", "remix", "audio", "more options")

    data class Options(
        val tabFallback: Boolean,
        val heuristic: Boolean,
        /** User-added id substrings (from the Debug dump) so an app update never needs a rebuild. */
        val extraYt: List<String>,
        val extraIg: List<String>,
    )

    fun detect(root: AccessibilityNodeInfo, pkg: String, o: Options): String? {
        val isIg = pkg.contains("instagram")
        val tabLabel = if (isIg) "reels" else "shorts"
        val extra = if (isIg) o.extraIg else o.extraYt

        val screen = Rect().also { root.getBoundsInScreen(it) }
        val railX = screen.left + screen.width() * 0.78f
        val rail = ArrayList<Int>(6)       // centerY of rail buttons found
        val b = Rect()

        var clipsViewer = false
        var feedTab = false
        var visited = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val n = stack.removeLast()
            visited++
            val id = n.viewIdResourceName?.substringAfter(":id/") ?: ""
            val desc = n.contentDescription?.toString()?.trim()?.lowercase()

            if (id.isNotEmpty()) {
                for (m in extra) if (m.isNotEmpty() && m in id) return "custom:$m"
                if (isIg) {
                    if ("clips_tab" in id && n.isSelected) return "id:clips_tab selected"
                    if ("clips_viewer" in id) clipsViewer = true
                    if ("feed_tab" in id) feedTab = true
                } else {
                    for (m in YT_PLAYER) if (m in id) return "id:$m"
                }
            }

            // Layout heuristic: clickable Like/Comment/Share... buttons hugging the right edge.
            if (o.heuristic && desc != null && n.isClickable && RAIL_WORDS.any { it in desc }) {
                n.getBoundsInScreen(b)
                if (b.width() < screen.width() * 0.3f && b.centerX() > railX) rail.add(b.centerY())
            }

            if (o.tabFallback && n.isSelected) {
                if (desc != null && (desc == tabLabel || desc.startsWith("$tabLabel,") || desc.startsWith("$tabLabel "))) {
                    return "tab:$tabLabel"
                }
            }

            for (i in 0 until n.childCount) n.getChild(i)?.let { stack.addLast(it) }
        }

        if (isIg && clipsViewer && !feedTab) return "id:clips_viewer fullscreen"
        if (rail.size >= 3) {
            val spread = (rail.max() - rail.min()).toFloat() / screen.height()
            if (spread > 0.12f) return "layout:right-rail x${rail.size}"
        }
        return null
    }

    /** Debug helper: dumps ids / classes / descriptions so new IDs can be found with `adb logcat -s Unscroll`. */
    fun dump(root: AccessibilityNodeInfo) {
        var visited = 0
        val stack = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        stack.addLast(root to 0)
        val sb = StringBuilder("---- window dump ----\n")
        while (stack.isNotEmpty() && visited < MAX_NODES) {
            val (n, depth) = stack.removeLast()
            visited++
            val id = n.viewIdResourceName ?: ""
            val cls = n.className?.toString()?.substringAfterLast('.') ?: ""
            val desc = n.contentDescription?.toString()?.take(40) ?: ""
            val txt = n.text?.toString()?.take(40) ?: ""
            if (id.isNotEmpty() || desc.isNotEmpty() || n.isSelected) {
                sb.append("  ".repeat(depth)).append(cls)
                if (id.isNotEmpty()) sb.append(" #").append(id.substringAfter(":id/"))
                if (desc.isNotEmpty()) sb.append(" desc=\"").append(desc).append('"')
                if (txt.isNotEmpty()) sb.append(" text=\"").append(txt).append('"')
                if (n.isSelected) sb.append(" [SELECTED]")
                sb.append('\n')
            }
            for (i in 0 until n.childCount) n.getChild(i)?.let { stack.addLast(it to depth + 1) }
        }
        sb.toString().chunked(3500).forEach { Log.d(TAG, it) }
    }
}
