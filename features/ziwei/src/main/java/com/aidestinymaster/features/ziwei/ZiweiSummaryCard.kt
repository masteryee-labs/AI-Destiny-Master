package com.aidestinymaster.features.ziwei

import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ZiweiSummaryCard(summary: ZiweiSummary, modifier: Modifier = Modifier) {
    Card(modifier.padding(12.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(summary.title, style = MaterialTheme.typography.titleMedium)
            summary.highlights.forEach { h ->
                Text("• $h", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
