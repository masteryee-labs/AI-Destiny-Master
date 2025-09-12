package com.aidestinymaster.core.lunar

import com.aidestinymaster.core.lunar.testdata.SolarTermExpected
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * One-off generator to export 2020–2030 solar term table into test resources.
 * Controlled by system property: -Dgenerate.solar.terms=true
 */
class SolarTermGenerateTest {
    @Test
    fun generateIfRequested() {
        val enabled = System.getProperty("generate.solar.terms", "false").toBoolean()
        if (!enabled) return
        try {
            val exportAll = System.getProperty("export.all")?.toBoolean() == true
            if (exportAll) {
                var start = 100
                while (start <= 9900) {
                    val end = start + 99
                    val map = SolarTermExpected.generate(start, end)
                    val json = SolarTermExpected.toJson(map)
                    val outRel = "core/lunar/src/test/resources/solar_terms_${'$'}start_${'$'}end.json"
                    val out = File(outRel)
                    out.parentFile?.mkdirs()
                    out.writeText(json, StandardCharsets.UTF_8)
                    println("[SolarTermGenerateTest] Wrote ${out.absolutePath} (${out.length()} bytes) range=${'$'}start..${'$'}end")
                    start += 100
                }
            } else {
                val from = System.getProperty("export.from")?.toIntOrNull() ?: 2020
                val to = System.getProperty("export.to")?.toIntOrNull() ?: 2030
                val outOverride = System.getProperty("export.out")
                val map = SolarTermExpected.generate(from, to)
                val json = SolarTermExpected.toJson(map)
                // Prefer project-root relative path
                val defaultRel = "core/lunar/src/test/resources/solar_terms_${'$'}from_${'$'}to.json"
                val rootRel = File(outOverride ?: defaultRel)
                val moduleRel = if (outOverride != null) File(outOverride) else File("src/test/resources/solar_terms_${'$'}from_${'$'}to.json")
                val outPath = if (rootRel.parentFile?.exists() == true || rootRel.parentFile?.mkdirs() == true) rootRel else moduleRel
                outPath.parentFile?.mkdirs()
                outPath.writeText(json, StandardCharsets.UTF_8)
                println("[SolarTermGenerateTest] Wrote ${outPath.absolutePath} (${outPath.length()} bytes) for range ${'$'}from..${'$'}to")
                println("[SolarTermGenerateTest] BEGIN_JSON")
                println(json)
                println("[SolarTermGenerateTest] END_JSON")
            }
        } catch (t: Throwable) {
            // Do not fail the suite; only log
            println("[SolarTermGenerateTest] Generation failed: ${t.message}")
        }
    }
}
