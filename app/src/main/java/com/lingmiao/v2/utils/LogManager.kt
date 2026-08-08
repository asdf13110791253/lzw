package com.lingmiao.v2.utils

import android.util.Log

/**
 * 日志管理工具类
 * 统一管理 App 的 Log 输出，方便后续一键关闭或文件保存
 */
object LogManager {
    // 默认的全局 Tag
    private const val DEFAULT_TAG = "LingMiao"

    fun d(tag: String = DEFAULT_TAG, msg: String) {
        Log.d(tag, msg)
    }

    fun i(tag: String = DEFAULT_TAG, msg: String) {
        Log.i(tag, msg)
    }

    fun w(tag: String = DEFAULT_TAG, msg: String) {
        Log.w(tag, msg)
    }

    fun e(tag: String = DEFAULT_TAG, msg: String) {
        Log.e(tag, msg)
    }

    // 兼容之前代码中的 LogManager.service(...) 调用
    fun service(msg: String) {
        i("Service", msg)
    }
}
