package com.aidestinymaster.data

import android.content.Context
import java.io.File

/**
 * 以 App 內部儲存保存每個名稱對應的 SyncPayload（JSON 字串由上層處理）。
 */
object LocalSyncStore {
    private fun dir(context: Context): File = File(context.filesDir, "sync").apply { mkdirs() }

    private fun file(context: Context, name: String): File = File(dir(context), "$name.json")

    fun read(context: Context, name: String, decode: (String) -> SyncPayload?): SyncPayload? {
        val f = file(context, name)
        if (!f.exists()) return null
        return runCatching { decode(f.readText(Charsets.UTF_8)) }.getOrNull()
    }

    fun write(context: Context, name: String, payload: SyncPayload, encode: (SyncPayload) -> String) {
        val f = file(context, name)
        f.writeText(encode(payload), Charsets.UTF_8)
    }
}

