package com.lingmiao.v2.engine.render

import android.graphics.*
import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.engine.aim.AimEngine
import com.lingmiao.v2.engine.aim.AimEngine.AimResult
import kotlin.math.*

/**
 * 灵喵渲染引擎 - Canvas 绘制
 * - 辅助线（粗细/颜色可调）
 * - 蚂蚁线
 * - 箭头
 * - 球位标记
 * - 60fps 优化
 */
class OverlayRenderer {

    // ── 配置 ──
    var aimColor: Int = AppConfig.aimColor
        private set
    var aimWidth: Float = AppConfig.aimWidth
        private set

    // ── 画笔缓存（避免每帧重建）──
    private val aimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val dashedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val ballPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val cueBallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.RED
        alpha = 120
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        // 外发光效果
    }

    // ── 路径对象复用 ──
    private val dashPath = Path()
    private val arrowPath = Path()

    // ── 当前渲染数据 ──
    private var currentAim: AimResult? = null
    private var detectedBalls: List<BallRenderInfo> = emptyList()
    private var pocketPositions: Array<FloatArray> = emptyArray()
    private var fps = 60f

    data class BallRenderInfo(
        val x: Float, val y: Float, val r: Float,
        val isCue: Boolean, val color: Int
    )

    init {
        applyConfig()
    }

    fun applyConfig() {
        aimColor = AppConfig.aimColor
        aimWidth = AppConfig.aimWidth.coerceIn(1f, 10f)

        aimPaint.color = aimColor
        aimPaint.strokeWidth = aimWidth
        dashedPaint.color = aimColor
        dashedPaint.strokeWidth = aimWidth * 0.7f
        arrowPaint.color = aimColor

        // 外发光（同色半透明）
        glowPaint.color = aimColor
        glowPaint.alpha = 60
        glowPaint.strokeWidth = aimWidth + 6f
    }

    // ── 数据更新 ──

    fun updateAim(result: AimResult?) {
        currentAim = result
    }

    fun updateBalls(balls: List<BallRenderInfo>) {
        detectedBalls = balls
    }

    fun updatePockets(pockets: Array<FloatArray>) {
        pocketPositions = pockets
    }

    fun setFps(f: Float) { fps = f.coerceIn(1f, 120f) }

    // ── 主绘制方法 ──

    fun render(canvas: Canvas) {
        // 1. 袋口
        for (p in pocketPositions) {
            canvas.drawCircle(p[0], p[1], 30f, pocketPaint)
        }

        // 2. 球
        for (ball in detectedBalls) {
            if (ball.isCue) {
                cueBallPaint.color = Color.WHITE
                cueBallPaint.alpha = 220
                canvas.drawCircle(ball.x, ball.y, ball.r, cueBallPaint)
            } else {
                ballPaint.color = ball.color
                ballPaint.strokeWidth = 2f
                canvas.drawCircle(ball.x, ball.y, ball.r, ballPaint)
            }
        }

        // 3. 瞄准线
        val aim = currentAim ?: return
        val path = aim.aimPath
        if (path.size < 4) return

        // 外发光（先画粗半透明，再画细实线）
        if (AppConfig.showAntLine) {
            drawDashedLine(canvas, path)
        } else {
            drawSolidLine(canvas, path)
        }

        // 4. 反射点
        for (rp in aim.reflectionPoints) {
            drawReflectionMarker(canvas, rp[0], rp[1])
        }

        // 5. 箭头（终点方向指示）
        if (path.size >= 4) {
            val lastIdx = path.size - 2
            val prevIdx = path.size - 4
            if (prevIdx >= 0) {
                drawArrow(canvas, path[prevIdx], path[prevIdx + 1], path[lastIdx], path[lastIdx + 1])
            }
        }

        // 6. 信息面板
        drawInfoPanel(canvas, aim)
    }

    // ── 绘制子方法 ──

    private fun drawSolidLine(canvas: Canvas, path: FloatArray) {
        // 外发光
        for (i in 0 until path.size / 2 - 1) {
            val x1 = path[i * 2]; val y1 = path[i * 2 + 1]
            val x2 = path[i * 2 + 2]; val y2 = path[i * 2 + 3]
            canvas.drawLine(x1, y1, x2, y2, glowPaint)
        }
        // 主线
        for (i in 0 until path.size / 2 - 1) {
            val x1 = path[i * 2]; val y1 = path[i * 2 + 1]
            val x2 = path[i * 2 + 2]; val y2 = path[i * 2 + 3]
            canvas.drawLine(x1, y1, x2, y2, aimPaint)
        }
    }

    private fun drawDashedLine(canvas: Canvas, path: FloatArray) {
        dashPath.reset()
        for (i in 0 until path.size / 2 - 1) {
            val x1 = path[i * 2]; val y1 = path[i * 2 + 1]
            val x2 = path[i * 2 + 2]; val y2 = path[i * 2 + 3]
            if (i == 0) dashPath.moveTo(x1, y1)
            dashPath.lineTo(x2, y2)
        }
        canvas.drawPath(dashPath, dashedPaint)
    }

    private fun drawReflectionMarker(canvas: Canvas, x: Float, y: Float) {
        val crossSize = 10f
        glowPaint.alpha = 40
        canvas.drawCircle(x, y, 8f, glowPaint)
        glowPaint.alpha = 60
        canvas.drawLine(x - crossSize, y, x + crossSize, y, aimPaint)
        canvas.drawLine(x, y - crossSize, x, y + crossSize, aimPaint)
    }

    private fun drawArrow(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float) {
        val dx = toX - fromX
        val dy = toY - fromY
        val len = sqrt(dx * dx + dy * dy)
        if (len < 0.001f) return

        val ux = dx / len; val uy = dy / len
        val arrowLen = 18f
        val arrowAngle = PI / 6.0

        val leftX = toX - arrowLen * cos(atan2(dy, dx) + arrowAngle).toFloat()
        val leftY = toY - arrowLen * sin(atan2(dy, dx) + arrowAngle).toFloat()
        val rightX = toX - arrowLen * cos(atan2(dy, dx) - arrowAngle).toFloat()
        val rightY = toY - arrowLen * sin(atan2(dy, dx) - arrowAngle).toFloat()

        arrowPath.reset()
        arrowPath.moveTo(toX, toY)
        arrowPath.lineTo(leftX, leftY)
        arrowPath.lineTo(rightX, rightY)
        arrowPath.close()
        canvas.drawPath(arrowPath, arrowPaint)
    }

    private fun drawInfoPanel(canvas: Canvas, aim: AimResult) {
        val text = if (aim.isDirect) {
            "直球 ${"%.1f".format(aim.angle)}° 力度${"%.0f".format(aim.power * 100)}%"
        } else {
            "${aim.banks}库翻袋 ${"%.1f".format(aim.angle)}°"
        }

        val padding = 12f
        val tw = textPaint.measureText(text)
        val px = 20f
        val py = 20f

        // 背景
        val bgPaint = Paint().ap
