package com.aidestinymaster.sync

import android.content.Context
import com.aidestinymaster.data.LocalSyncStore
import com.aidestinymaster.data.SyncPayload
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncManager(private val context: Context) {
    private val drive = DriveService(context)
    private val gson = Gson()

    private fun parsePayload(text: String?): SyncPayload? =
        runCatching { if (text == null) null else gson.fromJson(text, SyncPayload::class.java) }.getOrNull()

    private fun encodePayload(p: SyncPayload): String = gson.toJson(p)

    /**
     * 將本機資料上傳至 Drive App Folder（以 updatedAt 較新為準）。
     */
    suspend fun syncUp(name: String, json: String, encrypt: Boolean = true) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val local = SyncPayload(updatedAt = now, data = json)

        val remoteText = drive.downloadJson(name, decrypt = encrypt)
        val remote = parsePayload(remoteText)

        val shouldUpload = remote == null || local.updatedAt >= remote.updatedAt
        if (shouldUpload) {
            drive.uploadJson(name, encodePayload(local), encrypt)
            LocalSyncStore.write(context, name, local, ::encodePayload)
        }
    }

    /**
     * 從 Drive 下載資料，若遠端較新則覆蓋本機。
     * 回傳本機最新（可能為遠端覆蓋後）的純資料 JSON。
     */
    suspend fun syncDown(name: String, decrypt: Boolean = true): String? = withContext(Dispatchers.IO) {
        val remoteText = drive.downloadJson(name, decrypt)
        val remote = parsePayload(remoteText)
        val local = LocalSyncStore.read(context, name) { parsePayload(it) }

        val winner = when {
            remote == null && local == null -> null
            remote == null -> local
            local == null -> remote
            remote.updatedAt > local.updatedAt -> remote
            else -> local
        }

        if (winner != null) {
            LocalSyncStore.write(context, name, winner, ::encodePayload)
        }
        winner?.data
    }
}
