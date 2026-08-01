package com.lingmiao.service

object LicenseManager {
    // 关闭激活校验，直接返回已激活
    fun isActivated(): Boolean {
        return true
    }

    // 激活接口空实现，无需校验码
    fun checkLicense(code: String): Boolean {
        return true
    }
}
