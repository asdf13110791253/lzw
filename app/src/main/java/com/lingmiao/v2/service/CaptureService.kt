package com.lingmiao.v2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.lingmiao.v2.LingMiaoApp
import com.lingmiao.v2.config.AppConfig // ✅ 修正路径：去掉了 core
import com.lingmiao.v2.engine.ball.BallDetector // ✅ 修正路径：去掉了 core，加了 engine.ball
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

/**
 * 核心录屏捕获服务（前台服务，独立进程 :capture）
 *
 * 工作流程：
 *   1. 获取 MediaProjection 对象（从 Intent 中拿到用户授权结果）
 *   2. 创建 ImageReader + VirtualDisplay 持续抓帧
 *   3. 每帧 → Bitmap → JNI → C++ 识别
 *   4. C++ 返回球体坐标 → 通过回调传给 OverlayService 绘制
 *
 * 性能策略：
 *   - 降分辨率到 640x360 再处理（性能提升 3x）
 *   - 帧率限制在 20fps（50ms 间隔）
 *   - 异步处理，不阻塞 ImageReader 回调线程
 */
class CaptureService : Service() {

    companion object {
        private const val TAG = "CaptureService"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, CaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            context.startForegroundService(intent)
        }
    }

    private lateinit var mediaProjection: MediaProjection
    private lateinit var imageReader: ImageReader
    private lateinit var virtualDisplay: VirtualDisplay
    private lateinit var handlerThread: HandlerThread
    private lateinit var backgroundHandler: Handler
    private lateinit var scope: CoroutineScope

    // 帧率控制
    private var lastProcessTime = 0L
    private val frameInterval = 50L // 20fps

    // 处理中的帧计数（防止堆积）
    private var processing = false

    // 处理宽度（降分辨率）
    private var captureWidth = 640
    private var captureHeight = 360

    override fun onCreate() {
        super.onCreate()
        handlerThread = HandlerThread("capture-bg").apply { start() }
        backgroundHandler = Handler(handlerThread.looper)
        scope = CoroutineScope(Dispatchers.Default + Job())
        Log.i(TAG, "CaptureService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ⚠️ Android 14 硬性要求：必须在 onStartCommand 中立即调用 startForeground
        startForeground(NOTIFICATION_ID, createNotification())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, -1) ?: -1
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)

        if (resultCode == -1 || data == null) {
            Log.e(TAG, "Invalid MediaProjection data, stopping")
            stopSelf()
            return START_NOT_STICKY
        }

        // 延迟一点再初始化（等通知显示完）
        Handler(Looper.getMainLooper()).postDelayed({
            initMediaProjection(resultCode, data)
        }, 500)

        return START_STICKY
    }

    private fun initMediaProjection(resultCode: Int, data: Intent) {
        val mpm = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = mpm.getMediaProjection(resultCode, data)

        // 获取屏幕尺寸，按比例缩放到 640 宽
        val metrics = resources.displayMetrics
        val scale = captureWidth.toFloat() / metrics.widthPixels
        captureHeight = (metrics.heightPixels * scale).toInt()

        Log.i(TAG, "Capture resolution: ${captureWidth}x$captureHeight")

        // ImageReader 用于接收屏幕帧
        imageReader = ImageReader.newInstance(
            captureWidth, captureHeight,
            PixelFormat.RGBA_8888, 3
        )

        // 创建 VirtualDisplay，把屏幕内容投影到 ImageReader 的 Surface
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "LingMiaoCapture",
            captureWidth, captureHeight, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )

        // 监听新帧
        imageReader.setOnImageAvailableListener({ reader ->
            if (processing) return@setOnImageAvailableListener

            val now = System.currentTimeMillis()
            if (now - lastProcessTime < frameInterval) return@setOnImageAvailableListener
            lastProcessTime = now

            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            processing = true

            // 在后台线程处理
            scope.launch {
                try {
                    processFrame(image)
                } finally {
                    processing = false
                }
            }
        }, backgroundHandler)

        AppConfig.isAssistRunning = true
        Log.i(TAG, "MediaProjection initialized, capture started")
    }

    /**
     * 处理一帧图像：
     * 1. Image → Bitmap
     * 2. Bitmap → JNI → C++ 识别
     * 3. 识别结果 → OverlayService 绘制
     */
    private suspend fun processFrame(image: android.media.Image) {
        try {
            // Image → Bitmap
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // 调用 C++ 识别
            val result = BallDetector.detect(bitmap)

            // 计算辅助线
            val aimLine = BallDetector.computeAimLine(result)

            // 把结果发给 OverlayService 绘制
            if (aimLine != null && AppConfig.isLineVisible) {
                OverlayService.updateAimLine(aimLine.rawPoints)
            }

            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "processFrame error: ${e.message}")
        } finally {
            image.close()
        }
    }

    private fun createNotification(): Notification {
        // 确保通道存在
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    LingMiaoApp.CHANNEL_CAPTURE,
                    "灵喵-录屏服务",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        // 🔥 修复：为了防止 R.string 找不到资源，直接硬编码文字
        return NotificationCompat.Builder(this, LingMiaoApp.CHANNEL_CAPTURE)
            .setContentTitle("灵喵-录屏服务")
            .setContentText("正在截取台球画面")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        AppConfig.isAssistRunning = false

        if (::virtualDisplay.isInitialized) virtualDisplay.release()
        if (::mediaProjection.isInitialized) mediaProjection.stop()
        if (::imageReader.isInitialized) imageReader.close()

        scope.cancel()
        handlerThread.quitSafely()

        Log.i(TAG, "CaptureService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
