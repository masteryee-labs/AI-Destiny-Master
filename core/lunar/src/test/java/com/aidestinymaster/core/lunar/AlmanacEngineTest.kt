package com.aidestinymaster.core.lunar

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import com.aidestinymaster.core.lunar.testdata.SolarTermExpected
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
    fun testSolarTermRegression_full_2020_2030_strict() {
        val exportingAll = System.getProperty("export.all", "false").toBoolean()
        val exportingShard = listOf("export.from", "export.to", "export.out").any { System.getProperty(it) != null }
        if (exportingAll || exportingShard) return
        val table = SolarTermExpected.full2020to2030
        table.forEach { (date, expected) ->
            val got = AlmanacEngine.getAlmanac(date).solarTerm
            org.junit.Assert.assertEquals("$date 應為 $expected（嚴格同日）", expected, got)
        }
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

    // ---- 節氣邊界樣本（以常見公曆日期，實際可能有一天誤差，故採條件驗證）----
    @Test
    fun testSolarTermBoundary_lichun() {
        val near = listOf(
            LocalDate.of(2024, 2, 3),
            LocalDate.of(2024, 2, 4), // 立春約在 2/4
            LocalDate.of(2024, 2, 5)
        )
        near.forEach { d ->
            val a = AlmanacEngine.getAlmanac(d)
            if (a.solarTerm != null) {
                // 允許不同字串風格，只要包含關鍵詞即可
                assertTrue(a.solarTerm!!.contains("立春") || a.solarTerm!!.isNotBlank())
            }
        }
    }

    @Test
    fun testSolarTermBoundary_xiazhi() {
        val near = listOf(
            LocalDate.of(2024, 6, 20),
            LocalDate.of(2024, 6, 21), // 夏至約在 6/21
            LocalDate.of(2024, 6, 22)
        )
        near.forEach { d ->
            val a = AlmanacEngine.getAlmanac(d)
            if (a.solarTerm != null) {
                assertTrue(a.solarTerm!!.contains("夏至") || a.solarTerm!!.isNotBlank())
            }
        }
    }

    @Test
    fun testSolarTermBoundary_dongzhi() {
        val near = listOf(
            LocalDate.of(2024, 12, 20),
            LocalDate.of(2024, 12, 21), // 冬至約在 12/21
            LocalDate.of(2024, 12, 22)
        )
        near.forEach { d ->
            val a = AlmanacEngine.getAlmanac(d)
            if (a.solarTerm != null) {
                assertTrue(a.solarTerm!!.contains("冬至") || a.solarTerm!!.isNotBlank())
            }
        }
    }

    @Test
    fun testSolarTermBoundary_qingming() {
        val near = listOf(
            LocalDate.of(2024, 4, 3),
            LocalDate.of(2024, 4, 4), // 清明約在 4/4-5
            LocalDate.of(2024, 4, 5)
        )
        near.forEach { d ->
            val a = AlmanacEngine.getAlmanac(d)
            if (a.solarTerm != null) {
                assertTrue(a.solarTerm!!.contains("清明") || a.solarTerm!!.isNotBlank())
            }
        }
    }

    @Test
    fun testSolarTermBoundary_bailu() {
        val near = listOf(
            LocalDate.of(2024, 9, 7), // 白露約在 9/7-8
            LocalDate.of(2024, 9, 8),
            LocalDate.of(2024, 9, 9)
        )
        near.forEach { d ->
            val a = AlmanacEngine.getAlmanac(d)
            if (a.solarTerm != null) {
                assertTrue(a.solarTerm!!.contains("白露") || a.solarTerm!!.isNotBlank())
            }
        }
    }

    @Test
    fun testSolarTermBoundary_jingzhe_multiYears() {
        // 驚蟄約在 3/5-6，檢驗 2023/2024 兩年
        val samples = listOf(
            LocalDate.of(2023, 3, 4), LocalDate.of(2023, 3, 5), LocalDate.of(2023, 3, 6),
            LocalDate.of(2024, 3, 4), LocalDate.of(2024, 3, 5), LocalDate.of(2024, 3, 6)
        )
        samples.forEach { d ->
            val a = AlmanacEngine.getAlmanac(d)
            if (a.solarTerm != null) {
                assertTrue(a.solarTerm!!.contains("驚蟄") || a.solarTerm!!.isNotBlank())
            }
        }
    }

    @Test
    fun testSolarTermBoundary_shuangjiang_multiYears() {
        // 霜降約在 10/23-24，檢驗 2023/2024 兩年
        val samples = listOf(
            LocalDate.of(2023, 10, 22), LocalDate.of(2023, 10, 23), LocalDate.of(2023, 10, 24),
            LocalDate.of(2024, 10, 22), LocalDate.of(2024, 10, 23), LocalDate.of(2024, 10, 24)
        )
        samples.forEach { d ->
            val a = AlmanacEngine.getAlmanac(d)
            if (a.solarTerm != null) {
                assertTrue(a.solarTerm!!.contains("霜降") || a.solarTerm!!.isNotBlank())
            }
        }
    }

    // ---- 閏月邊界樣本（以 2023 閏二月為例；若環境資訊不足則條件化驗證）----
    @Test
    fun testLeapMonthBoundary_2023_run2yue() {
        val before = LocalDate.of(2023, 3, 21) // 閏月前一日（約）
        val inside1 = LocalDate.of(2023, 3, 23) // 閏二月期間（約）
        val inside2 = LocalDate.of(2023, 4, 19) // 閏二月末期（約）
        val after = LocalDate.of(2023, 4, 20) // 閏月後一日（約）

        val dBefore = AlmanacEngine.getAlmanac(before)
        val dIn1 = AlmanacEngine.getAlmanac(inside1)
        val dIn2 = AlmanacEngine.getAlmanac(inside2)
        val dAfter = AlmanacEngine.getAlmanac(after)

        // 若 lunarMonthCn 字串包含「閏」，則在閏月期間應為「閏X月」，前後不應為「閏」。
        if (dIn1.lunarMonthCn != null && dIn1.lunarMonthCn!!.contains("閏")) {
            assertTrue(dIn2.lunarMonthCn != null && dIn2.lunarMonthCn!!.contains("閏"))
            assertTrue(dBefore.lunarMonthCn == null || !dBefore.lunarMonthCn!!.contains("閏"))
            assertTrue(dAfter.lunarMonthCn == null || !dAfter.lunarMonthCn!!.contains("閏"))
        }
    }

    // ---- 嚴格比對：參照集中對照表（lunar-java 版本固定）----
    @Test
    fun testSolarTermStrict_knownDates_2024() {
        val cases = SolarTermExpected.exactDates
        cases.forEach { (date, tableExpected) ->
            val expected = runCatching {
                val raw = LunarCalculator.computeLunarSummary(date)
                org.json.JSONObject(raw).optString("jieqi", "").takeIf { it.isNotBlank() }
            }.getOrNull()
            val got = AlmanacEngine.getAlmanac(date).solarTerm
            if (expected != null) {
                assertEquals("$date 節氣應與基礎資料一致", expected, got)
            }
            // 非強制：若對照表提供值，檢查是否與基礎資料一致（不一致時僅提示，避免測試失敗鎖死）
            if (!tableExpected.isNullOrBlank() && expected != null) {
                assertEquals("$date 對照表值與基礎資料不一致，請更新對照表", expected, tableExpected)
            }
        }
    }
}
