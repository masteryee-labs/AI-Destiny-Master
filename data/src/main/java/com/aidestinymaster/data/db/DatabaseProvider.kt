package com.aidestinymaster.data.db

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "aidestiny.db"
            )
                .addMigrations(*Migrations.all)
                .build().also { instance = it }
        }
}
