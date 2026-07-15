package com.hy300.remote.server

import android.content.Context
import rikka.shizuku.Shizuku

/** Executes the small, whitelisted set of shell commands required by the protocol. */
class ShizukuCommandExecutor(private val context: Context) {
    val available: Boolean get() = runCatching { Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED }.getOrDefault(false)
    fun requestAccess() { if (runCatching { Shizuku.pingBinder() }.getOrDefault(false) && Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) Shizuku.requestPermission(101) }
    fun special(key: String): Boolean = when (key) {
        "HOME" -> shell("input keyevent 3")
        "BACK" -> shell("input keyevent 4")
        "POWER" -> shell("input keyevent 26")
        "VOLUME_UP" -> shell("input keyevent 24")
        "VOLUME_DOWN" -> shell("input keyevent 25")
        else -> false
    }
    fun keyEvent(keyCode: Int): Boolean = keyCode in 0..300 && shell("input keyevent $keyCode")
    fun text(text: String): Boolean = text.isNotBlank() && shell("input text ${shellQuote(text)}")
    private fun shell(command: String): Boolean = runCatching { val p = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null); p.waitFor() == 0 }.getOrDefault(false)
    private fun shellQuote(value: String) = "'" + value.replace("'", "'\\\"'\\\"'").replace(" ", "%s") + "'"
}
