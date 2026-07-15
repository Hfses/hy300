package com.hy300.remote.server

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        startForegroundService(Intent(this, RemoteServerService::class.java))
        setContent { ServerScreen(this) }
    }
}

@Composable private fun ServerScreen(activity: MainActivity) {
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); refresh++ } }
    val shizuku = remember(refresh) { ShizukuCommandExecutor(activity).available }
    MaterialTheme { Surface { Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("HY300 Remote Server", style = MaterialTheme.typography.headlineSmall)
        Text("Código de pareamento: ${RemoteServerService.currentPairCode}", style = MaterialTheme.typography.titleLarge)
        Text("WebSocket: porta 7300 • anúncio NSD ativo")
        Text("Acessibilidade: ${if (RemoteAccessibilityService.instance == null) "desativada" else "ativa"}")
        Text("Shizuku: ${if (shizuku) "autorizado" else "não autorizado"}")
        if (!shizuku) Button({ ShizukuCommandExecutor(activity).requestAccess() }) { Text("Solicitar acesso ao Shizuku") }
        Text("Ative o serviço em Configurações > Acessibilidade antes de conectar um celular.")
    } } }
}
