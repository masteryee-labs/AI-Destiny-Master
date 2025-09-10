package com.aidestinymaster.data.repository

import android.content.Context
import com.aidestinymaster.data.db.ChartDao
import com.aidestinymaster.data.db.ChartEntity
import com.aidestinymaster.data.db.DatabaseProvider
import java.util.UUID

class ChartRepository private constructor(private val dao: ChartDao) {
    suspend fun create(kind: String, input: Map<String, Any?>): String {
        val id = UUID.randomUUID().toString()
        val nowJson = (input["computedJson"] as? String) ?: "{}"
        val entity = ChartEntity(
            id = id,
            kind = kind,
            birthDate = (input["birthDate"] as? String) ?: "",
            birthTime = input["birthTime"] as? String,
            tz = (input["tz"] as? String) ?: "+00:00",
            place = input["place"] as? String,
            computedJson = nowJson,
            snapshotJson = nowJson
        )
        dao.upsert(entity)
        return id
    }

    suspend fun compute(kind: String, input: Map<String, Any?>): String {
        // TODO: 接入實際引擎（astro/lunar）。此處先回寫 snapshotJson = computedJson
        val id = create(kind, input)
        return id
    }

    suspend fun getComputed(id: String): ChartEntity? = dao.getById(id)

    companion object {
        fun from(context: Context): ChartRepository = ChartRepository(DatabaseProvider.get(context).chartDao())
    }
}

