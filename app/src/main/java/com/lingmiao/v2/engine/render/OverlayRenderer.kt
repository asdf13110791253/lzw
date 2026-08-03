package com.lingmiao.v2.engine.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import com.lingmiao.v2.engine.table.GeometryEngine

class OverlayRenderer(private val geometry: GeometryEngine) {

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private var currentAimPath: List<PointF>? = null

    fun updateAimPath(path: List<PointF>?) {
        currentAimPath = path
    }

    fun render(canvas: Canvas, aimPath: List<PointF>? = null) {
        val path = aimPath ?: currentAimPath ?: return
        if (path.isEmpty()) return

        paint.color = android.graphics.Color.YELLOW
        for (i in 0 until path.size - 1) {
            canvas.drawLine(
                path[i].x, path[i].y,
                path[i + 1].x, path[i + 1].y,
                paint
            )
        }

        paint.color = android.graphics.Color.WHITE
        path.forEach { point ->
            canvas.drawCircle(point.x, point.y, 10f, paint)
        }
    }
}
