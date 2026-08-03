package com.lingmiao.v2.engine.render

import android.graphics.Canvas
import android.graphics.Paint
import com.lingmiao.v2.engine.aim.AimEngine
import com.lingmiao.v2.engine.table.GeometryEngine

class OverlayRenderer(private val geometry: GeometryEngine) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    fun render(canvas: Canvas, aimPath: List<PointF>?) {
        if (aimPath.isNullOrEmpty()) return

        // 绘制瞄准线
        paint.color = android.graphics.Color.YELLOW
        for (i in 0 until aimPath.size - 1) {
            canvas.drawLine(
                aimPath[i].x, aimPath[i].y,
                aimPath[i + 1].x, aimPath[i + 1].y,
                paint
            )
        }

        // 绘制球位
        paint.color = android.graphics.Color.WHITE
        aimPath.forEach { point ->
            canvas.drawCircle(point.x, point.y, 10f, paint)
        }
    }
}
