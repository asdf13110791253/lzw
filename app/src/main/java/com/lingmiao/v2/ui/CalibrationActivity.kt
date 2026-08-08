package com.lingmiao.v2.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingmiao.v2.config.AppConfig

/**
 * 桌布校准 Activity
 *
 * 让用户手动对齐球桌四个角，得到透视变换矩阵，
 * 后续 C++ 层用这个矩阵把屏幕坐标映射回"标准球桌坐标"，
 * 这样无论游戏画面怎么缩放/变形，辅助线都能对准。
 */
class CalibrationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CalibrationScreen(
                        onSave = { points ->
                            // 保存四个角点
                            val prefs = AppConfig.prefs
                            prefs.edit().apply {
                                putFloat("cal_x0", points[0].x)
                                putFloat("cal_y0", points[0].y)
                                putFloat("cal_x1", points[1].x)
                                putFloat("cal_y1", points[1].y)
                                putFloat("cal_x2", points[2].x)
                                putFloat("cal_y2", points[2].y)
                                putFloat("cal_x3", points[3].x)
                                putFloat("cal_y3", points[3].y)
                                putBoolean("calibrated", true)
                            }.apply()
                            Toast.makeText(this, "校准完成 ✅", Toast.LENGTH_SHORT).show()
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun CalibrationScreen(
    onSave: (List<Offset>) -> Unit,
    onCancel: () -> Unit
) {
    var points by remember { mutableStateOf(listOf<Offset>()) }
    val maxPoints = 4

    Box(modifier = Modifier.fillMaxSize()) {
        // 绘制区域：去掉了黑色遮罩，完全透明，露出底下的台球桌面
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Transparent)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (points.size < maxPoints) {
                            points = points + offset
                        }
                    }
                }
        ) {
            // 画红色的点击点
            points.forEachIndexed { index, pt ->
                drawCircle(
                    color = androidx.compose.ui.graphics.Color.Red,
                    radius = 12f,
                    center = pt
                )
            }

            // 画黄色的四边形连线
            if (points.size >= 2) {
                val path = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    if (points.size == 4) {
                        close()
                    }
                }
                drawPath(
                    path = path,
                    color = androidx.compose.ui.graphics.Color.Yellow,
                    style = Stroke(width = 3f)
                )
            }
        }

        // ---------------- 精心打扮过的 UI 层 ----------------
        
        // 1. 顶部提示卡片
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.TopCenter)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "🪄 桌布校准",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color(0xFF764E54) // 豆沙粉
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "请依次点击球桌的四个角\n（左上 → 右上 → 右下 → 左下）",
                        fontSize = 14.sp,
                        color = androidx.compose.ui.graphics.Color(0xFF97747A)
                    )
                    Spacer(Modifier.height(12.dp))
                    // 进度提示
                    Text(
                        text = if (points.size == maxPoints) "✅ 四个点已全部标记，可以保存了！" 
                               else "📍 已标记 ${points.size} / $maxPoints 个点",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (points.size == maxPoints) 
                            androidx.compose.ui.graphics.Color(0xFF4CAF50) // 绿色
                        else 
                            androidx.compose.ui.graphics.Color(0xFF764E54)
                    )
                }
            }
        }

        // 2. 底部按钮区
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 重置按钮
            OutlinedButton(
                onClick = { points = emptyList() },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = androidx.compose.ui.graphics.Color(0xFF764E54)
                )
            ) {
                Text("重置", fontSize = 16.sp)
            }

            // 保存按钮
            Button(
                onClick = {
                    if (points.size == maxPoints) {
                        onSave(points)
                    }
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = points.size == maxPoints,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (points.size == maxPoints) 
                        androidx.compose.ui.graphics.Color(0xFFD15974) // 亮起时的高级粉
                    else 
                        androidx.compose.ui.graphics.Color(0xFFE0D6D2), // 禁用时的灰色
                    contentColor = androidx.compose.ui.graphics.Color.White
                )
            ) {
                Text("保存校准", fontSize = 16.sp)
            }
        }
    }
}
