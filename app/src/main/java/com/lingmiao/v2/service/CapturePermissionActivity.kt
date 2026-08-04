package com.lingmiao.v2.service

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

/**
 * 录屏授权中转 Activity
 *
 * 为什么需要这个 Activity？
 * - MediaProjection.createScreenCaptureIntent() 必须在一个 Activity 中启动
 * - 但我们的 CaptureService 是后台服务，没有 Activity
 * - 所以用一个"透明无UI"的 Activity 来中转：
 *   1. 收到 Service 的 Intent → 启动系统录屏授权弹窗
 *   2. 用户同意 → 把 resultCode + data 传回 Service
 *   3. 用户拒绝 → 通知 Service 停止
 */
class CapturePermissionActivity : Activity() {

    companion object {
        const val REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mpm = getSystemService(MediaProjectionManager::class.java)
        val intent = mpm.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                // 用户同意 → 启动 CaptureService 并传入授权结果
                val serviceIntent = Intent(this, CaptureService::class.java).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                startForegroundService(serviceIntent)
            }
            // 无论成功失败，这个中转 Activity 都可以关闭了
            finish()
        }
    }
}
        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // 1. 弹出对话框，证明新代码已运行
        AlertDialog.Builder(this)
            .setTitle("✅ 新悬浮窗已激活")
            .setMessage("如果你看到这个弹窗，说明代码已更新。\n点击确定后，会显示一个红色方块悬浮窗。")
            .setPositiveButton("确定") { _, _ ->
                createRedOverlay() // 然后显示一个红色方块
            }
            .setCancelable(false)
            .create()
            .apply {
                window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            }
            .show()
    }

    private fun createRedOverlay() {
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams(
            300, 200,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER }

        val redView = TextView(this).apply {
            text = "这是新悬浮窗！"
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(Color.RED); cornerRadius = 16f }
            gravity = Gravity.CENTER
        }
        wm.addView(redView, params)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, "测试", NotificationManager.IMPORTANCE_LOW).apply {
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(this)
            }
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, FloatingService::class.java).apply { action = "STOP" }
        val pi = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("新服务正在运行")
                .setContentText("红色方块已显示")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setContentIntent(pi)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("新服务正在运行")
                .setContentText("红色方块已显示")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setContentIntent(pi)
                .build()
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        super.onDestroy()
        // 简单处理，不清理视图
    }
}
