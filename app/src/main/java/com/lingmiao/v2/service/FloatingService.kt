package com.lingmiao.v2.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.render.OverlayRenderer
import com.lingmiao.v2.engine.table.GeometryEngine

class FloatingService : Service() {

    companion object {
        const val TAG = "FloatingService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "lingmiao_overlay"
        const val ACTION_STOP = "com.lingmiao.v2.STOP_FLOATING"

        fun start(context: Context) {
            // 权限防御：启动前检查悬浮窗权限，没有则跳转设置
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return
            }

            val serviceIntent = Intent(context, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: SurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null
    private val geometry = GeometryEngine()
    private lateinit var renderer: OverlayRenderer
    private var isRendering = false
    private var renderThread: Thread? = null

    private var lastFrameTime = 0L
    private val targetFrameTime = 16L
    private var frameCount = 0
    private var fpsTimer = 0L

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        renderer = OverlayRenderer(geometry)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        LogManager.service("🎯 悬浮窗服务已创建")

        // 二次检查权限（防止直接在系统设置中被关闭后服务仍被启动）
        if (!Settings.canDrawOverlays(this)) {
            LogManager.e(TAG, "没有悬浮窗权限，停止服务")
            stopSelf()
            return
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (overlayView == null) {
            createOverlay()
        }
        startRenderLoop()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "灵喵悬浮辅助", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "台球辅助悬浮窗运行中"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, FloatingService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("灵喵 LingMiao 运行中")
                .setContentText("点击关闭悬浮辅助")
                .setSmallIcon(android.R.drawable.ic_menu_compass) // 可替换为自己的图标
                .setOngoing(true)
                .setContentIntent(pendingStop)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("灵喵 LingMiao 运行中")
                .setContentText("点击关闭悬浮辅助")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setContentIntent(pendingStop)
                .priority = Notification.PRIORITY_LOW
                .build()
        }
    }

    private fun createOverlay() {
        // 再次确认权限
        if (!Settings.canDrawOverlays(this)) {
            LogManager.e(TAG, "createOverlay 失败: 无悬浮窗权限")
            stopSelf()
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        val sv = SurfaceView(this).apply {
            setZOrderOnTop(true)
            setBackgroundColor(Color.TRANSPARENT)
        }

        sv.holder.setFormat(PixelFormat.TRANSLUCENT)
        sv.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                surfaceHolder = holder
                startRenderLoop()
            }
            override fun surfaceChanged(holder: SurfaceHolder, fmt: Int, w: Int, h: Int) {}
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopRenderLoop()
                surfaceHolder = null
            }
        })

        windowManager.addView(sv, params)
        overlayView = sv
        LogManager.service("✅ 悬浮窗 SurfaceView 已创建")
    }

    private fun startRenderLoop() {
        if (isRendering || surfaceHolder == null) return
        isRendering = true

        renderThread = Thread({
            LogManager.service("🎬 渲染线程启动 (target 60fps)")
            fpsTimer = System.currentTimeMillis()

            while (isRendering && surfaceHolder != null) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastFrameTime

                if (elapsed >= targetFrameTime) {
                    renderFrame()
                    lastFrameTime = now
                    frameCount++

                    if (now - fpsTimer >= 1000) {
                        frameCount = 0
                        fpsTimer = now
                    }
                } else {
                    try { Thread.sleep(targetFrameTime - elapsed) } catch (_: InterruptedException) {}
                }
            }
            LogManager.service("🛑 渲染线程停止")
        }, "OverlayRender").apply { start() }
    }

    private fun stopRenderLoop() {
        isRendering = false
        renderThread?.interrupt()
        renderThread = null
    }

    private fun renderFrame() {
        val holder = surfaceHolder ?: return
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas == null) return
            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            renderer.render(canvas, null)
        } catch (e: Exception) {
            LogManager.e(TAG, "渲染异常: ${e.message}")
        } finally {
            if (canvas != null) {
                try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRenderLoop()

        // 移除悬浮窗
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView)
            } catch (e: IllegalArgumentException) {
                LogManager.e(TAG, "移除悬浮窗失败: ${e.message}")
            }
            overlayView = null
        }

        LogManager.service("🛑 悬浮窗服务已销毁")
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
