package com.aidestinymaster.features.design

import android.content.Context
import org.json.JSONObject
import java.nio.charset.Charset
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * DesignEngine: 提供以 assets 中 gates64.json 為主的映射功能；
 * 若找不到檔案則退回等分 360/64 的方案（與 DesignMapper.kt 一致）。
 */
object DesignEngine {

    data class GateRange(val id: Int, val start: Double, val end: Double)

    private const val ASSET_FILE = "gates64.json"
    private const val DEFAULT_GATE_WIDTH = 360.0 / 64.0

    fun loadGatesFromAssets(context: Context): List<GateRange>? = runCatching {
        val am = context.assets
        val text = am.open(ASSET_FILE).use { it.readBytes().toString(Charset.forName("UTF-8")) }
        val obj = JSONObject(text)
        val arr = obj.getJSONArray("gates")
        val list = mutableListOf<GateRange>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list += GateRange(
                id = o.getInt("id"),
                start = o.getDouble("start"),
                end = o.getDouble("end")
            )
        }
        list
    }.getOrNull()

    private fun norm360(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    fun mapAngleToGate(angleDeg: Double, gates: List<GateRange>): Int {
        val d = norm360(angleDeg)
        // 支援 [start, end)；最後一門包含 360 度邊界
        for (g in gates) {
            val s = norm360(g.start)
            val e = norm360(g.end)
            if (s <= e) {
                if (d >= s && d < e) return g.id
            } else {
                // 跨 0 度
                if (d >= s || d < e) return g.id
            }
        }
        // Fallback：等分計算
        val idx = floor(d / DEFAULT_GATE_WIDTH).toInt() + 1
        return min(64, max(1, idx))
    }

    fun mapPlanetsToGates(planets: PlanetPositions, gates: List<GateRange>): GateAssignments {
        val mapping = mutableMapOf<String, Int>()
        val active = mutableSetOf<Int>()
        planets.longitudes.forEach { (name, deg) ->
            val gate = mapAngleToGate(deg, gates)
            mapping[name] = gate
            active += gate
        }
        return GateAssignments(mapping, active)
    }

    /**
     * 方便呼叫端：若提供 context，優先用 assets；否則使用等分方案。
     */
    fun assign(planets: PlanetPositions, context: Context? = null): GateAssignments {
        val gatesFromAssets = context?.let { loadGatesFromAssets(it) }
        return if (gatesFromAssets != null && gatesFromAssets.size == 64) {
            mapPlanetsToGates(planets, gatesFromAssets)
        } else {
            // 等分 fallback
            mapPlanetsToGates(planets)
        }
    }
}
