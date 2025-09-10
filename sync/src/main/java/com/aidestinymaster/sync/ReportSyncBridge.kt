package com.aidestinymaster.sync

import android.content.Context
import com.aidestinymaster.data.db.ReportEntity
import com.aidestinymaster.data.repository.ReportRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReportSyncBridge(private val context: Context) {
    private val repo = ReportRepository.from(context)
    private val sync = SyncManager(context)
    private val gson = Gson()

    private data class ReportSyncData(
        val id: String,
        val type: String,
        val title: String,
        val createdAt: Long,
        val updatedAt: Long,
        val summary: String,
        val contentEnc: String,
        val chartRef: String
    )

    private fun toSyncData(e: ReportEntity) = ReportSyncData(
        id = e.id,
        type = e.type,
        title = e.title,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
        summary = e.summary,
        contentEnc = e.contentEnc,
        chartRef = e.chartRef
    )

    private fun toEntity(d: ReportSyncData) = ReportEntity(
        id = d.id,
        type = d.type,
        title = d.title,
        createdAt = d.createdAt,
        updatedAt = d.updatedAt,
        summary = d.summary,
        contentEnc = d.contentEnc,
        chartRef = d.chartRef
    )

    suspend fun push(id: String) = withContext(Dispatchers.IO) {
        val entity = repo.getOnce(id) ?: return@withContext
        val dataJson = gson.toJson(toSyncData(entity))
        val name = "reports/${id}.json"
        sync.syncUp(name, dataJson, encrypt = true)
    }

    suspend fun pull(id: String) = withContext(Dispatchers.IO) {
        val name = "reports/${id}.json"
        val dataJson = sync.syncDown(name, decrypt = true) ?: return@withContext
        runCatching { gson.fromJson(dataJson, ReportSyncData::class.java) }
            .getOrNull()?.let { repo.upsert(toEntity(it)) }
    }
}

