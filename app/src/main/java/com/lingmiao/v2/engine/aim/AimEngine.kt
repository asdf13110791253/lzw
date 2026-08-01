package com.lingmiao.v2.engine.aim

import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.core.log.LogManager
import kotlin.math.*

/**
 * 灵喵瞄准引擎
 * - 镜像算法（入射角=反射角）
 * - 角度补偿模式（模拟真实反弹）
 * - BFS 多库翻袋（1-5 库）
 */
object AimEngine {

    private const val TAG = "AimEngine"

    // 球桌标准尺寸 (mm)
    const val TABLE_W = 1270f
    const val TABLE_H = 635f
    const val BALL_R = 28.6f
    const val POCKET_R = 55f

    // 6 个袋口
    val POCKETS = arrayOf(
        floatArrayOf(0f, 0f),                    // 左上
        floatArrayOf(TABLE_W / 2f, -POCKET_R * 0.3f), // 上中
        floatArrayOf(TABLE_W, 0f),               // 右上
        floatArrayOf(0f, TABLE_H),               // 左下
        floatArrayOf(TABLE_W / 2f, TABLE_H + POCKET_R * 0.3f), // 下中
        floatArrayOf(TABLE_W, TABLE_H)           // 右下
    )

    data class AimResult(
        val cueBall: FloatArray,       // 母球位置
        val targetBall: FloatArray,    // 目标球位置
        val pocket: FloatArray,        // 目标袋口
        val aimPath: FloatArray,      // 瞄准路径点
        val banks: Int,               // 翻袋库数
        val angle: Float,             // 瞄准角度（度）
        val power: Float,             // 建议力度 (0-1)
        val isDirect: Boolean,        // 是否直球
        val reflectionPoints: List<FloatArray> // 反射点
    )

    /**
     * 计算最佳瞄准方案
     */
    fun calculate(
        cueBall: FloatArray,
        balls: List<FloatArray>,
        pocketOverride: FloatArray? = null
    ): AimResult? {
        if (balls.isEmpty()) return null

        val target = balls.firstOrNull { it.size > 3 && it[3] < 0.5f } ?: balls[0]
        val pockets = pocketOverride?.let { arrayOf(it) } ?: POCKETS

        // 对每个袋口计算得分，选最优
        var best: AimResult? = null
        var bestScore = Float.MAX_VALUE

        for (pocket in pockets) {
            // 1. 尝试直球
            val direct = calcDirect(cueBall, target, pocket)
            if (direct != null && direct.isDirect) {
                val score = direct.angle * 0.3f + direct.banks * 10f
                if (score < bestScore) { bestScore = score; best = direct }
                continue
            }

            // 2. 尝试翻袋 (1-5 库)
            for (banks in 1..AppConfig.maxBanks) {
                val result = calcBank(cueBall, target, pocket, banks)
                if (result != null) {
                    val score = result.angle * 0.3f + result.banks * 10f
                    if (score < bestScore) { bestScore = score; best = result }
                }
            }
        }

        if (best != null) {
            LogManager.aim("🎯 最优方案: ${if (best.isDirect) "直球" else "${best.banks}库翻袋"} " +
                "角度=${"%.1f".format(best.angle)}° 力度=${"%.0f".format(best.power * 100)}%")
        }
        return best
    }

    /**
     * 直球计算（镜像法）
     */
    private fun calcDirect(cue: FloatArray, target: FloatArray, pocket: FloatArray): AimResult? {
        // 目标球到袋口方向
        val dx = pocket[0] - target[0]
        val dy = pocket[1] - target[1]
        val len = sqrt(dx * dx + dy * dy)
        if (len < 0.001f) return null

        val ux = dx / len
        val uy = dy / len

        // 母球应在目标球背向袋口方向 2*R 处
        val idealCueX = target[0] - ux * BALL_R * 2f
        val idealCueY = target[1] - uy * BALL_R * 2f

        // 检查直线是否被其他球阻挡
        if (isPathBlocked(target, pocket, emptyList())) {
            return null // 被挡，不算直球
        }

        val angle = atan2(dy, dx) * 180f / PI.toFloat()
        val power = (len / TABLE_W).coerceIn(0.2f, 1f)

        val path = floatArrayOf(idealCueX, idealCueY, target[0], target[1], pocket[0], pocket[1])

        return AimResult(
            cueBall = cue,
            targetBall = target,
            pocket = pocket,
            aimPath = path,
            banks = 0,
            angle = angle,
            power = power,
            isDirect = true,
            reflectionPoints = emptyList()
        )
    }

    /**
     * 翻袋计算（BFS 搜索反射路径）
     */
    private fun calcBank(
        cue: FloatArray,
        target: FloatArray,
        pocket: FloatArray,
        banks: Int
    ): AimResult? {
        // 生成镜像袋口
        val mirrorPockets = generateMirrorPockets(pocket, banks)

        var bestResult: AimResult? = null
        var bestDist = Float.MAX_VALUE

        for (mp in mirrorPockets) {
            // 从母球到镜像袋口的虚拟直线
            val dx = mp[0] - cue[0]
            val dy = mp[1] - cue[1]
            val len = sqrt(dx * dx + dy * dy)
            if (len < 0.001f) continue

            // 计算反射点
            val refPoints = mutableListOf<FloatArray>()
            var cx = cue[0]
            var cy = cue[1]
            var dirX = dx / len
            var dirY = dy / len

            for (b in 0 until banks) {
                val hit = raycastTable(cx, cy, dirX, dirY)
                if (hit == null) break
                refPoints.add(floatArrayOf(hit[0], hit[1]))
                // 反射方向
                when (hit[2].toInt()) {
                    0 -> dirY = -dirY  // 上壁
                    1 -> dirY = -dirY  // 下壁
                    2 -> dirX = -dirX  // 左壁
                    3 -> dirX = -dirX  // 右壁
                }
                cx = hit[0] + dirX * 5f
                cy = hit[1] + dirY * 5f
            }

            if (refPoints.size == banks) {
                val dist = abs(len - TABLE_W * banks)
                if (dist < bestDist) {
                    bestDist = dist
                    val path = FloatArray((refPoints.size + 2) * 2)
                    path[0] = cue[0]; path[1] = cue[1]
                    refPoints.forEachIndexed { i, p ->
                        path[(i + 1) * 2] = p[0]
                        path[(i + 1) * 2 + 1] = p[1]
                    }
                    path[path.size - 2] = target[0]
                    path[path.size - 1] = target[1]

                    val angle = atan2(dirY, dirX) * 180f / PI.toFloat()
                    bestResult = AimResult(
                        cueBall = cue,
                        targetBall = target,
                        pocket = pocket,
                        aimPath = path,
                        banks = banks,
                        angle = angle,
                        power = (len / (TABLE_W * (banks + 1))).coerceIn(0.3f, 1f),
                        isDirect = false,
                        reflectionPoints = refPoints
                    )
                }
            }
        }

        return bestResult
    }

    /**
     * 生成镜像袋口（多库反射）
     */
    private fun generateMirrorPockets(pocket: FloatArray, banks: Int): List<FloatArray> {
        val results = mutableListOf<FloatArray>()
        // 简化：只生成水平和垂直镜像
        // 实际应枚举所有反射组合
        for (h in 0..banks) {
            for (v in 0..(banks - h)) {
                if (h + v != banks) continue
                var mx = pocket[0]
                var my = pocket[1]
                if (h % 2 == 1) mx = TABLE_W - mx  // 水平镜像
                if (v % 2 == 1) my = TABLE_H - my  // 垂直镜像
                // 多次反射
        
