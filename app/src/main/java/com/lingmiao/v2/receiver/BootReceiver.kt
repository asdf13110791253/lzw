package com.lingmiao.v2.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lingmiao.v2.service.KeepAliveService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, KeepAliveService::class.java)
            context?.startForegroundService(serviceIntent)
        }
    }
}
