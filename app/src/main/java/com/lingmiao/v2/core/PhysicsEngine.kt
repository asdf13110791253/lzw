package com.lingmiao.v2.core

import kotlin.math.*

/**
 * 台球物理引擎（纯 Kotlin 实现，与 C++ 版本逻辑一致）
 */
object PhysicsEngine {

    private const val BALL_RADIUS_PX = 20f
    private const val COMPENSATION_RATIO = 0.18f

    fun computeAimLine(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float,
        pocketX: Float, pocketY: Float,
        mode: String = "compensation",
        ballRadius: Float = BALL_RADIUS_PX
    ): FloatArray {
        val dx = targetX - cueX
        val dy = targetY - cueY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < ballRadius * 2) return FloatArray(0)

        val hitX = targetX - (dx / dist) * ballRadius
        val hitY = targetY - (dy / dist) * ballRadius

        val aimDx: Float
        val aimDy: Float

        if (mode == "mirror") {
            val totalDx = pocketX - cueX
            val totalDy = pocketY - cueY
            val totalDist = sqrt(totalDx * totalDx + totalDy * totalDy)
            aimDx = totalDx / totalDist
            aimDy = totalDy / totalDist
        } else {
            val baseDx = pocketX - hitX
            val baseDy = pocketY - hitY
            val baseDist = sqrt(baseDx * baseDx + baseDy * baseDy)
            val compAngle = atan2(baseDy, baseDx)
            val offsetAngle = COMPENSATION_RATIO * (PI / 180f)
            val finalAngle = compAngle + offsetAngle
            aimDx = cos(finalAngle).toFloat()
            aimDy = sin(finalAngle).toFloat()
        }

        val lineLength = 2000f
        val endX = hitX + aimDx * lineLength
        val endY = hitY + aimDy * lineLength

        return floatArrayOf(hitX, hitY, endX, endY)
    }

    fun computeBankShot(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float,
        pocketX: Float, pocketY: Float,
        tableBounds: FloatArray,
        bankCount: Int = 1
    ): FloatArray {
        val (left, top, right, bottom) = tableBounds

        val mirroredPockets = mutableListOf<Pair<Float, Float>>()
        mirroredPockets.add(Pair(pocketX, pocketY))

        if (bankCount >= 1) {
            mirroredPockets.add(Pair(pocketX, 2 * top - pocketY))
            mirroredPockets.add(Pair(pocketX, 2 * bottom - pocketY))
            mirroredPockets.add(Pair(2 * left - pocketX, pocketY))
            mirroredPockets.add(Pair(2 * right - pocketX, pocketY))
        }

        var bestPocket = mirroredPockets[0]
        var bestDist = Float.MAX_VALUE
        for (mp in mirroredPockets) {
            val d = sqrt((mp.first - targetX).pow(2) + (mp.second - targetY).pow(2))
            if (d < bestDist) {
                bestDist = d
                bestPocket = mp
            }
        }

        return computeAimLine(
            cueX, cueY, targetX, targetY,
            bestPocket.first, bestPocket.second,
            "compensation"
        )
    }

    fun isPointInTable(x: Float, y: Float, bounds: FloatArray): Boolean {
        return x >= bounds[0] && x <= bounds[2] &&
               y >= bounds[1] && y <= bounds[3]
    }

    private fun Float.pow(n: Int): Float = this.pow(n.toFloat())
}
