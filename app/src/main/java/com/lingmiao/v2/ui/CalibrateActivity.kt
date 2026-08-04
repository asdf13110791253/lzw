package com.lingmiao.v2.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingmiao.v2.core.event.EventBus
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.table.GeometryEngine
// 注意：FloatingService 不再需要 hideOverlay/showOverlay

class CalibrateActivity : ComponentActivity() {

    companion object {
        const val TAG = "Calibrate"
    }

    private val geoEngine = GeometryEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalibrateScreen(
                geoEngine = geoEngine,
                onSave = { corners ->
                    saveCorners(corners)
                    finish()
                },
                onCancel = { finish() }
            )
        }
    }

    // 移除 onResume/onPause 中的 FloatingService 调用

    private fun saveCorners(corners: GeometryEngine.Corners) {
        val prefs = getSharedPreferences("calibration", MODE_PRIVATE)
        prefs.edit().apply {
            putFloat("tlx", corners.tlx); putFloat("tly", corners.tly)
            putFloat("trx", corners.trx); putFloat("try_", corners.try_)
            putFloat("blx", corners.blx); putFloat("bly", corners.bly)
            putFloat("brx", corners.brx); putFloat("bry", corners.bry)
            apply()
        }
        EventBus.emitCalibrationUpdated(floatArrayOf(
            corners.tlx, corners.tly, corners.trx, corners.try_,
            corners.blx, corners.bly, corners.brx, corners.bry
        ))
        LogManager.geo("💾 校准数据已保存")
    }
}

@Composable
fun CalibrateScreen(
    geoEngine: GeometryEngine,
    onSave: (GeometryEngine.Corners) -> Unit,
    onCancel: () -> Unit
) {
    // ... 保持原有校准 UI 不变 ...
}
