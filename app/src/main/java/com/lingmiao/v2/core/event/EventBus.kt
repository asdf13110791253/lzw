package com.lingmiao.v2.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 灵喵事件总线 - Kotlin Flow 实现
 * 支持多订阅者、背压、生命周期感知
 */
object EventBus {

    // ── 事件类型常量 ──
    const val EVT_OVERLAY_CHANGED = "overlay_changed"
    const val EVT_CALIBRATION_UPDATED = "calibration_updated"
    const val EVT_DETECT_MODE_CHANGED = "detect_mode_changed"
    const val EVT_AIM_CONFIG_CHANGED = "aim_config_changed"
    const val EVT_PHYSICS_CHANGED = "physics_changed"
    const val EVT_REFLECTION_CHANGED = "reflection_changed"
    const val EVT_TABLE_TEXTURE_CHANGED = "table_texture_changed"
    const val EVT_ORIENTATION_CHANGED = "orientation_changed"
    const val EVT_REQUEST_SCREEN_CAPTURE = "request_screen_capture"
    const val EVT_STOP_SCREEN_CAPTURE = "stop_screen_capture"
    const val EVT_ERROR = "error"
    const val EVT_TOAST = "toast"
    const val EVT_BALLS_DETECTED = "balls_detected"
    const val EVT_AIM_CALCULATED = "aim_calculated"

    data class AppEvent(
        val type: String,
        val data: Any? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    // 热流：新订阅者收不到旧事件
    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    // 状态流：新订阅者立即收到最新值
    private val _stateEvents = MutableSharedFlow<AppEvent>(
        replay = 1,
        extraBufferCapacity = 16
    )
    val stateEvents: SharedFlow<AppEvent> = _stateEvents.asSharedFlow()

    // ── 发送方法 ──
    fun emit(type: String, data: Any? = null) {
        val event = AppEvent(type, data)
        _events.tryEmit(event)
        _stateEvents.tryEmit(event)
    }

    fun emitState(type: String, data: Any? = null) {
        _stateEvents.tryEmit(AppEvent(type, data))
    }

    // ── 便捷方法 ──
    fun emitOverlayChanged(enabled: Boolean) = emit(EVT_OVERLAY_CHANGED, enabled)
    fun emitCalibrationUpdated(corners: FloatArray) = emit(EVT_CALIBRATION_UPDATED, corners)
    fun emitDetectModeChanged(mode: Int) = emit(EVT_DETECT_MODE_CHANGED, mode)
    fun emitAimConfigChanged(color: Int, width: Float) = emit(EVT_AIM_CONFIG_CHANGED, intArrayOf(color, width.toInt()))
    fun emitError(msg: String, code: Int = 0) = emit(EVT_ERROR, "$code:$msg")
    fun emitToast(msg: String) = emit(EVT_TOAST, msg)
    fun emitBallsDetected(count: Int) = emit(EVT_BALLS_DETECTED, count)
    fun emitAimCalculated(data: FloatArray) = emit(EVT_AIM_CALCULATED, data)
}
