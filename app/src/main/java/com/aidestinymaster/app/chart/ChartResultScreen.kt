package com.aidestinymaster.app.chart

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aidestinymaster.data.db.DatabaseProvider

@Composable
fun ChartResultScreen(activity: ComponentActivity, chartId: String) {
    val dao = DatabaseProvider.get(activity).chartDao()
    val chart by dao.observeById(chartId).collectAsState(initial = null)
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Chart Result", style = MaterialTheme.typography.titleLarge)
        Text("ID: $chartId")
        Text("Kind: ${chart?.kind ?: "-"}")
        Text("Birth: ${chart?.birthDate ?: "-"} ${chart?.birthTime ?: ""}")
        Text("TZ: ${chart?.tz ?: "-"} @ ${chart?.place ?: "-"}")
        Text("Computed: ${chart?.computedJson ?: "{}"}")
    }
}
