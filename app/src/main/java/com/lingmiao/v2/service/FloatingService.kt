package com.lingmiao.v2.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.render.OverlayRenderer

/**
 * 悬浮窗服务 - 60fps 硬件加速渲染
 * 使用 SurfaceView 避免 View 绘制开销
 */
class FloatingService : Service() {

    companion object {
        const val TAG = "FloatingService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "lingmiao_overlay"

        fun start(context: Context) {
            val intent = Intent(context, FloatingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingService::class.java))
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: SurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null
    private lateinit var renderer: OverlayRenderer
    private var isRendering = false
    private var renderThread: Thread? = null

    // 帧率控制
    private var lastFrameTime = 0L
    private val targetFrameTime = 16L // ~60fps
    private var actualFps = 60f
    private var frameCount = 0
    private var fpsTimer = 0L

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        renderer = OverlayRenderer()
        renderer.applyConfig()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        LogManager.service("🎯 悬浮窗服务已创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
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
            action = "STOP"
        }
        val pendingStop = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("灵喵 LingMiao 运行中")
            .setContentText("点击关闭悬浮辅助")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(pendingStop)
            .build()
    }

    private fun createOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
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
                        actualFps = frameCount * 1000f / (now - fpsTimer)
                        frameCount = 0
                        fpsTimer = now
                        renderer.setFps(actualFps)
                    }
                } else {
                    // 精确休眠
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

            // 渲染辅助线
            renderer.render(canvas)
        } catch (e: Exception) {
            LogManager.e(TAG, "渲染异常: ${e.message}")
        } finally {
            if (canvas != null) {
                try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
