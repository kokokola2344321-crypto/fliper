package com.flipperdroid.ui.screens

import android.content.Context
import android.hardware.ConsumerIrManager
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flipperdroid.ui.theme.FlipperColors
import com.flipperdroid.modules.ir.IRManager
import com.flipperdroid.modules.ir.TVBgone
import kotlinx.coroutines.launch

@Composable
fun IRScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedTool by remember { mutableStateOf(0) }
    var logs by remember { mutableStateOf(listOf<String>()) }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val irManager = remember { IRManager(context) }

    val tools = listOf("TV-B-Gone (Выкл. ТВ)", "ИК сканер", "Custom IR команда")

    fun addLog(msg: String) {
        logs = listOf("[${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US).format(java.util.Date())}] $msg") + logs
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
                        Text("IR", color = FlipperColors.Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Инфракрасный порт", color = FlipperColors.TextSecondary, fontSize = 11.sp)
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

            Button(
                onClick = {
                    isRunning = true
                    scope.launch {
                        try {
                            when (selectedTool) {
                                0 -> {
                                    addLog("Запуск TV-B-Gone...")
                                    TVBgone.sendPowerOff(context)
                                    addLog("✅ Сигнал выключения отправлен")
                                }
                                1 -> {
                                    addLog("Сканирование ИК сигналов...")
                                    irManager.startScan { code ->
                                        addLog("Получен ИК код: ${code.joinToString(" ")}")
                                    }
                                }
                                2 -> {
                                    addLog("Отправка тестового ИК сигнала...")
                                    irManager.sendIR(38000, intArrayOf(9000, 4500, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560))
                                    addLog("✅ Сигнал отправлен")
                                }
                            }
                        } catch (e: Exception) {
                            addLog("❌ Ошибка: ${e.message}")
                            if (selectedTool == 1) {
                                addLog("ИК порт может не поддерживать прием сигналов")
                            }
                        }
                        isRunning = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FlipperColors.Primary, contentColor = FlipperColors.Black),
                shape = RoundedCornerShape(4.dp)
            ) { Text(if (isRunning) "ВЫПОЛНЯЕТСЯ..." else "ЗАПУСТИТЬ", fontWeight = FontWeight.Bold, fontSize = 13.sp) }

            Spacer(modifier = Modifier.height(4.dp))

            Box(modifier = Modifier.fillMaxWidth().height(250.dp)
                .background(Color(0xFF080808), RoundedCornerShape(8.dp))
                .border(1.dp, FlipperColors.TextDim, RoundedCornerShape(8.dp))
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