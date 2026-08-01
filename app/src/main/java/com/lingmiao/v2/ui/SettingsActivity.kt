package com.lingmiao.v2.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.lingmiao.v2.App
import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.ball.BallDetector
import com.lingmiao.v2.ui.theme.LingMiaoTheme

/**
 * 高级设置页
 * - 辅助线颜色/粗细
 * - 识别方案切换（8套）
 * - 物理参数预设
 * - 反射模式
 * - 蚂蚁线/角度显示
 */
class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LingMiaoTheme {
                SettingsScreen(
                    onBack = { finish() },
                    onOpenBattery = { openBatterySettings() },
                    onOpenAppSettings = { openAppSettings() }
                )
            }
        }
    }

    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val context = LocalContext.current

    // 状态
    var aimColor by remember { mutableStateOf(AppConfig.aimColor) }
    var aimWidth by remember { mutableStateOf(AppConfig.aimWidth) }
    var detectMode by remember { mutableStateOf(AppConfig.detectMode) }
    var reflectionMode by remember { mutableStateOf(AppConfig.reflectionMode) }
    var maxBanks by remember { mutableStateOf(AppConfig.maxBanks) }
    var showAntLine by remember { mutableStateOf(AppConfig.showAntLine) }
    var snapNearest by remember { mutableStateOf(AppConfig.snapToNearest) }
    var showAngle by remember { mutableStateOf(AppConfig.showAngle) }
    var tableTexture by remember { mutableStateOf(AppConfig.tableTexture) }
    var compensationRatio by remember { mutableStateOf(AppConfig.compensationRatio) }

    // 颜色预设
    val colorOptions = listOf(
        Color.YELLOW, Color.RED, Color.GREEN, Color.BLUE,
        Color.CYAN, Color.MAGENTA, Color.WHITE, Color(0xFFFF8C00)
    )
    val colorNames = listOf("黄", "红", "绿", "蓝", "青", "品红", "白", "橙")

    // 反射模式
    val reflectionModes = listOf("mirror" to "镜像反射", "compensation" to "角度补偿")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚙️ 高级设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            // ── 辅助线颜色 ──
            SettingsSection(title = "🎨 辅助线颜色") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colorOptions.forEachIndexed { index, color ->
                        val isSelected = aimColor == color.toArgb()
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = color,
                            border = if (isSelected) BorderStroke(3.dp, Color.White) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (isSelected) Text("✓", color = Color.Black, fontSize = 14.sp)
                            }
                            Modifier.clickable {
                                aimColor = color.toArgb()
                                AppConfig.aimColor = aimColor
                                LogManager.config("🎨 颜色 → #${Integer.toHexString(aimColor and 0xFFFFFF).uppercase()}")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 辅助线粗细 ──
            SettingsSection(title = "📏 辅助线粗细: ${"%.1f".format(aimWidth)}") {
                Slider(
                    value = aimWidth,
                    onValueChange = {
                        aimWidth = it
                        AppConfig.aimWidth = it
                    },
                    valueRange = 1f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7C4DFF),
                        activeTrackColor = Color(0xFF7C4DFF)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { aimWidth = 1f; AppConfig.aimWidth = 1f }) { Text("-", fontSize = 20.sp) }
                    TextButton(onClick = { aimWidth = 5f; AppConfig.aimWidth = 5f }) { Text("重置", fontSize = 14.sp) }
                    TextButton(onClick = { aimWidth = 10f; AppConfig.aimWidth = 10f }) { Text("+", fontSize = 20.sp) }
                }
            }

            // ── 识别方案 ──
            SettingsSection(title = "🔍 图像识别方案") {
                Column {
                    AppConfig.PRESETS.forEachIndexed { index, preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    detectMode = index
                                    BallDetector.applyPreset(index)
                                    AppConfig.detectMode = index
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = detectMode == index,
                                onClick = {
                                    detectMode = index
                                    BallDetector.applyPreset(index)
                                    AppConfig.detectMode = index
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF7C4DFF)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(preset.name, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "V=${preset.vThreshold} S=${preset.sMinDist} P=${preset.pSensitivity} dp=${preset.dp} (${preset.method})",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            // ── 反射模式 ──
            SettingsSection(title = "🪞 反射方案") {
                Column {
                    reflectionModes.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reflectionMode = key
                                    AppConfig.reflectionMode = key
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = reflectionMode == key,
                                onClick = {
                                    reflectionMode = key
                                    AppConfig.reflectionMode = key
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF7C4DFF)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("补偿比例: ${"%.2f".format(compensationRatio)}", fontSize = 13.sp)
                Slider(
                    value = compensationRatio,
                    onValueChange = {
                        compensationRatio = it
                        AppConfig.compensationRatio = it
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7C4DFF),
                        activeTrackColor = Color(0xFF7C4DFF)
                    )
                )
            }

            // ── 翻袋库数 ──
            SettingsSection(title = "🔄 手动翻袋库数: $maxBanks") {
                Slider(
                    value = maxBanks.toFloat(),
                    onValueChange = {
                        maxBanks = it.toInt()
                        AppConfig.maxBanks = maxBanks
                    },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7C4DFF),
                        activeTrackColor = Color(0xFF7C4DFF)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    (1..5).forEach { n ->
                        val selected = maxBanks == n
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) Color(0xFF7C4DFF) else Color.Transparent,
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text(
                                "$n库",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = if (selected) Color.White else Color.Gray,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ── 显示选项 ──
            SettingsSection(title = "👁 显示选项") {
                SwitchRow(
                    label = "显示蚂蚁线",
                    checked = showAntLine,
                    onCheckedChange = {
                        showAntLine = it
                        AppConfig.showAntLine = it
                    }
                )
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                SwitchRow(
                    label = "吸附最近球",
                    checked = snapNearest,
                    onCheckedChange = {
                        snapNearest = it
                        AppConfig.snapToNearest = it
                    }
                )
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                SwitchRow(
                    label = "显示角度",
                    checked = showAngle,
                    onCheckedChange = {
                        showAngle = it
                        AppConfig.showAngle = it
                    }
                )
            }

            // ── 桌布纹理 ──
            SettingsSection(title = "🟫 桌布纹理") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { n ->
                        val selected = tableTexture == n
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) Color(0xFF7C4DFF) else Color(0xFF2A2A2A),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "桌布$n",
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = if (selected) Color.White else Color.Gray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.TextAlign.Center
                            )
                            Modifier.clickable {
                                tableTexture = n
                                AppConfig.tableTexture = n
                            }
                        }
                    }
                }
            }

            // ── 系统设置入口 ──
            SettingsSection(title = "📱 系统设置") {
                Button(
                    onClick = onOpenBattery,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Text("🔋 电池优化白名单", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    )
                ) {
                    Text("⚙️ 应用设置页", color = Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── 退出按钮 ──
            Button(
                onClick = {
                    LogManager.i("Settings", "🚪 用户退出应用")
                    (context as? Activity)?.finishAffinity()
                    Runtime.getRuntime().exit(0)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935)
                )
            ) {
                Text("🚪 退出应用", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF7C4DFF)
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF7C4DFF),
                checkedTrackColor = Color(0xFF7C4DFF).copy(alpha = 0.5f)
            )
        )
    }
}
