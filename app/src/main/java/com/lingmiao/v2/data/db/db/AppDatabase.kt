package com.lingmiao.v2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lingmiao.v2.data.dao.CalibrationDao
import com.lingmiao.v2.data.entity.CalibrationEntity

/**
 * Room 数据库
 */
@Database(entities = [CalibrationEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {

    abstract fun calibrationDao(): CalibrationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: build(context).also { INSTANCE = it }
            }

        private fun build(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "lingmiao.db"
            )
            .fallbackToDestructiveMigration()
            .build()
    }
}
