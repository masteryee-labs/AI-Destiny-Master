package com.aidestinymaster.core.astro

import java.time.Instant

/**
 * Minimal natal chart builder using AstroCalculator stubs.
 * When real ephemeris is wired, these functions will automatically benefit.
 */
object NatalChartBuilder {

    data class BirthInput(
        val utcInstant: Instant,
        val lat: Double,
        val lon: Double,
        val houseSystem: AstroCalculator.HouseSystem = AstroCalculator.HouseSystem.WHOLE_SIGN_ASC
    )

    data class NatalChart(
        val planets: AstroCalculator.PlanetPositions,
        val houses: AstroCalculator.Houses,
        val aspects: List<AstroCalculator.Aspect>
    )

    data class NatalSummary(
        val aspectCounts: Map<Int, Int>
    )

    fun buildNatalChart(input: BirthInput): NatalChart {
        val p = AstroCalculator.computePlanets(input.utcInstant, input.lat, input.lon)
        val h = AstroCalculator.computeHouses(input.utcInstant, input.lat, input.lon, input.houseSystem)
        val a = AstroCalculator.computeAspects(p, AstroCalculator.defaultOrbs)
        return NatalChart(p, h, a)
    }

    fun summarizeNatal(natal: NatalChart): NatalSummary {
        val counts = natal.aspects.groupingBy { it.type }.eachCount()
        return NatalSummary(counts)
    }
}
