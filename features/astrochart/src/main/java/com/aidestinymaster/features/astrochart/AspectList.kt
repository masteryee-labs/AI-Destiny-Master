package com.aidestinymaster.features.astrochart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aidestinymaster.core.astro.AstroCalculator

@Composable
fun AspectList(aspects: List<AstroCalculator.Aspect>, modifier: Modifier = Modifier) {
    Card(modifier.padding(8.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("相位列表", style = MaterialTheme.typography.titleMedium)
            LazyColumn(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(aspects) { a ->
                    Text("${aspectName(a.type)}: ${a.p1} – ${a.p2} (Δ=${"%.1f".format(a.angle)}°, orb=${"%.1f".format(a.orb)}°)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun aspectName(type: Int): String = when (type) {
    0 -> "合"
    60 -> "六合"
    90 -> "四分"
    120 -> "三分"
    180 -> "衝"
    else -> "$type°"
}
