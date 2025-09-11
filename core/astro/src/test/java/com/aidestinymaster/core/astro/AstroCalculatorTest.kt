package com.aidestinymaster.core.astro

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AstroCalculatorTest {

    @Test
    fun testComputeAspects_hasBasicAngles() {
        val t = Instant.parse("2024-06-21T00:00:00Z")
        val planets = AstroCalculator.computePlanets(t, 0.0, 0.0)
        val aspects = AstroCalculator.computeAspects(planets)
        // 至少有 1~2 個基本相位被檢出（stub 下非零）
        assertTrue(aspects.isNotEmpty())
        // 角度與容許度合理
        aspects.forEach { a ->
            assertTrue(listOf(0,60,90,120,180).contains(a.type))
            assertTrue(a.orb >= 0.0)
        }
    }

    @Test
    fun testComputeAspects_coversAllAspectTypes() {
        // 以人工行星長度測 computeAspects 邏輯（不依賴時間與 stub 週期）
        fun make(vararg pairs: Pair<String, Double>) = AstroCalculator.PlanetPositions(mapOf(*pairs))
        val orbs = AstroCalculator.defaultOrbs

        // 0° 合相
        val conj = AstroCalculator.computeAspects(make("A" to 0.0, "B" to 0.5), orbs)
        assertTrue(conj.any { it.type == 0 })

        // 60° 六分相
        val sext = AstroCalculator.computeAspects(make("A" to 10.0, "B" to 70.0), orbs)
        assertTrue(sext.any { it.type == 60 })

        // 90° 四分相
        val square = AstroCalculator.computeAspects(make("A" to 100.0, "B" to 190.0), orbs)
        assertTrue(square.any { it.type == 90 })

        // 120° 三分相
        val trine = AstroCalculator.computeAspects(make("A" to 45.0, "B" to 165.0), orbs)
        assertTrue(trine.any { it.type == 120 })

        // 180° 對分相
        val opp = AstroCalculator.computeAspects(make("A" to 200.0, "B" to 20.0), orbs)
        assertTrue(opp.any { it.type == 180 })
    }

    @Test
    fun testComputeAspects_orbBoundary() {
        val orbs = AstroCalculator.defaultOrbs
        // 對分相 180°，容許 6°；在 185.5 與 174° 皆應落入
        val in1 = AstroCalculator.computeAspects(
            AstroCalculator.PlanetPositions(mapOf("A" to 0.0, "B" to 185.5)), orbs
        )
        val in2 = AstroCalculator.computeAspects(
            AstroCalculator.PlanetPositions(mapOf("A" to 0.0, "B" to 174.0)), orbs
        )
        assertTrue(in1.any { it.type == 180 })
        assertTrue(in2.any { it.type == 180 })

        // 超出 6° 容許：對分相應該不觸發
        val out = AstroCalculator.computeAspects(
            AstroCalculator.PlanetPositions(mapOf("A" to 0.0, "B" to 173.5)), orbs
        )
        assertTrue(out.none { it.type == 180 })
    }
}
