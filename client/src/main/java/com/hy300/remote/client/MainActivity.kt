package com.hy300.remote.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() { override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { RemoteTheme { RemoteScreen() } } } }

// ---------- Theme ----------

private val Navy = Color(0xFF0D1B2A)
private val NavySurface = Color(0xFF15263A)
private val NavySurfaceHigh = Color(0xFF1D3450)
private val Teal = Color(0xFF2EC4B6)
private val TealDim = Color(0xFF17726B)
private val TextPrimary = Color(0xFFE8F1F2)
private val TextMuted = Color(0xFF8FA7B8)

@Composable private fun RemoteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Teal, onPrimary = Navy,
            secondary = TealDim, onSecondary = TextPrimary,
            background = Navy, onBackground = TextPrimary,
            surface = NavySurface, onSurface = TextPrimary,
            surfaceVariant = NavySurfaceHigh, onSurfaceVariant = TextMuted,
            outline = Color(0xFF2C4258),
        ),
        content = content,
    )
}

// ---------- Screen ----------

@Composable private fun RemoteScreen() {
    var host by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Desconectado") }
    val discovered = remember { mutableStateListOf<ProjectorEndpoint>() }
    val context = LocalContext.current
    val remote = remember { RemoteConnection(context) { status = it } }
    val discovery = remember { ProjectorDiscovery(context, { endpoint -> if (discovered.none { it.host == endpoint.host && it.port == endpoint.port }) discovered += endpoint }, { }) }
    DisposableEffect(Unit) { discovery.start(); onDispose { discovery.stop(); remote.close() } }

    val connected = status == "Conectado"
    var sensitivity by remember { mutableFloatStateOf(1.5f) }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Header(status = status, connected = connected)
        ConnectionCard(host = host, onHostChange = { host = it }, discovered = discovered, connected = connected, remote = remote)
        TouchpadCard(remote = remote, sensitivity = sensitivity)
        ClickRow(remote = remote)
        DpadCard(remote = remote)
        QuickActionsCard(remote = remote)
        MediaCard(remote = remote)
        TextCard(remote = remote)
        SettingsCard(sensitivity = sensitivity, onSensitivityChange = { sensitivity = it })
    }
}

// ---------- Header ----------

@Composable private fun Header(status: String, connected: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("HY300", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text("Controle remoto", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        StatusPill(status = status, connected = connected)
    }
}

@Composable private fun StatusPill(status: String, connected: Boolean) {
    val dotColor = if (connected) Teal else if (status == "Conectando" || status == "Reconectando") Color(0xFFE9C46A) else Color(0xFFE76F51)
    Row(
        Modifier
            .background(NavySurfaceHigh, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).background(dotColor, CircleShape))
        Text(status, style = MaterialTheme.typography.labelMedium, color = TextPrimary)
    }
}

// ---------- Connection ----------

@Composable private fun ConnectionCard(host: String, onHostChange: (String) -> Unit, discovered: List<ProjectorEndpoint>, connected: Boolean, remote: RemoteConnection) {
    var expanded by remember { mutableStateOf(true) }
    var code by remember { mutableStateOf("") }
    LaunchedEffect(connected) { if (connected) expanded = false }
    SectionCard {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("Conexão")
            Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = if (expanded) "Recolher" else "Expandir", tint = TextMuted)
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                if (discovered.isNotEmpty()) {
                    Text("Projetores encontrados", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                    discovered.forEach { endpoint ->
                        AssistChip(
                            onClick = { onHostChange("${endpoint.host}:${endpoint.port}") },
                            label = { Text("${endpoint.model} — ${endpoint.host}") },
                            colors = AssistChipDefaults.assistChipColors(labelColor = TextPrimary),
                        )
                    }
                }
                OutlinedTextField(
                    value = host,
                    onValueChange = onHostChange,
                    label = { Text("IP:porta (ex.: 192.168.1.5:7300)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(onClick = { remote.connect(host) }, modifier = Modifier.fillMaxWidth()) { Text("Conectar") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Código de pareamento") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { remote.pair(code) }) { Text("Parear") }
                }
            }
        }
    }
}

// ---------- Touchpad ----------

@Composable private fun TouchpadCard(remote: RemoteConnection, sensitivity: Float) {
    Row(Modifier.fillMaxWidth().height(320.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(NavySurface, RoundedCornerShape(20.dp))
                .pointerInput(sensitivity) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        remote.send("pointer_move", "dx" to dragAmount.x * sensitivity, "dy" to dragAmount.y * sensitivity)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { remote.send("pointer_click", "button" to "left", "action" to "tap") },
                        onDoubleTap = { remote.send("pointer_click", "button" to "left", "action" to "tap"); remote.send("pointer_click", "button" to "left", "action" to "tap") },
                        onLongPress = { remote.send("pointer_long_press") },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Touchpad", style = MaterialTheme.typography.titleMedium, color = TextMuted)
                Text("Arraste para mover · toque para clicar\nsegure para clique longo", style = MaterialTheme.typography.bodySmall, color = TextMuted.copy(alpha = 0.7f), textAlign = TextAlign.Center)
            }
        }
        // Scroll rail
        Box(
            Modifier
                .width(56.dp)
                .fillMaxHeight()
                .background(NavySurfaceHigh, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        remote.send("pointer_scroll", "delta" to dragAmount * 2f)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxHeight().padding(vertical = 14.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, tint = TextMuted)
                Text("Scroll", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
            }
        }
    }
}

