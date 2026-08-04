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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import com.lingmiao.v2.core.event.EventBus
import com.lingmiao.v2.core.log.LogManager

class FloatingService : Service() {

    companion object {
        const val TAG = "CalibrationOverlay"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "calibration_overlay"

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

    private lateinit var wm: WindowManager
    private var rootView: RelativeLayout? = null
    private var rectView: View? = null          // 半透明矩形
    private var resizeHandle: View? = null      // 右下角手柄
    private var layoutParams: WindowManager.LayoutParams? = null

    // 矩形区域参数（相对于屏幕）
    private var rectX = 100f
    private var rectY = 300f
    private var rectW = 600f
    private var rectH = 400f

    // 微调步长
    private val moveStep = 5f
    private val resizeStep = 10f

    override fun onCreate() {
        super.onCreate()
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        createOverlay()
    }

    private fun createOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        // 窗口参数：全屏大小，但只占据指定区域
        layoutParams = WindowManager.LayoutParams(
            rectW.toInt(), rectH.toInt(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = rectX.toInt()
            y = rectY.toInt()
        }

        val ctx = this
        val root = RelativeLayout(ctx).apply {
            // 半透明矩形背景
            val bg = View(ctx).apply {
                background = GradientDrawable().apply {
                    setColor(Color.argb(60, 255, 255, 255))
                    setStroke(2, Color.argb(200, 100, 200, 255))
                    cornerRadius = 8f
                }
            }
            addView(bg, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT))

            // 右下角拖拽手柄
            val handle = TextView(ctx).apply {
                text = "◢"
                setTextColor(Color.argb(255, 0, 120, 255))
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(8, 8, 8, 8)
                background = GradientDrawable().apply { setColor(Color.argb(100, 0, 0, 0)); cornerRadius = 8f }
                setOnTouchListener(handleTouchListener)
            }
            val handleParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            }
            addView(handle, handleParams)
            resizeHandle = handle

            // 方向键面板（放在矩形内部偏上位置）
            val btnPanel = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            // 上按钮
            val btnUp = makeArrowButton("▲") { adjustPosition(0, -moveStep) }
            val midRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            val btnLeft = makeArrowButton("◀") { adjustPosition(-moveStep, 0f) }
            val btnRight = makeArrowButton("▶") { adjustPosition(moveStep, 0f) }
            midRow.addView(btnLeft)
            midRow.addView(btnRight)
            val btnDown = makeArrowButton("▼") { adjustPosition(0f, moveStep) }
            btnPanel.addView(btnUp)
            btnPanel.addView(midRow)
            btnPanel.addView(btnDown)
            val panelParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            addView(btnPanel, panelParams)

            // 保存按钮（底部居中）
            val saveBtn = Button(ctx).apply {
                text = "保存"
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply { setColor(Color.argb(200, 100, 100, 100)); cornerRadius = 20f }
                setOnClickListener { saveCalibration() }
            }
            val saveParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                bottomMargin = 50
            }
            addView(saveBtn, saveParams)
        }

        // 整体拖动（在非按钮区域有效）
        root.setOnTouchListener(overallDragListener)

        wm.addView(root, layoutParams)
        rootView = root
        rectView = bg
    }

    // 整体拖动的触摸监听器
    private val overallDragListener = View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.tag = floatArrayOf(event.rawX, event.rawY, layoutParams!!.x.toFloat(), layoutParams!!.y.toFloat())
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val data = v.tag as FloatArray
                val dx = event.rawX - data[0]
                val dy = event.rawY - data[1]
                rectX = data[2] + dx
                rectY = data[3] + dy
                layoutParams!!.x = rectX.toInt()
                layoutParams!!.y = rectY.toInt()
                wm.updateViewLayout(rootView, layoutParams)
                true
            }
            else -> false
        }
    }

    // 右下角拖拽调整大小的触摸监听器
    private val handleTouchListener = View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.tag = floatArrayOf(event.rawX, event.rawY, rectW, rectH)
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val data = v.tag as FloatArray
                val dx = event.rawX - data[0]
                val dy = event.rawY - data[1]
                rectW = (data[2] + dx).coerceAtLeast(150f)
                rectH = (data[3] + dy).coerceAtLeast(100f)
                layoutParams!!.width = rectW.toInt()
                layoutParams!!.height = rectH.toInt()
                wm.updateViewLayout(rootView, layoutParams)
                true
            }
            else -> false
        }
    }

    private fun adjustPosition(dx: Float, dy: Float) {
        rectX += dx
        rectY += dy
        layoutParams!!.x = rectX.toInt()
        layoutParams!!.y = rectY.toInt()
        wm.updateViewLayout(rootView, layoutParams)
    }

    private fun makeArrowButton(label: String, action: () -> Unit): Button {
        return Button(this).apply {
            text = label
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply { setColor(Color.argb(150, 80, 80, 80)); cornerRadius = 8f }
            setOnClickListener { action() }
        }
    }

    private fun saveCalibration() {
        val prefs = getSharedPreferences("calibration", MODE_PRIVATE)
        // 将矩形转换为四个角坐标
        val tlx = rectX
        val tly = rectY
        val trx = rectX + rectW
        val try_ = rectY
        val blx = rectX
        val bly = rectY + rectH
        val brx = rectX + rectW
        val bry = rectY + rectH

        prefs.edit().apply {
            putFloat("tlx", tlx); putFloat("tly", tly)
            putFloat("trx", trx); putFloat("try_", try_)
            putFloat("blx", blx); putFloat("bly", bly)
            putFloat("brx", brx); putFloat("bry", bry)
            apply()
        }
        EventBus.emitCalibrationUpdated(floatArrayOf(tlx, tly, trx, try_, blx, bly, brx, bry))
        Toast.makeText(this, "校准数据已保存", Toast.LENGTH_SHORT).show()
        LogManager.geo("💾 校准矩形: ${rectX},${rectY} ${rectW}x${rectH}")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(CHANNEL_ID, "校准悬浮窗", NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(this)
            }
        }
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(this, FloatingService::class.java).apply { action = "STOP" }
        val pi = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID).setContentTitle("校准框运行中").setContentText("拖拽手柄调整大小").setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true).setContentIntent(pi).build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this).setContentTitle("校准框运行中").setContentText("拖拽手柄调整大小").setSmallIcon(android.R.drawable.ic_menu_compass).setOngoing(true).setContentIntent(pi).build()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        rootView?.let { try { wm.removeView(it) } catch (_: Exception) {} }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
