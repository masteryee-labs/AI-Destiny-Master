package com.aidestinymaster.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface WalletDao {
    @Upsert
    suspend fun upsert(wallet: WalletEntity)

    @Query("SELECT * FROM wallet WHERE id = 'wallet' LIMIT 1")
    suspend fun get(): WalletEntity?

    @Query("UPDATE wallet SET coins = coins + :delta, lastEarnedAt = CASE WHEN :delta > 0 THEN strftime('%s','now')*1000 ELSE lastEarnedAt END, lastSpentAt = CASE WHEN :delta < 0 THEN strftime('%s','now')*1000 ELSE lastSpentAt END WHERE id = 'wallet'")
    suspend fun updateCoins(delta: Int)

    @Query("UPDATE wallet SET coins = :value WHERE id = 'wallet'")
    suspend fun setCoins(value: Int)
}

