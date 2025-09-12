package com.aidestinymaster.features.mix_ai

import com.aidestinymaster.features.design.DesignSummary
import java.util.Locale
import com.aidestinymaster.features.ziwei.ZiweiSummary
import com.aidestinymaster.features.ziwei.toMixedSnippet

object PromptBuilder {
    /**
     * 將各模組摘要合併成單一字串，用於 LLM 輸入。
     * 目前先支援 DesignSummary，其它模組日後接入時擴充此函式即可。
     */
    fun buildPrompt(mixed: MixedSummary, locale: Locale = Locale.TAIWAN): String {
        val sb = StringBuilder()
        when (locale.language.lowercase(Locale.ROOT)) {
            "zh" -> sb.appendLine("你是命理顧問。以下是使用者的盤面摘要，請以溫和中性的語氣提供：1) 關鍵特質（條列），2) 現階段機會與風險（事業/情感/健康/財務），3) 三項具體可執行建議，4) 應避免的決策陷阱（簡述）。")
            else -> sb.appendLine("You are a destiny advisor. Given the user's chart summaries, provide: 1) key traits (bulleted), 2) opportunities and risks (career/relationship/health/finance), 3) three actionable tips, 4) pitfalls to avoid (brief).")
        }
        sb.appendLine()
        sb.appendLine("# DATA START")

        mixed.design?.let { d ->
            sb.appendLine("[Design]")
            sb.appendLine("brief: ${d.brief}")
            sb.appendLine("highlights:")
            d.highlights.forEach { h -> sb.appendLine("- $h") }
            if (d.stats.isNotEmpty()) {
                sb.appendLine("stats:")
                d.stats.forEach { (k, v) -> sb.appendLine("- $k: $v") }
            }
            sb.appendLine()
        }

        mixed.ziwei?.let { z ->
            sb.appendLine(z.toMixedSnippet())
            sb.appendLine()
        }

        mixed.iching?.let { i ->
            sb.appendLine("[Iching]")
            sb.appendLine(i)
            sb.appendLine()
        }

        mixed.natal?.let { n ->
            sb.appendLine("[Natal]")
            if (n.stub.isNotBlank()) sb.appendLine(n.stub)
            sb.appendLine()
        }

        mixed.bazi?.let { b ->
            if (b.stub.isNotBlank()) {
                sb.appendLine("[BaZi]")
                sb.appendLine("summary:")
                val lines = b.stub.lines().filter { it.isNotBlank() }.take(10)
                lines.forEach { line ->
                    val cut = if (line.length > 120) line.substring(0, 120) + "…" else line
                    sb.appendLine("- $cut")
                }
                sb.appendLine()
            }
        }

        mixed.almanac?.let { a ->
            if (a.isNotBlank()) {
                sb.appendLine("[Almanac]")
                val lines = a.lines().filter { it.isNotBlank() }.take(10)
                lines.forEach { line ->
                    // 將 key: value 轉為條列，否則原樣附上
                    val idx = line.indexOf(": ")
                    val raw = if (idx > 0) {
                        val key = line.substring(0, idx).trim()
                        val value = line.substring(idx + 2).trim()
                        "$key: $value"
                    } else line
                    val cut = if (raw.length > 120) raw.substring(0, 120) + "…" else raw
                    sb.appendLine("- $cut")
                }
                sb.appendLine()
            }
        }

        // TODO: attach BaZi, Almanac when available

        sb.appendLine("# DATA END")
        return sb.toString()
    }
}
