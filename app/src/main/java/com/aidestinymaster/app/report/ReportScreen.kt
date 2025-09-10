package com.aidestinymaster.app.report

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aidestinymaster.data.repository.ReportRepository

@Composable
fun ReportScreen(activity: ComponentActivity, reportId: String) {
    val repo = ReportRepository.from(activity)
    val report by repo.getReportFlow(reportId).collectAsState(initial = null)
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Report", style = MaterialTheme.typography.titleLarge)
        Text("ID: $reportId")
        Spacer(Modifier.height(8.dp))
        Text("Title: ${report?.title ?: "-"}")
        Text("Summary: ${report?.summary ?: "-"}")
    }
}
