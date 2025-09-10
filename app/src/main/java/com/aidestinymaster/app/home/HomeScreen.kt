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
import com.aidestinymaster.app.report.ReportPrefs
import com.aidestinymaster.data.db.DatabaseProvider
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(activity: ComponentActivity, nav: NavController) {
    val viewModel = remember { ViewModelProvider(activity)[ReportViewModel::class.java] }
    val lastId by viewModel.lastId.collectAsState(null)
    val current by viewModel.current.collectAsState(null)
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("demo") }
    var content by remember { mutableStateOf("Hello at " + System.currentTimeMillis()) }
    var favQuery by remember { mutableStateOf("") }
    val favs by ReportPrefs.favsFlow(activity).collectAsState(initial = emptySet())
    var favItems by remember { mutableStateOf(listOf<com.aidestinymaster.data.db.ReportEntity>()) }
    LaunchedEffect(favs, favQuery) {
        val dao = DatabaseProvider.get(activity).reportDao()
        val all = favs.mapNotNull { id -> dao.getById(id) }
        favItems = if (favQuery.isBlank()) all else all.filter { it.title.contains(favQuery) || it.summary.contains(favQuery) }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Home", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") })
        OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch {
                val id = viewModel.create(type, content)
                nav.navigate(Routes.Report.replace("{reportId}", id))
            } }) { Text("Create & Open Report") }
            Button(onClick = { lastId?.let { nav.navigate(Routes.Report.replace("{reportId}", it)) } }, enabled = lastId != null) { Text("Open Last") }
            Button(onClick = { nav.navigate(Routes.ReportFavs) }) { Text("My Favorites") }
        }
        if (current != null) {
            Spacer(Modifier.height(8.dp))
            Text("Last: ${current!!.title}")
        }
        Spacer(Modifier.height(16.dp))
        Text("Favorites", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = favQuery, onValueChange = { favQuery = it }, label = { Text("Search favorites") })
        favItems.take(5).forEach { r ->
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                    Text(r.summary, maxLines = 1)
                }
                Button(onClick = { nav.navigate(Routes.Report.replace("{reportId}", r.id)) }) { Text("Open") }
            }
        }
    }
}
