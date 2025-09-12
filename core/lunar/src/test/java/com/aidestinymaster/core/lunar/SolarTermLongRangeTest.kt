package com.aidestinymaster.core.lunar

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.LocalDate

/**
 * Long-range regression that prefers reading pre-generated shards under
 * core/lunar/src/test/resources/solar_terms_YYYY_YYYY.json
 *
 * This test samples entries from each existing shard file and asserts that
 * AlmanacEngine returns the exact same solar-term name on the same date.
 *
 * It does NOT dynamically generate shards to avoid long runtime on CI.
 */
class SolarTermLongRangeTest {

    private fun readShard(file: File): Map<LocalDate, String> {
        val text = file.readText(StandardCharsets.UTF_8)
        // Very small and safe JSON parsing without external libs:
        // Expect a flat object: { "YYYY-MM-DD": "節氣", ... }
        val map = linkedMapOf<LocalDate, String>()
        val regex = Regex(""""(\d{4}-\d{2}-\d{2})"\s*:\s*"([^"]+)"""")
        regex.findAll(text).forEach { m ->
            val date = LocalDate.parse(m.groupValues[1])
            val name = m.groupValues[2]
            map[date] = name
        }
        return map
    }

    private fun sampleDates(entries: List<Map.Entry<LocalDate, String>>): List<Map.Entry<LocalDate, String>> {
        if (entries.isEmpty()) return emptyList()
        val out = mutableListOf<Map.Entry<LocalDate, String>>()
        val n = entries.size
        val picks = listOf(0, n / 5, 2 * n / 5, 3 * n / 5, 4 * n / 5, n - 1)
            .toSet().filter { it in 0 until n }.sorted()
        picks.forEach { idx -> out += entries[idx] }
        return out
    }

    @Test
    fun testLongRange_shardSampling_0100_9999_ifAvailable() {
        if (!System.getProperty("longRange.enabled", "false").toBoolean()) return
        val base = File("core/lunar/src/test/resources")
        if (!base.exists()) return
        val shardFiles = base.listFiles { f -> f.isFile && f.name.matches(Regex("solar_terms_\\d{4}_\\d{4}\\.json")) }
            ?.sortedBy { it.name } ?: return
        // Sample at most 20 shards to keep runtime modest; prefer wider coverage
        val sampledShards = if (shardFiles.size > 20) {
            val step = (shardFiles.size - 1).coerceAtLeast(1) / 19 + 1
            shardFiles.filterIndexed { idx, _ -> idx % step == 0 }
        } else shardFiles

        for (file in sampledShards) {
            val data = readShard(file)
            val entries = data.entries.sortedBy { it.key }
            val samples = sampleDates(entries)
            for (e in samples) {
                val got = AlmanacEngine.getAlmanac(e.key).solarTerm
                assertEquals("${e.key} from ${file.name}", e.value, got)
            }
        }
    }
}
