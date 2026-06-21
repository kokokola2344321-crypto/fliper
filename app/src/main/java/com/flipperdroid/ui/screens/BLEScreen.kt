package com.flipperdroid.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipperdroid.ui.theme.FlipperColors
import com.flipperdroid.modules.ble.*
import kotlinx.coroutines.launch

@Composable
fun BLEScreen(onBack: () -> Unit) {
    var selectedTool by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bleSpammer = remember { BLESpammer() }

    val tools = listOf("BLE Сканирование", "BLE Spam - iOS", "BLE Spam - Android", "BLE Spam - Samsung", "BLE Spam - All")

    fun addLog(msg: String) {
        logs = listOf("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}] $msg") + logs
        if (logs.size > 50) logs = logs.take(50)
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
                    Column {
                        Text("BLE", color = FlipperColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Bluetooth Low Energy", color = FlipperColors.TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.weight(1f).fillMaxWidth()
                .background(FlipperColors.Surface, RoundedCornerShape(4.dp))
                .border(1.dp, FlipperColors.Border, RoundedCornerShape(4.dp))
                .padding(4.dp)
            ) {
                LazyColumn {
                    items(tools.size) { index ->
                        val isSelected = index == selectedTool
                        Box(modifier = Modifier.fillMaxWidth()
                            .clickable { selectedTool = index }
                            .background(if (isSelected) FlipperColors.Selected else Color.Transparent, RoundedCornerShape(4.dp))
                            .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isSelected) "> " else "  ", color = FlipperColors.Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(tools[index], color = if (isSelected) FlipperColors.Primary else FlipperColors.TextSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = {
                        isRunning = true
                        scope.launch {
                            try {
                                val targets = when (selectedTool) {
                                    1 -> listOf("iOS")
                                    2 -> listOf("Android")
                                    3 -> listOf("Samsung")
                                    4 -> listOf("iOS", "Android", "Samsung")
                                    else -> emptyList()
                                }
                                if (selectedTool == 0) {
                                    addLog("Запуск BLE сканирования...")
                                    bleSpammer.startScan { device ->
                                        addLog("Найден: ${device.name ?: "Unknown"} [${device.address}]")
                                    }
                                } else {
                                    addLog("Запуск BLE Spam для ${targets.joinToString(", ")}...")
                                    bleSpammer.startSpam(targets)
                                    addLog("✅ Spam запущен (${targets.joinToString(", ")})")
                                }
                            } catch (e: Exception) {
                                addLog("❌ Ошибка: ${e.message}")
                            }
                            isRunning = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlipperColors.Primary, contentColor = FlipperColors.Black),
                    shape = RoundedCornerShape(4.dp)
                ) { Text(if (isRunning) "ВЫПОЛНЯЕТСЯ..." else "ЗАПУСТИТЬ", fontWeight = FontWeight.Bold, fontSize = 13.sp) }

                Button(
                    onClick = {
                        bleSpammer.stop()
                        addLog("Спам/скан остановлен")
                        isRunning = false
                    },
                    modifier = Modifier.height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlipperColors.Error, contentColor = FlipperColors.White),
                    shape = RoundedCornerShape(4.dp)
                ) { Text("СТОП", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.fillMaxWidth().height(200.dp)
                .background(Color(0xFF080808), RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .border(1.dp, FlipperColors.TextDim, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .padding(6.dp).verticalScroll(rememberScrollState())
            ) {
                Column {
                    logs.forEach {
                        Text(it, color = FlipperColors.TextPrimary, fontSize = 9.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                }
            }
        }
    }
}