@Composable private fun ClickRow(remote: RemoteConnection) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { remote.send("pointer_click", "button" to "left", "action" to "tap") },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Clique") }
        FilledTonalButton(
            onClick = { remote.send("pointer_long_press") },
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Clique longo") }
    }
}

// ---------- D-Pad ----------

@Composable private fun DpadCard(remote: RemoteConnection) {
    SectionCard {
        SectionTitle("Navegação")
        Text("Setas e OK exigem Shizuku no projetor", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(Modifier.height(10.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DpadButton(Icons.Default.KeyboardArrowUp, "Cima") { remote.send("key_event", "keycode" to 19) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                DpadButton(Icons.Default.KeyboardArrowLeft, "Esquerda") { remote.send("key_event", "keycode" to 21) }
                Button(
                    onClick = { remote.send("key_event", "keycode" to 23) },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                ) { Text("OK", fontWeight = FontWeight.Bold) }
                DpadButton(Icons.Default.KeyboardArrowRight, "Direita") { remote.send("key_event", "keycode" to 22) }
            }
            DpadButton(Icons.Default.KeyboardArrowDown, "Baixo") { remote.send("key_event", "keycode" to 20) }
        }
    }
}

@Composable private fun DpadButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(64.dp), shape = CircleShape) {
        Icon(icon, contentDescription = label)
    }
}

// ---------- Quick actions ----------

@Composable private fun QuickActionsCard(remote: RemoteConnection) {
    SectionCard {
        SectionTitle("Ações rápidas")
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(Modifier.weight(1f), icon = Icons.Default.ArrowBack, label = "Voltar") { remote.send("special_key", "key" to "BACK") }
            ActionButton(Modifier.weight(1f), icon = Icons.Default.Home, label = "Início") { remote.send("special_key", "key" to "HOME") }
            ActionButton(Modifier.weight(1f), icon = Icons.Default.Menu, label = "Recentes") { remote.send("special_key", "key" to "RECENTS") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(Modifier.weight(1f), label = "Vol −") { remote.send("special_key", "key" to "VOLUME_DOWN") }
            ActionButton(Modifier.weight(1f), label = "Vol +") { remote.send("special_key", "key" to "VOLUME_UP") }
            ActionButton(Modifier.weight(1f), label = "Energia") { remote.send("special_key", "key" to "POWER") }
        }
    }
}

@Composable private fun ActionButton(modifier: Modifier = Modifier, icon: ImageVector? = null, label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(56.dp), shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

// ---------- Media ----------

@Composable private fun MediaCard(remote: RemoteConnection) {
    SectionCard {
        SectionTitle("Mídia")
        Text("Requer Shizuku no projetor", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(Modifier.weight(1f), label = "Anterior") { remote.send("key_event", "keycode" to 88) }
            FilledTonalButton(
                onClick = { remote.send("key_event", "keycode" to 85) },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) { Icon(Icons.Default.PlayArrow, contentDescription = "Reproduzir ou pausar") }
            ActionButton(Modifier.weight(1f), label = "Próxima") { remote.send("key_event", "keycode" to 87) }
        }
    }
}

// ---------- Text input ----------

@Composable private fun TextCard(remote: RemoteConnection) {
    var text by remember { mutableStateOf("") }
    fun submit() { if (text.isNotBlank()) { remote.send("text_input", "text" to text); text = "" } }
    SectionCard {
        SectionTitle("Enviar texto")
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Texto para o projetor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = { submit() }, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.Send, contentDescription = "Enviar texto")
            }
        }
    }
}

// ---------- Settings ----------

@Composable private fun SettingsCard(sensitivity: Float, onSensitivityChange: (Float) -> Unit) {
    SectionCard {
        SectionTitle("Sensibilidade do cursor")
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Slider(value = sensitivity, onValueChange = onSensitivityChange, valueRange = 0.5f..3f, modifier = Modifier.weight(1f))
            Text("${"%.1f".format(sensitivity)}x", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
        }
    }
}

// ---------- Shared ----------

@Composable private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(NavySurface, RoundedCornerShape(20.dp))
            .padding(16.dp),
        content = content,
    )
}

@Composable private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
}
