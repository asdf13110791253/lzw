package com.lingmiao.v2.core

import android.graphics.Bitmap

/**
 * 球体检测器 —— JNI 桥接层
 *
 * 对应 C++ 中的 native 方法：
 * - detectBalls():  HSV 分割 + 霍夫圆变换，返回所有球的圆心坐标
 * - computeAimLine(): 根据球的位置 + 袋口位置，计算辅助线
 *
 * 通用策略（不依赖任何特定游戏UI）：
 * 1. 降分辨率到 640x360 再处理（性能提升 3x）
 * 2. HSV 阈值找白球（V>200, S<30）
 * 3. 霍夫圆变换找所有圆形物体
 * 4. 按颜色/亮度分类：白球 / 目标球 / 袋口
 * 5. 镜像点法计算击球路径
 */
object BallDetector {

    // ===== JNI 方法声明 =====

    /**
     * 检测屏幕中所有球体
     * @param bitmap 屏幕截图（RGBA_8888）
     * @param vThresh V通道阈值（亮度）
     * @param sThresh S通道阈值（饱和度）
     * @param pThresh 霍夫圆 param2 阈值（越小检测越多）
     * @return FloatArray: [x0,y0,r0, x1,y1,r1, ...] 白球在前，其余按大小排序
     */
    external fun nativeDetectBalls(
        bitmap: Bitmap,
        vThresh: Int,
        sThresh: Int,
        pThresh: Int
    ): FloatArray?

    /**
     * 计算辅助线坐标
     * @param balls nativeDetectBalls 的返回值
     * @param mode "mirror"=镜像反射 / "compensation"=角度补偿
     * @param bankCount 翻袋库数（反射次数）
     * @return FloatArray: [x0,y0, x1,y1, x2,y2, ...] 连线点序列
     */
    external fun nativeComputeAimLine(
        balls: FloatArray,
        mode: String,
        bankCount: Int
    ): FloatArray?

    /**
     * 透视校正：把屏幕坐标映射到标准球桌坐标
     * @param screenPoints 屏幕上的四个角点
     * @param tableWidth 标准球桌宽度（mm 或任意单位）
     * @param tableHeight 标准球桌高度
     * @return 3x3 透视变换矩阵（行优先）
     */
    external fun nativeComputePerspectiveMatrix(
        screenPoints: FloatArray,
        tableWidth: Float,
        tableHeight: Float
    ): FloatArray?

    // ===== Kotlin 封装 =====

    fun detect(bitmap: Bitmap): DetectionResult {
        val raw = nativeDetectBalls(
            bitmap,
            AppConfig.hsvV,
            AppConfig.hsvS,
            AppConfig.hsvP
        ) ?: return DetectionResult.EMPTY

        // 解析：[白球x,y,r, 目标球1x,y,r, 目标球2x,y,r, ...]
        val balls = mutableListOf<Ball>()
        for (i in raw.indices step 3) {
            balls.add(Ball(raw[i], raw[i + 1], raw[i + 2]))
        }

        // 第一个是白球（C++ 保证）
        val cueBall = balls.firstOrNull()
        val targetBalls = balls.drop(1)

        return DetectionResult(cueBall, targetBalls, raw)
    }

    fun computeAimLine(detection: DetectionResult): AimLine? {
        if (detection.raw.isEmpty()) return null
        val points = nativeComputeAimLine(
            detection.raw,
            AppConfig.reflectionMode,
            AppConfig.bankCount
        ) ?: return null

        val lines = mutableListOf<LineSegment>()
        for (i in 0 until points.size - 2 step 2) {
            lines.add(
                LineSegment(
                    PointF(points[i], points[i + 1]),
                    PointF(points[i + 2], points[i + 3])
                )
            )
        }
        return AimLine(lines, points)
    }

    // ===== 数据类 =====

    data class Ball(val x: Float, val y: Float, val radius: Float)
    data class PointF(val x: Float, val y: Float)
    data class LineSegment(val start: PointF, val end: PointF)

    data class DetectionResult(
        val cueBall: Ball?,
        val targetBalls: List<Ball>,
        val raw: FloatArray
    ) {
        companion object {
            val EMPTY = DetectionResult(null, emptyList(), FloatArray(0))
        }
    }

    data class AimLine(
        val segments: List<LineSegment>,
        val rawPoints: FloatArray
    )
}
