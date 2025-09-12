package com.aidestinymaster.features.design

import com.aidestinymaster.core.astro.AstroCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class DesignMapperTest {

    @Test
    fun mapPlanetsToGates_basicMapping() {
        // 使用 AstroCalculator 的固定時間產生可重現的行星角度
        val instant = Instant.ofEpochSecond(1_700_000_000)
        val planets = AstroCalculator.computePlanets(instant, 25.04, 121.56)

        val assignments = mapPlanetsToGates(planets)
        // 檢查範圍：所有 gate 應在 1..64
        assignments.planetToGate.values.forEach { gate ->
            assertTrue(gate in 1..64)
        }
        // 至少有 3 個獨立的活躍閘門
        assertTrue(assignments.activeGates.size >= 3)
    }

    @Test
    fun inferTypeAuthority_and_summary() {
        val instant = Instant.ofEpochSecond(1_700_000_000)
        val planets = AstroCalculator.computePlanets(instant, 25.04, 121.56)

        val assignments = mapPlanetsToGates(planets)
        val ta = inferTypeAuthority(assignments, planets)
        val summary = summarizeDesign(assignments, ta)

        assertTrue(ta.typeLabel.isNotBlank())
        assertTrue(ta.authorityLabel.isNotBlank())
        assertTrue(summary.brief.contains("類型"))
        assertTrue(summary.highlights.isNotEmpty())
        assertTrue(summary.stats["activeGates"] ?: 0 > 0)
    }

    @Test
    fun engine_assign_with_fallback_equals_mapper_when_no_context() {
        val instant = Instant.ofEpochSecond(1_700_000_000)
        val planets = AstroCalculator.computePlanets(instant, 25.04, 121.56)

        val byMapper = mapPlanetsToGates(planets)
        val byEngine = DesignEngine.assign(planets, context = null)

        // 在沒有 assets 載入時，engine 退回等分方案，應等同 mapper 結果
        assertEquals(byMapper.activeGates, byEngine.activeGates)
        assertEquals(byMapper.planetToGate, byEngine.planetToGate)
    }
}
