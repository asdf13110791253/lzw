package com.lingmiao.v2.core.config

import android.content.Context
import android.content.SharedPreferences
import com.lingmiao.v2.core.event.EventBus

object AppConfig {

    private const val PREFS = "lingmiao_config"
    private lateinit var prefs: SharedPreferences

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

    val DEFAULT_AIM_COLOR = 0xFFFFFF00.toInt()
    const val DEFAULT_AIM_WIDTH = 5.0f
    const val DEFAULT_DETECT_MODE = 1
    const val DEFAULT_MAX_BANKS = 2
    const val DEFAULT_BRIGHTNESS = 232
    const val DEFAULT_SENSITIVITY = 15
    const val DEFAULT_MIN_DIST = 15
    const val DEFAULT_COMP_RATIO = 0.18f

    data class DetectPreset(
        val name: String,
        val method: String,
        val vThreshold: Int,
        val sMinDist: Int,
        val pSensitivity: Int,
        val dp: Float
    )

    val PRESETS = listOf(
        DetectPreset("方案0 (Haar)", "Haar", 180, 15, 10, 1.0f),
        DetectPreset("方案1 (HSV)", "HSV", 200, 20, 15, 1.1f),
        DetectPreset("方案2 (Edge)", "Edge", 160, 10, 8, 1.2f),
        DetectPreset("方案3 (TFLite)", "TFLite", 190, 18, 12, 1.0f),
        DetectPreset("方案4 (Fusion)", "Fusion", 210, 25, 20, 1.3f),
        DetectPreset("方案5", "Custom", 220, 30, 25, 1.5f),
        DetectPreset("方案6", "Custom", 170, 12, 6, 0.9f),
        DetectPreset("方案7", "Custom", 195, 22, 18, 1.4f)
    )

    fun getCurrentPreset(): DetectPreset = PRESETS.getOrElse(detectMode) { PRESETS[0] }

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(K_FIRST_LAUNCH, true)
        set(v) = prefs.edit().putBoolean(K_FIRST_LAUNCH, v).apply()

    var isOverlayEnabled: Boolean
        get() = prefs.getBoolean(K_OVERLAY, false)
        set(v) {
            prefs.edit().putBoolean(K_OVERLAY, v).apply()
            EventBus.emitOverlayChanged(v)
        }

    var aimColor: Int
        get() = prefs.getInt(K_AIM_COLOR, DEFAULT_AIM_COLOR)
        set(v) {
            prefs.edit().putInt(K_AIM_COLOR, v).apply()
            EventBus.emitAimConfigChanged(v, aimWidth)
        }

    var aimWidth: Float
        get() = prefs.getFloat(K_AIM_WIDTH, DEFAULT_AIM_WIDTH).coerceIn(1f, 10f)
        set(v) {
            val clamped = v.coerceIn(1f, 10f)
            prefs.edit().putFloat(K_AIM_WIDTH, clamped).apply()
            EventBus.emitAimConfigChanged(aimColor, clamped)
        }

    var detectMode: Int
        get() = prefs.getInt(K_DETECT_MODE, DEFAULT_DETECT_MODE).coerceIn(0, 7)
        set(v) {
            val clamped = v.coerceIn(0, 7)
            prefs.edit().putInt(K_DETECT_MODE, clamped).apply()
            EventBus.emitDetectModeChanged(clamped)
        }

    var physicsPreset: String
        get() = prefs.getString(K_PHYSICS, "standard") ?: "standard"
        set(v) {
            prefs.edit().putString(K_PHYSICS, v).apply()
            EventBus.emit("physics_changed", v)
        }

    var maxBanks: Int
        get() = prefs.getInt(K_MAX_BANKS, DEFAULT_MAX_BANKS).coerceIn(1, 5)
        set(v) {
            val clamped = v.coerceIn(1, 5)
            prefs.edit().putInt(K_MAX_BANKS, clamped).apply()
        }

    var language: String
        get() = prefs.getString(K_LANGUAGE, "zh") ?: "zh"
        set(v) = prefs.edit().putString(K_LANGUAGE, v).apply()

    var tableTexture: Int
        get() = prefs.getInt(K_TABLE_TEX, 1).coerceIn(1, 5)
        set(v) {
            val clamped = v.coerceIn(1, 5)
            prefs.edit().putInt(K_TABLE_TEX, clamped).apply()
            EventBus.emit("table_texture_changed", clamped)
        }

    var reflectionMode: String
        get() = prefs.getString(K_REFLECTION, "mirror") ?: "mirror"
        set(v) {
            prefs.edit().putString(K_REFLECTION, v).apply()
            EventBus.emit("reflection_changed", v)
        }

    var compensationRatio: Float
        get() = prefs.getFloat(K_COMP_RATIO, DEFAULT_COMP_RATIO).coerceIn(0f, 1f)
        set(v) {
            val clamped = v.coerceIn(0f, 1f)
            prefs.edit().putFloat(K_COMP_RATIO, clamped).apply()
        }

    var showAntLine: Boolean
        get() = prefs.getBoolean(K_ANT_LINE, true)
        set(v) = prefs.edit().putBoolean(K_ANT_LINE, v).apply()

    var snapNearest: Boolean
        get() = prefs.getBoolean(K_SNAP, true)
        set(v) = prefs.edit().putBoolean(K_SNAP, v).apply()

    var brightnessThreshold: Int
        get() = prefs.getInt(K_BRIGHT, DEFAULT_BRIGHTNESS).coerceIn(0, 255)
        set(v) = prefs.edit().putInt(K_BRIGHT, v.coerceIn(0, 255)).apply()

    var circleSensitivity: Int
        get() = prefs.getInt(K_SENS, DEFAULT_SENSITIVITY).coerceIn(1, 100)
        set(v) = prefs.edit().putInt(K_SENS, v.coerceIn(1, 100)).apply()

    var minCircleDist: Int
        get() = prefs.getInt(K_MIN_DIST, DEFAULT_MIN_DIST).coerceIn(1, 100)
        set(v) = prefs.edit().putInt(K_MIN_DIST, v.coerceIn(1, 100)).apply()

    var showAngle: Boolean
        get() = prefs.getBoolean(K_SHOW_ANGLE, true)
        set(v) = prefs.edit().putBoolean(K_SHOW_ANGLE, v).apply()

    var orientation: Int
        get() = prefs.getInt(K_ORIENT, 0)
        set(v) = prefs.edit().putInt(K_ORIENT, v).apply()
}
