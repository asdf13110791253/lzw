package com.lingmiao.v2.core.config

import android.content.Context
import android.content.SharedPreferences
import com.lingmiao.v2.core.event.EventBus

/**
 * 灵喵全局配置 - 18 项设置全部持久化到 SharedPreferences
 */
object AppConfig {

    private const val PREFS = "lingmiao_config"
    private lateinit var prefs: SharedPreferences

    // ── 配置键 ──
    private const val K_FIRST_LAUNCH = "first_launch"
    private const val K_OVERLAY = "overlay_enabled"
    private const val K_AIM_COLOR = "aim_color"
    private const val K_AIM_WIDTH = "aim_width"
    private const val K_DETECT_MODE = "detect_mode"
    private const val K_PHYSICS = "physics_preset"
    private const val K_MAX_BANKS = "max_banks"
    private const val K_LANGUAGE = "language"
    private const val K_TABLE_TEX = "table_texture"
    private const val K_REFLECTION = "reflection_mode"
    private const val K_COMP_RATIO = "compensation_ratio"
    private const val K_ANT_LINE = "show_ant_line"
    private const val K_SNAP = "snap_nearest"
    private const val K_BRIGHT = "brightness_threshold"
    private const val K_SENS = "circle_sensitivity"
    private const val K_MIN_DIST = "min_circle_dist"
    private const val K_SHOW_ANGLE = "show_angle"
    private const val K_ORIENT = "orientation"

    // ── 默认值 ──
    val DEFAULT_AIM_COLOR = 0xFFFFFF00.toInt() // 黄
    const val DEFAULT_AIM_WIDTH = 5.0f
    const val DEFAULT_DETECT_MODE = 1
    const val DEFAULT_MAX_BANKS = 2
    const val DEFAULT_BRIGHTNESS = 232
    const val DEFAULT_SENSITIVITY = 15
    const val DEFAULT_MIN_DIST = 15
    const val DEFAULT_COMP_RATIO = 0.18f

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    // ── 首次启动 ──
    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(K_FIRST_LAUNCH, true)
        set(v) = prefs.edit().putBoolean(K_FIRST_LAUNCH, v).apply()

    // ── 悬浮窗 ──
    var isOverlayEnabled: Boolean
        get() = prefs.getBoolean(K_OVERLAY, false)
        set(v) {
            prefs.edit().putBoolean(K_OVERLAY, v).apply()
            EventBus.emitOverlayChanged(v)
        }

    // ── 辅助线颜色 (ARGB Int) ──
    var aimColor: Int
        get() = prefs.getInt(K_AIM_COLOR, DEFAULT_AIM_COLOR)
        set(v) {
            prefs.edit().putInt(K_AIM_COLOR, v).apply()
            EventBus.emitAimConfigChanged(v, aimWidth)
        }

    // ── 辅助线粗细 (1-10) ──
    var aimWidth: Float
        get() = prefs.getFloat(K_AIM_WIDTH, DEFAULT_AIM_WIDTH).coerceIn(1f, 10f)
        set(v) {
            val clamped = v.coerceIn(1f, 10f)
            prefs.edit().putFloat(K_AIM_WIDTH, clamped).apply()
            EventBus.emitAimConfigChanged(aimColor, clamped)
        }

    // ── 检测方案 (0-7) ──
    var detectMode: Int
        get() = prefs.getInt(K_DETECT_MODE, DEFAULT_DETECT_MODE).coerceIn(0, 7)
        set(v) {
            val clamped = v.coerceIn(0, 7)
            prefs.edit().putInt(K_DETECT_MODE, clamped).apply()
            EventBus.emitDetectModeChanged(clamped)
        }

    // ── 物理预设 ──
    var physicsPreset: String
        get() = prefs.getString(K_PHYSICS, "standard") ?: "standard"
        set(v) {
            prefs.edit().putString(K_PHYSICS, v).apply()
            EventBus.emit("physics_changed", v)
        }

