package com.lingmiao.v2.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle

/**
 * 录屏授权中转 Activity
 */
class CapturePermissionActivity : Activity() {

    companion object {
        const val REQUEST_CODE = 1001

        // 🔥 新增：这个方法是给 MainActivity 调用的，必须存在
        fun createIntent(context: Context): Intent {
            return Intent(context, CapturePermissionActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mpm = getSystemService(MediaProjectionManager::class.java)
        val intent = mpm.createScreenCaptureIntent()
        startActivityForResult(intent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                val serviceIntent = Intent(this, CaptureService::class.java).apply {
                    putExtra("resultCode", resultCode)
                    putExtra("data", data)
                }
                startForegroundService(serviceIntent)
            }
            finish()
        }
    }
}
