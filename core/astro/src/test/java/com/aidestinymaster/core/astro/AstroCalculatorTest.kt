package com.aidestinymaster.core.astro

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import org.junit.Assert.assertEquals

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

    @Test
    fun testFixedInstant_longitudes_and_houses() {
        // 固定時間與地點，驗證行星長度與 12 宮等距（依照 stub 規格）
        val t = Instant.parse("2024-01-01T00:00:00Z")
        val lat = 25.0330
        val lon = 121.5654
        val planets = AstroCalculator.computePlanets(t, lat, lon)
        // 依據 stub 的週期公式直接計算期望值
        fun norm360(d: Double): Double {
            var v = d % 360.0
            if (v < 0) v += 360.0
            return v
        }
        val sec = t.epochSecond.toDouble()
        val expected = mapOf(
            "Sun" to norm360((sec / 24000.0) * 360.0),
            "Moon" to norm360((sec / 3240.0) * 360.0),
            "Mercury" to norm360((sec / 14000.0) * 360.0),
            "Venus" to norm360((sec / 18000.0) * 360.0),
            "Mars" to norm360((sec / 47000.0) * 360.0),
            "Jupiter" to norm360((sec / 360000.0) * 360.0),
            "Saturn" to norm360((sec / 900000.0) * 360.0)
        )
        expected.forEach { (k, v) ->
            val actual = planets.longitudes[k] ?: error("missing $k")
            assertEquals(v, actual, 1e-9)
        }

        val houses = AstroCalculator.computeHouses(t, lat, lon)
        // 應為 [0,30,60,...,330]
        for (i in 0 until 12) {
            assertEquals(i * 30.0, houses.cusps[i], 1e-9)
        }
    }

    @Test
    fun testMultiPlanet_multiAspect_coverage() {
        // 建立多顆行星，讓多組 pair 落入不同相位，提升覆蓋度
        fun make(vararg pairs: Pair<String, Double>) = AstroCalculator.PlanetPositions(mapOf(*pairs))
        val orbs = AstroCalculator.defaultOrbs
        val pos = make(
            // 刻意排布：
            // A-B ~ 0°, A-C ~ 60°, A-D ~ 90°, B-C ~ 60°, C-D ~ 120°, B-D ~ 180°
            "A" to 10.0,
            "B" to 14.0,   // 與 A 差 ~4° -> 0° 內 orb
            "C" to 70.5,   // 與 A 差 ~60.5° -> 60° 內 orb
            "D" to 100.5   // 與 A 差 ~90.5° -> 90° 內 orb
        )
        val aspects = AstroCalculator.computeAspects(pos, orbs)
        assertTrue(aspects.any { it.type == 0 })
        assertTrue(aspects.any { it.type == 60 })
        assertTrue(aspects.any { it.type == 90 })
        // 再補充另外兩顆以觸發 120 與 180，避免更動前面行星位置
        val pos2 = AstroCalculator.PlanetPositions(pos.longitudes + mapOf(
            "E" to 190.0,   // 與 A 差 ~180°
            "F" to 130.0    // 與 C 差 ~59.5°，與 A 差 ~120°
        ))
        val aspects2 = AstroCalculator.computeAspects(pos2, orbs)
        assertTrue(aspects2.any { it.type == 120 })
        assertTrue(aspects2.any { it.type == 180 })
    }
}
