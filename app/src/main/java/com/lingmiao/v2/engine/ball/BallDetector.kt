package com.lingmiao.v2.engine.ball

import android.graphics.Bitmap
import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.core.log.LogManager
import kotlin.math.PI
import kotlin.math.sqrt
import java.util.ArrayDeque

object BallDetector {

    private const val TAG = "BallDetect"

    data class DetectedBall(
        val x: Float,
        val y: Float,
        val radius: Float,
        val confidence: Float,
        val isCueBall: Boolean,
        val ballType: Int,
        val color: Int
    )

    const val MODE_HAAR: Int = 0
    const val MODE_HSV: Int = 1
    const val MODE_EDGE: Int = 2
    const val MODE_TFLITE: Int = 3
    const val MODE_FUSION: Int = 4

    private var nativeAvailable = false

    fun init() {
        try {
            System.loadLibrary("lingmiao_engine")
            nativeAvailable = true
            LogManager.native("✅ BallDetector native 初始化成功")
        } catch (e: UnsatisfiedLinkError) {
            nativeAvailable = false
            LogManager.w(TAG, "⚠️ Native 库不可用，使用 CPU 模式")
        }
    }

    fun detect(bitmap: Bitmap, mode: Int = AppConfig.detectMode): List<DetectedBall> {
        if (nativeAvailable) {
            return detectNative(bitmap, mode)
        }
        return detectCpu(bitmap, mode)
    }

    private fun detectNative(bitmap: Bitmap, mode: Int): List<DetectedBall> {
        val preset = AppConfig.getCurrentPreset()
        val result = detectBallsNative(
            bitmap,
            preset.vThreshold,
            preset.sMinDist,
            preset.pSensitivity,
            preset.dp,
            mode
        ) ?: return emptyList()

        val balls = mutableListOf<DetectedBall>()
        var idx: Int = 0
        val count: Int = result[idx++].toInt()
        repeat(count) {
            val x = result[idx++]
            val y = result[idx++]
            val r = result[idx++]
            val conf = result[idx++]
            val isCue = result[idx++] > 0.5f
            val color = result[idx++].toInt()
            balls.add(DetectedBall(x, y, r, conf, isCue, 0, color))
        }
        LogManager.detect("Native 检测到 ${balls.size} 个球 (mode=$mode)")
        return balls
    }

    private fun detectCpu(bitmap: Bitmap, mode: Int): List<DetectedBall> {
        val preset = AppConfig.getCurrentPreset()
        val balls = mutableListOf<DetectedBall>()
        val w: Int = bitmap.width
        val h: Int = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val minR: Int = 10
        val maxR: Int = 25
        val threshold: Int = preset.vThreshold

        val step: Int = 3
        val visited = BooleanArray(w * h)

        for (sy: Int in 0 until h step step) {
            for (sx: Int in 0 until w step step) {
                val p = pixels[sy * w + sx]
                val lum = (p shr 16 and 0xFF) * 0.299f +
                        (p shr 8 and 0xFF) * 0.587f +
                        (p and 0xFF) * 0.114f

                if (lum > threshold && !visited[sy * w + sx]) {
                    val region = growRegion(pixels, w, h, sx, sy, threshold, visited)
                    val regionRadius: Int = region.second
                    if (regionRadius in minR..maxR) {
                        val sumXTotal: Int = region.first[0]
                        val sumYTotal: Int = region.first[1]
                        val areaCount: Int = region.first[2]
                        val centerX = sumXTotal.toFloat() / areaCount.toFloat()
                        val centerY = sumYTotal.toFloat() / areaCount.toFloat()
                        val isCue = lum > 250f
                        balls.add(
                            DetectedBall(
                                x = centerX,
                                y = centerY,
                                radius = regionRadius.toFloat(),
                                confidence = 0.6f,
                                isCueBall = isCue,
                                ballType = 0,
                                color = (if (isCue) 0xFFFFFF else 0xFF888888).toInt()
                            )
                        )
                    }
                }
            }
        }

        val result = balls.take(22)
        LogManager.detect("CPU 检测到 ${result.size} 个球 (mode=$mode)")
        return result
    }

    private fun growRegion(
        pixels: IntArray, w: Int, h: Int,
        sx: Int, sy: Int, threshold: Int, visited: BooleanArray
    ): Pair<IntArray, Int> {
        val queue = ArrayDeque<Int>()
        val startIndex: Int = sy * w + sx
        queue.add(startIndex)
        visited[startIndex] = true
        var sumX: Int = 0
        var sumY: Int = 0
        var count: Int = 0
        val maxCount: Int = 2000

        while (queue.isNotEmpty() && count < maxCount) {
            val idx: Int = queue.removeFirst()
            val x: Int = idx % w
            val y: Int = idx / w
            sumX += x
            sumY += y
            count += 1

            // 强制转Int，彻底消除Long类型报错
            val left = (idx - 1).toInt()
            val right = (idx + 1).toInt()
            val up = (idx - w).toInt()
            val down = (idx + w).toInt()
            val neighbors = intArrayOf(left, right, up, down)

            for (n: Int in neighbors) {
                if (n < 0 || n >= w * h) continue
                if (visited[n]) continue
                val nx: Int = n % w
                val ny: Int = n / w
                if (nx <= 0 || nx >= w - 1 || ny <= 0 || ny >= h - 1) continue
                val p = pixels[n]
                val lum = (p shr 16 and 0xFF) * 0.299f +
                        (p shr 8 and 0xFF) * 0.587f +
                        (p and 0xFF) * 0.114f
                if (lum > threshold) {
                    visited[n] = true
                    queue.add(n)
                }
            }
        }
        val areaNum: Int = count
        val radiusCalc: Int = sqrt(areaNum.toDouble() / PI).toInt()
        val radius: Int = if (radiusCalc < 5) 5 else radiusCalc
        return Pair(intArrayOf(sumX, sumY, areaNum), radius)
    }

    private external fun detectBallsNative(
        bitmap: Bitmap,
        vThreshold: Int,
        sMinDist: Int,
        pSensitivity: Int,
        dp: Float,
        mode: Int
    ): FloatArray?

    fun applyPreset(presetIndex: Int) {
        AppConfig.detectMode = presetIndex
        val p = AppConfig.getCurrentPreset()
        if (nativeAvailable) {
            setDetectParamsNative(p.vThreshold, p.sMinDist, p.pSensitivity)
        }
        LogManager.detect("🔧 切换到方案${presetIndex}: ${p.name}")
    }

    private external fun setDetectParamsNative(v: Int, s: Int, p: Int)
}
