package com.lingmiao.v2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * 灵喵 LingMiao —— 通用台球辅助
 *
 * 核心架构：
 *   UI层 (MainActivity / SettingsActivity / CalibrationActivity)
 *       ↓ 用户操作
 *   CaptureService (MediaProjection 抓帧 → JNI → C++ 识别)
 *       ↓ 识别结果 (球体坐标)
 *   OverlayService (SurfaceView 绘制辅助线)
 *
 * 特点：不依赖任何特定游戏UI，通用任意台球APP
 */
class LingMiaoApp : Application() {

    companion object {
        const val CHANNEL_CAPTURE = "lingmiao_capture_channel"
        const val CHANNEL_OVERLAY = "lingmiao_overlay_channel"

        @Volatile
        private lateinit var instance: LingMiaoApp

        fun getInstance(): LingMiaoApp = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_CAPTURE,
                    "灵喵-录屏服务",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "后台录制屏幕用于辅助线计算"
                    setShowBadge(false)
                }
            )

            notificationManager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_OVERLAY,
                    "灵喵-悬浮辅助",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "显示台球辅助瞄准线"
                    setShowBadge(false)
                }
            )
        }
    }
}
