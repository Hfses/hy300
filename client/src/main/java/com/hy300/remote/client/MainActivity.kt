package com.hy300.remote.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() { override fun onCreate(state: Bundle?) { super.onCreate(state); setContent { AllyTheme { RemoteScreen() } } } }

// ---------- Tema (paleta Ally Remote) ----------

private val Bg = Color(0xFF0A0B0E)
private val Panel = Color(0xFF13151B)
private val Panel2 = Color(0xFF1A1D25)
private val Line = Color(0xFF252A35)
private val Txt = Color(0xFFEEF1F5)
private val Dim = Color(0xFF8B94A3)
private val Red = Color(0xFFFF4438)
private val Red2 = Color(0xFFFF7A54)
private val Ok = Color(0xFF3DDC84)
private val Warn = Color(0xFFFFB020)

private val Mono = FontFamily.Monospace

@Composable private fun AllyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Red, onPrimary = Color.White,
            secondary = Panel2, onSecondary = Txt,
            background = Bg, onBackground = Txt,
            surface = Panel, onSurface = Txt,
            surfaceVariant = Panel2, onSurfaceVariant = Dim,
            outline = Line,
        ),
        content = content,
    )
}

// ---------- Tela ----------

private enum class Tab(val label: String) { MOUSE("MOUSE"), KBD("TECLADO"), SYS("SISTEMA") }

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
    var tab by remember { mutableStateOf(Tab.MOUSE) }
    var showSetup by remember { mutableStateOf(true) }
    LaunchedEffect(connected) { if (connected) showSetup = false }

    Column(Modifier.fillMaxSize().background(Bg)) {
        if (showSetup || !connected) {
            SetupScreen(
                host = host, onHostChange = { host = it },
                discovered = discovered, status = status, remote = remote,
            )
        } else {
            HeaderBar(status = status, connected = connected, onChangeHost = { showSetup = true })
            Box(Modifier.weight(1f).padding(10.dp)) {
                when (tab) {
                    Tab.MOUSE -> MouseView(remote, sensitivity)
                    Tab.KBD -> KeyboardView(remote)
                    Tab.SYS -> SystemView(remote, host, sensitivity, { sensitivity = it }, onDisconnect = { showSetup = true })
                }
            }
            NavBar(tab = tab, onSelect = { tab = it })
        }
    }
}

// ---------- Logo ----------

@Composable private fun LogoText(size: Int = 15) {
    Text(
        buildAnnotatedString {
            append("HY300")
            withStyle(SpanStyle(color = Red)) { append("\u27CB\u27CB") }
            append("REMOTE")
        },
        color = Txt, fontFamily = Mono, fontWeight = FontWeight.ExtraBold,
        fontSize = size.sp, letterSpacing = (size * 0.14).sp,
    )
}

// ---------- Tela de conexão ----------

@Composable private fun SetupScreen(host: String, onHostChange: (String) -> Unit, discovered: List<ProjectorEndpoint>, status: String, remote: RemoteConnection) {
    var code by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LogoText(size = 26)
        Text(
            "Controle o projetor HY300 pelo celular.\nAbra o HY300 Receiver no projetor e conecte:",
            color = Dim, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 19.sp,
            modifier = Modifier.padding(top = 10.dp, bottom = 26.dp),
        )
        if (discovered.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                discovered.forEach { endpoint ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Panel).border(1.dp, Line, RoundedCornerShape(12.dp))
                            .clickable { onHostChange("${endpoint.host}:${endpoint.port}"); remote.connect("${endpoint.host}:${endpoint.port}") }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(endpoint.model, color = Txt, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${endpoint.host}:${endpoint.port}", color = Dim, fontFamily = Mono, fontSize = 11.sp)
                        }
                        Text("CONECTAR", color = Red2, fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
            }
        }
        MonoField(value = host, onValueChange = onHostChange, placeholder = "IP do projetor (ex.: 192.168.1.5:7300)")
        Spacer(Modifier.height(12.dp))
        RedButton(text = "CONECTAR", modifier = Modifier.fillMaxWidth()) { remote.connect(host) }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                MonoField(value = code, onValueChange = { code = it }, placeholder = "Código de pareamento")
            }
            PanelButton(text = "PAREAR", modifier = Modifier.height(52.dp)) { remote.pair(code) }
        }
        Text(
            statusMessage(status),
            color = if (status == "Conectando" || status == "Reconectando") Warn else Dim,
            fontFamily = Mono, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 19.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
    }
}

