package com.lingmiao.v2.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingmiao.v2.service.CapturePermissionActivity
import com.lingmiao.v2.service.OverlayService

/**
 * 主界面 —— 权限引导 + 启动入口
 *
 * 流程：检查悬浮窗权限 → 申请录屏权限 → 启动 CaptureService + OverlayService
 */
class MainActivity : ComponentActivity() {

    // 录屏权限回调
    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            // 把授权结果转发给 CaptureService
            val intent = Intent(this, CapturePermissionActivity::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            startActivity(intent)
            // 启动悬浮窗
            OverlayService.start(this)
            finish() // 关闭主界面，减少内存
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LingMiaoTheme {
                MainScreen(
                    onStartAssist = { checkAndRequestPermissions() },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    },
                    onOpenCalibration = {
                        startActivity(Intent(this, CalibrationActivity::class.java))
                    }
                )
            }
        }
    }

    /**
     * 权限检查顺序：
     * 1. 悬浮窗权限（必须先有，否则录屏没意义）
     * 2. 录屏权限（MediaProjection）
     */
    private fun checkAndRequestPermissions() {
        // Step 1: 悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                return
            }
        }

        // Step 2: 录屏权限
        requestCapturePermission()
    }

    private fun requestCapturePermission() {
        val intent = CapturePermissionActivity.createIntent(this)
        captureLauncher.launch(intent)
    }
}

@Composable
fun LingMiaoTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF6200EE),
        secondary = Color(0xFF03DAC5),
        background = Color(0xFFF5F5F5)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onStartAssist: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCalibration: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎱 灵喵 LingMiao", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6200EE),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFE8EAF6), Color(0xFFF5F5F5))
                    )
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // 状态卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF2E7D32), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("已就绪", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "通用型台球辅助",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "支持任意台球APP · 纯图像识别 · 无需Root",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // 使用说明卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "使用说明",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. 如果截图权限按应用单独授权，请选择你当前正在运行的台球游戏。\n\n" +
                        "2. 桌布请尽量精确对齐球桌内缘，辅助线会更稳定。\n\n" +
                        "3. 系统设置相关入口已放在下方，需要时再手动处理即可。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                }
            }

            // 启动按钮
            Button(
                onClick = onStartAssist,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                Text("▶ 开启辅助", fontSize = 18.sp, color = Color.White)
            }

            // 设置 + 校准 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("⚙ 参数设置")
                }
                OutlinedButton(
                    onClick = onOpenCalibration,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("🎯 桌布校准")
                }
            }

            // 系统设置建议
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "系统设置建议",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "为避免悬浮服务被系统回收，建议你在需要时手动加入电池优化白名单或前往应用设置页。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                // 跳转电池优化设置
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text("电池优", color = Color.White)
                        }
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                intent.data = Uri.parse("package:com.lingmiao.v2")
                                startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                        ) {
                            Text("应用系", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
        external fun stringFromJNI(): String
        external fun bitmapToGray(bitmap: android.graphics.Bitmap): Boolean
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            LogManager.i("Perm", "✅ 悬浮窗权限已授予")
            proceedAfterPermissions()
        } else {
            LogManager.w("Perm", "❌ 悬浮窗权限被拒绝")
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            LogManager.i("Perm", "✅ 通知权限已授予")
        } else {
            LogManager.w("Perm", "❌ 通知权限被拒绝")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppConfig.init(this)
        AppConfig.isFirstLaunch = false

        KeepAliveService.start(this)

        setContent {
            LingMiaoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartOverlay = { checkAndRequestPermissions() },
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                        onOpenGuide = { startActivity(Intent(this, GuideActivity::class.java)) },
                        onTestOpenCV = { testOpenCV() },
                        overlayEnabled = AppConfig.isOverlayEnabled
                    )
                }
            }
        }

        subscribeEvents()
    }

    private fun subscribeEvents() {
        lifecycleScope.launch {
            EventBus.events.collectLatest { event ->
                when (event.type) {
                    EventBus.EVT_TOAST -> LogManager.i("Event", event.data as? String ?: "")
                    else -> LogManager.d("Event", "收到事件：${event.type}")
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        proceedAfterPermissions()
    }

    private fun proceedAfterPermissions() {
        FloatingService.start(this)
        AppConfig.isOverlayEnabled = true
        LogManager.service("🎯 悬浮校准面板已启动")
    }

    private fun testOpenCV() {
        try {
            LogManager.i("OpenCV", "OpenCV 测试方法（已跳过）")
        } catch (e: Exception) {
            LogManager.e("OpenCV", "❌ OpenCV测试异常：${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            FloatingService.stop(this)
            KeepAliveService.stop(this)
        }
    }
}

@Composable
fun MainScreen(
    onStartOverlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    onTestOpenCV: () -> Unit,
    overlayEnabled: Boolean
) {
    val context = LocalContext.current
    var localOverlayEnabled by remember { mutableStateOf(overlayEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        Text(text = "🎱", fontSize = 48.sp)
        Text(
            text = "灵喵 LingMiao",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "V2.0 · 台球辅助引擎",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = {
                localOverlayEnabled = !localOverlayEnabled
                if (localOverlayEnabled) onStartOverlay() else {
                    FloatingService.stop(context)
                    AppConfig.isOverlayEnabled = false
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (localOverlayEnabled) Color(0xFF7C4DFF) else Color(0xFF4CAF50)
            )
        ) {
            Text(
                text = if (localOverlayEnabled) "🎯 校准面板运行中" else "▶ 启动校准面板",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onTestOpenCV,
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("🖼️ 测试OpenCV灰度转换", fontSize = 16.sp)
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(0.8f).height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("⚙ 高级设置", fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onOpenGuide,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("📖 新手引导", fontSize = 14.sp, color = Color.Gray)
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = if (localOverlayEnabled) "校准面板运行中 · OpenCV已加载" else "待启动 · OpenCV就绪",
            fontSize = 12.sp,
            color = if (localOverlayEnabled) Color(0xFF4CAF50) else Color.Gray
        )
    }
}
