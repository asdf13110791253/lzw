package com.lingmiao.v2.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.lingmiao.LingMiaoApp

class ScreenCaptureService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1001, LingMiaoApp.instance.createNotify())
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
