package com.lingmiao.v2.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.lingmiao.v2.data.entity.CalibrationEntity

@Dao
interface CalibrationDao {

    @Query("SELECT * FROM calibration WHERE id = 0")
    fun get(): CalibrationEntity?

    @Query("SELECT * FROM calibration WHERE id = 0")
    fun getLive(): LiveData<CalibrationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(entity: CalibrationEntity)

    @Query("DELETE FROM calibration")
    fun clear()

    @Query("SELECT COUNT(*) FROM calibration")
    fun count(): Int
}
