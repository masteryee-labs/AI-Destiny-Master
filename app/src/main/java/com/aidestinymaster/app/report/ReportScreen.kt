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
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Row
import kotlinx.coroutines.launch
import com.aidestinymaster.sync.ReportSyncBridge
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
    val bridge = ReportSyncBridge(activity)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var status by androidx.compose.runtime.remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Report", style = MaterialTheme.typography.titleLarge)
        Text("ID: $reportId")
        Spacer(Modifier.height(8.dp))
        Text("Title: ${report?.title ?: "-"}")
        Text("Summary: ${report?.summary ?: "-"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { bridge.push(reportId); status = "Pushed" } }) { Text("Push") }
            Button(onClick = { scope.launch { bridge.pull(reportId); status = "Pulled" } }) { Text("Pull") }
        }
        if (status.isNotEmpty()) Text("Status: $status")
    }
}
