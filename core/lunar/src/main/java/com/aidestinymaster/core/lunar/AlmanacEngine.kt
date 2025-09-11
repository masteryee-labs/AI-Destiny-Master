package com.aidestinymaster.core.lunar

import org.json.JSONObject
import java.time.LocalDate

/**
 * Almanac engine built on top of lunar-java via reflection (no direct compile dependency).
 * Provides a light-weight, offline almanac day summary and a simple zodiac forecast stub.
 */
object AlmanacEngine {

    data class AlmanacDay(
        val date: LocalDate,
        val ganzhiYear: String?,
        val lunarMonthCn: String?,
        val lunarDayCn: String?,
        val solarTerm: String?,
        val yi: List<String> = emptyList(),
        val ji: List<String> = emptyList(),
        val zodiacAnimal: String? = null
    )

    data class ZodiacForecast(
        val year: Int,
        val animal: String,
        val summary: String,
        val advice: List<String>
    )

    /**
     * Returns an AlmanacDay using lunar-java fields available via reflection and a minimal Yi/Ji heuristic.
     */
    fun getAlmanac(date: LocalDate): AlmanacDay {
        val info = runCatching { JSONObject(LunarCalculator.computeLunarSummary(date)) }.getOrNull()
        val yearGz = info?.optString("ganzhiYear", "")?.takeIf { it.isNotBlank() }
        val monthCn = info?.optString("lunarMonth", "")?.takeIf { it.isNotBlank() }
        val dayCn = info?.optString("lunarDay", "")?.takeIf { it.isNotBlank() }
        val jq = info?.optString("jieqi", "")?.takeIf { it.isNotBlank() }
        val zodiac = runCatching {
            val solarCls = Class.forName("com.nlf.calendar.Solar")
            val fromYmd = solarCls.getMethod("fromYmd", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            val solar = fromYmd.invoke(null, date.year, date.monthValue, date.dayOfMonth)
            val lunar = solar.javaClass.getMethod("getLunar").invoke(solar)
            lunar.javaClass.getMethod("getYearShengXiao").invoke(lunar) as String
        }.getOrNull()

        // Minimal Yi/Ji suggestion: if there is a solar term (節氣) today, prefer "祭祀/拜訪" as 宜，避免 "動土/嫁娶" 為 忌
        val yi = mutableListOf<String>()
        val ji = mutableListOf<String>()
        if (!jq.isNullOrBlank()) {
            yi += listOf("祭祀", "拜訪")
            ji += listOf("動土", "嫁娶")
        } else {
            yi += listOf("學習", "整理", "寫計畫")
            ji += listOf("爭執", "重大簽約")
        }

        return AlmanacDay(
            date = date,
            ganzhiYear = yearGz,
            lunarMonthCn = monthCn,
            lunarDayCn = dayCn,
            solarTerm = jq,
            yi = yi,
            ji = ji,
            zodiacAnimal = zodiac
        )
    }

    /**
     * Very lightweight zodiac forecast. This is a stub following project policy: self-authored, generic, and safe.
     */
    fun getZodiacForecast(year: Int, animal: String): ZodiacForecast {
        val cycleHint = when ((year % 12 + 12) % 12) {
            0 -> "循環重啟，適合立新目標"
            1,2 -> "持續累積，步步為營"
            3,4 -> "人際活絡，注意節奏"
            5,6 -> "學習成長，穩健前行"
            7,8 -> "機會浮現，審慎評估"
            else -> "收斂調整，強化根基"
        }
        val summary = "${animal}年 ${year}：$cycleHint。切記量力而為，以穩為先。"
        val advice = listOf(
            "維持規律作息，養精蓄銳",
            "重要決策前先列利弊清單",
            "建立每週回顧，調整節奏"
        )
        return ZodiacForecast(year, animal, summary, advice)
    }
}
