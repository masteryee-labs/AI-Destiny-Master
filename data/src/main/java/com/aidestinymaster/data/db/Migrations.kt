package com.aidestinymaster.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 集中管理 Room Migrations。現階段版本為 1，預留未來升級用。
 */
object Migrations {
    val all: Array<Migration> = arrayOf(
        // 範例：從 1 → 2 新增欄位
        // object : Migration(1, 2) {
        //     override fun migrate(db: SupportSQLiteDatabase) {
        //         db.execSQL("ALTER TABLE reports ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        //     }
        // }
    )
}

