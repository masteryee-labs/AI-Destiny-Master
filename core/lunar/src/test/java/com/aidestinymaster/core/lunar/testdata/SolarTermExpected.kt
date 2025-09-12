package com.aidestinymaster.core.lunar.testdata

import java.time.LocalDate
import com.aidestinymaster.core.lunar.AlmanacEngine
import com.nlf.calendar.Solar
import org.json.JSONObject
import java.io.InputStream

/**
 * Centralized expected solar-term (節氣) dates for regression tests.
 *
 * Source: curated for the currently pinned lunar-java version; update when upgrading dependency.
 * Only includes a subset of well-known terms sufficient for our tests; extend as needed.
 */
object SolarTermExpected {
    // Curated subset of exact dates -> solar-term name (中文)
    val exactDates: Map<LocalDate, String> = mapOf(
        // 2023
        LocalDate.of(2023, 3, 6) to "驚蟄",
        LocalDate.of(2023, 4, 5) to "清明",
        LocalDate.of(2023, 6, 21) to "夏至",
        LocalDate.of(2023, 9, 8) to "白露",
        LocalDate.of(2023, 12, 22) to "冬至",
        // 2024
        LocalDate.of(2024, 2, 4) to "立春",
        LocalDate.of(2024, 4, 4) to "清明",
        LocalDate.of(2024, 6, 21) to "夏至",
        LocalDate.of(2024, 9, 7) to "白露",
        LocalDate.of(2024, 12, 21) to "冬至",
        // 2025
        LocalDate.of(2025, 3, 5) to "驚蟄",
        LocalDate.of(2025, 4, 5) to "清明",
        LocalDate.of(2025, 6, 21) to "夏至",
        LocalDate.of(2025, 9, 7) to "白露",
        LocalDate.of(2025, 12, 21) to "冬至",
    )

    /**
     * Auto-generate full-year solar-term days for a given range using the pinned lunar-java via LunarCalculator.
     * Returns a map of LocalDate -> jieqi name.
     */
    fun generate(fromYear: Int, toYearInclusive: Int): Map<LocalDate, String> {
        val useFast = System.getProperty("export.all", "false").toBoolean()
        val out = linkedMapOf<LocalDate, String>()
        for (y in fromYear..toYearInclusive) {
            if (useFast) {
                try {
                    val anchor = com.nlf.calendar.Solar.fromYmd(y, 7, 1)
                    val lunar = anchor.lunar
                    val table = lunar.jieQiTable // Map<String, Solar>
                    for ((name, s) in table) {
                        if (s.year == y) {
                            val d = LocalDate.of(s.year, s.month, s.day)
                            out[d] = name
                        }
                    }
                } catch (_: Throwable) {
                    // If fast path fails, fall back to per-day strict
                    var d = LocalDate.of(y, 1, 1)
                    val end = LocalDate.of(y, 12, 31)
                    while (!d.isAfter(end)) {
                        val jq = try { AlmanacEngine.getAlmanac(d).solarTerm } catch (_: Throwable) { null }
                        if (!jq.isNullOrBlank()) out[d] = jq!!
                        d = d.plusDays(1)
                    }
                }
            } else {
                // Strict per-day generation to match AlmanacEngine exactly (used in tests & small shards)
                var d = LocalDate.of(y, 1, 1)
                val end = LocalDate.of(y, 12, 31)
                while (!d.isAfter(end)) {
                    val jq = try { AlmanacEngine.getAlmanac(d).solarTerm } catch (_: Throwable) { null }
                    if (!jq.isNullOrBlank()) out[d] = jq!!
                    d = d.plusDays(1)
                }
            }
        }
        return out.toSortedMap()
    }

    private fun loadResourceJson(name: String): Map<LocalDate, String>? {
        return try {
            val path = "/$name"
            val ins: InputStream? = this::class.java.getResourceAsStream(path)
            if (ins != null) {
                val text = ins.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(text)
                val out = linkedMapOf<LocalDate, String>()
                json.keys().forEach { k ->
                    val v = json.optString(k, "")
                    if (v.isNotBlank()) out[LocalDate.parse(k)] = v
                }
                out
            } else null
        } catch (_: Throwable) { null }
    }

    /**
     * Load from classpath resource first, fallback to dynamic generation.
     */
    fun loadOrGenerate(fromYear: Int, toYearInclusive: Int, resource: String): Map<LocalDate, String> {
        val res = loadResourceJson(resource)
        if (res != null && res.isNotEmpty()) return res
        val gen = generate(fromYear, toYearInclusive)
        // Persist to resource path to stabilize CI (best-effort, ignore errors)
        runCatching {
            val json = toJson(gen)
            val rootRel = java.io.File("core/lunar/src/test/resources/$resource")
            val moduleRel = java.io.File("src/test/resources/$resource")
            val out = if (rootRel.parentFile?.exists() == true || rootRel.parentFile?.mkdirs() == true) rootRel else moduleRel
            out.parentFile?.mkdirs()
            out.writeText(json, Charsets.UTF_8)
        }
        return gen
    }

    // Pre-generated full table for regression: 2020–2030 from resource (if provided) else generated at runtime
    val full2020to2030: Map<LocalDate, String> by lazy {
        loadOrGenerate(2020, 2030, "solar_terms_2020_2030.json")
    }

    /** Helper to export a generated table to JSON (manual composer to avoid org.json in test runtime). */
    fun toJson(map: Map<LocalDate, String>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        val entries = map.entries.sortedBy { it.key }
        entries.forEachIndexed { idx, e ->
            val key = e.key.toString()
            val value = e.value.replace("\\", "\\\\").replace("\"", "\\\"")
            sb.append("  \"").append(key).append("\": \"").append(value).append("\"")
            if (idx != entries.lastIndex) sb.append(",")
            sb.append("\n")
        }
        sb.append("}\n")
        return sb.toString()
    }
}
