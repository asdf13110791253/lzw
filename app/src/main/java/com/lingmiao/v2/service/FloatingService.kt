package com.lingmiao.v2.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.lingmiao.v2.core.log.LogManager

class FloatingService : Service() {
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, FloatingService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        LogManager.i("悬浮服务", "服务启动成功")
    }

    override fun onDestroy() {
        super.onDestroy()
        LogManager.i("悬浮服务", "服务已关闭")
    }

    override fun onBind(p0: Intent?): IBinder? = null
}
