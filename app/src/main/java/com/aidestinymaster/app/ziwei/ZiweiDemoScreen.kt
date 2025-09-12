package com.aidestinymaster.app.ziwei

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidestinymaster.features.ziwei.*
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun ZiweiDemoScreen(activity: ComponentActivity) {
    val chartState = remember { mutableStateOf<ZiweiChart?>(null) }
    val summaryState = remember { mutableStateOf<ZiweiSummary?>(null) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("紫微 Demo", style = MaterialTheme.typography.titleLarge)
        Button(onClick = {
            val birth = ZonedDateTime.now(ZoneId.of("Asia/Taipei")).minusYears(20)
            val chart = ZiweiCalculator.computeZiweiChart(birth)
            val summary = ZiweiCalculator.summarizeZiwei(chart)
            chartState.value = chart
            summaryState.value = summary
        }) { Text("生成紫微盤與摘要") }
        chartState.value?.let { ZiweiChartView(it) }
        summaryState.value?.let { ZiweiSummaryCard(it) }
    }
}
