package com.lingmiao.v2.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.core.event.EventBus
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.ball.BallDetector
import com.lingmiao.v2.engine.aim.AimEngine

/**
 * 录屏服务 - MediaProjection + ImageReader
 * 实时捕获屏幕 → 转 Bitmap → 球检测 → 瞄准计算
 */
class ScreenCaptureService : Service() {

    companion object {
        const val TAG = "ScreenCapture"
        const val NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "lingmiao_capture"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }

    private lateinit var projectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    private lateinit var captureThread: HandlerThread
    private lateinit var captureHandler: Handler

    private var isCapturing = false
    private var frameCount = 0
    private var lastFpsTime = 0L
    private var currentFps = 0f

    override fun onCreate() {
        super.onCreate()
        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()

        captureThread = HandlerThread("ScreenCapture").apply { start() }
        captureHandler = Handler(captureThread.looper)

        startForeground(NOTIFICATION_ID, buildNotification())
        LogManager.service("📸 录屏服务已创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)

        if (resultCode != 0 && data != null) {
            startCapture(resultCode, data)
        }

        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        if (isCapturing) return

        mediaProjection = projectionManager.getMediaProjection(resultCode, data)
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                LogManager.service("📸 MediaProjection 已停止")
                stopCapture()
            }
        }, null)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, 0x1, 2).apply {
            setOnImageAvailableListener({ reader ->
                captureHandler.post { processFrame(reader.acquireLatestImage()) }
            }, captureHandler)
        }

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "LingMiaoCapture", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        isCapturing = true
        lastFpsTime = System.currentTimeMillis()
        LogManager.service("📸 录屏开始: ${width}x${height}")

        // 通知事件总线
        EventBus.emit("screen_capture_started", "$width x $height")
    }

    private fun processFrame(image: android.media.Image?) {
        if (image == null) return
        frameCount++

        // FPS 计算
        val now = System.currentTimeMillis()
        if (now - lastFpsTime >= 1000) {
            currentFps = frameCount * 1000f / (now - lastFpsTime)
            frameCount = 0
            lastFpsTime = now
        }

        try {
            // 转 Bitmap（简化处理，实际应做 RGBA 转换）
            val planes = image.planes
            if (planes.isNotEmpty()) {
                // 实际项目中这里做 NV21 → Bitmap 转换
                // 然后调用 BallDetector.detect(bitmap, AppConfig.detectMode)
                // 再调用 AimEngine.calculate(cueBall, balls)
                // 最后更新 OverlayRenderer
            }
        } catch (e: Exception) {
            LogManager.e(TAG, "帧处理异常: ${e.message}")
        } finally {
            image.close()
        }
    }

    private fun stopCapture() {
        isCapturing = false
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
        EventBus.emit("screen_capture_stopped", null)
        LogManager.service("📸 录屏已停止")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "灵喵录屏", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "屏幕捕获运行中"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("灵喵 录屏中")
            .setContentText("实时分析台球轨迹")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build()

    override fun onDestroy() {
        super.onDestroy()
        stopCapture()
        captureThread.quitSafely()
        LogManager.service("📸 录屏服务已销毁")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
