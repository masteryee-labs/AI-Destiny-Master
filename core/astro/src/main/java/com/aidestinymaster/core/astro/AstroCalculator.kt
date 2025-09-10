package com.aidestinymaster.core.astro

import org.json.JSONObject
import java.time.Instant

object AstroCalculator {
    fun computeSummary(utcInstant: Instant, lat: Double, lon: Double): String {
        // TODO: 接入 Astronomy Engine 取得行星/宮位資訊
        val obj = JSONObject()
            .put("ts", utcInstant.toString())
            .put("lat", lat)
            .put("lon", lon)
            .put("planets", JSONObject())
            .put("houses", JSONObject())
        return obj.toString()
    }
}

