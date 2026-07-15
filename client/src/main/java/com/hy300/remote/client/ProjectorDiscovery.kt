package com.hy300.remote.client

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper

data class ProjectorEndpoint(val name: String, val host: String, val port: Int, val model: String)

/** Resolves HY300 Remote servers announced on the current Wi-Fi network. */
class ProjectorDiscovery(context: Context, private val onFound: (ProjectorEndpoint) -> Unit, private val onError: (String) -> Unit) {
    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val main = Handler(Looper.getMainLooper())
    private var listener: NsdManager.DiscoveryListener? = null
    fun start() {
        if (listener != null) return
        listener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(type: String, error: Int) { main.post { onError("Não foi possível iniciar descoberta") }; stop() }
            override fun onStopDiscoveryFailed(type: String, error: Int) = Unit
            override fun onDiscoveryStarted(type: String) = Unit
            override fun onDiscoveryStopped(type: String) = Unit
            override fun onServiceLost(service: NsdServiceInfo) = Unit
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType != "_hy300remote._tcp.") return
                nsd.resolveService(service, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(s: NsdServiceInfo, error: Int) = Unit
                    override fun onServiceResolved(s: NsdServiceInfo) {
                        val address = s.host?.hostAddress ?: return
                        val endpoint = ProjectorEndpoint(s.serviceName, address, s.port, s.attributes["device"]?.let { String(it, Charsets.UTF_8) } ?: "HY300")
                        main.post { onFound(endpoint) }
                    }
                })
            }
        }
        nsd.discoverServices("_hy300remote._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
    }
    fun stop() { listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }; listener = null }
}
