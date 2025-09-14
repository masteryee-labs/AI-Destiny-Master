package com.aidestinymaster.app.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import com.aidestinymaster.app.nav.Routes
import com.aidestinymaster.data.prefs.UserPrefsRepository
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.first
import com.aidestinymaster.app.BuildConfig
import androidx.compose.material3.Card
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.aidestinymaster.app.ui.DesignTokens
import com.aidestinymaster.app.ui.glassCardColors
import com.aidestinymaster.app.ui.glassModifier

@Composable
fun OnboardingScreen(activity: ComponentActivity, nav: NavController, from: String? = null) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val prefsRepo = UserPrefsRepository.from(activity)

    var agreeTerms by remember { mutableStateOf(false) }
    var agreePrivacy by remember { mutableStateOf(false) }
    var enableSync by remember { mutableStateOf(false) }

    // 預設從偏好讀取同步開關（若有）
    LaunchedEffect(Unit) {
        runCatching { prefsRepo.syncEnabledFlow.first() }.onSuccess { enableSync = it }.onFailure { enableSync = false }
    }

    Column(Modifier.fillMaxWidth().padding(DesignTokens.Spacing.L.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.M.dp)) {
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(12.dp)) {
                    Text("歡迎使用 AI 命理大師", style = MaterialTheme.typography.titleLarge)
                    Text("本應用提供紫微、八字、占星等功能，並支援離線 AI 解讀。")
                }
            }
        }

        // 條款與隱私同意（小卡）
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                        Checkbox(checked = agreeTerms, onCheckedChange = { agreeTerms = it })
                        Text("我已閱讀並同意服務條款")
                        OutlinedButton(onClick = {
                            runCatching {
                                val uri = Uri.parse(BuildConfig.TERMS_URL)
                                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }) { Text("查看條款") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                        Checkbox(checked = agreePrivacy, onCheckedChange = { agreePrivacy = it })
                        Text("我已閱讀並同意隱私政策")
                        OutlinedButton(onClick = {
                            runCatching {
                                val uri = Uri.parse(BuildConfig.PRIVACY_URL)
                                activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }) { Text("查看隱私") }
                    }
                }
            }
        }

        // 可選：雲端同步開關
        // 啟用同步（小卡）
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                        Text("啟用雲端同步（可於設定中更改）")
                        Switch(checked = enableSync, onCheckedChange = { v ->
                            enableSync = v
                            scope.launch { prefsRepo.setSyncEnabled(v) }
                        })
                    }
                }
            }
        }

        // CTA 小卡：開始使用
        Card(Modifier.fillMaxWidth().glassModifier(), colors = glassCardColors()) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp)) {
                    // 快速導入出生資料（整合於 CTA 卡內）
                    OutlinedButton(onClick = { nav.navigate(Routes.ChartInput.replace("{kind}", "natal")) }) {
                        Text("導入出生資料（星盤）")
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.S.dp))
                    Button(onClick = {
                        scope.launch {
                            if (!agreeTerms || !agreePrivacy) return@launch
                            prefsRepo.setOnboardingDone(true)
                            val target = if (from == "settings") Routes.Settings else Routes.Home
                            nav.navigate(target) { popUpTo(Routes.Onboarding) { inclusive = true } }
                        }
                    }, enabled = agreeTerms && agreePrivacy) { Text("開始使用") }
                    Spacer(Modifier.height(DesignTokens.Spacing.S.dp))
                    Text(
                        "完成必需同意事項後才可開始使用",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
