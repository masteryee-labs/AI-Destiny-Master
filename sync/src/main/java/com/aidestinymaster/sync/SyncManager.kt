package com.aidestinymaster.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context) {
    private val drive = DriveService(context)

    suspend fun syncUp(name: String, json: String, encrypt: Boolean = true) =
        withContext(Dispatchers.IO) {
            drive.uploadJson(name, json, encrypt)
        }

    suspend fun syncDown(name: String, decrypt: Boolean = true): String? =
        withContext(Dispatchers.IO) {
            drive.downloadJson(name, decrypt)
        }
}

