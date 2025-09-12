package com.aidestinymaster.features.ziwei

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ZiweiChartView(chart: ZiweiChart, modifier: Modifier = Modifier) {
    Column(modifier) {
        chart.palaces.forEach { palace ->
            Card(Modifier.padding(8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(text = palace.name + "宮", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (palace.stars.isNotEmpty()) "星曜：" + palace.stars.joinToString("、") { it.name } else "星曜：—",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (palace.transforms.isNotEmpty()) "四化：" + palace.transforms.joinToString("、") { it.name } else "四化：—",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
