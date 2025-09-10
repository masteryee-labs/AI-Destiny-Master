package com.aidestinymaster.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey val sku: String,
    val type: String,
    val state: Int,
    val purchaseToken: String,
    val acknowledged: Boolean,
    val updatedAt: Long
)

