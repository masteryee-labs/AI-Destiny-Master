package com.aidestinymaster.core.lunar

import org.json.JSONObject
import java.time.LocalDate

object LunarCalculator {
    fun computeLunarSummary(date: LocalDate): String {
        return runCatching {
            val solarCls = Class.forName("com.nlf.calendar.Solar")
            val fromYmd = solarCls.getMethod("fromYmd", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            val solar = fromYmd.invoke(null, date.year, date.monthValue, date.dayOfMonth)
            val lunar = solar.javaClass.getMethod("getLunar").invoke(solar)
            val yearGz = lunar.javaClass.getMethod("getYearInGanZhi").invoke(lunar) as String
            val monthCn = lunar.javaClass.getMethod("getMonthInChinese").invoke(lunar) as String
            val dayCn = lunar.javaClass.getMethod("getDayInChinese").invoke(lunar) as String
            val jq = runCatching { lunar.javaClass.getMethod("getJieQi").invoke(lunar) as String? }.getOrNull()
            JSONObject()
                .put("date", date.toString())
                .put("ganzhiYear", yearGz)
                .put("lunarMonth", monthCn)
                .put("lunarDay", dayCn)
                .put("jieqi", jq)
                .toString()
        }.getOrElse {
            JSONObject().put("date", date.toString()).put("error", it.message ?: "lunar-java missing").toString()
        }
    }
}
