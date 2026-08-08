package com.lingmiao.v2.service

import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.lingmiao.v2.LingMiaoApp // 🔥 引入全局渠道定义

class FloatingService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1002
        
        // 🔥【关键修复】：之前您缺失的 start 方法，它必须存在！
        fun start(context: Context) {
            val intent = Intent(context, FloatingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // 弹出一个测试对话框，证明服务已经启动
        val dialog = AlertDialog.Builder(this)
            .setTitle("✅ 悬浮窗服务已激活")
            .setMessage("代码已成功更新并运行。\n点击确定后，屏幕中央会显示一个红色方块。")
            .setPositiveButton("确定") { _, _ ->
                createRedOverlay()
            }
            .setCancelable(false)
            .create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun createRedOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            300,
            200,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val redView = TextView(this).apply {
            text = "这是新的红色悬浮窗！"
            setTextColor(Color.WHITE)
            setPadding(20, 20, 20, 20)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(Color.RED)
                cornerRadius = 16f
            }
            gravity = Gravity.CENTER
        }

        wm.addView(redView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            // 🔥 优化：直接调用 LingMiaoApp 定义好的渠道常量，避免多个渠道冲突
            nm.createNotificationChannel(
                NotificationChannel(
                    LingMiaoApp.CHANNEL_OVERLAY,
                    "灵喵-悬浮辅助",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "显示台球辅助瞄准线"
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, FloatingService::class.java).apply {
            action = "STOP"
        }
        val pendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // 🔥 优化：统一使用 LingMiaoApp 的渠道 ID
        return NotificationCompat.Builder(this, LingMiaoApp.CHANNEL_OVERLAY)
            .setContentTitle("灵喵-悬浮辅助")
            .setContentText("红色测试方块已显示")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }
}
