package com.aidestinymaster.app.report

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aidestinymaster.app.nav.Routes
import com.aidestinymaster.data.db.DatabaseProvider

@Composable
fun ReportFavoritesScreen(activity: ComponentActivity, nav: NavController) {
    val ctx = activity
    val favs by ReportPrefs.favsFlow(ctx).collectAsState(initial = emptySet())
    var query by remember { mutableStateOf("") }
    val dao = DatabaseProvider.get(ctx).reportDao()
    var items by remember { mutableStateOf(listOf<com.aidestinymaster.data.db.ReportEntity>()) }

    LaunchedEffect(favs, query) {
        val all = favs.mapNotNull { id -> dao.getById(id) }
        items = if (query.isBlank()) all else all.filter { it.title.contains(query) || it.summary.contains(query) }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Favorites", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search") })
        Spacer(Modifier.height(8.dp))
        items.forEach { r ->
            Row(Modifier.fillMaxWidth().clickable { nav.navigate(Routes.Report.replace("{reportId}", r.id)) }.padding(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(r.title, style = MaterialTheme.typography.titleMedium)
                    Text(r.summary, maxLines = 2)
                }
            }
        }
        if (items.isEmpty()) Text("No favorites")
    }
}

