package com.flipperdroid.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.flipperdroid.modules.wifi.*
import com.flipperdroid.modules.root.RootShell
import kotlinx.coroutines.launch

@Composable
fun WiFiScreen(onBack: () -> Unit) {
    var selectedTool by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Готов") }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }
    var scannedNetworks by remember { mutableStateOf(listOf<ScannedNetwork>()) }
    val scope = rememberCoroutineScope()
    val nativeWiFi = remember { NativeWiFi() }

    val tools = listOf(
        "Сканировать сети",
        "Beacon Spam",
        "Deauth Attack",
        "Захват Handshake",
        "Подбор пароля",
        "Сканирование портов"
    )

    fun addLog(msg: String) {
        logs = listOf("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}] $msg") + logs
        if (logs.size > 100) logs = logs.take(100)
        status = msg
    }

    Surface(modifier = Modifier.fillMaxSize(), color = FlipperColors.Background) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // Header
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(FlipperColors.Surface, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .border(1.dp, FlipperColors.Border, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "<-",
                        color = FlipperColors.Primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onBack() }.padding(end = 12.dp)
                    )
                    Column {
                        Text("WiFi", color = FlipperColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(status, color = FlipperColors.TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Scan results
            if (scannedNetworks.isNotEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(150.dp)
                        .background(FlipperColors.Surface, RoundedCornerShape(4.dp))
                        .border(1.dp, FlipperColors.TextDim, RoundedCornerShape(4.dp))
                        .padding(4.dp)
                ) {
                    LazyColumn {
                        items(scannedNetworks) { net ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(net.ssid, color = FlipperColors.TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                                Text("${net.rssi}dBm", color = FlipperColors.TextDim, fontSize = 11.sp)
                                Text(if (net.encrypted) "🔒" else "🔓", fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Tools
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth()
                    .background(FlipperColors.Surface, RoundedCornerShape(4.dp))
                    .border(1.dp, FlipperColors.Border, RoundedCornerShape(4.dp))
                    .padding(4.dp)
            ) {
                LazyColumn {
                    items(tools.size) { index ->
                        val isSelected = index == selectedTool
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { selectedTool = index }
                                .background(if (isSelected) FlipperColors.Selected else Color.Transparent, RoundedCornerShape(4.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (isSelected) "> " else "  ",
                                    color = FlipperColors.Primary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    tools[index],
                                    color = if (isSelected) FlipperColors.Primary else FlipperColors.TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        if (isRunning) return@Button
                        isRunning = true
                        scope.launch {
                            try {
                                when (selectedTool) {
                                    0 -> {
                                        addLog("Сканирование WiFi сетей...")
                                        scannedNetworks = WiFiScanner.scan()
                                        addLog("Найдено ${scannedNetworks.size} сетей")
                                    }
                                    1 -> {
                                        addLog("Запуск Beacon Spam...")
                                        nativeWiFi.startBeaconSpam("FlipperDroid")
                                        addLog("Beacon Spam запущен")
                                    }
                                    2 -> {
                                        addLog("Запуск Deauth... Укажите BSSID цели")
                                        nativeWiFi.startDeauthAttack("FF:FF:FF:FF:FF:FF")
                                        addLog("Deauth атака запущена")
                                    }
                                    3 -> {
                                        addLog("Захват Handshake... Переключение в monitor mode")
                                        val result = nativeWiFi.captureHandshake("wlan0")
                                        addLog(result)
                                    }
                                    4 -> {
                                        addLog("Подбор пароля WPA/WPA2...")
                                        val result = PasswordCracker.crack("/sdcard/handshake.cap", "/sdcard/wordlist.txt")
                                        addLog(result)
                                    }
                                    5 -> {
                                        addLog("Сканирование портов...")
                                        val ports = listOf(22, 80, 443, 8080, 3306, 3389)
                                        for (port in ports) {
                                            val r = WiFiScanner.scanPort("192.168.1.1", port)
                                            if (r) addLog("Порт $port открыт")
                                        }
                                        addLog("Сканирование портов завершено")
                                    }
                                }
                            } catch (e: Exception) {
                                addLog("Ошибка: ${e.message}")
                            }
                            isRunning = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning) FlipperColors.TextDim else FlipperColors.Primary,
                        contentColor = FlipperColors.Black
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (isRunning) "ВЫПОЛНЯЕТСЯ..." else "ЗАПУСТИТЬ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        if (selectedTool in 1..2) {
                            nativeWiFi.stopAttack()
                            addLog("Атака остановлена")
                        } else {
                            addLog("Все задачи остановлены")
                        }
                        isRunning = false
                    },
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FlipperColors.Error,
                        contentColor = FlipperColors.White
                    ),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("СТОП", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Logs
            Box(
                modifier = Modifier.fillMaxWidth().height(130.dp)
                    .background(Color(0xFF080808), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .border(1.dp, FlipperColors.TextDim, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .padding(6.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    logs.forEach { log ->
                        Text(
                            log,
                            color = if (log.contains("Ошибка") || log.contains("❌")) FlipperColors.Error
                                   else if (log.contains("✅") || log.contains("запущен")) FlipperColors.Accent
                                   else FlipperColors.TextPrimary,
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}