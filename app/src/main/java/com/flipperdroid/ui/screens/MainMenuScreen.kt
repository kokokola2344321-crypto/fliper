package com.flipperdroid.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipperdroid.ui.theme.FlipperColors
import com.flipperdroid.modules.root.RootShell

data class MenuItem(
    val title: String,
    val subtitle: String,
    val icon: String,
    val route: String
)

@Composable
fun MainMenuScreen(onNavigate: (String) -> Unit) {
    val menuItems = listOf(
        MenuItem("WiFi", "Атаки, сканирование, handshake", "📡", "wifi"),
        MenuItem("BLE", "Сканирование и Spam", "🔵", "ble"),
        MenuItem("IR", "TV-B-Gone и пульты", "🔴", "ir"),
        MenuItem("About", "О приложении и root", "ℹ️", "about"),
    )

    var selectedIndex by remember { mutableStateOf(0) }
    var isRootChecked by remember { mutableStateOf(false) }
    var rootStatus by remember { mutableStateOf("Проверка...") }

    LaunchedEffect(Unit) {
        val hasRoot = RootShell.checkRoot()
        isRootChecked = true
        rootStatus = if (hasRoot) "✅ Root доступен" else "❌ Root НЕ найден"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = FlipperColors.Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header - Flipper style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FlipperColors.Surface, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .border(1.dp, FlipperColors.Border, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "FlipperDroid",
                        color = FlipperColors.Primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rootStatus,
                        color = if (rootStatus.contains("✅")) FlipperColors.Accent else FlipperColors.Error,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Menu items
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(FlipperColors.Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, FlipperColors.Border, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                LazyColumn {
                    itemsIndexed(menuItems) { index, item ->
                        val isSelected = index == selectedIndex
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIndex = index
                                    onNavigate(item.route)
                                }
                                .background(
                                    if (isSelected) FlipperColors.Selected else Color.Transparent,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isSelected) "> " else "  ",
                                    color = FlipperColors.Primary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.icon,
                                    fontSize = 18.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    Text(
                                        text = item.title,
                                        color = if (isSelected) FlipperColors.Primary else FlipperColors.TextSecondary,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = item.subtitle,
                                        color = FlipperColors.TextDim,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FlipperColors.Surface, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .border(1.dp, FlipperColors.Border, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Используйте кнопки для навигации ↑↓",
                    color = FlipperColors.TextDim,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}