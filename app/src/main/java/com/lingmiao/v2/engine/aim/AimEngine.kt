package com.lingmiao.v2.engine.aim

import android.graphics.PointF
import com.lingmiao.v2.engine.table.GeometryEngine
import kotlin.math.sqrt

object AimEngine {

    private const val MAX_BOUNCES = 3

    fun calculateShot(
        cueBall: PointF,
        targetBall: PointF,
        pocket: PointF,
        geometry: GeometryEngine
    ): List<PointF>? {
        // 简易逻辑：直接瞄准入袋点（真实物理需反射计算）
        val path = mutableListOf<PointF>()
        path.add(cueBall)
        
        // 检查路径是否被阻挡
        if (!isPathBlocked(cueBall, targetBall, geometry)) {
            path.add(targetBall)
            path.add(pocket)
            return path
        }
        
        // 尝试一库反弹
        val reflectionPoint = geometry.calculateReflection(cueBall, targetBall, pocket)
        if (reflectionPoint != null && !isPathBlocked(cueBall, reflectionPoint, geometry)) {
            path.add(reflectionPoint)
            path.add(targetBall)
            path.add(pocket)
            return path
        }
        
        return null
    }

    fun isPathBlocked(start: PointF, end: PointF, geometry: GeometryEngine): Boolean {
        // 简易射线检测
        val dx = end.x - start.x
        val dy = end.y - start.y
        val distance = sqrt(dx * dx + dy * dy)
        
        // 步进检查中间点
        val steps = (distance / 5).toInt()
        for (i in 1 until steps) {
            val x = start.x + dx * i / steps
            val y = start.y + dy * i / steps
            if (geometry.isPointOnCushion(x, y)) {
                return true
            }
        }
        return false
    }

    fun raycastTable(origin: PointF, direction: PointF, geometry: GeometryEngine): PointF? {
        // 简易光线投射，返回与库边的交点
        return geometry.intersectWithCushion(origin, direction)
    }
}