private fun statusMessage(status: String) = when (status) {
    "Desconectado" -> "O HY300 Receiver está aberto no projetor?\nCelular no mesmo Wi-Fi?"
    else -> status
}

// ---------- Header ----------

@Composable private fun HeaderBar(status: String, connected: Boolean, onChangeHost: () -> Unit) {
    val dotColor = if (connected) Ok else if (status == "Conectando" || status == "Reconectando") Warn else Red
    Row(
        Modifier.fillMaxWidth().background(Panel).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).background(dotColor, CircleShape))
        LogoText(size = 15)
        Spacer(Modifier.weight(1f))
        Text(
            "\u27F2",
            color = Txt, fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Panel2).border(1.dp, Line, RoundedCornerShape(10.dp))
                .clickable(onClick = onChangeHost)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
    HorizontalDivider(color = Line, thickness = 1.dp)
}

// ---------- Nav inferior ----------

@Composable private fun NavBar(tab: Tab, onSelect: (Tab) -> Unit) {
    HorizontalDivider(color = Line, thickness = 1.dp)
    Row(Modifier.fillMaxWidth().background(Panel).navigationBarsPadding()) {
        Tab.entries.forEach { item ->
            val active = item == tab
            Column(
                Modifier.weight(1f).clickable { onSelect(item) }.padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    when (item) { Tab.MOUSE -> "\u25C9"; Tab.KBD -> "\u2328"; Tab.SYS -> "\u2699" },
                    color = if (active) Red2 else Dim, fontSize = 19.sp,
                )
                Text(
                    item.label,
                    color = if (active) Red2 else Dim, fontFamily = Mono,
                    fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp, letterSpacing = 2.sp,
                )
            }
        }
    }
}

// ---------- Aba MOUSE ----------

@Composable private fun MouseView(remote: RemoteConnection, sensitivity: Float) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Touchpad com grade
            Box(
                Modifier
                    .weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Panel)
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
                    .drawBehind {
                        val step = 34.dp.toPx()
                        var x = step
                        while (x < size.width) { drawLine(Line.copy(alpha = 0.35f), Offset(x, 0f), Offset(x, size.height), 1f); x += step }
                        var y = step
                        while (y < size.height) { drawLine(Line.copy(alpha = 0.35f), Offset(0f, y), Offset(size.width, y), 1f); y += step }
                    }
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
                Text(
                    "arraste = mover \u00B7 toque = clique\nsegure = clique longo",
                    color = Dim, fontFamily = Mono, fontSize = 11.sp,
                    textAlign = TextAlign.Center, lineHeight = 22.sp, letterSpacing = 0.5.sp,
                )
            }
            // Rail de scroll
            Column(
                Modifier
                    .width(52.dp).fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Panel2)
                    .border(1.dp, Line, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            remote.send("pointer_scroll", "delta" to dragAmount * 2f)
                        }
                    }
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("\u25B2", color = Dim, fontSize = 13.sp)
                Text("S\nC\nR\nO\nL\nL", color = Dim, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 2.sp, lineHeight = 13.sp, textAlign = TextAlign.Center)
                Text("\u25BC", color = Dim, fontSize = 13.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelButton("CLIQUE", Modifier.weight(1f).height(52.dp)) { remote.send("pointer_click", "button" to "left", "action" to "tap") }
            PanelButton("CLIQUE LONGO", Modifier.weight(1f).height(52.dp)) { remote.send("pointer_long_press") }
        }
    }
}

// ---------- Aba TECLADO ----------

