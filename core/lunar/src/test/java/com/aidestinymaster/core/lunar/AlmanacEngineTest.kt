package com.aidestinymaster.core.lunar

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AlmanacEngineTest {

    @Test
    fun testGetAlmanac_basicFieldsAndYiJi() {
        val date = LocalDate.of(2024, 4, 4) // 附近為清明，僅測結構
        val day = AlmanacEngine.getAlmanac(date)
        assertTrue(day.date == date)
        // 結構與最小內容（允許在缺少 lunar-java 時為 null）
        // 主要驗證 Yi/Ji 的產生與函式穩定
        // 宜/忌至少各有一項（以簡易規則生成）
        assertTrue(day.yi.isNotEmpty())
        assertTrue(day.ji.isNotEmpty())
        // 生肖可由 lunar-java 反射取得，若不可用不做強制
    }

    @Test
    fun testZodiacForecast_stub() {
        val z = AlmanacEngine.getZodiacForecast(2025, "龍")
        assertTrue(z.year == 2025)
        assertTrue(z.animal == "龍")
        assertTrue(z.summary.isNotBlank())
        assertTrue(z.advice.size >= 3)
    }

    @Test
    fun testYiJi_withSolarTerm_dayHasRitualPreferred() {
        // 2024-04-04 傳統為清明（具節氣）。若環境無 lunar-java，solarTerm 可能為 null，則不強制此測試條件。
        val d = AlmanacEngine.getAlmanac(LocalDate.of(2024, 4, 4))
        if (d.solarTerm != null) {
            // 有節氣時，Yi 應偏向 祭祀/拜訪；Ji 避免 動土/嫁娶
            assertTrue(d.yi.contains("祭祀"))
            assertTrue(d.ji.contains("動土"))
        }
    }

    @Test
    fun testYiJi_noSolarTerm_dayHasStudyPreferred() {
        // 選擇一般日；若誤中節氣，則不強制條件。
        val d = AlmanacEngine.getAlmanac(LocalDate.of(2024, 4, 6))
        if (d.solarTerm == null) {
            assertTrue(d.yi.contains("學習"))
            assertTrue(d.ji.contains("爭執"))
        }
    }
}
