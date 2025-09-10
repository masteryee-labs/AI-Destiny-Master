package com.aidestinymaster.sync

import android.content.Context
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.WalletEntity
import com.aidestinymaster.data.repository.WalletRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WalletSyncBridge(private val context: Context) {
    private val repo = WalletRepository.from(context)
    private val sync = SyncManager(context)
    private val gson = Gson()

    suspend fun push() = withContext(Dispatchers.IO) {
        val entity = repo.get() ?: repo.ensure()
        sync.syncUp("wallet/wallet.json", gson.toJson(entity), encrypt = true)
    }

    suspend fun pull() = withContext(Dispatchers.IO) {
        val json = sync.syncDown("wallet/wallet.json", decrypt = true) ?: return@withContext
        runCatching { gson.fromJson(json, WalletEntity::class.java) }.getOrNull()?.let {
            DatabaseProvider.get(context).walletDao().upsert(it)
        }
    }
}

