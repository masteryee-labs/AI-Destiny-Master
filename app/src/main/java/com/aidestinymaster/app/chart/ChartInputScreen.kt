package com.aidestinymaster.app.chart

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aidestinymaster.app.nav.Routes
import com.aidestinymaster.data.repository.ChartRepository
import kotlinx.coroutines.launch

@Composable
fun ChartInputScreen(activity: ComponentActivity, kind: String, nav: NavController) {
    val repo = ChartRepository.from(activity)
    val scope = rememberCoroutineScope()
    var birthDate by remember { mutableStateOf("2000-01-01") }
    var birthTime by remember { mutableStateOf("12:00") }
    var tz by remember { mutableStateOf("+08:00") }
    var place by remember { mutableStateOf("Taipei") }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Chart Input ($kind)", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Birth Date") })
        OutlinedTextField(value = birthTime, onValueChange = { birthTime = it }, label = { Text("Birth Time") })
        OutlinedTextField(value = tz, onValueChange = { tz = it }, label = { Text("Time Zone") })
        OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text("Place") })
        Row { Button(onClick = {
            scope.launch {
                val id = repo.create(kind, mapOf(
                    "birthDate" to birthDate,
                    "birthTime" to birthTime,
                    "tz" to tz,
                    "place" to place,
                    "computedJson" to "{}"
                ))
                nav.navigate(Routes.ChartResult.replace("{chartId}", id))
            }
        }) { Text("Create & View Result") } }
    }
}
