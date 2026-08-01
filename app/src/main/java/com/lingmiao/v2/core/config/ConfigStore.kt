package com.lingmiao.v2.core.config

import android.content.Context
import android.content.SharedPreferences

object ConfigStore {
    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences("lingmiao_cfg", Context.MODE_PRIVATE)
    }

    fun getString(key: String, def: String = ""): String {
        return sp.getString(key, def) ?: def
    }

    fun putString(key: String, value: String) {
        sp.edit().putString(key, value).apply()
    }
}
