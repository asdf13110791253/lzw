package com.lingmiao.v2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * 保活服务：防止 App 在后台被系统杀掉，提升悬浮窗稳定性
 */
class KeepAliveService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "keep_alive_channel"

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification())
        } else {
            // 低版本直接启动前台，兼容旧手机
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "后台保活",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        // 用停止服务的按钮，让用户可以主动退出
        val stopIntent = Intent(this, KeepAliveService::class.java).apply {
            action = "STOP"
        }
        val pendingStop = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("台球辅助运行中")
            .setContentText("触控校准服务已保活")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true) // 不可滑动清除
            .setPriority(NotificationCompat.PRIORITY_LOW) // 低优先级，不影响正常通知
            .setContentIntent(pendingStop)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 收到“停止”指令时，停止保活
        if (intent?.action == "STOP") {
            stopSelf()
        }
        return START_STICKY // 如果被杀掉，系统会自动尝试重启
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
    }
}
