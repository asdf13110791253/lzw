package com.lingmiao.v2.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.lingmiao.v2.core.log.LogManager

class KeepAliveService : Service() {
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, KeepAliveService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        LogManager.i("保活服务", "后台保活开启")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogManager.i("保活服务", "保活服务关闭")
    }

    override fun onBind(p0: Intent?): IBinder? = null
}
