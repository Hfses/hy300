package com.hy300.remote.server

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.serialization.json.*
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit

class RemoteServerService : Service() {
    private val pairCode = (100000..999999).random().toString().also { currentPairCode = it }
    private val tokens = mutableMapOf<String, Instant>()
    private lateinit var socket: RemoteSocket
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { createChannel(); startForeground(1, notification()); socket = RemoteSocket(); socket.start(); return START_STICKY }
    override fun onDestroy() { socket.stop(); super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun notification() = NotificationCompat.Builder(this, "remote").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("HY300 Remote ativo").setContentText("Aguardando cliente na porta 7300").build()
    private fun createChannel() { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel("remote", "HY300 Remote", NotificationManager.IMPORTANCE_LOW)) }
    private inner class RemoteSocket : WebSocketServer(InetSocketAddress(7300)) {
        private var active: WebSocket? = null
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) = Unit
        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) { if (active == conn) { active = null; RemoteAccessibilityService.instance?.removeCursor() } }
        override fun onMessage(conn: WebSocket, message: String) {
            val o = runCatching { Json.parseToJsonElement(message).jsonObject }.getOrNull() ?: return
            val event = o["event"]?.jsonPrimitive?.content ?: return
            if (event == "pair_request") { if (o["pair_code"]?.jsonPrimitive?.content == pairCode) { val t = token(); tokens[t] = Instant.now().plus(30, ChronoUnit.DAYS); conn.send(json("pair_success", "auth_token" to t, "expires" to tokens[t].toString())) } else conn.close(4003, "Código inválido"); return }
            if (event == "auth") { val t = o["token"]?.jsonPrimitive?.content; if (t != null && tokens[t]?.isAfter(Instant.now()) == true && (active == null || active == conn)) { active = conn; conn.send(json("auth_success")) } else conn.close(4001, "Não autorizado"); return }
            if (conn != active) { conn.close(4001, "Autenticação exigida"); return }
            val a = RemoteAccessibilityService.instance
            when (event) {
                "pointer_move" -> a?.move(o.float("dx"), o.float("dy"))
                "pointer_click" -> a?.tap(o["action"]?.jsonPrimitive?.content == "long")
                "pointer_long_press" -> a?.tap(true)
                "pointer_scroll" -> a?.scroll(o.float("delta"))
                "sync" -> a?.sync()?.let { conn.send("{\"event\":\"sync_response\",\"cursor_x\":${it[0]},\"cursor_y\":${it[1]},\"screen_width\":${it[2]},\"screen_height\":${it[3]}}") }
                "ping" -> conn.send(json("pong"))
            }
        }
        override fun onError(conn: WebSocket?, ex: Exception) = Unit
        override fun onStart() = Unit
    }
    private fun JsonObject.float(name: String) = this[name]?.jsonPrimitive?.floatOrNull ?: 0f
    private fun json(event: String, vararg fields: Pair<String, String>) = buildJsonObject { put("event", event); fields.forEach { put(it.first, it.second) } }.toString()
    private fun token() = ByteArray(24).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    companion object { @Volatile var currentPairCode: String = "iniciando" }
}
