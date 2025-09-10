package com.aidestinymaster.core.astro

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Astronomy
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Ecliptic
import io.github.cosinekitty.astronomy.EquatorRef
import io.github.cosinekitty.astronomy.Observer
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import kotlin.math.abs
import kotlin.math.min

object AstroCalculator {
    private val bodies = listOf(
        Body.Sun,
        Body.Moon,
        Body.Mercury,
        Body.Venus,
        Body.Mars,
        Body.Jupiter,
        Body.Saturn,
        Body.Uranus,
        Body.Neptune
    )

    fun computeSummary(utcInstant: Instant, lat: Double, lon: Double): String {
        val observer = Observer(lat, lon, 0.0)
        val time = Astronomy.Time(utcInstant)

        val planets = JSONObject()
        val longitudes = mutableMapOf<String, Double>()

        bodies.forEach { body ->
            val eq = Astronomy.Equator(body, time, observer, EquatorRef.EQUATOR_OF_DATE, Aberration.CORRECTED)
            val ecl: Ecliptic = Astronomy.Ecliptic(eq)
            val name = body.name.lowercase()
            planets.put(name, JSONObject().put("elon", ecl.elon).put("elat", ecl.elat))
            longitudes[name] = ecl.elon
        }

        val aspects = JSONArray()
        val keys = longitudes.keys.toList()
        for (i in 0 until keys.size) {
            for (j in i + 1 until keys.size) {
                val a = longitudes[keys[i]] ?: continue
                val b = longitudes[keys[j]] ?: continue
                val d0 = abs(a - b) % 360.0
                val delta = min(d0, 360.0 - d0)
                val kind = aspectKind(delta)
                if (kind != null) {
                    aspects.put(
                        JSONObject()
                            .put("a", keys[i])
                            .put("b", keys[j])
                            .put("angle", delta)
                            .put("kind", kind)
                    )
                }
            }
        }

        val obj = JSONObject()
            .put("time", utcInstant.toString())
            .put("observer", JSONObject().put("lat", lat).put("lon", lon))
            .put("planets", planets)
            .put("aspects", aspects)
            .put("housesSystem", "none")
            .put("houses", JSONArray())
        return obj.toString()
    }

    private fun aspectKind(delta: Double, orb: Double = 6.0): String? {
        val list = listOf(0.0 to "conjunction", 60.0 to "sextile", 90.0 to "square", 120.0 to "trine", 180.0 to "opposition")
        for ((angle, name) in list) {
            if (abs(delta - angle) <= orb) return name
        }
        return null
    }
}

