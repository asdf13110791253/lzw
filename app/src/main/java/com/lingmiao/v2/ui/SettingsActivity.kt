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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.ball.BallDetector
import com.lingmiao.v2.ui.theme.LingMiaoTheme

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

// ===================== 可组合界面 =====================

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenBattery: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val context = LocalContext.current

    // 配置状态
    var aimColor by remember { mutableStateOf(AppConfig.aimColor) }
    var aimWidth by remember { mutableStateOf(AppConfig.aimWidth) }
    var detectMode by remember { mutableStateOf(AppConfig.detectMode) }
    var reflectionMode by remember { mutableStateOf(AppConfig.reflectionMode) }
    var maxBanks by remember { mutableStateOf(AppConfig.maxBanks) }
    var showAntLine by remember { mutableStateOf(AppConfig.showAntLine) }
    var snapNearest by remember { mutableStateOf(AppConfig.snapNearest) }
    var showAngle by remember { mutableStateOf(AppConfig.showAngle) }
    var tableTexture by remember { mutableStateOf(AppConfig.tableTexture) }
    var compensationRatio by remember { mutableStateOf(AppConfig.compensationRatio) }

    // 颜色选项
    val colorOptions = listOf(
        Color.Yellow, Color.Red, Color.Green, Color.Blue,
        Color.Cyan, Color.Magenta, Color.White, Color(0xFFFF8C00)
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
                        val isSelected = aimColor == color.value.toInt()
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
                                aimColor = color.value.toInt()
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
                        val isSelected = detectMode == index
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    detectMode = index
                                    BallDetector.applyPreset(index)
                                    AppConfig.detectMode = index
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.name,
                                color = if (isSelected) Color(0xFF7C4DFF) else Color.Unspecified,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Text("✓", color = Color(0xFF7C4DFF), fontWeight = FontWeight.Bold)
                            }
                        }
                        if (index < AppConfig.PRESETS.size - 1) {
                            Divider(color = Color.Gray.copy(alpha = 0.3f))
                        }
                    }
                }
            }

            // ── 反射模式 ──
            SettingsSection(title = "🔄 反射模式") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reflectionModes.forEach { (key, label) ->
                        val isSelected = reflectionMode == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF7C4DFF) else Color.Transparent,
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f).clickable {
                                reflectionMode = key
                                AppConfig.reflectionMode = key
                            }
                        ) {
                            Text(
                                label,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                color = if (isSelected) Color.White else Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── 手动翻袋库数 ──
            SettingsSection(title = "🎯 手动翻袋库数: $maxBanks") {
                Slider(
                    value = maxBanks.toFloat(),
                    onValueChange = {
                        maxBanks = it.toInt()
                        AppConfig.maxBanks = maxBanks
                    },
                    valueRange = 1f..5f,
                    steps = 4,
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
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.clickable {
                                maxBanks = n
                                AppConfig.maxBanks = n
                            }
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
                        AppConfig.snapNearest = it
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
                            color = if (selected) Color(0xFF7C4DFF) else Color.Transparent,
                            border = BorderStroke(1.dp, Color.Gray),
                            modifier = Modifier.weight(1f).clickable {
                                tableTexture = n
                                AppConfig.tableTexture = n
                            }
                        ) {
                            Text(
                                "纹理$n",
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = if (selected) Color.White else Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── 角度补偿系数 ──
            SettingsSection(title = "📐 角度补偿系数: ${"%.2f".format(compensationRatio)}") {
                Slider(
                    value = compensationRatio,
                    onValueChange = {
                        compensationRatio = it
                        AppConfig.compensationRatio = it
                    },
                    valueRange = 0.5f..2.0f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF7C4DFF),
                        activeTrackColor = Color(0xFF7C4DFF)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { compensationRatio = 0.5f; AppConfig.compensationRatio = 0.5f }) { Text("0.5") }
                    TextButton(onClick = { compensationRatio = 1.0f; AppConfig.compensationRatio = 1.0f }) { Text("1.0") }
                    TextButton(onClick = { compensationRatio = 2.0f; AppConfig.compensationRatio = 2.0f }) { Text("2.0") }
                }
            }

            // ── 系统设置快捷入口 ──
            SettingsSection(title = "⚡ 系统设置") {
                Button(
                    onClick = onOpenBattery,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C00))
                ) {
                    Text("🔋 电池优化设置")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                ) {
                    Text("📱 应用权限设置")
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ===================== 辅助可组合组件 =====================

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
