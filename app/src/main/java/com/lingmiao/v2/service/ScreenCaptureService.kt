package com.lingmiao.v2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.lingmiao.v2.LingMiaoApp

/**
 * 录屏服务
 * 
 * 职责：
 * 1. 接收 CapturePermissionActivity 授权的录屏令牌
 * 2. 启动 MediaProjection 开始抓取屏幕画面
 * 3. 后续将画面传给 C++ 引擎进行台球识别
 */
class ScreenCaptureService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1004

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra("resultCode", resultCode)
                putExtra("data", data)
            }
            context.startForegroundService(intent)
        }
    }

    private var mediaProjection: MediaProjection? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", -1) ?: -1
        val data = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra("data", Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra("data")
        }

        if (resultCode != -1 && data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            // 获取屏幕捕捉对象
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            
            // TODO: 🔥 在这里对接您的 C++ 图像识别引擎
            // 例如：MediaProjection 创建 VirtualDisplay 获取画面，然后传给 native 层
            // LogManager.i("ScreenCapture", "录屏服务已启动，准备对接 C++ 识别")
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    LingMiaoApp.CHANNEL_CAPTURE,
                    "灵喵-录屏服务",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "后台录制屏幕用于辅助线计算"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, LingMiaoApp.CHANNEL_CAPTURE)
            .setContentTitle("灵喵-录屏服务")
            .setContentText("正在准备截取台球画面")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // 停止屏幕捕捉，释放资源
        mediaProjection?.stop()
        mediaProjection = null
    }
}
