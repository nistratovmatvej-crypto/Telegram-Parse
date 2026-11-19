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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RadarBackground = Color(0xFF101113)
private val RadarSurface = Color(0xFF191B1F)
private val RadarText = Color(0xFFF2F3F5)
private val RadarMuted = Color(0xFF9298A2)
private val RadarAccent = Color(0xFFD7E1EE)
private val RadarLine = Color(0xFF2A2D33)

private val RadarColors = darkColorScheme(
    primary = RadarAccent,
    onPrimary = Color(0xFF111318),
    secondary = RadarMuted,
    background = RadarBackground,
    surface = RadarSurface,
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

private enum class MainTab { Search, Saved, Settings }

@Composable
private fun SocialRadarApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        containerColor = RadarBackground,
        bottomBar = {
            NavigationBar(
                containerColor = RadarBackground,
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
                    icon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = "Сохранено") },
                    label = { Text("База") }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(RadarBackground)
                .padding(insets)
        ) {
            when (selectedTab) {
                0 -> SearchScreen()
                1 -> SavedScreen()
                else -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun SearchScreen() {
    var query by rememberSaveable { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(RadarSurface, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = RadarAccent)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "social radar",
                color = RadarText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp
            )
            Spacer(Modifier.weight(1f))
            Text("TELEGRAM", color = RadarMuted, fontSize = 10.sp, letterSpacing = 1.4.sp)
        }

        Spacer(Modifier.height(58.dp))
        Text(
            "Найди своё\nсообщество.",
            color = RadarText,
            fontSize = 38.sp,
            lineHeight = 42.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1.8).sp
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Поиск публичных Telegram-групп, каналов и обсуждений.",
            color = RadarMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(30.dp))

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                status = null
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("Например: разработка игр с ИИ", color = RadarMuted)
            },
            minLines = 3,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = RadarText,
                unfocusedTextColor = RadarText,
                focusedBorderColor = RadarMuted,
                unfocusedBorderColor = RadarLine,
                cursorColor = RadarAccent,
                focusedContainerColor = RadarSurface,
                unfocusedContainerColor = RadarSurface
            )
        )

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                status = if (query.isBlank()) {
                    "Сначала введи тему поиска."
                } else {
                    "Поисковый движок подключается на следующем этапе. Запрос сохранён в текущем экране."
                }
            },
            enabled = query.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RadarAccent,
                contentColor = Color(0xFF111318),
                disabledContainerColor = RadarLine,
                disabledContentColor = RadarMuted
            )
        ) {
            Text("Начать поиск", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
        }

        if (status != null) {
            Spacer(Modifier.height(12.dp))
            Text(status!!, color = RadarMuted, fontSize = 12.sp, lineHeight = 17.sp)
        }

        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = RadarLine)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("РЕЖИМ", color = RadarMuted, fontSize = 10.sp, letterSpacing = 1.2.sp)
            Spacer(Modifier.weight(1f))
            Text("MAXIMUM  ·  PUBLIC", color = RadarText, fontSize = 10.sp, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun SavedScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("База", color = RadarText, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("Сохранённые сообщества появятся здесь после подключения поискового движка.", color = RadarMuted, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = RadarLine)
        Text("Пока ничего не сохранено", color = RadarMuted, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Настройки", color = RadarText, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Text("TELEGRAM", color = RadarMuted, fontSize = 10.sp, letterSpacing = 1.4.sp)
        Surface(color = RadarSurface, shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Подключение аккаунта", color = RadarText, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "MTProto / TDLib · обязательная авторизация. Экран входа появится после подключения нативной библиотеки TDLib.",
                    color = RadarMuted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(10.dp))
                Text("Не подключено", color = Color(0xFFD6B98C), fontSize = 12.sp)
            }
        }
        Text("ПОИСК", color = RadarMuted, fontSize = 10.sp, letterSpacing = 1.4.sp)
        SettingRow("Глубина исследования", "Максимальная")
        SettingRow("Типы результатов", "Все публичные")
        SettingRow("Хранение", "Локально на устройстве")
        Spacer(Modifier.height(8.dp))
        Text("Social Radar · 0.1.0-alpha", color = RadarMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SettingRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = RadarText, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = RadarMuted, fontSize = 12.sp)
    }
}
