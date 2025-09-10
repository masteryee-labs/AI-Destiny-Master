package com.aidestinymaster.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ChartDao {
    @Upsert
    suspend fun upsert(chart: ChartEntity)

    @Query("SELECT * FROM charts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ChartEntity?

    @Query("SELECT id FROM charts")
    suspend fun listIds(): List<String>
}
