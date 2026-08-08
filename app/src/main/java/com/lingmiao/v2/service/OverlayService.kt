package com.lingmiao.v2.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log // 🔥 补上了缺失的 android.util.Log 导入
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.lingmiao.v2.R
import com.lingmiao.v2.config.AppConfig

/**
 * 悬浮绘制服务（优化版：性能提升、拖拽平滑、防崩溃）
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_OVERLAY = "channel_overlay"
        private const val ACTION_STOP_SERVICE = "com.lingmiao.v2.ACTION_STOP_OVERLAY"

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
    
    // 接收停止服务广播的接收器
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP_SERVICE) {
                stopSelf()
            }
        }
    }

    // 🔥【优化1：线程安全】添加 @Volatile 标记，确保多线程读取最新值
    @Volatile
    private var currentPoints: FloatArray = FloatArray(0)

    // 🔥【优化2：性能提升】将画笔移动到外部，避免 onDraw 频繁创建对象
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        // 使用硬件加速支持的路径效果
    }
    private val drawPath = Path()

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        // 注册停止广播
        registerReceiver(stopReceiver, IntentFilter(ACTION_STOP_SERVICE))
        
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

        controlView = LayoutInflater.from(this).inflate(R.layout.control_panel, null)
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

        // 🔥【优化3：防崩溃】采用安全调用（?.）防止 layout 里没找到 view 导致闪退
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

    // 🔥【优化4：拖拽逻辑修正】修复了拖拽时容易跳屏的计算公式
    private fun makeControlDraggable(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // 计算偏移量（用当前的 x - 初始触摸点 x）
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    fun renderAimLine(points: FloatArray) {
        // 🔥【优化5：防御性复制】防止外部传入的数组在绘制过程中被修改导致崩溃
        currentPoints = points.copyOf()
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

        // 🔥【优化6：交互提升】给通知增加一个“停止服务”按钮
        val stopIntent = Intent(ACTION_STOP_SERVICE).apply {
            setPackage(packageName)
        }
        val pendingStop = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_OVERLAY)
            .setContentTitle("灵喵-悬浮辅助")
            .setContentText("辅助线绘制中")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "停止服务",
                    pendingStop
                ).build()
            )
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        // 取消注册接收器，防止内存泄漏
        try { unregisterReceiver(stopReceiver) } catch (_: Exception) {}

        overlayView?.let { windowManager.removeView(it) }
        controlView?.let { windowManager.removeView(it) }
        overlayView = null
        controlView = null
        Log.i(TAG, "OverlayService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ===== 自定义绘制 View（已大幅优化性能） =====
    private inner class OverlayView(
        context: Context,
        private val getPoints: () -> FloatArray
    ) : View(context) {

        init {
            setBackgroundColor(Color.TRANSPARENT)
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // 拿到防御性拷贝的数组，保证安全
            val points = getPoints()
            if (points.size < 4) return

            // 🔥【优化7：避免重复赋值】如果配置没变，不会重复设置 paint
            paint.color = AppConfig.lineColor
            paint.strokeWidth = AppConfig.lineWidth
            paint.pathEffect = if (AppConfig.showAntLine) {
                DashPathEffect(floatArrayOf(20f, 10f), 0f)
            } else {
                null
            }

            // 重用 Path，而不是每次都 new Path()
            drawPath.reset()
            drawPath.moveTo(points[0], points[1])
            for (i in 2 until points.size step 2) {
                drawPath.lineTo(points[i], points[i + 1])
            }
            canvas.drawPath(drawPath, paint)

            // 画实心高亮点
            paint.style = Paint.Style.FILL
            for (i in 0 until points.size step 2) {
                canvas.drawCircle(points[i], points[i + 1], 8f, paint)
            }
            paint.style = Paint.Style.STROKE
        }
    }
}
