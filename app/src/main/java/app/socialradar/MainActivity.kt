package app.socialradar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RadarBackground = Color(0xFF0B0D10)
private val RadarPanel = Color(0xFF12161B)
private val RadarPanelRaised = Color(0xFF181D23)
private val RadarText = Color(0xFFF1F3F5)
private val RadarMuted = Color(0xFF89929D)
private val RadarAccent = Color(0xFFD9E2EC)
private val RadarAccentInk = Color(0xFF111820)
private val RadarLine = Color(0xFF252C34)
private val RadarGreen = Color(0xFF91B8A1)

private val RadarColors = darkColorScheme(
    primary = RadarAccent,
    onPrimary = RadarAccentInk,
    secondary = RadarMuted,
    background = RadarBackground,
    surface = RadarPanel,
    onBackground = RadarText,
    onSurface = RadarText,
    outline = RadarLine
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = RadarColors) {
                SocialRadarApp()
            }
        }
    }
}

private enum class MainTab { Search, History, Settings }

@Composable
private fun SocialRadarApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var queryHistory by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val wideLayout = LocalConfiguration.current.screenWidthDp >= 700

    val onSearch: (String) -> Unit = { value ->
        val normalized = value.trim()
        if (normalized.isNotEmpty()) {
            queryHistory = listOf(normalized) + queryHistory.filterNot { it.equals(normalized, true) }.take(29)
        }
    }

    if (wideLayout) {
        Row(
            modifier = Modifier.fillMaxSize().background(RadarBackground)
        ) {
            Sidebar(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it },
                modifier = Modifier.width(236.dp).fillMaxHeight()
            )
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) {
                MainContent(
                    selectedTab = selectedTab,
                    history = queryHistory,
                    onSearch = onSearch,
                    onSelectSearch = { selectedTab = 0 }
                )
            }
        }
    } else {
        Scaffold(
            containerColor = RadarBackground,
            contentWindowInsets = WindowInsets.navigationBars,
            bottomBar = {
                NavigationBar(
                    containerColor = RadarPanel,
                    contentColor = RadarText,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Outlined.Search, contentDescription = "Поиск") },
                        label = { Text("Поиск") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Outlined.History, contentDescription = "История") },
                        label = { Text("История") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = "Настройки") },
                        label = { Text("Настройки") }
                    )
                }
            }
        ) { insets ->
            Box(Modifier.fillMaxSize().padding(insets)) {
                MainContent(
                    selectedTab = selectedTab,
                    history = queryHistory,
                    onSearch = onSearch,
                    onSelectSearch = { selectedTab = 0 }
                )
            }
        }
    }
}

