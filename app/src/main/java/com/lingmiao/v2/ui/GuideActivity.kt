package com.lingmiao.v2.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
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
import com.lingmiao.v2.config.AppConfig
import com.lingmiao.v2.utils.LogManager
import com.lingmiao.v2.service.FloatingService
import com.lingmiao.v2.service.ScreenCaptureService
import com.lingmiao.v2.ui.theme.LingMiaoTheme // 🔥 统一从外部引用 Theme，解决重复定义冲突

/**
 * 新手引导页（权限授权引导）
 */
class GuideActivity : ComponentActivity() {

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val context = this
        if (Settings.canDrawOverlays(context)) {
            LogManager.i("Guide", "✅ 悬浮窗权限已授予")
            nextStep()
        }
    }

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            LogManager.i("Guide", "✅ 录屏权限已授予")
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
            nextStep()
        }
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) LogManager.i("Guide", "✅ 通知权限已授予")
        nextStep()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LingMiaoTheme {
                GuideScreen(
                    onRequestOverlay = { requestOverlayPermission() },
                    onRequestScreenCapture = { requestScreenCapture() },
                    onRequestNotification = { requestNotification() },
                    onRequestBattery = { requestBatteryOptimization() },
                    onFinish = { finishGuide() }
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayLauncher.launch(intent)
    }

    private fun requestScreenCapture() {
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun requestNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            nextStep()
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
            LogManager.i("Guide", "📋 请在设置中将灵喵加入电池优化白名单")
        }
        nextStep()
    }

    private fun nextStep() {
        recreate()
    }

    private fun finishGuide() {
        AppConfig.isFirstLaunch = false
        FloatingService.start(this)
        AppConfig.isOverlayEnabled = true
        finish()
    }
}

// 🔥 重要：删除了原先这里重复定义的 `fun LingMiaoTheme`，统一使用上面 import 的 Theme

@Composable
fun GuideScreen(
    onRequestOverlay: () -> Unit,
    onRequestScreenCapture: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(0) }

    val steps = listOf(
        StepInfo(
            title = "悬浮窗权限",
            desc = "灵喵需要在其他应用上层显示辅助线，请授予悬浮窗权限。",
            buttonText = "授予悬浮窗权限",
            action = onRequestOverlay
        ),
        StepInfo(
            title = "录屏权限",
            desc = "需要录屏权限来实时分析台球游戏画面，仅本地处理不联网。",
            buttonText = "授予录屏权限",
            action = onRequestScreenCapture
        ),
        StepInfo(
            title = "通知权限",
            desc = "用于前台服务通知，确保辅助功能持续运行。",
            buttonText = "授予通知权限",
            action = onRequestNotification
        ),
        StepInfo(
            title = "电池优化",
            desc = "建议将灵喵加入电池优化白名单，防止后台被系统回收。",
            buttonText = "前往设置",
            action = onRequestBattery
        )
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            Text(
                text = "📖 灵喵使用引导",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "按步骤完成授权，即可开始使用",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(40.dp))

            LinearProgressIndicator(
                progress = (currentStep + 1) / steps.size.toFloat(),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(6.dp),
                color = Color(0xFF7C4DFF)
            )

            Spacer(Modifier.height(32.dp))

            val step = steps[currentStep.coerceIn(0, steps.size - 1)]
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F0FF)
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "${currentStep + 1}/${steps.size} ${step.title}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7C4DFF)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = step.desc,
                        fontSize = 15.sp,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (currentStep < steps.size - 1) {
                        steps[currentStep].action()
                        currentStep++
                    } else {
                        steps[currentStep].action()
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF7C4DFF)
                )
            ) {
                Text(
                    text = if (currentStep < steps.size - 1) step.buttonText else "✅ 全部完成，启动灵喵",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

data class StepInfo(
    val title: String,
    val desc: String,
    val buttonText: String,
    val action: () -> Unit
)
