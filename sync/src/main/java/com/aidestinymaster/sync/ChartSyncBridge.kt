package com.aidestinymaster.sync

import android.content.Context
import com.aidestinymaster.data.db.ChartEntity
import com.aidestinymaster.data.repository.ChartRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChartSyncBridge(private val context: Context) {
    private val repo = ChartRepository.from(context)
    private val sync = SyncManager(context)
    private val gson = Gson()

    suspend fun push(id: String) = withContext(Dispatchers.IO) {
        val entity = repo.getComputed(id) ?: return@withContext
        val json = gson.toJson(entity)
        sync.syncUp("charts/${id}.json", json, encrypt = true)
    }

    suspend fun pull(id: String) = withContext(Dispatchers.IO) {
        val json = sync.syncDown("charts/${id}.json", decrypt = true) ?: return@withContext
        runCatching { gson.fromJson(json, ChartEntity::class.java) }.getOrNull()?.let {
            // Reuse repository create/upsert path via DAO (not exposed: use provider)
            com.aidestinymaster.data.db.DatabaseProvider.get(context).chartDao().upsert(it)
        }
    }
}

