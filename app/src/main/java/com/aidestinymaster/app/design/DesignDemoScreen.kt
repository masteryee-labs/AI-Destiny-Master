package com.aidestinymaster.app.design

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aidestinymaster.features.design.BodyGraph
import com.aidestinymaster.features.design.DesignSummary
import com.aidestinymaster.features.design.DesignViewModel
import com.aidestinymaster.features.design.NodeColorTheme
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.aidestinymaster.data.prefs.UserPrefsRepository
import androidx.compose.material3.Card
import androidx.compose.foundation.background
import com.aidestinymaster.app.ui.DesignTokens
import androidx.compose.ui.graphics.Color

@Composable
fun DesignDemoScreen(activity: ComponentActivity) {
    val vm: DesignViewModel = viewModel(viewModelStoreOwner = activity)
    val summaryState = remember { mutableStateOf<DesignSummary?>(null) }
    val themeState = remember { mutableStateOf(NodeColorTheme.Vitality) }
    val liveReloadState = remember { mutableStateOf(false) }
    val channelsPresetState = remember { mutableStateOf("abstract_channels.json") }
    val ctx = LocalContext.current
    val prefs = remember { UserPrefsRepository.from(ctx) }
    val reduceMotion by prefs.reduceMotionFlow.collectAsState(initial = false)

    Column(
        modifier = Modifier.fillMaxSize().padding(DesignTokens.Spacing.L.dp),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.M.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 控制面板卡
        Card(Modifier.fillMaxSize()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Text("能量圖 Demo", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = {
                        val instant = java.time.Instant.ofEpochSecond(1_700_000_000)
                        vm.updateFromStub(instant)
                        summaryState.value = vm.state.summary
                    }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
                        Text("生成範例資料並顯示圖像")
                    }
                    // 主題切換（Segmented 模擬）
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                        val items = listOf(NodeColorTheme.Vitality, NodeColorTheme.Stability, NodeColorTheme.Insight)
                        items.forEach { th ->
                            val selected = themeState.value == th
                            if (selected) Button(onClick = { }, enabled = false) { Text(th.display) }
                            else OutlinedButton(onClick = { themeState.value = th }) { Text(th.display) }
                        }
                    }
                    // 即時重載開關
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                        Text("即時重載通道表（開發用）")
                        Switch(checked = liveReloadState.value, onCheckedChange = { liveReloadState.value = it }, colors = SwitchDefaults.colors())
                    }
                    // 通道樣式預設方案
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                        val presets = listOf(
                            "Default" to "abstract_channels.json",
                            "Thin" to "abstract_channels_thin.json",
                            "Color" to "abstract_channels_color.json"
                        )
                        presets.forEach { (label, asset) ->
                            val selected = channelsPresetState.value == asset
                            if (selected) Button(onClick = { }, enabled = false) { Text(label) }
                            else OutlinedButton(onClick = { channelsPresetState.value = asset }) { Text(label) }
                        }
                    }
                }
            }
        }

        // 圖像與摘要卡
        Card(Modifier.fillMaxSize()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    // BodyGraph 繪製（若尚未有資料也可顯示空圖）
                    BodyGraph(
                        viewModel = vm,
                        modifier = Modifier.weight(1f),
                        theme = themeState.value,
                        enableLiveReload = liveReloadState.value,
                        channelsAssetName = channelsPresetState.value,
                        reduceMotion = reduceMotion
                    )
                    val summary = summaryState.value
                    if (summary != null) {
                        Text(summary.brief, style = MaterialTheme.typography.bodyMedium)
                        summary.highlights.take(3).forEach { h -> Text("• $h", style = MaterialTheme.typography.bodySmall) }
                    } else {
                        Text("點擊上方按鈕以生成範例資料", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
