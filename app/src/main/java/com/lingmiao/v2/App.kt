package com.lingmiao.v2

import android.app.Application
import com.lingmiao.v2.core.event.EventBus
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.opencv.NativeLoader

/**
 * 灵喵 LingMiao V2.0 - Application 入口
 */
class App : Application() {

    // ✅ 合并后的唯一 Companion Object
    companion object {
        const val TAG = "LingMiaoApp"
        lateinit var instance: App
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 1. 初始化日志
        LogManager.init(getExternalFilesDir(null)?.absolutePath + "/logs")

        // 2. 加载 Native 库
        try {
            NativeLoader.loadAll()
            LogManager.i(TAG, "✅ Native 库加载成功")
        } catch (e: Throwable) {
            LogManager.e(TAG, "⚠️ Native 库加载失败（非致命）: ${e.message}")
        }

        // 3. 初始化事件总线
        EventBus.getInstance()

        LogManager.i(TAG, "🎱 灵喵 LingMiao V2.0 启动完成")
    }

    override fun onTerminate() {
        super.onTerminate()
        LogManager.shutdown()
    }

    fun isNativeReady(): Boolean = NativeLoader.isLoaded()

    internal set

    var isForeground = false
        internal set

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_MODERATE) {
            LogManager.w(TAG, "内存紧张 level=$level，释放缓存")
        }
    }
}
