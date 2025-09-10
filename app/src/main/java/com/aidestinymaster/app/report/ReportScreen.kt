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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import com.aidestinymaster.billing.Entitlement
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
    val ctx = LocalContext.current
    val favs by ReportPrefs.favsFlow(ctx).collectAsState(initial = emptySet())
    val notes by ReportPrefs.notesFlow(ctx).collectAsState(initial = emptyMap())
    var noteText by androidx.compose.runtime.remember { mutableStateOf("") }
    LaunchedEffect(reportId, notes) { noteText = notes[reportId] ?: "" }
    val ent = remember { Entitlement.from(activity) }
    var vip by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { vip = ent.hasVip() }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Report", style = MaterialTheme.typography.titleLarge)
        Text("ID: $reportId")
        Spacer(Modifier.height(8.dp))
        Text("Title: ${report?.title ?: "-"}")
        Text("Summary: ${report?.summary ?: "-"}")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { bridge.push(reportId); status = "Pushed" } }, enabled = vip) { Text("Push") }
            Button(onClick = { scope.launch { bridge.pull(reportId); status = "Pulled" } }, enabled = vip) { Text("Pull") }
            Button(onClick = {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, report?.title ?: "Report")
                    putExtra(Intent.EXTRA_TEXT, report?.summary ?: "")
                }
                ctx.startActivity(Intent.createChooser(sendIntent, "分享報告"))
            }, enabled = vip) { Text("Share") }
            Button(onClick = { scope.launch { ReportPrefs.toggleFav(ctx, reportId) } }) { Text(if (favs.contains(reportId)) "Unfavorite" else "Favorite") }
        }
        if (!vip) Text("需要 VIP 權益以使用推送/拉取/分享功能", color = MaterialTheme.colorScheme.error)
        if (status.isNotEmpty()) Text("Status: $status")
        OutlinedTextField(value = noteText, onValueChange = { noteText = it }, label = { Text("Note") })
        Button(onClick = { scope.launch { ReportPrefs.setNote(ctx, reportId, noteText) } }) { Text("Save Note") }
    }
}
