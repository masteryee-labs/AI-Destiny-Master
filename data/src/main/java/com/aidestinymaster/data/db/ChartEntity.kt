package com.aidestinymaster.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "charts")
data class ChartEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val birthDate: String,
    val birthTime: String?,
    val tz: String,
    val place: String?,
    val computedJson: String,
    val snapshotJson: String
)

