package com.aidestinymaster.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val name: String?,
    val lang: String,
    val theme: String,
    val syncEnabled: Boolean
)

