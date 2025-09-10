package com.aidestinymaster.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PurchaseDao {
    @Upsert
    suspend fun upsert(purchase: PurchaseEntity)

    @Query("SELECT * FROM purchases WHERE state = 1")
    suspend fun getActive(): List<PurchaseEntity>

    @Query("SELECT * FROM purchases WHERE sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): PurchaseEntity?
}

