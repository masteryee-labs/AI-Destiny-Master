package com.aidestinymaster.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Upsert
    suspend fun upsert(report: ReportEntity)

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReportEntity?

    @Query("SELECT * FROM reports WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ReportEntity?>

    @Query("SELECT * FROM reports ORDER BY updatedAt DESC LIMIT :limit")
    fun listRecent(limit: Int): Flow<List<ReportEntity>>

    @Query("SELECT * FROM reports WHERE title LIKE :kw OR summary LIKE :kw ORDER BY updatedAt DESC")
    fun search(kw: String): Flow<List<ReportEntity>>

    @Query("SELECT id FROM reports")
    suspend fun listIds(): List<String>

    @Query("DELETE FROM reports WHERE id = :id")
    suspend fun deleteById(id: String)
}
