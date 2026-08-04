package com.lingmiao.v2

import android.Manifest
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.core.event.EventBus
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.service.FloatingService
import com.lingmiao.v2.service.KeepAliveService
import com.lingmiao.v2.ui.GuideActivity
import com.lingmiao.v2.ui.SettingsActivity
import com.lingmiao.v2.ui.theme.LingMiaoTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
