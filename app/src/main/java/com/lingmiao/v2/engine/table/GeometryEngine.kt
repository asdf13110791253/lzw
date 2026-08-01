package com.lingmiao.v2.engine.table

import com.lingmiao.v2.core.log.LogManager
import kotlin.math.*

/**
 * 灵喵几何引擎
 * - DLT 透视变换（梯形→矩形）
 * - 反射物理计算
 * - 自适应球桌大小
 */
class GeometryEngine {

    companion object {
        private const val TAG = "Geometry"
        const val STD_TABLE_W = 1270f
        const val STD_TABLE_H = 635f
        const val BALL_R = 28.6f
        const val POCKET_R = 55f
    }

    // 四角校准点（屏幕坐标）
    data class Corners(
        var tlx: Float, var tly: Float,
        var trx: Float, var try_: Float,
        var blx: Float, var bly: Float,
        var brx: Float, var bry: Float
    )

    private var corners: Corners? = null
    private var screenW = 0f
    private var screenH = 0f

    // 3x3 单应性矩阵
    private var hMatrix: FloatArray? = null
    private var hInvMatrix: FloatArray? = null

    // 自适应缩放
    private var scaleX = 1f
    private var scaleY = 1f

    fun setCalibration(c: Corners, sw: Float, sh: Float) {
        corners = c
        screenW = sw
        screenH = sh

        // 计算单应性矩阵
        val src = floatArrayOf(
            c.tlx, c.tly,
            c.trx, c.try_,
            c.brx, c.bry,
            c.blx, c.bly
        )
        val dst = floatArrayOf(
            0f, 0f,
            STD_TABLE_W, 0f,
            STD_TABLE_W, STD_TABLE_H,
            0f, STD_TABLE_H
        )
        hMatrix = computeDLT(src, dst)
        hInvMatrix = hMatrix?.let { invert3x3(it) }

        // 自适应缩放
        val detectedW = abs(c.trx - c.tlx).coerceAtLeast(abs(c.brx - c.blx))
        val detectedH = abs(c.bly - c.tly).coerceAtLeast(abs(c.bry - c.try_))
        scaleX = STD_TABLE_W / detectedW.coerceAtLeast(1f)
        scaleY = STD_TABLE_H / detectedH.coerceAtLeast(1f)

        LogManager.geo("📐 校准完成: TL(${c.tlx},${c.tly}) TR(${c.trx},${c.try_}) " +
                      "BL(${c.blx},${c.bly}) BR(${c.brx},${c.bry})")
        LogManager.geo("   缩放: ${"%.3f".format(scaleX)}x${"%.3f".format(scaleY)}")
    }

    fun isCalibrated(): Boolean = hMatrix != null

    // ── 坐标变换 ──

    fun screenToWorld(sx: Float, sy: Float): FloatArray? {
        val m = hInvMatrix ?: return null
        val w = m[6] * sx + m[7] * sy + m[8]
        if (abs(w) < 0.0001f) return null
        val wx = (m[0] * sx + m[1] * sy + m[2]) / w
        val wy = (m[3] * sx + m[4] * sy + m[5]) / w
        return floatArrayOf(wx, wy)
    }

    fun worldToScreen(wx: Float, wy: Float): FloatArray? {
        val m = hMatrix ?: return null
        val w = m[6] * wx + m[7] * wy + m[8]
        if (abs(w) < 0.0001f) return null
        val sx = (m[0] * wx + m[1] * wy + m[2]) / w
        val sy = (m[3] * wx + m[4] * wy + m[5]) / w
        return floatArrayOf(sx, sy)
    }

    fun getPocketsScreen(): Array<FloatArray> {
        val pockets = arrayOf(
            floatArrayOf(0f, 0f),
            floatArrayOf(STD_TABLE_W / 2f, -POCKET_R * 0.3f),
            floatArrayOf(STD_TABLE_W, 0f),
            floatArrayOf(0f, STD_TABLE_H),
            floatArrayOf(STD_TABLE_W / 2f, STD_TABLE_H + POCKET_R * 0.3f),
            floatArrayOf(STD_TABLE_W, STD_TABLE_H)
        )
        return pockets.map { worldToScreen(it[0], it[1]) ?: it }.toTypedArray()
    }

    // ── 反射计算 ──

