package com.lingmiao.v2.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.widget.LinearLayout
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.table.GeometryEngine

class FloatingService : Service() {

    companion object {
        const val TAG = "FloatingService"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "lingmiao_overlay"
        const val ACTION_STOP = "com.lingmiao.v2.STOP_FLOATING"
        const val ACTION_UP = "up"
        const val ACTION_DOWN = "down"
        const val ACTION_LEFT = "left"
        const val ACTION_RIGHT = "right"
        const val ACTION_SAVE = "save"

        private var instance: FloatingService? = null

        fun start(context: Context) {
            if (!Settings.canDrawOverlays(context)) {
                Toast.makeText(context, "请先开启悬浮窗权限", Toast.LENGTH_SHORT).show()
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
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

        fun hideOverlay() {
            instance?.overlayView?.visibility = View.GONE
        }

        fun showOverlay() {
            instance?.overlayView?.visibility = View.VISIBLE
        }
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isRendering = false
    private var renderThread: Thread? = null
    private var lastFrameTime = 0L
    private val targetFrameTime = 16L
    private var frameCount = 0
    private var fpsTimer = 0L

    private var currentCorners = GeometryEngine.Corners(100f, 100f, 900f, 100f, 100f, 800f, 900f, 800f)

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        LogManager.service("🎯 悬浮校准服务已创建")

        if (!Settings.canDrawOverlays(this)) {
            LogManager.e(TAG, "没有悬浮窗权限，停止服务")
            stopSelf()
            return
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
        }
        if (overlayView == null) createOverlay()
        startRenderLoop()
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "灵喵悬浮校准", NotificationManager.IMPORTANCE_LOW).apply {
                description = "九宫格校准辅助运行中"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, FloatingService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("灵喵 校准辅助")
                .setContentText("正在调整对齐基准")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setContentIntent(pendingStop)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("灵喵 校准辅助")
                .setContentText("正在调整对齐基准")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(true)
                .setContentIntent(pendingStop)
                .setPriority(Notification.PRIORITY_LOW)
                .build()
        }
    }

    private fun createOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            LogManager.e(TAG, "createOverlay 失败: 无悬浮窗权限")
            stopSelf()
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 200

        val rootView = buildCalibrationPanel()

        // 拖动功能
        rootView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(rootView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(rootView, params)
        overlayView = rootView
        LogManager.service("✅ 九宫格校准悬浮窗已创建")
    }

    private fun buildCalibrationPanel(): LinearLayout {
        val ctx = this
        // 半透明深灰背景
        val bgDrawable = GradientDrawable().apply {
            setColor(Color.argb(180, 30, 30, 30))
            cornerRadius = 20f
        }

        val mainLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            background = bgDrawable
        }

        // 第一行：田字格 + 上下箭头
        val row1 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // 田字格方块（四个红色小方块）
        val gridBlock = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        val rowA = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        rowA.addView(makeColorBlock(Color.RED, 20))
        rowA.addView(makeColorBlock(Color.RED, 20))
        val rowB = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        rowB.addView(makeColorBlock(Color.RED, 20))
        rowB.addView(makeColorBlock(Color.RED, 20))
        gridBlock.addView(rowA)
        gridBlock.addView(rowB)

        // 上下箭头列
        val arrowCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 0, 0, 0)
        }
        arrowCol.addView(makeArrowButton("▲", ACTION_UP))
        arrowCol.addView(makeArrowButton("▼", ACTION_DOWN))

        row1.addView(gridBlock)
        row1.addView(arrowCol)
        mainLayout.addView(row1)

        // 第二行：左右箭头
        val row2 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 12, 0, 0)
        }
        row2.addView(makeArrowButton("◀", ACTION_LEFT))
        row2.addView(View(ctx).apply { layoutParams = LinearLayout.LayoutParams(40, 1) }) // 间隔
        row2.addView(makeArrowButton("▶", ACTION_RIGHT))
        mainLayout.addView(row2)

        // 第三行：中间向上箭头
        val row3 = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, 12, 0, 0)
        }
        row3.addView(makeArrowButton("▲", ACTION_UP))
        mainLayout.addView(row3)

        // 底部保存按钮（带三角形角标）
        val saveRow = RelativeLayout(ctx).apply {
            setPadding(0, 16, 0, 0)
        }
        val saveBtn = Button(ctx).apply {
            text = "保存"
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                setColor(Color.argb(150, 180, 180, 180))
                cornerRadius = 30f
            }
            background = btnBg
            setOnClickListener {
                // 保存当前校准角
                val prefs = ctx.getSharedPreferences("calibration", MODE_PRIVATE)
                prefs.edit().apply {
                    putFloat("tlx", currentCorners.tlx); putFloat("tly", currentCorners.tly)
                    putFloat("trx", currentCorners.trx); putFloat("try_", currentCorners.try_)
                    putFloat("blx", currentCorners.blx); putFloat("bly", currentCorners.bly)
                    putFloat("brx", currentCorners.brx); putFloat("bry", currentCorners.bry)
                    apply()
                }
                Toast.makeText(ctx, "校准数据已保存", Toast.LENGTH_SHORT).show()
            }
        }
        val paramsBtn = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        paramsBtn.addRule(RelativeLayout.CENTER_HORIZONTAL)
        saveBtn.layoutParams = paramsBtn

        // 右下角深蓝色三角形（用TextView + 旋转或直接绘制，这里简化用一个小块）
        val triangle = TextView(ctx).apply {
            text = "◢"
            setTextColor(Color.argb(255, 0, 50, 180))
            textSize = 18f
        }
        val paramsTri = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        paramsTri.addRule(RelativeLayout.ALIGN_PARENT_END)
        paramsTri.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        triangle.layoutParams = paramsTri

        saveRow.addView(saveBtn)
        saveRow.addView(triangle)
        mainLayout.addView(saveRow)

        return mainLayout
    }

    private fun makeColorBlock(color: Int, sizeDp: Int): View {
        val px = (sizeDp * resources.displayMetrics.density).toInt()
        val view = View(this)
        view.layoutParams = LinearLayout.LayoutParams(px, px).apply {
            setMargins(2, 2, 2, 2)
        }
        view.setBackgroundColor(color)
        return view
    }

    private fun makeArrowButton(label: String, action: String): Button {
        val btn = Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            val btnBg = GradientDrawable().apply {
                setColor(Color.argb(150, 100, 100, 100))
                cornerRadius = 8f
            }
            background = btnBg
            setOnClickListener {
                val step = 2f
                currentCorners = when (action) {
                    ACTION_UP -> currentCorners.copy(tly = currentCorners.tly - step, try_ = currentCorners.try_ - step)
                    ACTION_DOWN -> currentCorners.copy(tly = currentCorners.tly + step, try_ = currentCorners.try_ + step)
                    ACTION_LEFT -> currentCorners.copy(tlx = currentCorners.tlx - step, trx = currentCorners.trx - step)
                    ACTION_RIGHT -> currentCorners.copy(tlx = currentCorners.tlx + step, trx = currentCorners.trx + step)
                    else -> currentCorners
                }
                // 可以触发渲染更新，这里暂时省略
            }
        }
        return btn
    }

    private fun startRenderLoop() {
        if (isRendering) return
        isRendering = true
        renderThread = Thread({
            while (isRendering) {
                val now = System.currentTimeMillis()
                val elapsed = now - lastFrameTime
                if (elapsed >= targetFrameTime) {
                    // 这里可以绘制半透明引导线（Canvas绘制在overlayView上）
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
        }, "OverlayRender").apply { start() }
    }

    private fun stopRenderLoop() {
        isRendering = false
        renderThread?.interrupt()
        renderThread = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        stopRenderLoop()
        if (overlayView != null) {
            try { windowManager.removeView(overlayView) } catch (e: IllegalArgumentException) {}
            overlayView = null
        }
        LogManager.service("🛑 悬浮校准服务已销毁")
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null
}
