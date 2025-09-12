package com.aidestinymaster.data.repository

import android.content.Context
import com.aidestinymaster.data.db.AppDatabase
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.ReportDao
import com.aidestinymaster.data.db.ReportEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ReportRepository private constructor(private val dao: ReportDao) {

    fun getReportFlow(id: String): Flow<ReportEntity?> = dao.observeById(id)

    suspend fun getOnce(id: String): ReportEntity? = dao.getById(id)

    suspend fun upsert(entity: ReportEntity) = dao.upsert(entity)

    suspend fun createFromAi(type: String, chartId: String, content: String): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val title = generateTitle(type, now)
        val summary = summarize(content)
        val entity = ReportEntity(
            id = id,
            type = type,
            title = title,
            createdAt = now,
            updatedAt = now,
            summary = summary,
            contentEnc = content,
            chartRef = chartId
        )
        dao.upsert(entity)
        return id
    }

    private fun generateTitle(type: String, timestamp: Long): String =
        "${type.uppercase()} Report #${timestamp}"

    private fun summarize(content: String, maxLen: Int = 120): String =
        content.replace("\n", " ").take(maxLen)

    suspend fun delete(id: String) = dao.deleteById(id)
    suspend fun listIds(): List<String> = dao.listIds()

    companion object {
        fun from(context: Context): ReportRepository {
            val db: AppDatabase = DatabaseProvider.get(context)
            return ReportRepository(db.reportDao())
        }
    }
}

