package com.aidestinymaster.app.chart

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aidestinymaster.data.db.DatabaseProvider
import com.google.gson.Gson
import com.aidestinymaster.app.paywall.PaywallSheet
import com.aidestinymaster.billing.Entitlement
import com.aidestinymaster.billing.EntitlementEvents
import kotlinx.coroutines.launch
import androidx.compose.material3.Card
import androidx.compose.foundation.background
import com.aidestinymaster.app.ui.DesignTokens
import com.aidestinymaster.app.ui.glassCardColors
import com.aidestinymaster.app.ui.glassModifier
import androidx.compose.ui.res.stringResource
import com.aidestinymaster.app.R
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartResultScreen(activity: ComponentActivity, chartId: String) {
    val dao = DatabaseProvider.get(activity).chartDao()
    val chart by dao.observeById(chartId).collectAsState(initial = null)
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val summary: String = remember(chart?.computedJson) {
        val json = chart?.computedJson ?: return@remember "{}"
        runCatching {
            val obj = gson.fromJson(json, Map::class.java) as Map<*, *>
            val keys = obj.keys.map { it.toString() }
            if (keys.isEmpty()) json.take(120) else keys.joinToString(limit = 5)
        }.getOrDefault(json.take(120))
    }
    var showPaywall by remember { mutableStateOf(false) }
    var hasVip by remember { mutableStateOf(false) }
    var hasPro by remember { mutableStateOf(false) }
    LaunchedEffect(chart?.kind) {
        val ent = Entitlement.from(activity)
        hasVip = ent.hasVip()
        hasPro = chart?.kind?.let { ent.hasPro(it) } ?: false
    }
    // 全域權益變更：其他頁購買或恢復後，立即更新
    LaunchedEffect(Unit) {
        EntitlementEvents.events.collect {
            val ent = Entitlement.from(activity)
            hasVip = ent.hasVip()
            hasPro = chart?.kind?.let { ent.hasPro(it) } ?: false
        }
    }
    Column(Modifier.fillMaxWidth().padding(DesignTokens.Spacing.L.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.M.dp)) {
        // 標題卡
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(id = R.string.chart_result_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(id = R.string.id_label, chartId))
                }
            }
        }
        // 基本資訊卡
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(id = R.string.chart_result_meta_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(id = R.string.chart_result_kind, chart?.kind ?: activity.getString(R.string.unknown)))
                    Text(stringResource(id = R.string.chart_result_birth, (chart?.birthDate ?: "-") + " " + (chart?.birthTime ?: "")))
                    Text(stringResource(id = R.string.chart_result_tz_place, chart?.tz ?: "-", chart?.place ?: "-"))
                }
            }
        }
        // 摘要卡（人類可讀）
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(id = R.string.summary), style = MaterialTheme.typography.titleMedium)
                    Text(if (summary.isBlank()) activity.getString(R.string.unknown) else summary)
                }
            }
        }
        // 動作列：建立報告、分享、付費牆
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                scope.launch {
                    val content = chart?.computedJson ?: "{}"
                    val type = chart?.kind ?: "chart"
                    val id = com.aidestinymaster.data.repository.ReportRepository.from(activity)
                        .createFromAi(type = type, chartId = chartId, content = content)
                    // 以 deep link 導向報告詳頁，避免依賴 NavController 參照
                    val uri = Uri.parse("aidm://report/" + id)
                    activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }
            }) { Text(stringResource(id = R.string.create_and_open_report)) }
            Button(onClick = {
                val text = summary.ifBlank { (chart?.computedJson ?: "").take(240) }
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.chart_result_title))
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                activity.startActivity(Intent.createChooser(send, activity.getString(R.string.share)))
            }) { Text(stringResource(id = R.string.share)) }
            if (!(hasVip || hasPro)) {
                Button(onClick = { showPaywall = true }) { Text(stringResource(id = R.string.go_paywall)) }
            }
        }
    }
    PaywallSheet(
        activity = activity,
        visible = showPaywall,
        onDismiss = { showPaywall = false },
        onPurchased = {
            scope.launch {
                val ent = Entitlement.from(activity)
                hasVip = ent.hasVip()
                hasPro = chart?.kind?.let { ent.hasPro(it) } ?: false
                showPaywall = false
            }
        }
    )
}
