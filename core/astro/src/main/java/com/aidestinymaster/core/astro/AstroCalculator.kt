package com.aidestinymaster.core.astro

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

/**
 * Lightweight astro calculator facade.
 * NOTE: This file intentionally avoids hard dependency on external ephemeris; it offers deterministic stubs
 * so downstream UI and tests can proceed. When Astronomy Engine is added, wire real values here.
 */
object AstroCalculator {

    enum class HouseSystem { WHOLE_SIGN }

    data class PlanetPositions(val longitudes: Map<String, Double>)
    data class Houses(val system: HouseSystem, val cusps: List<Double>)
    data class Aspect(val type: Int, val p1: String, val p2: String, val angle: Double, val orb: Double)
    data class Orbs(val map: Map<Int, Double>)

    val defaultOrbs = Orbs(mapOf(0 to 6.0, 60 to 6.0, 90 to 6.0, 120 to 6.0, 180 to 6.0))

    // --- Helpers ---
    private fun norm360(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }
    private fun angleDiff(a: Double, b: Double): Double {
        val d = abs(norm360(a) - norm360(b))
        return min(d, 360.0 - d)
    }

    /**
     * Deterministic stub planet longitudes based on epoch seconds.
     * Provides basic bodies to unlock downstream features.
     */
    fun computePlanets(utcInstant: Instant, lat: Double, lon: Double): PlanetPositions {
        val t = utcInstant.epochSecond.toDouble()
        // Simple periodic cycles (NOT astronomical):
        val positions = mapOf(
            "Sun" to norm360((t / 24000.0) * 360.0),      // ~1/day cycle
            "Moon" to norm360((t / 3240.0) * 360.0),       // ~7.5/day cycle
            "Mercury" to norm360((t / 14000.0) * 360.0),
            "Venus" to norm360((t / 18000.0) * 360.0),
            "Mars" to norm360((t / 47000.0) * 360.0),
            "Jupiter" to norm360((t / 360000.0) * 360.0),
            "Saturn" to norm360((t / 900000.0) * 360.0)
        )
        return PlanetPositions(positions)
    }

    /**
     * Simple whole-sign style: 12 cusps equally spaced (stub), starting from Aries 0°.
     * Replace with real house computation when engine is wired.
     */
    fun computeHouses(utcInstant: Instant, lat: Double, lon: Double, system: HouseSystem = HouseSystem.WHOLE_SIGN): Houses {
        val base = 0.0 // Aries 0°
        val cusps = (0 until 12).map { i -> norm360(base + i * 30.0) }
        return Houses(system, cusps)
    }

    /**
     * Compute basic aspects (0/60/90/120/180, orb from Orbs).
     */
    fun computeAspects(planets: PlanetPositions, orbs: Orbs = defaultOrbs): List<Aspect> {
        val keys = planets.longitudes.keys.toList()
        val result = mutableListOf<Aspect>()
        for (i in 0 until keys.size) for (j in i + 1 until keys.size) {
            val p1 = keys[i]
            val p2 = keys[j]
            val a1 = planets.longitudes[p1] ?: continue
            val a2 = planets.longitudes[p2] ?: continue
            val d = angleDiff(a1, a2)
            for (type in listOf(0, 60, 90, 120, 180)) {
                val orb = orbs.map[type] ?: continue
                val diff = abs(d - type)
                if (diff <= orb) {
                    result += Aspect(type, p1, p2, d, diff)
                    break
                }
            }
        }
        return result
    }

    /**
     * JSON summary used by debug UI / tests; includes planets, houses, and aspects.
     */
    fun computeSummary(utcInstant: Instant, lat: Double, lon: Double): String {
        val planets = computePlanets(utcInstant, lat, lon)
        val houses = computeHouses(utcInstant, lat, lon)
        val aspects = computeAspects(planets)
        val planetsJson = JSONObject()
        planets.longitudes.forEach { (k, v) -> planetsJson.put(k, floor(v * 100.0) / 100.0) }
        val housesArr = JSONArray(houses.cusps.map { floor(it * 100.0) / 100.0 })
        val aspectsArr = JSONArray(aspects.map { a ->
            JSONObject().put("type", a.type).put("p1", a.p1).put("p2", a.p2)
                .put("angle", floor(a.angle * 100.0) / 100.0).put("orb", floor(a.orb * 100.0) / 100.0)
        })
        return JSONObject()
            .put("time", utcInstant.toString())
            .put("observer", JSONObject().put("lat", lat).put("lon", lon))
            .put("planets", planetsJson)
            .put("housesSystem", houses.system.name)
            .put("houses", housesArr)
            .put("aspects", aspectsArr)
            .put("note", "stub: replace with Astronomy Engine when available")
            .toString()
    }
}
