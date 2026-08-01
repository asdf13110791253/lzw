src/main/java/package com.lingmiao.v2.core.log

import android.util.Log

object LingMiaoLogger {
    private const val TAG = "LingMiao"

    fun i(msg: String) {
        Log.i(TAG, msg)
    }

    fun e(msg: String) {
        Log.e(TAG, msg)
    }

    fun d(msg: String) {
        Log.d(TAG, msg)
    }
}
