package com.aidestinymaster.features.design

import androidx.lifecycle.ViewModel
import com.aidestinymaster.core.astro.AstroCalculator

class DesignViewModel : ViewModel() {
    data class UiState(
        val planets: PlanetPositions? = null,
        val assignments: GateAssignments? = null,
        val typeAuthority: TypeAuthority? = null,
        val summary: DesignSummary? = null
    )

    var state: UiState = UiState()
        private set

    fun updateFromPlanets(planets: PlanetPositions) {
        val assigns = mapPlanetsToGates(planets)
        val ta = inferTypeAuthority(assigns, planets)
        val sum = summarizeDesign(assigns, ta)
        state = UiState(planets = planets, assignments = assigns, typeAuthority = ta, summary = sum)
    }

    // 便於測試：用 AstroCalculator 的簡化行星輸入
    fun updateFromStub(instant: java.time.Instant, lat: Double = 25.04, lon: Double = 121.56) {
        val p = AstroCalculator.computePlanets(instant, lat, lon)
        updateFromPlanets(p)
    }
}
