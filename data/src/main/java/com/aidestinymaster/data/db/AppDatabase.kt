package com.aidestinymaster.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReportEntity::class,
        ChartEntity::class,
        WalletEntity::class,
        PurchaseEntity::class,
        UserProfileEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun chartDao(): ChartDao
    abstract fun walletDao(): WalletDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun userProfileDao(): UserProfileDao
}
