package com.aidestinymaster.core.astro

import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

object AstroCalculator {
    // Placeholder implementation without external ephemeris dependency.
    fun computeSummary(utcInstant: Instant, lat: Double, lon: Double): String {
        return JSONObject()
            .put("time", utcInstant.toString())
            .put("observer", JSONObject().put("lat", lat).put("lon", lon))
            .put("planets", JSONObject())
            .put("aspects", JSONArray())
            .put("housesSystem", "none")
            .put("houses", JSONArray())
            .put("note", "stub: astronomy engine not wired yet")
            .toString()
    }
}
