package com.hy300.remote.client

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

private val Context.dataStore by preferencesDataStore("remote")
class RemoteConnection(private val context: Context, private val update: (String) -> Unit) {
    private var socket: WebSocketClient? = null
    private val key = stringPreferencesKey("token")
    fun connect(host: String, token: String? = null) {
        update("Conectando")
        socket = object : WebSocketClient(URI("ws://$host")) {
            override fun onOpen(h: ServerHandshake?) { if (token.isNullOrBlank()) update("Pareamento pendente") else send(event("auth", "token" to token)) }
            override fun onMessage(message: String?) {
                if (message?.contains("pair_success") == true) {
                    val tokenValue = Regex("\\\"auth_token\\\":\\\"([^\\\"]+)\\\"").find(message)?.groupValues?.get(1)
                    if (tokenValue != null) { CoroutineScope(Dispatchers.IO).launch { context.dataStore.edit { it[key] = tokenValue } }; send(event("auth", "token" to tokenValue)) }
                }
                if (message?.contains("auth_success") == true) update("Conectado")
            }
            override fun onClose(code: Int, reason: String?, remote: Boolean) { update("Desconectado") }
            override fun onError(ex: Exception?) { update("Erro de conexão") }
        }.also { it.connect() }
    }
    suspend fun savedToken() = context.dataStore.data.first()[key]
    suspend fun pair(code: String) { socket?.send(event("pair_request", "pair_code" to code)); /* token is captured by the UI callback in a production hardening pass */ }
    fun send(event: String, vararg values: Pair<String, Any>) { socket?.send(event(event, *values)) }
    private fun event(name: String, vararg values: Pair<String, Any>) = buildJsonObject { put("event", name); values.forEach { (k, v) -> when (v) { is String -> put(k, v); is Number -> put(k, v); else -> put(k, v.toString()) } } }.toString()
}
