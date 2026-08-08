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
import androidx.core.content.ContextCompat
import com.lingmiao.v2.service.CapturePermissionActivity
import com.lingmiao.v2.service.FloatingService
import com.lingmiao.v2.service.KeepAliveService

// 如果您的项目中使用了 LogManager、EventBus 或 AppConfig，
// 请确保它们已经在您的项目中定义，并在此加上 import。

class MainActivity : ComponentActivity() {

    // 录屏权限回调（来自原第一段代码）
    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, CapturePermissionActivity::class.java).apply {
                putExtra("resultCode", result.resultCode)
                putExtra("data", result.data)
            }
            startActivity(intent)
            FloatingService.start(this)
            finish()
        }
    }

    // 悬浮窗权限回调（来自原第二段代码）
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            proceedAfterPermissions()
        } else {
            // 权限被拒绝的提示
            // LogManager.w("Perm", "❌ 悬浮窗权限被拒绝")
        }
    }

    // 通知权限回调（来自原第二段代码）
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // LogManager.i("Perm", "✅ 通知权限已授予")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AppConfig 初始化（如果您代码里有 AppConfig 的话）
        // AppConfig.init(this)
        // AppConfig.isFirstLaunch = false

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
                        // onTestOpenCV = { testOpenCV() }, // 如果您有测试OpenCV方法可解开此注释
                        overlayEnabled = false // 根据您的 AppConfig 修改
                    )
                }
            }
        }

        // subscribeEvents()  // 如果您有 EventBus 可以解开此注释
    }

    /**
     * 权限检查顺序：
     * 1. 悬浮窗权限
     * 2. 录屏权限
     */
    private fun checkAndRequestPermissions() {
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
                return
            }
        }

        // 检查通知权限（Android 13+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        proceedAfterPermissions()
    }

    // 权限通过后的处理
    private fun proceedAfterPermissions() {
        // 启动悬浮窗服务
        FloatingService.start(this)
        // AppConfig.isOverlayEnabled = true
        // LogManager.service("🎯 悬浮校准面板已启动")
    }

    // 申请录屏权限 (来自原第一段代码)
    private fun requestCapturePermission() {
        val intent = CapturePermissionActivity.createIntent(this)
        captureLauncher.launch(intent)
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
    onStartOverlay: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit,
    overlayEnabled: Boolean
) {
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

        // 启动辅助按钮
        Button(
            onClick = {
                localOverlayEnabled = !localOverlayEnabled
                if (localOverlayEnabled) {
                    onStartOverlay()
                } else {
                    // FloatingService.stop(context) // 如需停止服务解开注释
                    // AppConfig.isOverlayEnabled = false
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
