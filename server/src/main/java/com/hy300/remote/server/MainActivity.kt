package com.hy300.remote.server

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
 override fun onCreate(state: Bundle?) { super.onCreate(state); startService(Intent(this, RemoteServerService::class.java)); setContent { var tick by remember { mutableIntStateOf(0) }; LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); tick++ } }; MaterialTheme { Surface { Column(Modifier.padding(24.dp)) { Text("HY300 Remote Server", style = MaterialTheme.typography.headlineSmall); Text("Código de pareamento: ${RemoteServerService.currentPairCode}"); Text("WebSocket: porta 7300"); Text("Ative o serviço em Configurações > Acessibilidade."); Text("Status Accessibility: ${if (RemoteAccessibilityService.instance == null) "desativado" else "ativo"}"); Text("Shizuku: pendente de validação ADB/Wireless Debugging") } } } } }
}
