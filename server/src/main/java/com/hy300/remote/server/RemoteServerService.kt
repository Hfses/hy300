package com.hy300.remote.server

import android.app.*
import android.content.Intent
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
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
    private var nsd: NsdManager? = null
    private var registration: NsdManager.RegistrationListener? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { createChannel(); startForeground(1, notification()); if (!::socket.isInitialized) { socket = RemoteSocket(); socket.start(); registerNsd() }; return START_STICKY }
    override fun onDestroy() { if (::socket.isInitialized) socket.stop(); registration?.let { nsd?.unregisterService(it) }; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun notification() = NotificationCompat.Builder(this, "remote").setSmallIcon(android.R.drawable.stat_sys_data_bluetooth).setContentTitle("HY300 Remote ativo").setContentText("Aguardando cliente na porta 7300").build()
    private fun createChannel() { (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(NotificationChannel("remote", "HY300 Remote", NotificationManager.IMPORTANCE_LOW)) }
    private fun registerNsd() {
        val info = NsdServiceInfo().apply { serviceName = "HY300 Remote"; serviceType = "_hy300remote._tcp."; port = 7300; setAttribute("device", android.os.Build.MODEL); setAttribute("android", android.os.Build.VERSION.RELEASE); setAttribute("version", BuildConfig.VERSION_NAME); setAttribute("accessibility", "true") }
        nsd = getSystemService(Context.NSD_SERVICE) as NsdManager
        registration = object : NsdManager.RegistrationListener { override fun onServiceRegistered(s: NsdServiceInfo) = Unit; override fun onRegistrationFailed(s: NsdServiceInfo, e: Int) = Unit; override fun onServiceUnregistered(s: NsdServiceInfo) = Unit; override fun onUnregistrationFailed(s: NsdServiceInfo, e: Int) = Unit }
        nsd?.registerService(info, NsdManager.PROTOCOL_DNS_SD, registration)
    }
    private inner class RemoteSocket : WebSocketServer(InetSocketAddress(7300)) {
        private var active: WebSocket? = null
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) = Unit
        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) { if (active == conn) { active = null; RemoteAccessibilityService.instance?.removeCursor() } }
        override fun onMessage(conn: WebSocket, message: String) {
            val o = runCatching { Json.parseToJsonElement(message).jsonObject }.getOrNull() ?: return
            val event = o["event"]?.jsonPrimitive?.content ?: return
            if (event == "pair_request") { if (o["pair_code"]?.jsonPrimitive?.content == pairCode) { val t = token(); val expiry = Instant.now().plus(30, ChronoUnit.DAYS); tokens[t] = expiry; preferences().edit().putLong(t, expiry.toEpochMilli()).apply(); conn.send(json("pair_success", "auth_token" to t, "expires" to expiry.toString())) } else conn.close(4003, "Código inválido"); return }
            if (event == "auth") { val t = o["token"]?.jsonPrimitive?.content; val expiry = t?.let { tokens[it] ?: preferences().getLong(it, 0).let { ms -> if (ms == 0L) null else Instant.ofEpochMilli(ms) } }; if (t != null && expiry?.isAfter(Instant.now()) == true && (active == null || active == conn)) { tokens[t] = expiry; active = conn; conn.send(json("auth_success")) } else conn.close(4001, "Não autorizado"); return }
            if (conn != active) { conn.close(4001, "Autenticação exigida"); return }
            val a = RemoteAccessibilityService.instance
            when (event) {
                "pointer_move" -> a?.move(o.float("dx"), o.float("dy"))
                "pointer_click" -> a?.tap(o["action"]?.jsonPrimitive?.content == "long")
                "pointer_long_press" -> a?.tap(true)
                "pointer_scroll" -> a?.scroll(o.float("delta"))
                "text_input" -> a?.setFocusedText(o["text"]?.jsonPrimitive?.content.orEmpty())
                "special_key" -> a?.global(o["key"]?.jsonPrimitive?.content.orEmpty())
                "sync" -> a?.sync()?.let { conn.send("{\"event\":\"sync_response\",\"cursor_x\":${it[0]},\"cursor_y\":${it[1]},\"screen_width\":${it[2]},\"screen_height\":${it[3]}}") }
                "ping" -> conn.send(json("pong"))
            }
        }
        override fun onError(conn: WebSocket?, ex: Exception) = Unit
        override fun onStart() = Unit
    }
    private fun JsonObject.float(name: String) = this[name]?.jsonPrimitive?.floatOrNull ?: 0f
    private fun preferences() = getSharedPreferences("auth_tokens", Context.MODE_PRIVATE)
    private fun json(event: String, vararg fields: Pair<String, String>) = buildJsonObject { put("event", event); fields.forEach { put(it.first, it.second) } }.toString()
    private fun token() = ByteArray(24).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
    companion object { @Volatile var currentPairCode: String = "iniciando" }
}