@Composable
private fun Sidebar(
    selectedTab: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(RadarPanel)
            .padding(horizontal = 18.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).background(RadarAccent, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = RadarAccentInk, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text("SOCIAL RADAR", color = RadarText, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
                Text("TELEGRAM DISCOVERY", color = RadarMuted, fontSize = 8.sp, letterSpacing = 1.1.sp)
            }
        }
        Spacer(Modifier.height(38.dp))
        Text("РАБОЧЕЕ ПРОСТРАНСТВО", color = RadarMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.25.sp, modifier = Modifier.padding(start = 10.dp, bottom = 12.dp))
        SidebarItem("Поиск", Icons.Outlined.Search, selectedTab == 0) { onSelect(0) }
        SidebarItem("История запросов", Icons.Outlined.History, selectedTab == 1) { onSelect(1) }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = RadarLine)
        Spacer(Modifier.height(14.dp))
        SidebarItem("Настройки", Icons.Outlined.Settings, selectedTab == 2) { onSelect(2) }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Box(Modifier.size(30.dp).background(RadarPanelRaised, CircleShape), contentAlignment = Alignment.Center) {
                Text("SR", color = RadarAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Локальный профиль", color = RadarText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("Данные на устройстве", color = RadarMuted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SidebarItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(if (selected) RadarPanelRaised else Color.Transparent, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) RadarAccent else RadarMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(title, color = if (selected) RadarText else RadarMuted, fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun MainContent(
    selectedTab: Int,
    history: List<String>,
    onSearch: (String) -> Unit,
    onSelectSearch: () -> Unit
) {
    when (selectedTab) {
        0 -> SearchScreen(onSearch)
        1 -> HistoryScreen(history, onSelectSearch)
        else -> SettingsScreen()
    }
}

@Composable
private fun SearchScreen(onSearch: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    val wideLayout = LocalConfiguration.current.screenWidthDp >= 700

    Column(
        modifier = Modifier.fillMaxSize().padding(
            horizontal = if (wideLayout) 42.dp else 22.dp,
            vertical = if (wideLayout) 28.dp else 20.dp
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("ПОИСК СООБЩЕСТВ", color = RadarMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.6.sp)
                Text("Найди свою аудиторию.", color = RadarText, fontSize = if (wideLayout) 27.sp else 23.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.7).sp)
            }
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(38.dp).background(RadarPanelRaised, CircleShape), contentAlignment = Alignment.Center) {
                Text("SR", color = RadarAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(if (wideLayout) 28.dp else 22.dp))
        Surface(
            color = RadarPanel,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RadarLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(if (wideLayout) 20.dp else 15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = RadarMuted, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("ТЕМА ИЛИ КЛЮЧЕВЫЕ СЛОВА", color = RadarMuted, fontSize = 9.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("RU · EN", color = RadarMuted, fontSize = 9.sp, letterSpacing = 0.8.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; status = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Например, независимые разработчики игр", color = Color(0xFF66717D), fontSize = 14.sp) },
                    minLines = if (wideLayout) 2 else 2,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = RadarText,
                        unfocusedTextColor = RadarText,
                        focusedBorderColor = Color(0xFF657789),
                        unfocusedBorderColor = RadarLine,
                        cursorColor = RadarAccent,
                        focusedContainerColor = RadarBackground,
                        unfocusedContainerColor = RadarBackground
                    )
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(RadarGreen, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text("Только публичные сообщества", color = RadarMuted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (query.isNotBlank()) {
                                onSearch(query)
                                status = "Запрос добавлен в историю. Подключение Telegram-поиска ещё в разработке."
                            }
                        },
                        enabled = query.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RadarAccent,
                            contentColor = RadarAccentInk,
                            disabledContainerColor = RadarPanelRaised,
                            disabledContentColor = RadarMuted
                        )
                    ) {
                        Text("Найти", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(7.dp))
                        Icon(Icons.Outlined.ArrowUpward, contentDescription = null, modifier = Modifier.size(15.dp))
                    }
                }
                if (status != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(status!!, color = RadarMuted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Результаты", color = RadarText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(9.dp))
            Surface(color = RadarPanelRaised, shape = RoundedCornerShape(6.dp)) {
                Text("0", color = RadarMuted, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Tune, contentDescription = null, tint = RadarMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text("ФИЛЬТРЫ", color = RadarMuted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = RadarPanel,
            shape = RoundedCornerShape(18.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, RadarLine)
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(54.dp).background(RadarPanelRaised, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = RadarAccent, modifier = Modifier.size(23.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Здесь появятся сообщества", color = RadarText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Введи тему выше, чтобы подготовить поиск публичных Telegram-каналов, групп и обсуждений.",
                        color = RadarMuted,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.widthIn(max = 340.dp)
                    )
                    Spacer(Modifier.height(18.dp))
                    Text("MAXIMUM SEARCH  ·  PUBLIC DATA", color = RadarMuted, fontSize = 8.sp, letterSpacing = 1.2.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Social Radar  0.1.0-alpha  ·  Результаты поиска будут храниться на устройстве", color = RadarMuted, fontSize = 9.sp)
    }
}

@Composable
private fun HistoryScreen(history: List<String>, onSelectSearch: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("ИСТОРИЯ", color = RadarMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(5.dp))
        Text("История запросов", color = RadarText, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.7).sp)
        Spacer(Modifier.height(20.dp))
        if (history.isEmpty()) {
            EmptySection(
                title = "Пока нет запросов",
                description = "Все выполненные поисковые запросы будут собираться здесь.",
                action = "Перейти к поиску",
                onAction = onSelectSearch
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { item ->
                    Row(
                        Modifier.fillMaxWidth()
                            .background(RadarPanel, RoundedCornerShape(12.dp))
                            .clickable(onClick = onSelectSearch)
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = RadarMuted, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(item, color = RadarText, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySection(title: String, description: String, action: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(RadarPanel, RoundedCornerShape(16.dp)).padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(title, color = RadarText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(description, color = RadarMuted, fontSize = 12.sp, lineHeight = 18.sp)
        Spacer(Modifier.height(15.dp))
        Text(action, color = RadarAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onAction))
    }
}

@Composable
private fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Text("ПАРАМЕТРЫ ПРИЛОЖЕНИЯ", color = RadarMuted, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.5.sp)
        Text("Настройки", color = RadarText, fontSize = 27.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.7).sp)
        Spacer(Modifier.height(3.dp))
        Text("TELEGRAM", color = RadarMuted, fontSize = 9.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.SemiBold)
        Surface(color = RadarPanel, shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, RadarLine)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Подключение аккаунта", color = RadarText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Surface(color = Color(0xFF342B20), shape = RoundedCornerShape(6.dp)) {
                        Text("НЕ ПОДКЛЮЧЕНО", color = Color(0xFFD8B98D), fontSize = 8.sp, letterSpacing = 0.5.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Авторизация через MTProto / TDLib. Экран входа появится после интеграции нативной библиотеки Telegram.",
                    color = RadarMuted,
                    fontSize = 11.sp,
                    lineHeight = 17.sp
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text("ПОИСК", color = RadarMuted, fontSize = 9.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.SemiBold)
        SettingsRow("Глубина исследования", "Максимальная")
        SettingsRow("Типы результатов", "Все публичные")
        SettingsRow("Языки расширения", "Русский · English")
        SettingsRow("Хранение данных", "На устройстве")
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = RadarLine)
        Text("Social Radar · 0.1.0-alpha", color = RadarMuted, fontSize = 10.sp)
    }
}

@Composable
private fun SettingsRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = RadarText, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = RadarMuted, fontSize = 11.sp)
    }
}
