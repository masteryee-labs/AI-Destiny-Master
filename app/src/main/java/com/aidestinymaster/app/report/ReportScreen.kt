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
import androidx.lifecycle.ViewModelProvider

@Composable
fun ReportScreen(activity: ComponentActivity, reportId: String) {
    val vm = ViewModelProvider(activity)[ReportViewModel::class.java]
    val report by vm.current.collectAsState(null)
    // 若當前載入的不是此 id，可以簡易再載一次
    //（簡化：直接顯示已有 current，真實場景應透過 observeById(reportId)）
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Report", style = MaterialTheme.typography.titleLarge)
        Text("ID: $reportId")
        Spacer(Modifier.height(8.dp))
        Text("Title: ${report?.title ?: "-"}")
        Text("Summary: ${report?.summary ?: "-"}")
    }
}

