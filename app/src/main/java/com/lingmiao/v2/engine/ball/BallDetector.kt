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

    const val MODE_HAAR = 0
    const val MODE_HSV = 1
    const val MODE_EDGE = 2
    const val MODE_TFLITE = 3
    const val MODE_FUSION = 4

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
        var idx = 0
        val count = result[idx++].toInt()
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
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        val minR = 10
        val maxR = 25
        val threshold = preset.vThreshold

        val step = 3
        val visited = BooleanArray(w * h)

        for (sy in 0 until h step step) {
            for (sx in 0 until w step step) {
                val p = pixels[sy * w + sx]
                val lum = (p shr 16 and 0xFF) * 0.299f +
                        (p shr 8 and 0xFF) * 0.587f +
                        (p and 0xFF) * 0.114f

                if (lum > threshold && !visited[sy * w + sx]) {
                    val region = growRegion(pixels, w, h, sx, sy, threshold, visited)
                    if (region.second in minR..maxR) {
                        val isCue = lum > 250f
                        balls.add(
                            DetectedBall(
                                x = region.first[0] / region.second.toFloat(),
                                y = region.first[1] / region.second.toFloat(),
                                radius = region.second.toFloat(),
                                confidence = 0.6f,
                                isCueBall = isCue,
                                ballType = 0,
                                color = if (isCue) 0xFFFFFF else 0xFF888888
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
        queue.add(sy * w + sx)
        visited[sy * w + sx] = true
        var sumX = 0; var sumY = 0; var count = 0
        val maxCount = 2000

        while (queue.isNotEmpty() && count < maxCount) {
            val idx = queue.removeFirst()
            val x = idx % w
            val y = idx / w
            sumX += x; sumY += y; count++

            val neighbors = arrayOf(
                idx - 1, idx + 1, idx - w, idx + w
            )
            for (n in neighbors) {
                if (n < 0 || n >= w * h) continue
                if (visited[n]) continue
                val nx = n % w; val ny = n / w
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
        // 拆分计算，彻底消除Long类型推断报错
        val countDouble = count.toDouble()
        val divideRes = countDouble / PI
        val radius: Int = sqrt(divideRes).toInt().coerceAtLeast(5)
        return Pair(intArrayOf(sumX, sumY), radius)
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
