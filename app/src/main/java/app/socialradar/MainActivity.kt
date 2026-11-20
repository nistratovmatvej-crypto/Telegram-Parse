package app.socialradar

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Bg = Color(0xFF080B10)
private val Surface1 = Color(0xFF10151D)
private val Surface2 = Color(0xFF151C26)
private val Surface3 = Color(0xFF1B2430)
private val TextPrimary = Color(0xFFF2F5F8)
private val TextSecondary = Color(0xFF8793A3)
private val Accent = Color(0xFF8BB7FF)
private val Accent2 = Color(0xFF58D6BD)
private val Border = Color(0xFF222D3A)
private val RadarScheme = darkColorScheme(
    primary = Accent, onPrimary = Bg, secondary = Accent2, background = Bg,
    surface = Surface1, onSurface = TextPrimary, onBackground = TextPrimary,
    outline = Border
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = RadarScheme) { SocialRadar() } }
    }
}

@Composable
private fun SocialRadar() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var history by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { TelegramSession(context) }
    LaunchedEffect(session) { if (session.apiId.isNotBlank()) session.connect() }
    DisposableEffect(session) { onDispose { session.close() } }
    val results by session.results.collectAsState()
    val auth by session.auth.collectAsState()
    val busy by session.busy.collectAsState()
    val sessionMessage by session.message.collectAsState()
    val wide = LocalConfiguration.current.screenWidthDp >= 700
    val saveQuery: (String) -> Unit = { raw ->
        val q = raw.trim()
        if (q.isNotEmpty()) history = listOf(q) + history.filterNot { it.equals(q, true) }.take(29)
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF0B1421), Bg, Bg)))) {
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRailPanel(tab, { tab = it }, Modifier.width(240.dp).fillMaxHeight())
                AnimatedContent(tab, label = "page", modifier = Modifier.weight(1f)) { page ->
                    Page(page, history, saveQuery, { tab = 0 }, session, results, auth, busy, sessionMessage)
                }
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF0D1219), tonalElevation = 0.dp) {
                        NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Outlined.Search, null) }, label = { Text("Поиск") })
                        NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Outlined.History, null) }, label = { Text("История") })
                        NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Outlined.Settings, null) }, label = { Text("Настройки") })
                    }
                }
            ) { padding ->
                AnimatedContent(tab, label = "screen", modifier = Modifier.fillMaxSize().padding(padding)) { page ->
                    Page(page, history, saveQuery, { tab = 0 }, session, results, auth, busy, sessionMessage)
                }
            }
        }
    }
}

