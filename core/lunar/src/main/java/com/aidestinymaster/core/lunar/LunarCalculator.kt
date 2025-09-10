package com.aidestinymaster.core.lunar

import org.json.JSONObject
import java.time.LocalDate

object LunarCalculator {
    fun computeLunarSummary(date: LocalDate): String {
        // TODO: 接入 lunar-java，輸出農曆/干支/節氣等摘要
        val obj = JSONObject()
            .put("date", date.toString())
            .put("lunar", JSONObject().put("year", "庚子").put("month", 1).put("day", 1))
            .put("ganzhi", "庚子年 甲子月 甲子日")
        return obj.toString()
    }
}

