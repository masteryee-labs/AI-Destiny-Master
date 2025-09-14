package com.aidestinymaster.app.settings.noads

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.aidestinymaster.app.report.ReportViewModel
import com.aidestinymaster.data.prefs.UserPrefsRepository
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(activity: ComponentActivity) {
    val ctx = activity
    val scope = rememberCoroutineScope()
    val prefsRepo = remember { UserPrefsRepository.from(ctx) }
    val theme by prefsRepo.themeFlow.collectAsState(initial = "system")
    val notifEnabled by prefsRepo.notifEnabledFlow.collectAsState(initial = false)

    // Keep signature-compatible and keep app settings visible for no-ads debug builds
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("設定 (No-Ads Debug)", style = MaterialTheme.typography.titleLarge)

        Text("主題：$theme")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { scope.launch { prefsRepo.setTheme("system") } }) { Text("系統") }
            Button(onClick = { scope.launch { prefsRepo.setTheme("light") } }) { Text("亮") }
            Button(onClick = { scope.launch { prefsRepo.setTheme("dark") } }) { Text("暗") }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("啟用通知")
            Switch(checked = notifEnabled, onCheckedChange = { enabled ->
                scope.launch { prefsRepo.setNotifEnabled(enabled) }
            })
        }

        // Demonstrate that ViewModels still work in this variant (no ads involved)
        val reportVm = remember { ViewModelProvider(activity)[ReportViewModel::class.java] }
        Button(onClick = { /* no-op for now in no-ads variant */ }) { Text("Report VM Ready: ${reportVm.hashCode()}") }

        Text("此變體不包含任何 Google Mobile Ads 相關功能與相依，僅用於啟動/初始化驗證。")
    }
}
