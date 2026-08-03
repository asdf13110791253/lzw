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
        get() = prefs.getBoolean(K_ANT_LINE, true)
        set(v) = prefs.edit().putBoolean(K_ANT_LINE, v).apply()

    // ── 自动吸附 ──
    var snapNearest: Boolean
        get() = prefs.getBoolean(K_SNAP, true)
        set(v) = prefs.edit().putBoolean(K_SNAP, v).apply()

    // ── 亮度阈值 (0-255) ──
    var brightnessThreshold: Int
        get() = prefs.getInt(K_BRIGHT, DEFAULT_BRIGHTNESS).coerceIn(0, 255)
        set(v) = prefs.edit().putInt(K_BRIGHT, v.coerceIn(0, 255)).apply()

    // ── 灵敏度 (1-100) ──
    var circleSensitivity: Int
        get() = prefs.getInt(K_SENS, DEFAULT_SENSITIVITY).coerceIn(1, 100)
        set(v) = prefs.edit().putInt(K_SENS, v.coerceIn(1, 100)).apply()

    // ── 最小距离 (1-100) ──
    var minCircleDist: Int
        get() = prefs.getInt(K_MIN_DIST, DEFAULT_MIN_DIST).coerceIn(1, 100)
        set(v) = prefs.edit().putInt(K_MIN_DIST, v.coerceIn(1, 100)).apply()

    // ── 显示角度 ──
    var showAngle: Boolean
        get() = prefs.getBoolean(K_SHOW_ANGLE, true)
        set(v) = prefs.edit().putBoolean(K_SHOW_ANGLE, v).apply()

    // ── 屏幕方向 ──
    var orientation: Int
        get() = prefs.getInt(K_ORIENT, 0)
        set(v) = prefs.edit().putInt(K_ORIENT, v).apply()
}
