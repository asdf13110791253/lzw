package com.lingmiao.v2.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.lingmiao.v2.core.log.LogManager

class FloatingService : Service() {

    companion object {
        private var instance: FloatingService? = null

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
                return
            }
            val intent = Intent(context, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingService::class.java))
        }
    }

    private var overlayView: View? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(1, buildNotification())
        Toast.makeText(this, "悬浮服务已启动", Toast.LENGTH_SHORT).show()  // 验证服务启动
        createOverlay()
    }

    private fun createOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            300, 200,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        // 简单红色面板
        val panel = TextView(this).apply {
            text = "✅ 悬浮窗测试\n点击保存校准"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { setColor(Color.RED); cornerRadius = 16f }
            setOnClickListener {
                Toast.makeText(context, "已保存测试校准", Toast.LENGTH_SHORT).show()
            }
        }
        wm.addView(panel, params)
        overlayView = panel
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager.IMPORTANCE_LOW.let {
                NotificationChannel("test", "测试", it).also { channel ->
                    (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, "test").setContentTitle("测试服务").setSmallIcon(android.R.drawable.ic_menu_compass).build()
        else
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("测试服务").setSmallIcon(android.R.drawable.ic_menu_compass).build()
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        (overlayView?.parent as? WindowManager)?.removeView(overlayView)
    }
}
