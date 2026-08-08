package com.lingmiao.v2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat // 🔥 补上了重要依赖
import com.lingmiao.v2.R
import com.lingmiao.v2.config.AppConfig // 🔥 修正了 AppConfig 的路径

/**
 * 悬浮绘制服务（前台服务，独立进程 :overlay）
 *
 * 职责：
 *   1. 创建 TYPE_APPLICATION_OVERLAY 的透明窗口
 *   2. 在 SurfaceView 上绘制辅助线（不拦截触控）
 *   3. 接收 CaptureService 的识别结果并刷新绘制
 *
 * 设计要点：
 *   - FLAG_NOT_FOCUSABLE + FLAG_NOT_TOUCHABLE → 不抢触摸事件
 *   - 独立进程 → 即使游戏崩溃，辅助线依然存活
 *   - SurfaceView 硬件加速 → 60fps 流畅绘制
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_OVERLAY = "channel_overlay"

        @Volatile
        private var instance: OverlayService? = null

        fun start(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, OverlayService::class.java)
            context.stopService(intent)
        }

        fun updateAimLine(points: FloatArray) {
            instance?.renderAimLine(points)
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var controlView: View? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private var currentPoints: FloatArray = FloatArray(0)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlayWindow()
        Log.i(TAG, "OverlayService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    private fun createOverlayWindow() {
        overlayView = OverlayView(this) { currentPoints }

        val overlayParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(overlayView, overlayParams)

        controlView = LayoutInflater.from(this)
            .inflate(R.layout.control_panel, null)

        val controlParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 100
        }

        controlView?.findViewById<View>(R.id.btn_toggle_line)?.setOnClickListener {
            AppConfig.isLineVisible = !AppConfig.isLineVisible
            overlayView?.postInvalidate()
        }
        controlView?.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            stopSelf()
        }

        windowManager.addView(controlView, controlParams)
        makeControlDraggable(controlView!!, controlParams)
    }

    private fun makeControlDraggable(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    fun renderAimLine(points: FloatArray) {
        currentPoints = points
        overlayView?.postInvalidate()
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_OVERLAY,
                    "灵喵-悬浮辅助",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        return NotificationCompat.Builder(this, CHANNEL_OVERLAY)
            .setContentTitle("灵喵-悬浮辅助") // 🔥 修复了 getString
            .setContentText("辅助线绘制中") // 🔥 修复了 getString
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null

        overlayView?.let { windowManager.removeView(it) }
        controlView?.let { windowManager.removeView(it) }
        overlayView = null
        controlView = null

        Log.i(TAG, "OverlayService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ===== 自定义绘制 View =====
    private class OverlayView(
        context: Context,
        private val getPoints: () -> FloatArray
    ) : View(context) {

        private val path = Path()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        init {
            setBackgroundColor(Color.TRANSPARENT)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val points = getPoints()
            if (points.size < 4) return

            paint.color = AppConfig.lineColor
            paint.strokeWidth = AppConfig.lineWidth
            paint.pathEffect = if (AppConfig.showAntLine) {
                DashPathEffect(floatArrayOf(20f, 10f), 0f)
            } else {
                null
            }

            path.reset()
            path.moveTo(points[0], points[1])
            for (i in 2 until points.size step 2) {
                path.lineTo(points[i], points[i + 1])
            }
            canvas.drawPath(path, paint)

            paint.style = Paint.Style.FILL
            for (i in 0 until points.size step 2) {
                canvas.drawCircle(points[i], points[i + 1], 8f, paint)
            }
            paint.style = Paint.Style.STROKE
        }
    }
}