    // ── 最大翻袋库数 (1-5) ──
    var maxBanks: Int
        get() = prefs.getInt(K_MAX_BANKS, DEFAULT_MAX_BANKS).coerceIn(1, 5)
        set(v) {
            val clamped = v.coerceIn(1, 5)
            prefs.edit().putInt(K_MAX_BANKS, clamped).apply()
        }

    // ── 语言 ──
    var language: String
        get() = prefs.getString(K_LANGUAGE, "zh") ?: "zh"
        set(v) = prefs.edit().putString(K_LANGUAGE, v).apply()

    // ── 桌布纹理 (1-5) ──
    var tableTexture: Int
        get() = prefs.getInt(K_TABLE_TEX, 1).coerceIn(1, 5)
        set(v) {
            val clamped = v.coerceIn(1, 5)
            prefs.edit().putInt(K_TABLE_TEX, clamped).apply()
            EventBus.emit("table_texture_changed", clamped)
        }

    // ── 反射模式 ──
    var reflectionMode: String
        get() = prefs.getString(K_REFLECTION, "mirror") ?: "mirror"
        set(v) {
            prefs.edit().putString(K_REFLECTION, v).apply()
            EventBus.emit("reflection_changed", v)
        }

    // ── 补偿比例 (0-1) ──
    var compensationRatio: Float
        get() = prefs.getFloat(K_COMP_RATIO, DEFAULT_COMP_RATIO).coerceIn(0f, 1f)
        set(v) {
            val clamped = v.coerceIn(0f, 1f)
            prefs.edit().putFloat(K_COMP_RATIO, clamped).apply()
        }

    // ── 蚂蚁线 ──
    var showAntLine: Boolean
        get() = prefs.getBoolean(K_ANT_LINE, false)
        set(v) = prefs.edit().putBoolean(K_ANT_LINE, v).apply()

    // ── 吸附最近球 ──
    var snapToNearest: Boolean
        get() = prefs.getBoolean(K_SNAP, true)
        set(v) = prefs.edit().putBoolean(K_SNAP, v).apply()

    // ── 亮度阈值 ──
    var brightnessThreshold: Int
        get() = prefs.getInt(K_BRIGHT, DEFAULT_BRIGHTNESS).coerceIn(50, 300)
        set(v) = prefs.edit().putInt(K_BRIGHT, v.coerceIn(50, 300)).apply()

    // ── 圆检测灵敏度 ──
    var circleSensitivity: Int
        get() = prefs.getInt(K_SENS, DEFAULT_SENSITIVITY).coerceIn(5, 50)
        set(v) = prefs.edit().putInt(K_SENS, v.coerceIn(5, 50)).apply()

    // ── 最小圆间距 ──
    var minCircleDist: Int
        get() = prefs.getInt(K_MIN_DIST, DEFAULT_MIN_DIST).coerceIn(5, 50)
        set(v) = prefs.edit().putInt(K_MIN_DIST, v.coerceIn(5, 50)).apply()

    // ── 显示角度 ──
    var showAngle: Boolean
        get() = prefs.getBoolean(K_SHOW_ANGLE, false)
        set(v) = prefs.edit().putBoolean(K_SHOW_ANGLE, v).apply()

    // ── 横竖屏 (0=竖 1=横) ──
    var orientation: Int
        get() = prefs.getInt(K_ORIENT, 0).coerceIn(0, 1)
        set(v) {
            val clamped = v.coerceIn(0, 1)
            prefs.edit().putInt(K_ORIENT, clamped).apply()
            EventBus.emit("orientation_changed", clamped)
        }

    // ── 8 套识别方案预设 ──
    data class DetectPreset(
        val name: String,
        val vThreshold: Int,
        val sMinDist: Int,
        val pSensitivity: Int,
        val dp: Float,
        val method: String
    )

