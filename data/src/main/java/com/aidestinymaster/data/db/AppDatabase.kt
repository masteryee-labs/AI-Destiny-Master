package com.aidestinymaster.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReportEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
}

