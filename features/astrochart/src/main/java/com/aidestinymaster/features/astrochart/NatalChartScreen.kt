package com.aidestinymaster.features.astrochart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aidestinymaster.core.astro.AstroCalculator

@Composable
fun NatalChartScreen(
    planets: AstroCalculator.PlanetPositions,
    houses: AstroCalculator.Houses? = null,
    aspects: List<AstroCalculator.Aspect>,
    modifier: Modifier = Modifier
) {
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AstroChartCanvas(planets = planets, houses = houses, modifier = Modifier)
        AspectList(aspects = aspects, modifier = Modifier)
    }
}