    val PRESETS = arrayOf(
        DetectPreset("标准-室内", 232, 15, 15, 0.7f, "haar"),
        DetectPreset("增强-室内", 200, 12, 18, 0.75f, "hsv"),
        DetectPreset("强光环境", 260, 20, 12, 0.8f, "hsv"),
        DetectPreset("弱光环境", 180, 10, 20, 0.65f, "tf"),
        DetectPreset("高对比度", 250, 18, 14, 0.78f, "edge"),
        DetectPreset("低对比度", 150, 8, 22, 0.6f, "tf"),
        DetectPreset("高速模式", 232, 15, 8, 0.85f, "haar"),
        DetectPreset("精确模式", 232, 15, 25, 0.9f, "tf")
    )

    fun getCurrentPreset(): DetectPreset = PRESETS[detectMode]

    // ── 导出/导入 ──
    fun exportAll(): Map<String, Any?> = mapOf(
        "aim_color" to aimColor,
        "aim_width" to aimWidth,
        "detect_mode" to detectMode,
        "physics_preset" to physicsPreset,
        "max_banks" to maxBanks,
        "language" to language,
        "table_texture" to tableTexture,
        "reflection_mode" to reflectionMode,
        "compensation_ratio" to compensationRatio,
        "show_ant_line" to showAntLine,
        "snap_nearest" to snapToNearest,
        "brightness_threshold" to brightnessThreshold,
        "circle_sensitivity" to circleSensitivity,
        "min_circle_dist" to minCircleDist,
        "show_angle" to showAngle,
        "orientation" to orientation,
        "is_overlay_enabled" to isOverlayEnabled,
        "is_first_launch" to isFirstLaunch
    )

    fun importAll(data: Map<String, Any?>) {
        val editor = prefs.edit()
        data.forEach { (key, value) ->
            when (key) {
                "aim_color" -> editor.putInt(K_AIM_COLOR, value as? Int ?: DEFAULT_AIM_COLOR)
                "aim_width" -> editor.putFloat(K_AIM_WIDTH, (value as? Number)?.toFloat() ?: DEFAULT_AIM_WIDTH)
                "detect_mode" -> editor.putInt(K_DETECT_MODE, (value as? Number)?.toInt() ?: DEFAULT_DETECT_MODE)
                "physics_preset" -> editor.putString(K_PHYSICS, value as? String ?: "standard")
                "max_banks" -> editor.putInt(K_MAX_BANKS, (value as? Number)?.toInt() ?: DEFAULT_MAX_BANKS)
                "language" -> editor.putString(K_LANGUAGE, value as? String ?: "zh")
                "table_texture" -> editor.putInt(K_TABLE_TEX, (value as? Number)?.toInt() ?: 1)
                "reflection_mode" -> editor.putString(K_REFLECTION, value as? String ?: "mirror")
                "compensation_ratio" -> editor.putFloat(K_COMP_RATIO, (value as? Number)?.toFloat() ?: DEFAULT_COMP_RATIO)
                "show_ant_line" -> editor.putBoolean(K_ANT_LINE, value as? Boolean ?: false)
                "snap_nearest" -> editor.putBoolean(K_SNAP, value as? Boolean ?: true)
                "brightness_threshold" -> editor.putInt(K_BRIGHT, (value as? Number)?.toInt() ?: DEFAULT_BRIGHTNESS)
                "circle_sensitivity" -> editor.putInt(K_SENS, (value as? Number)?.toInt() ?: DEFAULT_SENSITIVITY)
                "min_circle_dist" -> editor.putInt(K_MIN_DIST, (value as? Number)?.toInt() ?: DEFAULT_MIN_DIST)
                "show_angle" -> editor.putBoolean(K_SHOW_ANGLE, value as? Boolean ?: false)
                "orientation" -> editor.putInt(K_ORIENT, (value as? Number)?.toInt() ?: 0)
                "is_overlay_enabled" -> editor.putBoolean(K_OVERLAY, value as? Boolean ?: false)
                "is_first_launch" -> editor.putBoolean(K_FIRST_LAUNCH, value
