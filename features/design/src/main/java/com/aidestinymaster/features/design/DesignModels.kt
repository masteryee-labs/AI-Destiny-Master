package com.aidestinymaster.features.design

import com.aidestinymaster.core.astro.AstroCalculator

/**
 * 自製資料模型：避免引用受保護教材用語，採通用且中性的名稱。
 */

data class GateAssignments(
    val planetToGate: Map<String, Int>,
    val activeGates: Set<Int>
)

data class TypeAuthority(
    val typeLabel: String,       // 範例："回應型"、"指引型"、"發起型"、"映照型"（中性描述）
    val authorityLabel: String,  // 範例："情緒權衡"、"直覺權衡"、"自我權衡"（中性描述）
    val notes: String = ""
)

data class DesignSummary(
    val brief: String,
    val highlights: List<String>,
    val stats: Map<String, Int>
)

// 方便 UI 使用的擴充屬性
val GateAssignments.count get() = activeGates.size

// 讓使用端直覺一點
typealias PlanetPositions = AstroCalculator.PlanetPositions
