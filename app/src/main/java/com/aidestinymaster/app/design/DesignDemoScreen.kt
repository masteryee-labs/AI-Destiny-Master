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

@Composable
fun DesignDemoScreen(activity: ComponentActivity) {
    val vm: DesignViewModel = viewModel(viewModelStoreOwner = activity)
    val summaryState = remember { mutableStateOf<DesignSummary?>(null) }
    val themeState = remember { mutableStateOf(NodeColorTheme.Vitality) }
    val liveReloadState = remember { mutableStateOf(false) }
    val channelsPresetState = remember { mutableStateOf("abstract_channels.json") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("天賦設計圖 Demo", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))

        Button(onClick = {
            // 使用固定時間的 stub 生成可重現之資料
            val instant = java.time.Instant.ofEpochSecond(1_700_000_000)
            vm.updateFromStub(instant)
            summaryState.value = vm.state.summary
        }, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)) {
            Text("生成範例資料並顯示圖像")
        }

        Spacer(Modifier.height(12.dp))
        // 主題切換 Segmented（以三個 OutlinedButton 模擬）
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val items = listOf(NodeColorTheme.Vitality, NodeColorTheme.Stability, NodeColorTheme.Insight)
            items.forEach { th ->
                val selected = themeState.value == th
                if (selected) {
                    Button(onClick = { /* no-op */ }, enabled = false) { Text(th.display) }
                } else {
                    OutlinedButton(onClick = { themeState.value = th }) { Text(th.display) }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("即時重載通道表（開發用）")
            Switch(
                checked = liveReloadState.value,
                onCheckedChange = { liveReloadState.value = it },
                colors = SwitchDefaults.colors()
            )
        }

        Spacer(Modifier.height(8.dp))
        // 通道樣式預設方案切換
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val presets = listOf(
                "Default" to "abstract_channels.json",
                "Thin" to "abstract_channels_thin.json",
                "Color" to "abstract_channels_color.json"
            )
            presets.forEach { (label, asset) ->
                val selected = channelsPresetState.value == asset
                if (selected) {
                    Button(onClick = { /* no-op */ }, enabled = false) { Text(label) }
                } else {
                    OutlinedButton(onClick = { channelsPresetState.value = asset }) { Text(label) }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // BodyGraph 繪製（若尚未有資料也可顯示空圖）
        BodyGraph(
            viewModel = vm,
            modifier = Modifier.weight(1f),
            theme = themeState.value,
            enableLiveReload = liveReloadState.value,
            channelsAssetName = channelsPresetState.value
        )

        Spacer(Modifier.height(8.dp))
        val summary = summaryState.value
        if (summary != null) {
            Text(summary.brief, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            summary.highlights.take(3).forEach { h ->
                Text("• $h", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            Text("點擊上方按鈕以生成範例資料", style = MaterialTheme.typography.bodySmall)
        }
    }
}
