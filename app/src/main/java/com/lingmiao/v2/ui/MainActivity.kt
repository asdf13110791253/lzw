package com.lingmiao.v2.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.lingmiao.v2.App
import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.core.event.EventBus
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.service.FloatingService
import com.lingmiao.v2.service.KeepAliveService
import com.lingmiao.v2.ui.theme.LingMiaoTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

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
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化
        AppConfig.init(this)
        AppConfig.isFirstLaunch = false

        // 启动保活服务
        KeepAliveService.start(this)

        setContent {
            LingMiaoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartOverlay = { checkAndRequestPermissions() },
                        onOpenCalibrate = { startActivity(Intent(this, CalibrateActivity::class.java)) },
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                        onOpenGuide = { startActivity(Intent(this, GuideActivity::class.java)) }
                    )
                }
            }
        }

        // 监听事件
        subscribeEvents()
    }

    private fun subscribeEvents() {
        // 在协程中收集事件
    }

    private fun checkAndRequestPermissions() {
        // 1. 悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
            return
        }

        // 2. 通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        proceedAfterPermissions()
    }

    private fun proceedAfterPermissions() {
        // 启动悬浮窗
        FloatingService.start(this)
        AppConfig.isOverlayEnabled = true
        LogManager.service("🎯 悬浮辅助已启动")
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
    onOpenCalibrate: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGuide: () -> Unit
) {
    val context = LocalContext.current
    var overlayEnabled by remember { mutableStateOf(AppConfig.isOverlayEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        // Logo 区域
        Text(
            text = "🎱",
            fontSize = 48.sp
        )
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

        // 主按钮 - 启动悬浮辅助
        Button(
            onClick = {
                overlayEnabled = !overlayEnabled
                if (overlayEnabled) onStartOverlay() else {
                    FloatingService.stop(context)
                    AppConfig.isOverlayEnabled = false
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (overlayEnabled) Color(0xFF7C4DFF) else Color(0xFF4CAF50)
            )
        ) {
            Text(
                text = if (overlayEnabled) "🎯 悬浮辅助运行中" else "▶ 启动悬浮辅助",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(16.dp))

        // 校准按钮
        OutlinedButton(
            onClick = onOpenCalibrate,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("📐 四角校准", fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        // 高级设置
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("⚙ 高级设置", fontSize = 16.sp)
        }

        Spacer(Modifier.height(12.dp))

        // 新手引导
        TextButton(
            onClick = onOpenGuide,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("📖 新手引导", fontSize = 14.sp, color = Color.Gray)
        }

        Spacer(Modifier.weight(1f))

        // 底部状态
        Text(
            text = if (overlayEnabled) "运行中 · 60fps · 已校准" else "待启动",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(Modifier.height(8.dp))
    }
}
