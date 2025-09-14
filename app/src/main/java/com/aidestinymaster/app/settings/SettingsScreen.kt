package com.aidestinymaster.app.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.util.Log
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.ui.res.stringResource
import com.aidestinymaster.app.R
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.aidestinymaster.sync.GoogleAuthManager
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.Credential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.aidestinymaster.data.prefs.UserPrefsRepository
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.aidestinymaster.app.BuildConfig
import com.aidestinymaster.billing.BillingManager
import com.aidestinymaster.data.repository.PurchaseRepository
import com.aidestinymaster.data.db.PurchaseEntity
import com.aidestinymaster.app.ads.RewardedAdDialog
import com.aidestinymaster.billing.EntitlementEvents
import com.aidestinymaster.app.ads.RewardedAdsManager
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.AdapterStatus
import androidx.compose.material3.Card
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.aidestinymaster.app.ui.DesignTokens
import com.aidestinymaster.app.ui.glassModifier
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface

@Composable
private fun SectionHeader(icon: ImageVector, title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(activity: androidx.activity.ComponentActivity) {
    val viewModel = remember { ViewModelProvider(activity)[SettingsViewModel::class.java] }
    val ctx = activity
    val prefsRepo = remember { UserPrefsRepository.from(ctx) }
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val email by viewModel.email.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    // Credential Manager Sign-in with Google
    val credentialManager = remember { CredentialManager.create(ctx) }

    val scroll = rememberScrollState()
    // Use solid card background on Settings to avoid perceived overlap from translucent glass
    val cardColors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    Surface(color = MaterialTheme.colorScheme.background) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(scroll)
            .padding(start = DesignTokens.Spacing.L.dp, top = DesignTokens.Spacing.L.dp, end = DesignTokens.Spacing.L.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.L.dp)
    ) {
        // 頂部統一風格卡片（品牌高光條）
        Card(Modifier.fillMaxWidth().glassModifier(), colors = cardColors) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(id = R.string.settings), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        // Rewarded 狀態（只在 Debug 顯示，以避免干擾正式版設定頁）
        val showDebug = BuildConfig.SHOW_DEBUG_UI
        if (showDebug) {
            val dailyMaxTop = RewardedAdsManager.getDailyMax()
            var dailyTop by remember { mutableStateOf(RewardedAdsManager.getDailyCount(ctx)) }
            var cooldownSecTop by remember { mutableStateOf(RewardedAdsManager.getCooldownRemainingSeconds(ctx)) }
            LaunchedEffect(Unit) {
                while (true) {
                    dailyTop = RewardedAdsManager.getDailyCount(ctx)
                    cooldownSecTop = RewardedAdsManager.getCooldownRemainingSeconds(ctx)
                    kotlinx.coroutines.delay(1000)
                }
            }
            val canShow = cooldownSecTop == 0 && dailyTop < dailyMaxTop
            val statusColor = if (canShow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val statusIcon = if (canShow) "✅" else "⏳"
            Text(
                text = "$statusIcon Rewarded 狀態：今日 $dailyTop / $dailyMaxTop 次；冷卻剩餘 $cooldownSecTop 秒",
                color = statusColor,
                modifier = Modifier.semantics {
                    contentDescription = (
                        if (canShow) "獎勵廣告可播放。" else "獎勵廣告暫不可播放。"
                    ) + "今日 $dailyTop 次，共 $dailyMaxTop 次。冷卻剩餘 $cooldownSecTop 秒。"
                }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
            val loginText = stringResource(id = R.string.signed_in, (email ?: "(not signed)"))
            Text(loginText)
        }
        val scope = rememberCoroutineScope()
        FlowRow(
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = "account_and_misc_actions" },
            horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
            verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)
        ) {
            Button(onClick = {
                val webClientId = runCatching {
                    val clazz = Class.forName("${ctx.packageName}.BuildConfig")
                    val field = clazz.getField("GOOGLE_WEB_CLIENT_ID")
                    field.get(null) as String
                }.getOrDefault("")
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                scope.launch {
                    try {
                        val response = credentialManager.getCredential(context = ctx, request = request)
                        val cred: Credential = response.credential
                        if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleCred = GoogleIdTokenCredential.createFrom(cred.data)
                            val emailAddr = googleCred.id
                            viewModel.onSignedIn(emailAddr)
                        } else {
                            viewModel.onSignedIn(null)
                        }
                    } catch (t: Throwable) {
                        viewModel.onSignedIn(null)
                    }
                }
            }) { Text(stringResource(id = R.string.sign_in)) }
            Button(modifier = run {
                val desc = stringResource(id = R.string.sign_out)
                Modifier.semantics { contentDescription = desc }
            }, onClick = {
                // Credential Manager 無全域 signOut；用舊 Identity 清單清理 Google 登入快取
                com.google.android.gms.auth.api.identity.Identity.getSignInClient(ctx).signOut()
                    .addOnCompleteListener { viewModel.onSignedIn(null) }
            }) { Text(stringResource(id = R.string.sign_out)) }
            Button(modifier = Modifier.semantics { contentDescription = "立即同步" }, onClick = { com.aidestinymaster.sync.SyncBatchScheduler.scheduleNow(ctx) }) { Text("立即同步") }
            val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                scope.launch { prefsRepo.setNotifEnabled(granted) }
            }
            // 減少測試按鈕露出（正式版一致性），僅保留必要權限請求入口
            Button(modifier = Modifier.semantics { contentDescription = "請求通知權限" }, onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }) { Text("啟用通知權限") }
            Button(onClick = {
                // 隱私政策連結（可於未來以 BuildConfig 或 DataStore 注入正式 URL）
                val url = runCatching {
                    val f = BuildConfig::class.java.getField("PRIVACY_URL")
                    (f.get(null) as? String).orEmpty()
                }.getOrDefault("").ifEmpty { "https://your-name.github.io/ai-destiny-master-privacy/" }
                ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }) { Text("隱私政策") }
            Button(modifier = Modifier.semantics { contentDescription = "恢復購買" }, onClick = {
                // 恢復購買
                scope.launch {
                    val billing = BillingManager.from(ctx)
                    val purchases = billing.queryPurchasesAsync()
                    val mapped = purchases.map { p ->
                        val acked = billing.acknowledgeIfNeeded(p)
                        PurchaseEntity(
                            sku = p.products.firstOrNull() ?: "",
                            type = if (p.products.isNotEmpty()) "inapp" else "unknown",
                            state = p.purchaseState,
                            purchaseToken = p.purchaseToken,
                            acknowledged = acked || p.isAcknowledged,
                            updatedAt = System.currentTimeMillis()
                        )
                    }
                    PurchaseRepository.from(ctx).syncFromBilling(mapped)
                    // 通知權益可能已更新（恢復購買）
                    EntitlementEvents.emitChanged()
                }
            }) { Text("恢復購買") }
        }

        // App 一般設定（語言／重整星象卡／隱私／條款／支援）
        val lang by prefsRepo.langFlow.collectAsState(initial = "zh-TW")
        Card(Modifier.fillMaxWidth(), colors = cardColors) {
            Column(Modifier.padding(0.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.M.dp)) {
                    SectionHeader(icon = Icons.Filled.Settings, title = "App 一般設定", subtitle = "語言、首頁星象卡、隱私/條款/支援")
                    // 語言（App 通用）
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        maxItemsInEachRow = 3
                    ) {
                        Text("語言（App）：$lang")
                        Button(onClick = { scope.launch { prefsRepo.setLang("zh-TW") } }) { Text("繁中") }
                        Button(onClick = { scope.launch { prefsRepo.setLang("en") } }) { Text("English") }
                    }
                    HorizontalDivider()
                    // 重整星象卡
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)
                    ) {
                        Text("設定將即時套用於 Home 星象卡。")
                        Button(onClick = {
                            // 導回 Home 以重整星象卡
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("aidm://home"))
                            ctx.startActivity(intent)
                        }) { Text("重整星象卡") }
                    }
                    HorizontalDivider()
                    // 隱私 / 條款 / 支援
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)
                    ) {
                        Button(onClick = {
                            // 開啟隱私政策頁
                            val url = BuildConfig.PRIVACY_URL
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }) { Text("隱私政策") }
                        Button(onClick = {
                            // 開啟服務條款頁
                            val url = BuildConfig.TERMS_URL
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }) { Text("服務條款") }
                        Button(onClick = {
                            // 開啟 GitHub Issues 支援頁
                            val url = "https://github.com/masteryee-labs/ai-destiny-master-privacy/issues"
                            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }) { Text("開啟支援頁 (Issues)") }
                    }
                }
            }
        }

        // Astro 專屬設定：語言覆蓋與宮位系統（卡片化）
        val astro by SettingsPrefs.flow(ctx).collectAsState(initial = SettingsPrefs.Settings())
        Card(Modifier.fillMaxWidth(), colors = cardColors) {
            Column(Modifier.padding(0.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(3.dp).background(MaterialTheme.colorScheme.primary))
                Column(Modifier.padding(DesignTokens.Spacing.M.dp), verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.M.dp)) {
                    SectionHeader(icon = Icons.Filled.Language, title = "星象設定", subtitle = "語言覆蓋、宮位系統、高緯回退")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        maxItemsInEachRow = 3
                    ) {
                        Text("星象語言：${when (astro.language) { "system" -> "系統預設"; "zh" -> "繁中"; else -> "English" }}")
                        Button(onClick = { scope.launch { SettingsPrefs.setLanguage(ctx, "system") } }) { Text("系統預設") }
                        Button(onClick = { scope.launch { SettingsPrefs.setLanguage(ctx, "zh") } }) { Text("繁中") }
                        Button(onClick = { scope.launch { SettingsPrefs.setLanguage(ctx, "en") } }) { Text("English") }
                    }
                    HorizontalDivider()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        maxItemsInEachRow = 3
                    ) {
                        val housesLabel = when (astro.houses) { "ASC" -> "Whole Sign（Asc 起點）"; "ARIES" -> "Whole Sign（0°牡羊）"; else -> "Placidus" }
                        Text("宮位系統：$housesLabel")
                        Button(onClick = { scope.launch { SettingsPrefs.setHouses(ctx, "ASC") } }) { Text("Asc 起點") }
                        Button(onClick = { scope.launch { SettingsPrefs.setHouses(ctx, "ARIES") } }) { Text("0°牡羊") }
                        Button(onClick = { scope.launch { SettingsPrefs.setHouses(ctx, "PLACIDUS") } }) { Text("Placidus") }
                    }
                    // 說明文字：一般使用者可理解的解釋
                    Text("說明：Whole Sign 以整個星座為一宮，Asc 起點代表第一宮從上升點所屬星座開始；0°牡羊則固定以牡羊座為第一宮。Placidus 為常見的等時制，靠近高緯地區時可能出現極端宮位。")
                    HorizontalDivider()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        maxItemsInEachRow = 2
                    ) {
                        val modeLabel = when (astro.highLatFallback) { "ASC" -> "High-Lat Fallback：Whole Sign（Asc）"; else -> "High-Lat Fallback：Regiomontanus（預留）" }
                        Text(modeLabel)
                        Button(onClick = { scope.launch { SettingsPrefs.setHighLatFallback(ctx, "ASC") } }) { Text("高緯回退：Asc") }
                        Button(onClick = { scope.launch { SettingsPrefs.setHighLatFallback(ctx, "REGIO") } }) { Text("高緯回退：Regio") }
                    }
                    Text("說明：高緯回退用於緯度較高時的星盤計算，選擇 Asc 代表改用 Whole Sign 以確保可讀性；Regio 為替代演算法（預留）。")
                    HorizontalDivider()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.S.dp),
                        maxItemsInEachRow = 3
                    ) {
                        Text("診斷模式：${if (astro.diagnostics) "ON" else "OFF"}")
                        Button(onClick = { scope.launch { SettingsPrefs.setDiagnostics(ctx, true) } }) { Text("開啟") }
                        Button(onClick = { scope.launch { SettingsPrefs.setDiagnostics(ctx, false) } }) { Text("關閉") }
                    }
                }
            }
        }
        // 錯誤區塊顯示（保持在設定頁內部底部）
        if (error != null) {
            Text("錯誤：$error", color = MaterialTheme.colorScheme.error)
        }
    }
    }
}
