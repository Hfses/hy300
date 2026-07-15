package com.hy300.remote.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() { override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { RemoteScreen() } } }

@Composable private fun RemoteScreen() {
    var host by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }; var text by remember { mutableStateOf("") }; var status by remember { mutableStateOf("Desconectado") }
    val remote = remember { RemoteConnection(LocalContext.current) { status = it } }; val scope = rememberCoroutineScope()
    DisposableEffect(Unit) { onDispose { remote.close() } }
    MaterialTheme { Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("HY300 Remote", style = MaterialTheme.typography.headlineSmall); Text(status)
        OutlinedTextField(host, { host = it }, label = { Text("IP:porta (ex.: 192.168.1.5:7300)") }, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button({ remote.connect(host) }) { Text("Conectar") }; OutlinedTextField(code, { code = it }, label = { Text("Código") }, modifier = Modifier.weight(1f)); Button({ remote.pair(code) }) { Text("Parear") } }
        Text("Touchpad")
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF263238)).pointerInput(Unit) { detectTransformGestures { _, pan, _, _ -> remote.send("pointer_move", "dx" to pan.x, "dy" to pan.y) } }.pointerInput(Unit) { detectTapGestures(onTap = { remote.send("pointer_click", "button" to "left", "action" to "tap") }, onLongPress = { remote.send("pointer_long_press") }) })
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { listOf("HOME", "BACK", "VOLUME_UP", "VOLUME_DOWN", "POWER").forEach { key -> Button({ remote.send("special_key", "key" to key) }) { Text(key.replace("_", " ")) } } }
        OutlinedTextField(text, { text = it }, label = { Text("Texto para o projetor") }, singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) { remote.send("text_input", "text" to text); text = "" } }), modifier = Modifier.fillMaxWidth())
    } }
}
