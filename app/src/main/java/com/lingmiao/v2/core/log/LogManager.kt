package com.lingmiao.v2.core

import kotlin.math.*

/**
 * 台球物理引擎（纯 Kotlin 实现，与 C++ 版本逻辑一致）
 *
 * 核心算法：
 *   1. 镜像点法（Mirror Image Method）
 *      - 以目标球为圆心，球半径画圆
 *      - 白球方向线与该圆的交点 = 碰撞点
 *      - 镜像点 = 碰撞点关于目标球中心的对称点
 *      - 辅助线 = 白球 → 碰撞点 → 镜像点方向延长到袋口
 *
 *   2. 角度补偿模式（Angle Compensation）
 *      - 真实台球碰撞有能量损耗 + 自旋效应
 *      - 补偿比例 0.18（经验值，可调）
 *
 *   3. 翻袋计算（Bank Shot）
 *      - 用库边镜像法计算反弹路径
 */
object PhysicsEngine {

    private const val BALL_RADIUS_PX = 20f // 默认球半径（像素），实际由 C++ 检测得到
    private const val COMPENSATION_RATIO = 0.18f // 角度补偿比例

    // ===== 公开 API =====

    /**
     * 计算瞄准线（主接口，供 C++ 或 Kotlin 调用）
     *
     * @param cueX, cueY 白球中心坐标
     * @param targetX, targetY 目标球中心坐标
     * @param pocketX, pocketY 袋口坐标
     * @param mode "mirror" 或 "compensation"
     * @param ballRadius 球半径（像素）
     * @return FloatArray: [碰撞点X, 碰撞点Y, 延长线终点X, 延长线终点Y]
     */
    fun computeAimLine(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float,
        pocketX: Float, pocketY: Float,
        mode: String = "compensation",
        ballRadius: Float = BALL_RADIUS_PX
    ): FloatArray {
        // Step1: 计算碰撞点（白球→目标球方向，距离目标球中心 ballRadius 处）
        val dx = targetX - cueX
        val dy = targetY - cueY
        val dist = sqrt(dx * dx + dy * dy)

        if (dist < ballRadius * 2) {
            // 两球已经重叠，无法计算
            return FloatArray(0)
        }

        // 碰撞点 = 目标球中心 - (白球→目标球方向) * ballRadius
        val hitX = targetX - (dx / dist) * ballRadius
        val hitY = targetY - (dy / dist) * ballRadius

        // Step2: 计算延长线方向
        val aimDx: Float
        val aimDy: Float

        if (mode == "mirror") {
            // 镜像反射：入射角 = 出射角
            // 简化模型：白球→碰撞点→袋口 三点一线
            val totalDx = pocketX - cueX
            val totalDy = pocketY - cueY
            val totalDist = sqrt(totalDx * totalDx + totalDy * totalDy)
            aimDx = totalDx / totalDist
            aimDy = totalDy / totalDist
        } else {
            // 角度补偿模式
            // 基础方向：碰撞点→袋口
            val baseDx = pocketX - hitX
            val baseDy = pocketY - hitY
            val baseDist = sqrt(baseDx * baseDx + baseDy * baseDy)

            // 补偿：向"白球侧"偏转一点
            val compAngle = atan2(baseDy, baseDx)
            val offsetAngle = COMPENSATION_RATIO * (PI / 180f) // 转弧度
            val finalAngle = compAngle + offsetAngle

            aimDx = cos(finalAngle).toFloat()
            aimDy = sin(finalAngle).toFloat()
        }

        // Step3: 延长线终点（屏幕外 2000px）
        val lineLength = 2000f
        val endX = hitX + aimDx * lineLength
        val endY = hitY + aimDy * lineLength

        return floatArrayOf(hitX, hitY, endX, endY)
    }

    /**
     * 计算翻袋路径（库边反弹）
     *
     * @param cueX, cueY 白球
     * @param targetX, targetY 目标球
     * @param pocketX, pocketY 袋口
     * @param tableBounds 球桌边界 [left, top, right, bottom]
     * @param bankCount 反弹次数
     * @return FloatArray: 多点序列 [x0,y0, x1,y1, x2,y2, ...]
     */
    fun computeBankShot(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float,
        pocketX: Float, pocketY: Float,
        tableBounds: FloatArray,
        bankCount: Int = 1
    ): FloatArray {
        val (left, top, right, bottom) = tableBounds

        // 对袋口做镜像（关于库边）
        val mirroredPockets = mutableListOf<Pair<Float, Float>>()
        mirroredPockets.add(Pair(pocketX, pocketY)) // 原始袋口

        if (bankCount >= 1) {
            // 关于上下库边镜像
            mirroredPockets.add(Pair(pocketX, 2 * top - pocketY))
            mirroredPockets.add(Pair(pocketX, 2 * bottom - pocketY))
            // 关于左右库边镜像
            mirroredPockets.add(Pair(2 * left - pocketX, pocketY))
            mirroredPockets.add(Pair(2 * right - pocketX, pocketY))
        }

        // 选最近的镜像袋口
        var bestPocket = mirroredPockets[0]
        var bestDist = Float.MAX_VALUE
        for (mp in mirroredPockets) {
            val d = sqrt((mp.first - targetX).pow(2) + (mp.second - targetY).pow(2))
            if (d < bestDist) {
                bestDist = d
                bestPocket = mp
            }
        }

        // 用最佳镜像袋口计算瞄准线
        val result = computeAimLine(
            cueX, cueY, targetX, targetY,
            bestPocket.first, bestPocket.second,
            "compensation"
        )

        return result
    }

