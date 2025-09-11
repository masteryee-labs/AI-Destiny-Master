package com.aidestinymaster.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before

open class DataDbTestBase {
    protected lateinit var db: AppDatabase

    @Before
    fun setUpDb() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDownDb() {
        if (this::db.isInitialized) {
            db.close()
        }
    }
}
