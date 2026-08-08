package com.lingmiao.v2.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingmiao.v2.config.AppConfig // ✅ 修正了 AppConfig 的路径

/**
 * 参数设置页
 *
 * 核心可调参数（对应 C++ 层的识别算法）：
 * - HSV 阈值（V 亮度 / S 饱和度 / P 色相）
 * - 反射模式（镜像反射 / 角度补偿）
 * - 辅助线颜色
 * - 辅助线粗细
 * - 是否显示蚂蚁线
 * - 是否吸附最近球
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val prefs = remember { AppConfig.prefs }

    // HSV 参数（默认值，对应 C++ 的 inRange 阈值）
    var vValue by remember { mutableFloatStateOf(prefs.getFloat("v_value", 200f)) }
    var sValue by remember { mutableFloatStateOf(prefs.getFloat("s_value", 30f)) }
    var pValue by remember { mutableFloatStateOf(prefs.getFloat("p_value", 15f)) }

    // 反射模式
    var reflectionMode by remember { mutableStateOf(prefs.getString("reflection_mode", "compensation") ?: "compensation") }

    // 辅助线颜色
    var lineColor by remember { mutableStateOf(prefs.getInt("line_color", 0xFFFF1744.toInt())) }

    // 辅助线粗细
    var lineWidth by remember { mutableFloatStateOf(prefs.getFloat("line_width", 5f)) }

    // 翻袋库数
    var bankCount by remember { mutableIntStateOf(prefs.getInt("bank_count", 2)) }

    // 开关
    var showAntLine by remember { mutableStateOf(prefs.getBoolean("show_ant_line", false)) }
    var snapNearest by remember { mutableStateOf(prefs.getBoolean("snap_nearest", true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙ 参数设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← 返回") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ===== HSV 阈值卡片 =====
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("图像识别参数", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))

                    // V: 亮度阈值
                    Text("V — 亮度阈值: ${vValue.toInt()}", fontSize = 13.sp)
                    Slider(
                        value = vValue, onValueChange = {
                            vValue = it
                            prefs.edit().putFloat("v_value", it).apply()
                        },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // S: 饱和度阈值
                    Text("S — 饱和度阈值: ${sValue.toInt()}", fontSize = 13.sp)
                    Slider(
                        value = sValue, onValueChange = {
                            sValue = it
                            prefs.edit().putFloat("s_value", it).apply()
                        },
                        valueRange = 0f..255f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // P: 圆检测灵敏度
                    Text("P — 圆检测灵敏度: ${pValue.toInt()}", fontSize = 13.sp)
                    Slider(
                        value = pValue, onValueChange = {
                            pValue = it
                            prefs.edit().putFloat("p_value", it).apply()
                        },
                        valueRange = 5f..50f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ===== 反射方案卡片 =====
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("反射方案", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        FilterChip(
                            selected = reflectionMode == "mirror",
                            onClick = {
                                reflectionMode = "mirror"
                                prefs.edit().putString("reflection_mode", "mirror").apply()
                            },
                            label = { Text("镜像反射") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        FilterChip(
                            selected = reflectionMode == "compensation",
                            onClick = {
                                reflectionMode = "compensation"
                                prefs.edit().putString("reflection_mode", "compensation").apply()
                            },
                            label = { Text("角度补偿") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ===== 辅助线颜色卡片 =====
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("辅助线颜色", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(
                            0xFFFF1744.toInt() /*红*/,
                            0xFF00E676.toInt() /*绿*/,
                            0xFF2979FF.toInt() /*蓝*/,
                            0xFFFFD600.toInt() /*黄*/,
                            0xFF00E5FF.toInt() /*青*/
                        ).forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(color), shape = MaterialTheme.shapes.small)
                                    .border(
                                        width = if (lineColor == color) 3.dp else 0.dp,
                                        color = Color.Black,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable {
                                        lineColor = color
                                        prefs.edit().putInt("line_color", color).apply()
                                    }
                            )
                        }
                    }
                }
            }

            // ===== 线宽 + 翻袋库数 =====
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("线宽: ${lineWidth.toInt()}", fontSize = 13.sp)
                    Slider(
                        value = lineWidth, onValueChange = {
                            lineWidth = it
                            prefs.edit().putFloat("line_width", it).apply()
                        },
                        valueRange = 1f..15f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("翻袋库数: $bankCount", fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = {
                            if (bankCount > 0) {
                                bankCount--
                                prefs.edit().putInt("bank_count", bankCount).apply()
                            }
                        }) { Text("-") }
                        Spacer(Modifier.width(16.dp))
                        Text("$bankCount", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = {
                            if (bankCount < 5) {
                                bankCount++
                                prefs.edit().putInt("bank_count", bankCount).apply()
                            }
                        }) { Text("+") }
                    }
                }
            }

            // ===== 开关 =====
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("显示蚂蚁线")
                        Switch(
                            checked = showAntLine,
                            onCheckedChange = {
                                showAntLine = it
                                prefs.edit().putBoolean("show_ant_line", it).apply()
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("吸附最近球")
                        Switch(
                            checked = snapNearest,
                            onCheckedChange = {
                                snapNearest = it
                                prefs.edit().putBoolean("snap_nearest", it).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
