package com.hy300.remote.server

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import kotlin.math.sqrt

class RemoteAccessibilityService : AccessibilityService() {
    private lateinit var windows: WindowManager
    private var cursor: View? = null
    private var x = 0f; private var y = 0f
    private val main = Handler(Looper.getMainLooper())
    override fun onServiceConnected() { instance = this; windows = getSystemService(WINDOW_SERVICE) as WindowManager }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit
    override fun onDestroy() { removeCursor(); instance = null; super.onDestroy() }
    fun move(dx: Float, dy: Float) {
        main.removeCallbacksAndMessages(HIDE_CURSOR)
        main.post {
        val d = sqrt(dx * dx + dy * dy); val m = if (d > 25) 2.5f else 1f
        val display = resources.displayMetrics
        x = (x + dx * m).coerceIn(0f, display.widthPixels.toFloat()); y = (y + dy * m).coerceIn(0f, display.heightPixels.toFloat())
        showCursor(); updateCursor()
        }
    }
    fun showOverlayTest() { main.post { x = resources.displayMetrics.widthPixels / 2f; y = resources.displayMetrics.heightPixels / 2f; showCursor(); updateCursor() } }
    fun tap(longPress: Boolean = false) { main.post { gesture(x, y, if (longPress) 650 else 40) } }
    fun scroll(delta: Float) { main.post { gesture(x, y, 0, x, (y + delta).coerceIn(0f, resources.displayMetrics.heightPixels.toFloat()), 180) } }
    fun sync() = floatArrayOf(x, y, resources.displayMetrics.widthPixels.toFloat(), resources.displayMetrics.heightPixels.toFloat())
    fun global(key: String): Boolean = when (key) {
        "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
        "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)
        "RECENTS" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
        else -> false
    }
    /** Fallback for ordinary text when Shizuku is unavailable; depends on the focused app exposing ACTION_SET_TEXT. */
    fun setFocusedText(text: String): Boolean {
        val node = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply { putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text) })
    }
    private fun gesture(x1: Float, y1: Float, duration: Long, x2: Float = x1, y2: Float = y1, endDuration: Long = duration) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, maxOf(duration, endDuration))).build(), null, null)
    }
    private fun showCursor() { if (cursor != null) return; cursor = View(this).apply { setBackgroundColor(Color.WHITE) }; windows.addView(cursor, params()) }
    private fun updateCursor() { cursor?.let { windows.updateViewLayout(it, params()) } }
    private fun params() = WindowManager.LayoutParams(20, 20, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, PixelFormat.TRANSLUCENT).apply { this.x = x.toInt(); this.y = y.toInt() }
    fun hideCursorAfterDisconnect() { main.removeCallbacksAndMessages(HIDE_CURSOR); main.postAtTime({ removeCursorNow() }, HIDE_CURSOR, android.os.SystemClock.uptimeMillis() + 5_000) }
    fun removeCursor() { main.post { removeCursorNow() } }
    private fun removeCursorNow() { cursor?.let { windows.removeView(it) }; cursor = null }
    companion object { private val HIDE_CURSOR = Any(); @Volatile var instance: RemoteAccessibilityService? = null }
}
