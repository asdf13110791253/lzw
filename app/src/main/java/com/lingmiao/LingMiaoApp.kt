package com.lingmiao.v2

import android.app.Application
import com.lingmiao.core.config.ConfigStore
import com.lingmiao.core.log.LingMiaoLogger
import com.lingmiao.data.db.LingMiaoDatabase

class LingMiaoApp : Application() {
    companion object {
        lateinit var instance: LingMiaoApp
            private set
        lateinit var config: ConfigStore
            private set
        lateinit var logger: LingMiaoLogger
            private set
        lateinit var database: LingMiaoDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 初始化日志
        logger = LingMiaoLogger()
        // 配置存储
        config = ConfigStore(this)
        // 数据库初始化
        database = LingMiaoDatabase(this)
    }
}
