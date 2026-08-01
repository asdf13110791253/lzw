package com.lingmiao.v2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.service.KeepAliveService

/**
 * 开机自启广播接收器
 * 监听 BOOT_COMPLETED 后拉起保活服务
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                LogManager.i(TAG, "📡 收到开机广播，启动保活服务")
                KeepAliveService.start(context)
            }
        }
    }
}
