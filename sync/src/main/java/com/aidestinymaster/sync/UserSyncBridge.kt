package com.aidestinymaster.sync

import android.content.Context
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.UserProfileEntity
import com.aidestinymaster.data.repository.UserRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserSyncBridge(private val context: Context) {
    private val repo = UserRepository.from(context)
    private val sync = SyncManager(context)
    private val gson = Gson()

    suspend fun push() = withContext(Dispatchers.IO) {
        val entity = repo.get() ?: repo.ensure()
        sync.syncUp("user/me.json", gson.toJson(entity), encrypt = true)
    }

    suspend fun pull() = withContext(Dispatchers.IO) {
        val json = sync.syncDown("user/me.json", decrypt = true) ?: return@withContext
        runCatching { gson.fromJson(json, UserProfileEntity::class.java) }.getOrNull()?.let {
            DatabaseProvider.get(context).userProfileDao().upsert(it)
        }
    }
}

