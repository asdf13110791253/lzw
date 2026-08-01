package com.lingmiao.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [], version = 1, exportSchema = false)
abstract class LingMiaoDatabase : RoomDatabase() {
    companion object {
        @Volatile
        private var INSTANCE: LingMiaoDatabase? = null

        fun getInstance(context: Context): LingMiaoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LingMiaoDatabase::class.java,
                    "lingmiao_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
