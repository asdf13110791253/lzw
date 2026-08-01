package com.lingmiao.v2.ui

import android.app.Activity
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lingmiao.v2.core.config.AppConfig
import com.lingmiao.v2.core.event.EventBus
import com.lingmiao.v2.core.log.LogManager
import com.lingmiao.v2.engine.table.GeometryEngine
import kotlinx.coroutines.delay

/**
 * 四角校准页 - 拖拽四角对齐球桌内缘
 * 实时透视预览
 */
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

    private fun saveCorners(corners: GeometryEngine.Corners) {
        // 保存到配置
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
    val context = LocalContext.current
    val screenW = context.resources.displayMetrics.widthPixels.toFloat()
    val screenH = context.resources.displayMetrics.heightPixels.toFloat()

    // 四角位置（初始值：屏幕内缩 10%）
    var tlx by remember { mutableStateOf(screenW * 0.1f) }
    var tly by remember { mutableStateOf(screenH * 0.15f) }
    var trx by remember { mutableStateOf(screenW * 0.9f) }
    var try_ by remember { mutableStateOf(screenH * 0.15f) }
    var blx by remember { mutableStateOf(screenW * 0.1f) }
    var bly by remember { mutableStateOf(screenH * 0.85f) }
    var brx by remember { mutableStateOf(screenW * 0.9f) }
    var bry by remember { mutableStateOf(screenH * 0.85f) }

    // 透视预览
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // 拖拽中的角
    var activeCorner by remember { mutableStateOf(-1) }

    // 校验
    val isValid = remember(tlx, tly, trx, try_, blx, bly, brx, bry) {
        val w1 = trx - tlx; val w2 = brx - blx
        val h1 = bly - tly; val h2 = bry - try_
        w1 > 100f && w2 > 100f && h1 > 100f && h2 > 100f &&
        tlx < trx && blx < brx && tly < bly && try_ < bry
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 半透明背景 + 透视预览区域
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // 绘制透视区域（半透明白色四边形）
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(tlx, tly)
                    lineTo(trx, try_)
                    lineTo(brx, bry)
                    lineTo(blx, bly)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.08f)
                )
                // 边框
                drawPath(
                    path = path,
                    color = Color(0xFF7C4DFF),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )
            }

            // 四角拖拽手柄
            CornerHandle(
                x = tlx, y = tly, label = "↖",
                onDrag = { dx, dy -> tlx = (tlx + dx).coerceIn(0f, screenW/2); tly = (tly + dy).coerceIn(0f, screenH/2) }
            )
            CornerHandle(
                x = trx, y = try_, label = "↗",
                onDrag = { dx, dy -> trx = (trx + dx).coerceIn(screenW/2, screenW); try_ = (try_ + dy).coerceIn(0f, screenH/2) }
            )
            CornerHandle(
                x = blx, y = bly, label = "↙",
                onDrag = { dx, dy -> blx = (blx + dx).coerceIn(0f, screenW/2); bly = (bly + dy).coerceIn(screenH/2, screenH) }
            )
            CornerHandle(
                x = brx, y = bry, label = "↘",
                onDrag = { dx, dy -> brx = (brx + dx).coerceIn(screenW/2, screenW); bry = (bry + dy).coerceIn(screenH/2, screenH) }
            )

            // 顶部提示
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "📐 四角校准",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "拖拽四角对齐球桌内缘\n辅助线会更稳定",
                    fontSize = 14.sp,
                    color = Color.LightGray,
                    textAlign = androidx.compose.ui.text.TextAlign.Center
                )
            }

            // 底部按钮
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("取消", fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        geoEngine.setCalibration(
                            GeometryEngine.Corners(tlx, tly, trx, try_, blx, bly, brx, bry),
                            screenW, screenH
                        )
                        onSave(GeometryEngine.Corners(tlx, tly, trx, try_, blx, bly, brx, bry))
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isValid) Color(0xFF7C4DFF) else Color.Gray
                    )
                ) {
                    Text("💾 保存并应用", fontSize = 16.sp, color = Color.White)
                }
            }

            // 实时坐标显示
            Card(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("TL:(${tlx.toInt()},${tly.toInt()})", color = Color.White, fontSize = 11.sp)
                    Text("TR:(${trx.toInt()},${try_.toInt()})", color = Color.White, fontSize = 11.sp)
                    Text("BL:(${blx.toInt()},${bly.toInt()})", color = Color.White, fontSize = 11.sp)
                    Text("BR:(${brx.toInt()},${bry.toInt()})", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun CornerHandle(
    x: Float, y: Float, label: String,
    onDrag: (Float, Float) -> Unit
) {
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .offset(x = androidx.compose.ui.unit.Dp(x + offset.x), y = androidx.compose.ui.unit.Dp(y + offset.y))
            .size(48.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset += dragAmount
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = { offset = Offset.Zero }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF7C4DFF),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(label, color = Color.White, fontSize = 18.sp)
            }
        }
    }
}