@Composable private fun KeyboardView(remote: RemoteConnection) {
    var text by remember { mutableStateOf("") }
    fun submit() { if (text.isNotBlank()) { remote.send("text_input", "text" to text); text = "" } }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                MonoField(value = text, onValueChange = { text = it }, placeholder = "Digitar no projetor\u2026", imeSend = true, onSend = { submit() })
            }
            RedButton("ENVIAR", Modifier.height(52.dp)) { submit() }
        }

        KLabel("NAVEGA\u00C7\u00C3O")
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            KeyButton("\u25B2", Modifier.width(76.dp)) { remote.send("key_event", "keycode" to 19) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                KeyButton("\u25C0", Modifier.width(76.dp)) { remote.send("key_event", "keycode" to 21) }
                KeyButton("OK", Modifier.width(76.dp), accent = true) { remote.send("key_event", "keycode" to 23) }
                KeyButton("\u25B6", Modifier.width(76.dp)) { remote.send("key_event", "keycode" to 22) }
            }
            KeyButton("\u25BC", Modifier.width(76.dp)) { remote.send("key_event", "keycode" to 20) }
        }

        KLabel("M\u00CDDIA \u00B7 VOLUME")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            KeyButton("\u23EE", Modifier.weight(1f)) { remote.send("key_event", "keycode" to 88) }
            KeyButton("\u25B6\u2758\u2758", Modifier.weight(1f)) { remote.send("key_event", "keycode" to 85) }
            KeyButton("\u23ED", Modifier.weight(1f)) { remote.send("key_event", "keycode" to 87) }
            KeyButton("VOL\u2212", Modifier.weight(1f)) { remote.send("special_key", "key" to "VOLUME_DOWN") }
            KeyButton("VOL+", Modifier.weight(1f)) { remote.send("special_key", "key" to "VOLUME_UP") }
        }
    }
}

// ---------- Aba SISTEMA ----------

@Composable private fun SystemView(remote: RemoteConnection, host: String, sensitivity: Float, onSensitivityChange: (Float) -> Unit, onDisconnect: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SysCard("A\u00C7\u00D5ES") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("VOLTAR", Modifier.weight(1f)) { remote.send("special_key", "key" to "BACK") }
                KeyButton("IN\u00CDCIO", Modifier.weight(1f)) { remote.send("special_key", "key" to "HOME") }
                KeyButton("RECENTES", Modifier.weight(1f)) { remote.send("special_key", "key" to "RECENTS") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyButton("VOL \u2212", Modifier.weight(1f)) { remote.send("special_key", "key" to "VOLUME_DOWN") }
                KeyButton("VOL +", Modifier.weight(1f)) { remote.send("special_key", "key" to "VOLUME_UP") }
            }
            DangerButton("ENERGIA \u00B7 DESLIGAR TELA") { remote.send("special_key", "key" to "POWER") }
        }

        SysCard("CURSOR") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Slider(
                    value = sensitivity, onValueChange = onSensitivityChange, valueRange = 0.5f..3f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(thumbColor = Red, activeTrackColor = Red, inactiveTrackColor = Panel2),
                )
                Text("${"%.1f".format(sensitivity)}x", color = Txt, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text("Sensibilidade do cursor no projetor", color = Dim, fontFamily = Mono, fontSize = 11.sp)
        }

        SysCard("CONEX\u00C3O") {
            Text(host.ifBlank { "\u2014" }, color = Txt, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            PanelButton("TROCAR PROJETOR", Modifier.fillMaxWidth().height(48.dp)) { onDisconnect() }
        }
    }
}

// ---------- Componentes compartilhados ----------

@Composable private fun KLabel(text: String) {
    Text(text, color = Dim, fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 3.sp, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
}

@Composable private fun SysCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Panel).border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Text(title, color = Dim, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 3.5.sp)
        content()
    }
}

@Composable private fun KeyButton(text: String, modifier: Modifier = Modifier, accent: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (accent) Red else Panel2)
            .border(1.dp, if (accent) Red else Line, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (accent) Color.White else Dim, fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
    }
}

@Composable private fun PanelButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Panel2)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Txt, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.5.sp, maxLines = 1)
    }
}

@Composable private fun RedButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Red)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, maxLines = 1)
    }
}

@Composable private fun DangerButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Red.copy(alpha = 0.08f))
            .border(1.dp, Red.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Red2, fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.5.sp)
    }
}

@Composable private fun MonoField(value: String, onValueChange: (String) -> Unit, placeholder: String, imeSend: Boolean = false, onSend: () -> Unit = {}) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Dim, fontFamily = Mono, fontSize = 14.sp) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontFamily = Mono, fontSize = 15.sp, color = Txt),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = if (imeSend) KeyboardOptions(imeAction = ImeAction.Send) else KeyboardOptions.Default,
        keyboardActions = if (imeSend) KeyboardActions(onSend = { onSend() }) else KeyboardActions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Red, unfocusedBorderColor = Line,
            focusedContainerColor = Panel, unfocusedContainerColor = Panel,
            cursorColor = Red,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}
