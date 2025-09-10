package com.aidestinymaster.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 集中管理 Room Migrations。現階段版本為 1，預留未來升級用。
 */
object Migrations {
    val all: Array<Migration> = arrayOf(
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS charts (id TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, birthDate TEXT NOT NULL, birthTime TEXT, tz TEXT NOT NULL, place TEXT, computedJson TEXT NOT NULL, snapshotJson TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS wallet (id TEXT NOT NULL PRIMARY KEY, coins INTEGER NOT NULL, lastEarnedAt INTEGER, lastSpentAt INTEGER)")
                db.execSQL("CREATE TABLE IF NOT EXISTS purchases (sku TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, state INTEGER NOT NULL, purchaseToken TEXT NOT NULL, acknowledged INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS user_profile (id TEXT NOT NULL PRIMARY KEY, name TEXT, lang TEXT NOT NULL, theme TEXT NOT NULL, syncEnabled INTEGER NOT NULL)")
            }
        }
    )
}
