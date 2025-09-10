package com.aidestinymaster.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet")
data class WalletEntity(
    @PrimaryKey val id: String = "wallet",
    val coins: Int,
    val lastEarnedAt: Long?,
    val lastSpentAt: Long?
)

