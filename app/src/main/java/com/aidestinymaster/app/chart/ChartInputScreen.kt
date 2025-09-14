package com.aidestinymaster.app.chart

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
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
import kotlinx.coroutines.launch
import com.aidestinymaster.app.paywall.PaywallSheet
import androidx.compose.material3.Card
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.aidestinymaster.app.ui.DesignTokens
import androidx.compose.ui.res.stringResource
import com.aidestinymaster.app.R
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartInputScreen(activity: ComponentActivity, kind: String, nav: NavController) {
    val repo = ChartRepository.from(activity)
    val scope = rememberCoroutineScope()
    var birthDate by remember { mutableStateOf("2000-01-01") }
    var birthTime by remember { mutableStateOf("12:00") }
    var tz by remember { mutableStateOf("+08:00") }
    var place by remember { mutableStateOf("Taipei") }
    var error by remember { mutableStateOf("") }
    // 排盤免費：移除建立圖表的權益限制；AI 詳解才需要權益
    var showPaywall by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(DesignTokens.Spacing.L.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.M.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(12.dp)) {
                    val title = stringResource(id = R.string.chart_input_title, kind)
                    Text(title, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
            OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text(stringResource(id = R.string.birth_date_label)) })
            Button(onClick = {
                val c = Calendar.getInstance()
                DatePickerDialog(activity, { _, y, m, d -> birthDate = String.format("%04d-%02d-%02d", y, m+1, d) }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
            }) { Text(stringResource(id = R.string.pick)) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
            OutlinedTextField(value = birthTime, onValueChange = { birthTime = it }, label = { Text(stringResource(id = R.string.birth_time_label)) })
            Button(onClick = {
                val c = Calendar.getInstance()
                TimePickerDialog(activity, { _, h, min -> birthTime = String.format("%02d:%02d", h, min) }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
            }) { Text(stringResource(id = R.string.pick)) }
        }
        OutlinedTextField(value = tz, onValueChange = { tz = it }, label = { Text(stringResource(id = R.string.time_zone_label)) })
        OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text(stringResource(id = R.string.place_label)) })
        if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        Row { Button(onClick = {
            scope.launch {
                val dateOk = Regex("\\d{4}-\\d{2}-\\d{2}").matches(birthDate)
                val timeOk = Regex("\\d{2}:\\d{2}").matches(birthTime)
                val tzOk = Regex("[+-]\\d{2}:\\d{2}").matches(tz)
                if (!dateOk || !timeOk || !tzOk) {
                    error = activity.getString(R.string.input_error_datetime)
                    return@launch
                } else error = ""
                val id = repo.compute(kind, mapOf(
                    "birthDate" to birthDate,
                    "birthTime" to birthTime,
                    "tz" to tz,
                    "place" to place,
                    "computedJson" to "{}"
                ))
                nav.navigate(Routes.ChartResult.replace("{chartId}", id))
            }
        }, enabled = true) { Text(stringResource(id = R.string.create_and_open_report)) }
            Spacer(Modifier.width(DesignTokens.Spacing.S.dp))
            Button(onClick = { showPaywall = true }) { Text(stringResource(id = R.string.go_paywall)) }
        }
    }
    PaywallSheet(
        activity = activity,
        visible = showPaywall,
        onDismiss = { showPaywall = false },
        onPurchased = {
            scope.launch {
                showPaywall = false
            }
        }
    )
}
