package com.lingmiao.v2.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.lingmiao.v2.core.event.EventBus
import com.lingmiao.v2.core.log.LogManager

class KeepAliveService : Service() {

    companion object {
        const val TAG = "KeepAlive"
        const val NOTIFICATION_ID = 1000
        const val CHANNEL_ID = "lingmiao_alive"

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

    private var startTime = 0L

    override fun onCreate() {
        super.onCreate()
        startTime = System.currentTimeMillis()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        LogManager.service("🛡 保活服务已启动")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 防御 intent 为 null（系统自动重启时）
        if (intent != null) {
            val running = intent.getStringArrayListExtra("running") ?: emptyList()
            if (FloatingService::class.java.name !in running) {
                EventBus.emit("service_restart_needed", "floating")
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restart = Intent(applicationContext, KeepAliveService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restart)
        } else {
            startService(restart)
        }
        LogManager.w(TAG, "⚠️ 任务被移除，尝试保活")
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_MODERATE -> LogManager.w(TAG, "内存中等紧张 level=$level")
            TRIM_MEMORY_COMPLETE -> {
                LogManager.e(TAG, "内存严重不足！释放非关键资源")
                EventBus.emit("low_memory_critical", level)
            }
            TRIM_MEMORY_RUNNING_LOW -> LogManager.w(TAG, "运行内存偏低")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        LogManager.e(TAG, "🚨 onLowMemory 触发！")
        EventBus.emit("low_memory", null)
    }

    fun getUptime(): Long = System.currentTimeMillis() - startTime

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "灵喵保活", NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "保持灵喵服务常驻"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("灵喵 守护中")
                .setContentText("保障悬浮辅助稳定运行")
                .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("灵喵 守护中")
                .setContentText("保障悬浮辅助稳定运行")
                .setSmallIcon(android.R.drawable.ic_lock_idle_low_battery)
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_MIN)
                .build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LogManager.service("🛡 保活服务已销毁（将被重启）")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
