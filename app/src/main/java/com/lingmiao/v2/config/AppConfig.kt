package com.lingmiao.v2.config

import android.content.Context
import android.content.SharedPreferences
import com.lingmiao.v2.LingMiaoApp

/**
 * 全局配置管理
 *
 * 封装 SharedPreferences，供所有模块读取参数：
 * - HSV 阈值 → C++ 图像识别
 * - 反射模式 → C++ 物理计算
 * - 线颜色/粗细 → OverlayService 绘制
 * - 校准点 → C++ 坐标映射
 */
object AppConfig {

    // 🔥【重要修复】：去掉了 private，否则其他页面读取配置会闪退
    val prefs: SharedPreferences by lazy {
        LingMiaoApp.getInstance().getSharedPreferences("lingmiao_config", Context.MODE_PRIVATE)
    }

    // ===== 图像识别参数（传给 C++） =====
    val hsvV: Int get() = prefs.getInt("hsv_v", 200)
    val hsvS: Int get() = prefs.getInt("hsv_s", 30)
    val hsvP: Int get() = prefs.getInt("hsv_p", 15)

    // ===== 物理参数 =====
    val reflectionMode: String get() = prefs.getString("reflection_mode", "compensation") ?: "compensation"
    val bankCount: Int get() = prefs.getInt("bank_count", 2)

    // ===== 绘制参数 =====
    val lineColor: Int get() = prefs.getInt("line_color", 0xFFFF1744.toInt())
    val lineWidth: Float get() = prefs.getFloat("line_width", 5f)
    val showAntLine: Boolean get() = prefs.getBoolean("show_ant_line", false)
    val snapNearest: Boolean get() = prefs.getBoolean("snap_nearest", true)

    // ===== 校准数据 =====
    val isCalibrated: Boolean get() = prefs.getBoolean("calibrated", false)
    fun getCalibrationPoints(): FloatArray {
        return floatArrayOf(
            prefs.getFloat("cal_x0", 0f),
            prefs.getFloat("cal_y0", 0f),
            prefs.getFloat("cal_x1", 0f),
            prefs.getFloat("cal_y1", 0f),
            prefs.getFloat("cal_x2", 0f),
            prefs.getFloat("cal_y2", 0f),
            prefs.getFloat("cal_x3", 0f),
            prefs.getFloat("cal_y3", 0f)
        )
    }

    // ===== 运行时状态 =====
    var isAssistRunning: Boolean
        get() = prefs.getBoolean("assist_running", false)
        set(value) = prefs.edit().putBoolean("assist_running", value).apply()

    var isLineVisible: Boolean
        get() = prefs.getBoolean("line_visible", true)
        set(value) = prefs.edit().putBoolean("line_visible", value).apply()


    // ==========================================================
    // 🔥【新增】：为 BallDetector 补全的检测模式和预设参数
    // ==========================================================
    var detectMode: Int = 0

    data class Preset(
        val name: String,
        val vThreshold: Int,
        val sMinDist: Int,
        val pSensitivity: Int,
        val dp: Float
    )

    fun getCurrentPreset(): Preset {
        return when (detectMode) {
            0 -> Preset("标准室内", 200, 30, 15, 2f)
            1 -> Preset("强光室内", 230, 40, 10, 2f)
            2 -> Preset("暗光模式", 180, 20, 20, 3f)
            3 -> Preset("高对比度", 240, 50, 5, 1f)
            else -> Preset("标准室内", 200, 30, 15, 2f)
        }
    }
}