    /**
     * 判断点是否在球桌内（透视校正后）
     */
    fun isPointInTable(x: Float, y: Float, bounds: FloatArray): Boolean {
        return x >= bounds[0] && x <= bounds[2] &&
               y >= bounds[1] && y <= bounds[3]
    }

    // ===== 辅助函数 =====
    private fun Float.pow(n: Int): Float = this.pow(n.toFloat())
}
        val dir = File(logDir)
        if (!dir.exists()) dir.mkdirs()

        rotateLogs(dir)

        val today = fileDateFormat.format(Date())
        logFile = File(dir, "lingmiao_${today}.log")

        try {
            writer = FileWriter(logFile, true)
            fileLogging = true
            i("Logger", "📝 日志系统初始化完成")
        } catch (e: IOException) {
            writer = null
            fileLogging = false
        }
    }

    fun setMinLevel(level: Int) { minLevel = level }

    // ── 公共接口 ──
    fun v(tag: String, msg: String) = log(VERBOSE, tag, msg)
    fun d(tag: String, msg: String) = log(DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(INFO, tag, msg)
    fun w(tag: String, msg: String) = log(WARN, tag, msg)
    fun e(tag: String, msg: String) = log(ERROR, tag, msg)
    fun e(tag: String, msg: String, t: Throwable?) {
        log(ERROR, tag, msg)
        if (t != null) log(ERROR, tag, getStackTrace(t))
    }

    // ── 快速标签 ──
    fun aim(msg: String) = i("AimEngine", msg)
    fun detect(msg: String) = i("BallDetect", msg)
    fun geo(msg: String) = i("Geometry", msg)
    fun render(msg: String) = i("Renderer", msg)
    fun native(msg: String) = i("Native", msg)
    fun service(msg: String) = i("Service", msg)
    fun config(msg: String) = i("Config", msg)

    // ── 测试/桌面模式（无 Android）──
    fun v(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("V/$TAG_PREFIX-$tag: $msg") else v(tag, msg)
    }
    fun d(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("D/$TAG_PREFIX-$tag: $msg") else d(tag, msg)
    }
    fun i(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("I/$TAG_PREFIX-$tag: $msg") else i(tag, msg)
    }
    fun w(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("W/$TAG_PREFIX-$tag: $msg") else w(tag, msg)
    }
    fun e(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("E/$TAG_PREFIX-$tag: $msg") else e(tag, msg)
    }

    private fun log(level: Int, tag: String, msg: String) {
        if (level < minLevel) return

        val fullTag = "$TAG_PREFIX/$tag"
        val line = "${dateFormat.format(Date())} [${levelChar(level)}] $fullTag: $msg\n"

        when (level) {
            VERBOSE -> Log.v(fullTag, msg)
            DEBUG -> Log.d(fullTag, msg)
            INFO -> Log.i(fullTag, msg)
            WARN -> Log.w(fullTag, msg)
            ERROR -> Log.e(fullTag, msg)
        }

        if (fileLogging && writer != null) {
            try {
                writer?.write(line)
                writer?.flush()
                checkRotate()
            } catch (_: IOException) {}
        }
    }

    private fun levelChar(level: Int): Char = when (level) {
        VERBOSE -> 'V'; DEBUG -> 'D'; INFO -> 'I'
        WARN -> 'W'; ERROR -> 'E'; else -> '?'
    }

    private fun getStackTrace(t: Throwable): String {
        val sb = StringBuilder(t.toString() + "\n")
        t.stackTrace.forEach { sb.append("    at $it\n") }
        return sb.toString()
    }

    private fun checkRotate() {
        val f = logFile ?: return
        if (!f.exists() || f.length() <= MAX_LOG_SIZE) return

        try { writer?.close() } catch (_: IOException) {}
        writer = null

        val dir = f.parentFile ?: return
        val files = dir.listFiles { _, name -> name.startsWith("lingmiao_") && name.endsWith(".log") }
        files?.let {
            it.sortBy { f2 -> f2.lastModified() }
            if (it.size >= MAX_LOG_FILES) {
                for (i in 0..it.size - MAX_LOG_FILES) it[i].delete()
            }
        }

        val today = fileDateFormat.format(Date())
        logFile = File(dir, "lingmiao_${today}_${System.currentTimeMillis()}.log")
        try {
            writer = FileWriter(logFile, true)
        } catch (_: IOException) {
            writer = null
            fileLogging = false
        }
    }

    private fun rotateLogs(dir: File) {
        val files = dir.listFiles { _, name -> name.startsWith("lingmiao_") && name.endsWith(".log") }
        files?.let {
            it.sortBy { f2 -> f2.lastModified() }
            if (it.size >= MAX_LOG_FILES) {
                for (i in 0..it.size - MAX_LOG_FILES) it[i].delete()
            }
        }
    }

    fun shutdown() {
        if (writer != null) {
            try { writer?.flush(); writer?.close() } catch (_: IOException) {}
            writer = null
        }
        fileLogging = false
    }
}