@Composable
private fun NavigationRailPanel(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.background(Color(0xB90D1219)).padding(20.dp)) {
        Brand()
        Spacer(Modifier.height(40.dp))
        Text("WORKSPACE", color = TextSecondary, fontSize = 10.sp, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 10.dp, bottom = 12.dp))
        NavItem("Исследование", Icons.Outlined.Search, tab == 0) { onTab(0) }
        NavItem("История", Icons.Outlined.History, tab == 1) { onTab(1) }
        Spacer(Modifier.weight(1f))
        NavItem("Настройки", Icons.Outlined.Settings, tab == 2) { onTab(2) }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(Accent2, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text("LOCAL · PRIVATE", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(Brush.linearGradient(listOf(Accent, Color(0xFF5079B7))), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Search, null, tint = Bg, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text("SOCIAL RADAR", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
            Text("TELEGRAM DISCOVERY", color = TextSecondary, fontSize = 8.sp, letterSpacing = 1.2.sp)
        }
    }
}

@Composable
private fun NavItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, action: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).background(if (active) Surface3 else Color.Transparent, RoundedCornerShape(12.dp)).clickable { action() }.padding(horizontal = 12.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (active) Accent else TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = if (active) TextPrimary else TextSecondary, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun Page(tab: Int, history: List<String>, onSave: (String) -> Unit, goSearch: () -> Unit, session: TelegramSession, results: List<RadarChat>, auth: RadarAuth, busy: Boolean, sessionMessage: String) {
    when (tab) {
        0 -> SearchPage(onSave, session, results, auth, busy, sessionMessage)
        1 -> HistoryPage(history, goSearch)
        else -> SettingsPage(session, auth, sessionMessage)
    }
}

@Composable
private fun SearchPage(onSave: (String) -> Unit, session: TelegramSession, results: List<RadarChat>, auth: RadarAuth, busy: Boolean, sessionMessage: String) {
    var query by rememberSaveable { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    val wide = LocalConfiguration.current.screenWidthDp >= 700
    val infinite = rememberInfiniteTransition(label = "ambient")
    val halo by infinite.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "halo")
    Column(Modifier.fillMaxSize().padding(horizontal = if (wide) 48.dp else 22.dp, vertical = if (wide) 34.dp else 25.dp)) {
        Brand()
        Spacer(Modifier.height(36.dp))
        Text("DISCOVER PUBLIC COMMUNITIES", color = Accent2, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
        Spacer(Modifier.height(8.dp))
        Text("Find your signal.", color = TextPrimary, fontSize = if (wide) 35.sp else 29.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp)
        Text("Search beyond the obvious.", color = TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(24.dp))
        Surface(color = Surface1, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B394A)), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, null, tint = Accent, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SEARCH INTELLIGENCE", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("RU  /  EN", color = TextSecondary, fontSize = 9.sp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(value = query, onValueChange = { query = it; notice = "" }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Тема, интерес или ключевые слова", color = Color(0xFF667384)) }, minLines = 2, shape = RoundedCornerShape(15.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary, focusedBorderColor = Accent.copy(alpha = .5f), unfocusedBorderColor = Border, cursorColor = Accent, focusedContainerColor = Bg, unfocusedContainerColor = Bg))
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).scale(halo).background(Accent2, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("Public sources only", color = TextSecondary, fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { if (query.isNotBlank()) { onSave(query); session.search(query); notice = "" } }, enabled = query.isNotBlank() && !busy, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg, disabledContainerColor = Surface3, disabledContentColor = TextSecondary)) {
                        Text(if (busy) "Searching…" else "Search", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.ArrowUpward, null, modifier = Modifier.size(14.dp))
                    }
                }
                AnimatedVisibility(notice.isNotEmpty(), enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut() + slideOutVertically { it / 3 }) {
                    Text(notice, color = TextSecondary, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Results", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Surface(color = Surface3, shape = RoundedCornerShape(7.dp)) { Text(results.size.toString(), color = TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
            Spacer(Modifier.weight(1f))
            Text("LIVE SEARCH", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(12.dp))
        Surface(Modifier.fillMaxWidth().weight(1f).animateContentSize(), color = Surface1.copy(alpha = .8f), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            if (results.isNotEmpty()) {
                LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results, key = { it.id }) { chat ->
                        Row(Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(14.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).background(Color(0xFF1A2E42), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Search, null, tint = Accent) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(chat.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                Text(chat.type, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(66.dp).scale(halo).background(Brush.radialGradient(listOf(Color(0xFF1B3550), Surface2)), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Search, null, tint = Accent, modifier = Modifier.size(25.dp))
                        }
                        Spacer(Modifier.height(18.dp))
                        Text(if (auth.ready) "Public Telegram search" else "Connect Telegram to search", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(7.dp))
                        Text(sessionMessage.ifBlank { if (auth.ready) "Введите запрос для поиска публичных каналов и групп." else "Подключение аккаунта и API credentials — в настройках." }, color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(16.dp))
                        Text("CHANNELS  ·  GROUPS  ·  DISCUSSIONS", color = TextSecondary, fontSize = 8.sp, letterSpacing = 1.1.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Social Radar  ·  0.1.0 alpha", color = TextSecondary.copy(alpha = .65f), fontSize = 9.sp)
    }
}

@Composable
private fun HistoryPage(history: List<String>, goSearch: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("ACTIVITY", color = Accent2, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))
        Text("Search history", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.7).sp)
        Spacer(Modifier.height(20.dp))
        if (history.isEmpty()) {
            EmptyCard("Nothing here yet", "Your recent queries will appear here on this device.", "Start exploring", goSearch)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(history) { q ->
                    Row(Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(15.dp)).clickable { goSearch() }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.History, null, tint = Accent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(q, color = TextPrimary, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(title: String, desc: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Surface1, RoundedCornerShape(18.dp)).padding(22.dp)) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        Spacer(Modifier.height(7.dp))
        Text(desc, color = TextSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(15.dp))
        Row(Modifier.clickable { onAction() }, verticalAlignment = Alignment.CenterVertically) {
            Text(action, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(5.dp))
            Icon(Icons.Outlined.ArrowForward, null, tint = Accent, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun SettingsPage(session: TelegramSession, auth: RadarAuth, sessionMessage: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCredentials by remember { mutableStateOf(false) }
    var credentialId by remember { mutableStateOf(session.apiId) }
    var credentialHash by remember { mutableStateOf(session.apiHash) }
    var authInput by remember { mutableStateOf("") }
    var authDialogShown by remember { mutableStateOf(false) }
    var lastAuthDialogState by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        Text("CONFIGURATION", color = Accent2, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
        Text("Settings", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.7).sp)
        Spacer(Modifier.height(4.dp))
        Text("TELEGRAM ACCOUNT", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.SemiBold)
        Surface(color = Surface1, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(Modifier.fillMaxWidth().padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(Color(0xFF1A2E42), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Search, null, tint = Accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Telegram / MTProto", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(auth.label, color = TextSecondary, fontSize = 10.sp)
                    }
                    Surface(color = Color(0xFF332A1E), shape = RoundedCornerShape(7.dp)) {
                        Text(if (auth.ready) "ONLINE" else "OFFLINE", color = if (auth.ready) Accent2 else Color(0xFFE0B77E), fontSize = 8.sp, letterSpacing = .6.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                    }
                }
                Spacer(Modifier.height(13.dp))
                Text("TDLib engine is included. API credentials are stored locally; phone, login code and 2FA are handled inside this app.", color = TextSecondary, fontSize = 11.sp, lineHeight = 17.sp)
                if (sessionMessage.isNotBlank()) Text(sessionMessage, color = TextSecondary, fontSize = 10.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(12.dp))
                Text(if (auth.ready) "Disconnect account" else "Connect / configure ↗", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { if (auth.ready) session.logout() else showCredentials = true })
            }
        }
        Text("SEARCH", color = TextSecondary, fontSize = 9.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.SemiBold)
        SettingLine("Search strategy", "Phrase + keywords")
        SettingLine("Source scope", "Public communities")
        SettingLine("Storage", "Local device")
        Spacer(Modifier.height(7.dp))
        HorizontalDivider(color = Border)
        Text("Social Radar · 0.1.0-alpha", color = TextSecondary, fontSize = 10.sp)
    }
    if (showCredentials) AlertDialog(
        onDismissRequest = { showCredentials = false },
        title = { Text("Telegram API credentials") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Получите API ID и API Hash на my.telegram.org/apps. Они нужны официальному MTProto-клиенту.", fontSize = 12.sp)
            OutlinedTextField(credentialId, { credentialId = it }, label = { Text("API ID") }, singleLine = true)
            OutlinedTextField(credentialHash, { credentialHash = it }, label = { Text("API Hash") }, singleLine = true)
        } },
        confirmButton = { TextButton(onClick = { showCredentials = false; session.configure(credentialId, credentialHash) }) { Text("Запустить TDLib") } },
        dismissButton = { TextButton(onClick = { showCredentials = false }) { Text("Отмена") } }
    )
    val needsAuthInput = auth.phoneRequired || auth.codeRequired || auth.passwordRequired || auth.emailRequired || auth.emailCodeRequired || auth.registrationRequired
    LaunchedEffect(auth.label) { if (needsAuthInput && lastAuthDialogState != auth.label) { lastAuthDialogState = auth.label; authDialogShown = true; authInput = "" } }
    if (needsAuthInput && authDialogShown) AlertDialog(
        onDismissRequest = { authDialogShown = false },
        title = { Text(if (auth.phoneRequired) "Номер телефона" else if (auth.codeRequired) "Код Telegram" else if (auth.passwordRequired) "Пароль 2FA" else if (auth.emailRequired) "Email" else if (auth.emailCodeRequired) "Email-код" else "Ваше имя") },
        text = { OutlinedTextField(authInput, { authInput = it }, label = { Text(if (auth.phoneRequired) "+номер телефона" else if (auth.codeRequired) "Код из Telegram" else if (auth.passwordRequired) "Пароль 2FA" else if (auth.emailRequired) "Email" else if (auth.emailCodeRequired) "Код из email" else "Имя") }, visualTransformation = if (auth.passwordRequired) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, singleLine = true) },
        confirmButton = { TextButton(onClick = { if (auth.phoneRequired) session.submitPhone(authInput) else if (auth.codeRequired) session.submitCode(authInput) else if (auth.passwordRequired) session.submitPassword(authInput) else if (auth.emailRequired) session.submitEmail(authInput) else if (auth.emailCodeRequired) session.submitEmailCode(authInput) else session.register(authInput); authDialogShown = true }) { Text("Отправить") } },
        dismissButton = { TextButton(onClick = { authDialogShown = false }) { Text("Позже") } }
    )
}

@Composable
private fun SettingLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextPrimary, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = TextSecondary, fontSize = 11.sp)
    }
}
