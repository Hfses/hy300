package com.hy300.remote.client

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

private val Context.dataStore by preferencesDataStore("remote")

/** A single-client, local-network connection. It never sends commands before auth succeeds. */
class RemoteConnection(context: Context, private val onStatus: (String) -> Unit) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tokenKey = stringPreferencesKey("token")
    private val hostKey = stringPreferencesKey("host")
    private var socket: WebSocketClient? = null
    private var host = ""
    private var authenticated = false
    private var lastPong = 0L
    private val heartbeat = Timer("hy300-heartbeat", true).apply { scheduleAtFixedRate(5_000, 5_000) { beat() } }

    fun connect(endpoint: String) {
        host = endpoint.trim().removePrefix("ws://").removeSuffix("/")
        if (host.isBlank()) { onStatus("Informe o IP do projetor"); return }
        authenticated = false; onStatus("Conectando")
        socket?.close()
        socket = object : WebSocketClient(URI("ws://$host")) {
            override fun onOpen(handshake: ServerHandshake?) {
                scope.launch {
                    app.dataStore.edit { it[hostKey] = host }
                    val token = app.dataStore.data.first()[tokenKey]
                    if (token.isNullOrBlank()) onStatus("Pareamento pendente") else send(payload("auth", "token" to token))
                }
            }
            override fun onMessage(message: String?) { handle(message) }
            override fun onClose(code: Int, reason: String?, remote: Boolean) { authenticated = false; onStatus("Desconectado") }
            override fun onError(ex: Exception?) { authenticated = false; onStatus("Erro de conexão") }
        }.also { it.connect() }
    }
    fun pair(code: String) { socket?.takeIf { it.isOpen }?.send(payload("pair_request", "pair_code" to code.trim())) ?: onStatus("Conecte antes de parear") }
    fun send(event: String, vararg values: Pair<String, Any>) { if (authenticated) socket?.send(payload(event, *values)) }
    fun close() { heartbeat.cancel(); socket?.close() }
    private fun handle(message: String?) {
        val body = runCatching { Json.parseToJsonElement(message ?: "").jsonObject }.getOrNull() ?: return
        when (body["event"]?.jsonPrimitive?.content) {
            "pair_success" -> { val token = body["auth_token"]?.jsonPrimitive?.content ?: return; scope.launch { app.dataStore.edit { it[tokenKey] = token }; socket?.send(payload("auth", "token" to token)) } }
            "auth_success" -> { authenticated = true; lastPong = System.currentTimeMillis(); onStatus("Conectado") ; send("sync") }
            "pong" -> lastPong = System.currentTimeMillis()
        }
    }
    private fun beat() {
        if (!authenticated) return
        val now = System.currentTimeMillis()
        if (now - lastPong > 12_000) { authenticated = false; onStatus("Reconectando"); val endpoint = host; socket?.close(); if (endpoint.isNotBlank()) connect(endpoint) } else socket?.send(payload("ping"))
    }
    private fun payload(event: String, vararg values: Pair<String, Any>) = buildJsonObject { put("event", event); values.forEach { (key, value) -> when (value) { is String -> put(key, value); is Int -> put(key, value); is Float -> put(key, value); is Double -> put(key, value); else -> put(key, value.toString()) } } }.toString()
}
