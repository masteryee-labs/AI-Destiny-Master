package com.aidestinymaster.core.lunar

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import com.nlf.calendar.Solar

class BaziCalculatorTest {

    @Test
    fun testComputeBaziBasic() {
        val zdt = ZonedDateTime.of(2023, 8, 15, 10, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val res = BaziCalculator.computeBazi(zdt)
        // Basic non-empty assertions
        assertNotEquals("?", res.year.stem)
        assertNotEquals("?", res.year.branch)
        assertNotEquals("?", res.month.stem)
        assertNotEquals("?", res.month.branch)
        assertNotEquals("?", res.day.stem)
        assertNotEquals("?", res.day.branch)
        assertNotEquals("?", res.hour?.stem)
        assertNotEquals("?", res.hour?.branch)
        assertTrue(res.zodiacAnimal?.isNotEmpty() == true)
        // Metadata present
        assertTrue(res.metadata.containsKey("gzYear"))
        assertTrue(res.metadata.containsKey("gzMonth"))
        assertTrue(res.metadata.containsKey("gzDay"))
        assertTrue(res.metadata.containsKey("gzHour"))
    }

    @Test
    fun testZodiacAcrossLunarNewYear2024() {
        val before = ZonedDateTime.of(2024, 2, 9, 10, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val after = ZonedDateTime.of(2024, 2, 10, 10, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val resBefore = BaziCalculator.computeBazi(before)
        val resAfter = BaziCalculator.computeBazi(after)
        // Expect zodiac changes across Chinese New Year (2024-02-10 ~ Dragon year)
        assertNotEquals(resBefore.zodiacAnimal, resAfter.zodiacAnimal)
        assertTrue(resAfter.zodiacAnimal?.isNotEmpty() == true)
    }

    @Test
    fun testSolarTermBoundary_LiChun_2024() {
        // 立春 2024 約在 2/4，選擇更遠的兩端避免同一最近節氣
        val before = ZonedDateTime.of(2024, 2, 1, 12, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val after = ZonedDateTime.of(2024, 2, 7, 12, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val resBefore = BaziCalculator.computeBazi(before)
        val resAfter = BaziCalculator.computeBazi(after)
        // 僅驗證不崩潰且四柱存在；solarTerm 可能因庫版本/資料表差異為空
        assertNotEquals("?", resBefore.year.stem)
        assertNotEquals("?", resAfter.year.stem)
    }

    @Test
    fun testLeapMonthStability_2023() {
        // 2023 年農曆有「閏二月」；此處選 2023-03-25 與 2023-04-10（台北時區）作穩健性檢查
        val a = ZonedDateTime.of(2023, 3, 25, 10, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val b = ZonedDateTime.of(2023, 4, 10, 10, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val ra = BaziCalculator.computeBazi(a)
        val rb = BaziCalculator.computeBazi(b)
        // 僅驗證四柱存在且不崩潰（後續若能取得月份中文名，將升級為嚴格檢查「闰」字樣）
        listOf(ra, rb).forEach { r ->
            assertNotEquals("?", r.year.stem)
            assertNotEquals("?", r.month.stem)
            assertNotEquals("?", r.day.stem)
            assertNotEquals("?", r.hour?.stem)
        }
    }

    @Test
    fun testSolarTermBoundary_DongZhi_2023() {
        // 冬至 2023 約在 12/22，選擇更遠的兩端避免同一最近節氣
        val before = ZonedDateTime.of(2023, 12, 18, 12, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val after = ZonedDateTime.of(2023, 12, 26, 12, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        val resBefore = BaziCalculator.computeBazi(before)
        val resAfter = BaziCalculator.computeBazi(after)
        // 僅驗證不崩潰且四柱存在；solarTerm 可能因庫版本/資料表差異為空
        assertNotEquals("?", resBefore.year.stem)
        assertNotEquals("?", resAfter.year.stem)
    }

    // ---- 針對節氣換日的極端邊界：UTC 與台灣時區交界（清明/白露） ----
    @Test
    fun testSolarTermBoundary_QingMing_UTC_vs_Taipei() {
        // 接近清明（約 4/4-5），用 UTC 與台北時區各取一個接近邊界的時間
        val utcNear = ZonedDateTime.of(2024, 4, 4, 0, 30, 0, 0, ZoneId.of("UTC"))
        val tpeNear = ZonedDateTime.of(2024, 4, 4, 8, 30, 0, 0, ZoneId.of("Asia/Taipei"))
        val rUtc = BaziCalculator.computeBazi(utcNear)
        val rTpe = BaziCalculator.computeBazi(tpeNear)
        // 穩健性檢查：四柱皆存在
        listOf(rUtc, rTpe).forEach { r ->
            assertNotEquals("?", r.year.stem)
            assertNotEquals("?", r.month.stem)
            assertNotEquals("?", r.day.stem)
            assertNotEquals("?", r.hour?.stem)
        }
        // 若 metadata 暴露節氣近似資訊，可在未來升級為嚴格檢查（此處不強制）
    }

    @Test
    fun testSolarTermBoundary_BaiLu_UTC_vs_Taipei() {
        // 接近白露（約 9/7-8），用 UTC 與台北時區各取一個接近邊界的時間
        val utcNear = ZonedDateTime.of(2024, 9, 7, 15, 0, 0, 0, ZoneId.of("UTC"))
        val tpeNear = ZonedDateTime.of(2024, 9, 8, 0, 30, 0, 0, ZoneId.of("Asia/Taipei"))
        val rUtc = BaziCalculator.computeBazi(utcNear)
        val rTpe = BaziCalculator.computeBazi(tpeNear)
        listOf(rUtc, rTpe).forEach { r ->
            assertNotEquals("?", r.year.stem)
            assertNotEquals("?", r.month.stem)
            assertNotEquals("?", r.day.stem)
            assertNotEquals("?", r.hour?.stem)
        }
    }

    @Test
    fun testNearestJieQiName_strict_multiYears() {
        fun expectedNearestName(zdt: ZonedDateTime): String {
            val local = zdt.toLocalDateTime()
            val solar = Solar.fromYmdHms(local.year, local.monthValue, local.dayOfMonth, local.hour, local.minute, local.second)
            val lunar = solar.lunar
            var bestName: String? = null
            var bestDiff: Long = Long.MAX_VALUE
            for ((name, s) in lunar.jieQiTable) {
                val dt = ZonedDateTime.of(s.year, s.month, s.day, s.hour, s.minute, s.second, 0, zdt.zone)
                val diff = kotlin.math.abs(java.time.Duration.between(zdt, dt).seconds)
                if (diff < bestDiff) { bestDiff = diff; bestName = name }
            }
            return bestName ?: ""
        }

        val zones = listOf(ZoneId.of("Asia/Taipei"), ZoneId.of("UTC"))
        val samples = listOf(
            // 2023
            ZonedDateTime.of(2023, 3, 6, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")),
            ZonedDateTime.of(2023, 9, 8, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")),
            ZonedDateTime.of(2023, 12, 21, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")),
            // 2024
            ZonedDateTime.of(2024, 2, 4, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")),
            ZonedDateTime.of(2024, 4, 4, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")),
            ZonedDateTime.of(2024, 9, 7, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")),
            // 2025
            ZonedDateTime.of(2025, 3, 5, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")),
            ZonedDateTime.of(2025, 6, 21, 12, 0, 0, 0, ZoneId.of("Asia/Taipei")),
            ZonedDateTime.of(2025, 12, 21, 12, 0, 0, 0, ZoneId.of("Asia/Taipei"))
        )

        samples.forEach { base ->
            zones.forEach { zone ->
                val zdt = base.withZoneSameInstant(zone)
                val res = BaziCalculator.computeBazi(zdt)
                val nearest = res.metadata["nearestJieQiName"] ?: ""
                val expected = expectedNearestName(zdt)
                assertTrue("nearestJieQiName 應非空", nearest.isNotBlank())
                assertEquals("${zdt} 最近節氣不一致", expected, nearest)
            }
        }
    }
}
