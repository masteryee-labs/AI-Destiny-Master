package com.aidestinymaster.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    suspend fun get(): UserProfileEntity?

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}

