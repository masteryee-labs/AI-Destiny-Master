package com.aidestinymaster.data

/**
 * 簡易同步資料包：包含內容與 updatedAt（毫秒）。
 */
data class SyncPayload(
    val updatedAt: Long,
    val data: String
)

