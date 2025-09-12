package com.aidestinymaster.features.design

import com.aidestinymaster.core.astro.AstroCalculator
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * 以 360° 等分為 64 門（每門 5.625°）的自製近似映射。
 * 不引用受保護教材原文；此為 MVP 用之中性方案。
 */
private const val GATE_WIDTH_DEG = 360.0 / 64.0 // 5.625

fun mapPlanetsToGates(planets: PlanetPositions): GateAssignments {
    val mapping = mutableMapOf<String, Int>()
    val active = mutableSetOf<Int>()
    planets.longitudes.forEach { (name, deg) ->
        val d = ((deg % 360.0) + 360.0) % 360.0
        val idx = floor(d / GATE_WIDTH_DEG).toInt() + 1
        val gate = min(64, max(1, idx))
        mapping[name] = gate
        active += gate
    }
    return GateAssignments(mapping, active)
}

/**
 * 簡化的「類型 / 權衡」推論：
 * - 類型：依啟動性分數與活躍閘門數量給予中性描述（發起型 / 回應型 / 指引型 / 映照型）。
 * - 權衡：以日月相對角度給予「情緒權衡」或「直覺權衡」等中性描述。
 * 註：此為自製啟發式規則，避免引用受保護教材的特定定義。
 */
fun inferTypeAuthority(assignments: GateAssignments, planets: PlanetPositions? = null): TypeAuthority {
    val activeCount = assignments.activeGates.size

    // 啟動性分數：以行星加權（若有提供 planets 以利更穩定輸出）。
    val names = planets?.longitudes?.keys ?: assignments.planetToGate.keys
    val energyScore = names.fold(0) { acc, n ->
        val w = when (n) {
            "Sun" -> 3
            "Moon" -> 2
            "Mars" -> 2
            "Jupiter" -> 1
            "Saturn" -> 1
            else -> 1
        }
        acc + w
    }

    val typeLabel = when {
        energyScore >= 10 && activeCount >= 6 -> "發起型"
        activeCount >= 6 -> "回應型"
        activeCount <= 3 -> "指引型"
        else -> "映照型"
    }

    // 權衡（Authority）：若提供行星，依日月經度相對判斷，否則以活躍數量代替。
    val authorityLabel = planets?.let { p ->
        val sun = p.longitudes["Sun"] ?: 0.0
        val moon = p.longitudes["Moon"] ?: 0.0
        val diff = kotlin.math.abs(((sun - moon + 540.0) % 360.0) - 180.0)
        if (diff > 90.0) "情緒權衡" else "直覺權衡"
    } ?: run {
        if (activeCount % 2 == 0) "情緒權衡" else "直覺權衡"
    }

    val notes = buildString {
        append("活躍閘門數：").append(activeCount.toString()).append("；")
        append("能量權重：").append(energyScore.toString())
    }
    return TypeAuthority(typeLabel = typeLabel, authorityLabel = authorityLabel, notes = notes)
}

// Overload to match required signature exactly.
fun inferTypeAuthority(assignments: GateAssignments): TypeAuthority = inferTypeAuthority(assignments, null)

/**
 * 摘要：提供簡短說明、亮點條列與統計值，供 UI 顯示或後續 AI 提示詞使用。
 */
fun summarizeDesign(assignments: GateAssignments, typeAuth: TypeAuthority): DesignSummary {
    val brief = "天賦設計圖摘要：類型為${typeAuth.typeLabel}，決策以${typeAuth.authorityLabel}為主。共啟動 ${assignments.activeGates.size} 個閘門。"

    val planetGroups = assignments.planetToGate.entries.groupBy { it.value }
    val topGates = planetGroups.entries
        .sortedByDescending { it.value.size }
        .take(3)
        .map { (gate, entries) -> "閘門 #$gate：${entries.joinToString { it.key }}" }

    val highlights = buildList {
        addAll(topGates)
        add("活躍閘門數：${assignments.activeGates.size}")
        add("分布均勻度（近似）：${(assignments.activeGates.size / 64.0 * 100).toInt()}%")
    }

    val stats = mapOf(
        "activeGates" to assignments.activeGates.size,
        "uniqueGates" to assignments.activeGates.size,
        "mappedPlanets" to assignments.planetToGate.size
    )

    return DesignSummary(brief = brief, highlights = highlights, stats = stats)
}
