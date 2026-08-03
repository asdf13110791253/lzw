package com.lingmiao.v2.engine.table

import android.graphics.PointF
import kotlin.math.sqrt

class GeometryEngine {

    // 模拟台球桌边界 (单位：像素)
    private val tableLeft = 100f
    private val tableTop = 200f
    private val tableRight = 980f
    private val tableBottom = 1540f

    // ---------- 新增 ----------
    data class Corners(
        val tlx: Float, val tly: Float,
        val trx: Float, val try_: Float,
        val blx: Float, val bly: Float,
        val brx: Float, val bry: Float
    )

    fun setCalibration(corners: Corners, screenW: Float, screenH: Float) {
        // 此处可存储校准数据，暂为空实现
    }
    // -------------------------

    fun calculateReflection(start: PointF, middle: PointF, end: PointF): PointF? {
        val dx = end.x - middle.x
        val dy = end.y - middle.y
        val angle = Math.atan2(dy.toDouble(), dx.toDouble())
        val reflectionX = middle.x + Math.cos(angle).toFloat() * 100
        val reflectionY = middle.y - Math.sin(angle).toFloat() * 100
        return PointF(reflectionX, reflectionY)
    }

    fun intersectWithCushion(origin: PointF, direction: PointF): PointF? {
        var closest: PointF? = null
        var minDist = Float.MAX_VALUE

        val edges = listOf(
            Pair(PointF(tableLeft, tableTop), PointF(tableRight, tableTop)),
            Pair(PointF(tableLeft, tableBottom), PointF(tableRight, tableBottom)),
            Pair(PointF(tableLeft, tableTop), PointF(tableLeft, tableBottom)),
            Pair(PointF(tableRight, tableTop), PointF(tableRight, tableBottom))
        )

        for (edge in edges) {
            val intersection = lineIntersection(origin, direction, edge.first, edge.second)
            if (intersection != null) {
                val dist = distance(origin, intersection)
                if (dist < minDist) {
                    minDist = dist
                    closest = intersection
                }
            }
        }
        return closest
    }

    fun isPointOnCushion(x: Float, y: Float): Boolean {
        return x <= tableLeft || x >= tableRight || y <= tableTop || y >= tableBottom
    }

    private fun lineIntersection(p1: PointF, p2: PointF, q1: PointF, q2: PointF): PointF? {
        val d1x = p2.x - p1.x
        val d1y = p2.y - p1.y
        val d2x = q2.x - q1.x
        val d2y = q2.y - q1.y
        val cross = d1x * d2y - d1y * d2x
        if (Math.abs(cross) < 1e-6) return null
        val t1 = ((q1.x - p1.x) * d2y - (q1.y - p1.y) * d2x) / cross
        val t2 = ((q1.x - p1.x) * d1y - (q1.y - p1.y) * d1x) / cross
        if (t1 in 0f..1f && t2 in 0f..1f) {
            return PointF(p1.x + t1 * d1x, p1.y + t1 * d1y)
        }
        return null
    }

    private fun distance(a: PointF, b: PointF): Float {
        return sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))
    }

    fun invert3x3(matrix: FloatArray): FloatArray? {
        if (matrix.size != 9) return null
        val a = matrix
        val det = a[0] * (a[4]*a[8] - a[5]*a[7]) -
                  a[1] * (a[3]*a[8] - a[5]*a[6]) +
                  a[2] * (a[3]*a[7] - a[4]*a[6])
        if (Math.abs(det) < 1e-6) return null
        val invDet = 1f / det
        return floatArrayOf(
            (a[4]*a[8] - a[5]*a[7]) * invDet,
            (a[2]*a[7] - a[1]*a[8]) * invDet,
            (a[1]*a[5] - a[2]*a[4]) * invDet,
            (a[5]*a[6] - a[3]*a[8]) * invDet,
            (a[0]*a[8] - a[2]*a[6]) * invDet,
            (a[2]*a[3] - a[0]*a[5]) * invDet,
            (a[3]*a[7] - a[4]*a[6]) * invDet,
            (a[1]*a[6] - a[0]*a[7]) * invDet,
            (a[0]*a[4] - a[1]*a[3]) * invDet
        )
    }
}
