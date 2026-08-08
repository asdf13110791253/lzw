package com.lingmiao.v2.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 全局事件总线
 * 用于 Activity/Fragment/Service 之间的解耦通信
 */
object EventBus {

    data class Event(val type: String, val data: Any? = null)

    // 内部可变流
    private val _events = MutableSharedFlow<Event>()
    // 对外只读流，供外部 collect 监听
    val events: SharedFlow<Event> = _events.asSharedFlow()

    // 发送事件（需要在协程作用域内调用，例如 lifecycleScope.launch { }）
    suspend fun send(type: String, data: Any? = null) {
        _events.emit(Event(type, data))
    }

    // 🔥 修复：直接在 object 内部定义常量，禁止使用 companion object
    const val EVT_TOAST = "EVT_TOAST"
}
