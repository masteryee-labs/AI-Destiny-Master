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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.Calendar
import com.aidestinymaster.app.nav.Routes
import com.aidestinymaster.data.repository.ChartRepository
import com.aidestinymaster.billing.Entitlement
import kotlinx.coroutines.launch

@Composable
fun ChartInputScreen(activity: ComponentActivity, kind: String, nav: NavController) {
    val repo = ChartRepository.from(activity)
    val scope = rememberCoroutineScope()
    var birthDate by remember { mutableStateOf("2000-01-01") }
    var birthTime by remember { mutableStateOf("12:00") }
    var tz by remember { mutableStateOf("+08:00") }
    var place by remember { mutableStateOf("Taipei") }
    var error by remember { mutableStateOf("") }

    val ent = remember { Entitlement.from(activity) }
    var allowed by remember { mutableStateOf(true) }
    LaunchedEffect(kind) {
        allowed = ent.hasPro(kind)
    }
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Chart Input ($kind)", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Birth Date (YYYY-MM-DD)") })
            Button(onClick = {
                val c = Calendar.getInstance()
                DatePickerDialog(activity, { _, y, m, d -> birthDate = String.format("%04d-%02d-%02d", y, m+1, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }) { Text("Pick") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = birthTime, onValueChange = { birthTime = it }, label = { Text("Birth Time (HH:MM)") })
            Button(onClick = {
                val c = Calendar.getInstance()
                TimePickerDialog(activity, { _, h, min -> birthTime = String.format("%02d:%02d", h, min) }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
            }) { Text("Pick") }
        }
        OutlinedTextField(value = tz, onValueChange = { tz = it }, label = { Text("Time Zone") })
        OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text("Place") })
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        if (!allowed) Text("需要 Pro 或 VIP 權益方可使用 ($kind)", color = MaterialTheme.colorScheme.error)
        Row { Button(onClick = {
            scope.launch {
                val dateOk = Regex("\\d{4}-\\d{2}-\\d{2}").matches(birthDate)
                val timeOk = Regex("\\d{2}:\\d{2}").matches(birthTime)
                val tzOk = Regex("[+-]\\d{2}:\\d{2}").matches(tz)
                if (!dateOk || !timeOk || !tzOk) {
                    error = "請輸入正確的日期/時間/時區格式"
                    return@launch
                } else error = ""
                val id = repo.create(kind, mapOf(
                    "birthDate" to birthDate,
                    "birthTime" to birthTime,
                    "tz" to tz,
                    "place" to place,
                    "computedJson" to "{}"
                ))
                nav.navigate(Routes.ChartResult.replace("{chartId}", id))
            }
        }, enabled = allowed) { Text("Create & View Result") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { nav.navigate(Routes.Paywall) }) { Text("Go Paywall") }
        }
    }
}
