package com.aidestinymaster.sync

import android.content.Context
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.PurchaseEntity
import com.aidestinymaster.data.repository.PurchaseRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PurchaseSyncBridge(private val context: Context) {
    private val repo = PurchaseRepository.from(context)
    private val sync = SyncManager(context)
    private val gson = Gson()

    suspend fun push(sku: String) = withContext(Dispatchers.IO) {
        val entity = DatabaseProvider.get(context).purchaseDao().getBySku(sku) ?: return@withContext
        sync.syncUp("purchases/${sku}.json", gson.toJson(entity), encrypt = true)
    }

    suspend fun pull(sku: String) = withContext(Dispatchers.IO) {
        val json = sync.syncDown("purchases/${sku}.json", decrypt = true) ?: return@withContext
        runCatching { gson.fromJson(json, PurchaseEntity::class.java) }.getOrNull()?.let {
            DatabaseProvider.get(context).purchaseDao().upsert(it)
        }
    }
}

