package com.aidestinymaster.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val summary: String,
    val contentEnc: String,
    val chartRef: String
)

