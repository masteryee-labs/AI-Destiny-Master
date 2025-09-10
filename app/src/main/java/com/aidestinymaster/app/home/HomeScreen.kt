package com.aidestinymaster.app.home

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import com.aidestinymaster.app.report.ReportViewModel
import com.aidestinymaster.app.nav.Routes
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(activity: ComponentActivity, nav: NavController) {
    val viewModel = remember { ViewModelProvider(activity)[ReportViewModel::class.java] }
    val lastId by viewModel.lastId.collectAsState(null)
    val current by viewModel.current.collectAsState(null)
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("demo") }
    var content by remember { mutableStateOf("Hello at " + System.currentTimeMillis()) }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Home", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") })
        OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { viewModel.create(type, content) } }) { Text("Create Report") }
            Button(onClick = { lastId?.let { nav.navigate("${Routes.Report.replace("{reportId}", it)}") } }, enabled = lastId != null) { Text("Open Report") }
        }
        if (current != null) {
            Spacer(Modifier.height(8.dp))
            Text("Last: ${current!!.title}")
        }
    }
}