    fun reflect(px: Float, py: Float, dx: Float, dy: Float, wall: Int, compRatio: Float): FloatArray {
        val mode = com.lingmiao.v2.core.config.AppConfig.reflectionMode
        val ndx: Float
        val ndy: Float

        if (mode == "compensation") {
            val angle = atan2(dy, dx)
            val factor = 1.0 - compRatio * 0.15
            val newAngle = when (wall) {
                0, 1 -> if (wall == 0) PI - angle * factor else -angle * factor
                else -> PI / 2 - (angle - PI / 2) * factor
            }
            ndx = cos(newAngle).toFloat()
            ndy = sin(newAngle).toFloat()
        } else {
            // 镜像反射
            when (wall) {
                0, 1 -> { ndx = dx; ndy = -dy }  // 水平壁
                else  -> { ndx = -dx; ndy = dy }  // 垂直壁
            }
        }

        return floatArrayOf(px + ndx * 10f, py + ndy * 10f, ndx, ndy)
    }

    fun reflectPoint(x: Float, y: Float, wall: Int): FloatArray {
        return when (wall) {
            0 -> floatArrayOf(x, -y)        // 上壁
            1 -> floatArrayOf(x, 2 * STD_TABLE_H - y) // 下壁
            2 -> floatArrayOf(-x, y)        // 左壁
            3 -> floatArrayOf(2 * STD_TABLE_W - x, y) // 右壁
            else -> floatArrayOf(x, y)
        }
    }

    // ── 工具方法 ──

    fun isInsideTable(x: Float, y: Float): Boolean =
        x >= 0 && x <= STD_TABLE_W && y >= 0 && y <= STD_TABLE_H

    fun adaptToTableSize(detectedW: Float, detectedH: Float) {
        scaleX = STD_TABLE_W / detectedW.coerceAtLeast(1f)
        scaleY = STD_TABLE_H / detectedH.coerceAtLeast(1f)
        LogManager.geo("自适应: 检测=${"%.0f".format(detectedW)}x${"%.0f".format(detectedH)} → " +
                      "标准=${"%.0f".format(STD_TABLE_W)}x${"%.0f".format(STD_TABLE_H)}")
    }

    fun getScale(): Pair<Float, Float> = Pair(scaleX, scaleY)

    fun getCorners(): Corners? = corners

    // ── DLT 算法（直接线性变换）──

    private fun computeDLT(src: FloatArray, dst: FloatArray): FloatArray {
        // 8x9 矩阵 A
        val A = Array(8) { FloatArray(9) }
        for (i in 0 until 4) {
            val x = src[i * 2]; val y = src[i * 2 + 1]
            val u = dst[i * 2]; val v = dst[i * 2 + 1]
            A[i * 2]     = floatArrayOf(x, y, 1f, 0f, 0f, 0f, -u * x, -u * y, -u)
            A[i * 2 + 1] = floatArrayOf(0f, 0f, 0f, x, y, 1f, -v * x, -v * y, -v)
        }

        // A^T * A → 9x9
        val ATA = Array(9) { FloatArray(9) }
        for (i in 0 until 9) {
            for (j in 0 until 9) {
                var sum = 0f
                for (k in 0 until 8) sum += A[k][i] * A[k][j]
                ATA[i][j] = sum
            }
        }

        // 高斯消元
        gaussianElimination(ATA)

        // 归一化
        var norm = 0f
        for (i in 0 until 9) norm += ATA[i][8] * ATA[i][8]
        norm = sqrt(norm)
        val h = FloatArray(9)
        if (norm > 0.0001f) {
            for (i in 0 until 9) h[i] = ATA[i][8] / norm
        }
        return h
    }

    private fun gaussianElimination(M: Array<FloatArray>) {
        val n = 9
        for (k in 0 until n) {
            var maxRow = k
            var maxVal = abs(M[k][k])
            for (i in k + 1 until n) {
                if (abs(M[i][k]) > maxVal) { maxVal = abs(M[i][k]); maxRow = i }
            }
            if (maxRow != k) { val tmp = M[k]; M[k] = M[maxRow]; M[maxRow] = tmp }
            if (abs(M[k][k]) < 1e-10f) continue
            for (i in k + 1 until n) {
                val f = M[i][k] / M[k][k]
                for (j in k until n + 1) M[i][j] -= f * M[k][j]
            }
        }
        // 回代
        for (i in n - 1 downTo 0) {
            var sum = 0f
            for (j in i + 1 until n) sum += M[i][j] * M[j][n]
            if (abs(M[i][i]) > 1e-10f) M[i][n] = (M[i][n] - sum) / M[i][i]
   
