package com.flipperdroid.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipperdroid.ui.theme.FlipperColors
import com.flipperdroid.modules.root.RootShell

@Composable
fun AboutScreen(onBack: () -> Unit) {
    var rootInfo by remember { mutableStateOf("Проверка...") }

    LaunchedEffect(Unit) {
        rootInfo = if (RootShell.checkRoot()) "✅ Root: ДА" else "❌ Root: НЕТ"
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FlipperColors.Background) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Box(modifier = Modifier.fillMaxWidth()
                .background(FlipperColors.Surface, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .border(1.dp, FlipperColors.Border, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "<-", color = FlipperColors.Primary, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBack() }.padding(end = 12.dp))
                    Text("About", color = FlipperColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(modifier = Modifier.fillMaxWidth().weight(1f)
                .background(FlipperColors.Surface, RoundedCornerShape(8.dp))
                .border(1.dp, FlipperColors.Border, RoundedCornerShape(8.dp))
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
            ) {
                Column {
                    Text("FlipperDroid v1.0.0", color = FlipperColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Инструмент для пентеста на Android", color = FlipperColors.TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("✓ Функции:", color = FlipperColors.Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    listOf(
                        "WiFi Beacon/Deauth атаки",
                        "Захват WPA Handshake",
                        "Подбор паролей WiFi",
                        "BLE сканирование и Spam",
                        "TV-B-Gone (ИК пульт)",
                        "Сканирование портов",
                        "Evil Portal"
                    ).forEach { Text("  • $it", color = FlipperColors.TextPrimary, fontSize = 12.sp) }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Статус root:", color = FlipperColors.TextSecondary, fontSize = 14.sp)
                    Text(
                        rootInfo,
                        color = if (rootInfo.contains("✅")) FlipperColors.Accent else FlipperColors.Error,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("⚠️ Для WiFi атак требуется root", color = FlipperColors.Warning, fontSize = 11.sp)
                    Text("⚠️ Используйте только на своих устройствах", color = FlipperColors.Warning, fontSize = 11.sp)
                }
            }
        }
    }
}