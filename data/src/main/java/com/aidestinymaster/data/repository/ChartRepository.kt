package com.aidestinymaster.data.repository

import android.content.Context
import com.aidestinymaster.data.db.ChartDao
import com.aidestinymaster.data.db.ChartEntity
import com.aidestinymaster.data.db.DatabaseProvider
import java.util.UUID
import java.time.*
import com.aidestinymaster.core.astro.AstroCalculator
import com.aidestinymaster.core.lunar.LunarCalculator

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
        val id = UUID.randomUUID().toString()
        val birthDate = (input["birthDate"] as? String) ?: "2000-01-01"
        val birthTime = (input["birthTime"] as? String) ?: "12:00"
        val tzStr = (input["tz"] as? String) ?: "+00:00"
        val place = input["place"] as? String
        val tz = runCatching { ZoneOffset.of(tzStr) }.getOrElse { ZoneOffset.UTC }
        val local = LocalDateTime.parse("${birthDate}T${birthTime}:00")
        val instant = local.toInstant(tz)

        val computed = when (kind.lowercase()) {
            "natal", "astro", "astrochart" -> AstroCalculator.computeSummary(instant, 25.04, 121.56) // 台北作為預設
            "almanac", "lunar" -> LunarCalculator.computeLunarSummary(local.toLocalDate())
            else -> AstroCalculator.computeSummary(instant, 25.04, 121.56)
        }
        val entity = ChartEntity(
            id = id,
            kind = kind,
            birthDate = birthDate,
            birthTime = birthTime,
            tz = tzStr,
            place = place,
            computedJson = computed,
            snapshotJson = computed
        )
        dao.upsert(entity)
        return id
    }

    suspend fun getComputed(id: String): ChartEntity? = dao.getById(id)

    companion object {
        fun from(context: Context): ChartRepository = ChartRepository(DatabaseProvider.get(context).chartDao())
    }
}
