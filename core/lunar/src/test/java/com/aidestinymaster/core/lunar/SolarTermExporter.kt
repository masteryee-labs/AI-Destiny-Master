package com.aidestinymaster.core.lunar

import com.aidestinymaster.core.lunar.testdata.SolarTermExpected
import java.io.File
import java.nio.charset.StandardCharsets

object SolarTermExporter {
    @JvmStatic
    fun main(args: Array<String>) {
        val from = args.getOrNull(0)?.toIntOrNull() ?: 2020
        val to = args.getOrNull(1)?.toIntOrNull() ?: 2030
        val outArg = args.getOrNull(2)
        val map = SolarTermExpected.generate(from, to)
        val json = SolarTermExpected.toJson(map)
        val outPath = outArg?.let { File(it) }
            ?: File("core/lunar/src/test/resources/solar_terms_2020_2030.json")
        outPath.parentFile?.mkdirs()
        outPath.writeText(json, StandardCharsets.UTF_8)
        println("[SolarTermExporter] Wrote ${outPath.absolutePath} (${outPath.length()} bytes), range=$from..$to")
    }
}
