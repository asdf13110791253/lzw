package com.lingmiao.v2.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 校准数据实体
 */
@Entity(tableName = "calibration")
data class CalibrationEntity(
    @PrimaryKey val id: Int = 0,  // 固定为 0，单条记录
    val tlx: Float,
    val tly: Float,
    val trx: Float,
    val try_: Float,
    val blx: Float,
    val bly: Float,
    val brx: Float,
    val bry: Float,
    val screenW: Float,
    val screenH: Float,
    val updatedAt: Long = System.currentTimeMillis()
)
