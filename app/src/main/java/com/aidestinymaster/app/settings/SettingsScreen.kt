package com.aidestinymaster.app.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("設定", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("登入狀態：${email ?: "未登入"}")
        }
        val scope = rememberCoroutineScope()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
            }) { Text("Sign In with Google") }
            Button(onClick = {
                // Credential Manager 無全域 signOut；用舊 Identity 清單清理 Google 登入快取
                com.google.android.gms.auth.api.identity.Identity.getSignInClient(ctx).signOut()
                    .addOnCompleteListener { viewModel.onSignedIn(null) }
            }) { Text("Sign Out") }
            Button(onClick = { com.aidestinymaster.sync.SyncBatchScheduler.scheduleNow(ctx) }) { Text("立即同步") }
            Button(onClick = {
                scope.launch { prefsRepo.setOnboardingDone(false) }
            }) { Text("前往導覽") }
            Button(onClick = {
                scope.launch { prefsRepo.setOnboardingDone(false) }
            }) { Text("重置導覽") }
            val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                scope.launch { prefsRepo.setNotifEnabled(granted) }
            }
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33) {
                    notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            }) { Text("測試通知權限") }
        }

        // Language（App 通用）
        val lang by prefsRepo.langFlow.collectAsState(initial = "zh-TW")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("語言（App）：$lang")
            Button(onClick = { scope.launch { prefsRepo.setLang("zh-TW") } }) { Text("繁中") }
            Button(onClick = { scope.launch { prefsRepo.setLang("en") } }) { Text("English") }
        }

        // Astro 專屬設定：語言覆蓋與宮位系統
        val astro by SettingsPrefs.flow(ctx).collectAsState(initial = SettingsPrefs.Settings())
        Text("星象設定", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("星象語言：${when (astro.language) { "system" -> "系統預設"; "zh" -> "繁中"; else -> "English" }}")
            Button(onClick = { scope.launch { SettingsPrefs.setLanguage(ctx, "system") } }) { Text("系統預設") }
            Button(onClick = { scope.launch { SettingsPrefs.setLanguage(ctx, "zh") } }) { Text("繁中") }
            Button(onClick = { scope.launch { SettingsPrefs.setLanguage(ctx, "en") } }) { Text("English") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val housesLabel = when (astro.houses) { "ASC" -> "Whole Sign（Asc 起點）"; "ARIES" -> "Whole Sign（0°牡羊）"; else -> "Placidus" }
            Text("宮位系統：$housesLabel")
            Button(onClick = { scope.launch { SettingsPrefs.setHouses(ctx, "ASC") } }) { Text("Asc 起點") }
            Button(onClick = { scope.launch { SettingsPrefs.setHouses(ctx, "ARIES") } }) { Text("0°牡羊") }
            Button(onClick = { scope.launch { SettingsPrefs.setHouses(ctx, "PLACIDUS") } }) { Text("Placidus") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val modeLabel = when (astro.highLatFallback) { "ASC" -> "High-Lat Fallback：Whole Sign（Asc）"; else -> "High-Lat Fallback：Regiomontanus（預留）" }
            Text(modeLabel)
            Button(onClick = { scope.launch { SettingsPrefs.setHighLatFallback(ctx, "ASC") } }) { Text("高緯回退：Asc") }
            Button(onClick = { scope.launch { SettingsPrefs.setHighLatFallback(ctx, "REGIO") } }) { Text("高緯回退：Regio") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("診斷模式：${if (astro.diagnostics) "ON" else "OFF"}")
            Button(onClick = { scope.launch { SettingsPrefs.setDiagnostics(ctx, true) } }) { Text("開啟") }
            Button(onClick = { scope.launch { SettingsPrefs.setDiagnostics(ctx, false) } }) { Text("關閉") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("設定將即時套用於 Home 星象卡。")
            Button(onClick = {
                // 導回 Home 以重整星象卡
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("aidm://home"))
                ctx.startActivity(intent)
            }) { Text("重整星象卡") }
        }

        // Theme
        val theme by prefsRepo.themeFlow.collectAsState(initial = "system")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("主題：$theme")
            Button(onClick = { scope.launch { prefsRepo.setTheme("system") } }) { Text("系統") }
            Button(onClick = { scope.launch { prefsRepo.setTheme("light") } }) { Text("亮") }
            Button(onClick = { scope.launch { prefsRepo.setTheme("dark") } }) { Text("暗") }
        }

        // Notifications
        val notifEnabled by prefsRepo.notifEnabledFlow.collectAsState(initial = false)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("啟用通知")
            Switch(checked = notifEnabled, onCheckedChange = { enabled ->
                scope.launch { prefsRepo.setNotifEnabled(enabled) }
            })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("啟用同步")
            Switch(checked = syncEnabled, onCheckedChange = { enabled ->
                scope.launch { viewModel.setSyncEnabled(enabled) }
            })
        }

        // ---- Debug: Test Ads ----
        Text("測試廣告（Debug 專用）", style = MaterialTheme.typography.titleMedium)

        // Interstitial state
        var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
        var interstitialStatus by remember { mutableStateOf("未載入") }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                interstitialStatus = "載入中..."
                InterstitialAd.load(
                    ctx,
                    // 由 BuildConfig 提供（Debug=測試ID，Release=正式ID/或空值）
                    BuildConfig.ADMOB_INTERSTITIAL_ID,
                    AdRequest.Builder().build(),
                    object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) {
                            interstitialAd = ad
                            interstitialStatus = "已載入，可顯示"
                            Log.d("AIDestinyMaster", "Test Interstitial loaded")
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    Log.d("AIDestinyMaster", "Interstitial dismissed")
                                    interstitialAd = null
                                }
                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    Log.w("AIDestinyMaster", "Interstitial failed to show: ${adError.message}")
                                    interstitialAd = null
                                }
                                override fun onAdShowedFullScreenContent() {
                                    Log.d("AIDestinyMaster", "Interstitial showed")
                                }
                            }
                        }
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            interstitialStatus = "載入失敗：${loadAdError.message}"
                            Log.w("AIDestinyMaster", "Test Interstitial failed: ${loadAdError.message}")
                            interstitialAd = null
                        }
                    }
                )
            }) { Text("載入測試插頁式廣告") }

            Button(onClick = {
                val ad = interstitialAd
                if (ad != null) {
                    ad.show(activity)
                } else {
                    interstitialStatus = "尚未載入，請先載入"
                }
            }) { Text("顯示插頁式") }
        }
        Text("狀態：$interstitialStatus")

        Spacer(Modifier.padding(4.dp))

        // Banner state
        var showBanner by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { showBanner = !showBanner }) {
                Text(if (showBanner) "隱藏測試橫幅" else "顯示測試橫幅")
            }
        }
        if (showBanner) {
            AndroidView(
                factory = { context ->
                    AdView(context).apply {
                        // 由 BuildConfig 提供
                        adUnitId = BuildConfig.ADMOB_BANNER_ID
                        setAdSize(AdSize.BANNER)
                        loadAd(AdRequest.Builder().build())
                    }
                },
                update = { _ ->
                    // Optionally reload
                }
            )
        }

        // Rewarded Ads
        Spacer(Modifier.padding(4.dp))
        Text("測試獎勵型廣告（Rewarded）", style = MaterialTheme.typography.titleMedium)
        var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
        var rewardedStatus by remember { mutableStateOf("未載入") }
        var lastReward by remember { mutableStateOf("-") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                rewardedStatus = "載入中..."
                RewardedAd.load(
                    ctx,
                    BuildConfig.ADMOB_REWARDED_ID,
                    AdRequest.Builder().build(),
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            rewardedAd = ad
                            rewardedStatus = "已載入，可顯示"
                            Log.d("AIDestinyMaster", "Test Rewarded loaded")
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    Log.d("AIDestinyMaster", "Rewarded dismissed")
                                    rewardedAd = null
                                }
                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    Log.w("AIDestinyMaster", "Rewarded failed to show: ${adError.message}")
                                    rewardedAd = null
                                }
                                override fun onAdShowedFullScreenContent() {
                                    Log.d("AIDestinyMaster", "Rewarded showed")
                                }
                            }
                        }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedStatus = "載入失敗：${error.message}"
                            Log.w("AIDestinyMaster", "Test Rewarded failed: ${error.message}")
                            rewardedAd = null
                        }
                    }
                )
            }) { Text("載入 Rewarded") }

            Button(onClick = {
                val ad = rewardedAd
                if (ad != null) {
                    ad.show(activity) { rewardItem: RewardItem ->
                        lastReward = "type=${rewardItem.type}, amount=${rewardItem.amount}"
                        Log.d("AIDestinyMaster", "Rewarded earned: $lastReward")
                    }
                } else {
                    rewardedStatus = "尚未載入，請先載入"
                }
            }) { Text("顯示 Rewarded") }
        }
        Text("Rewarded 狀態：$rewardedStatus / 上次回饋：$lastReward")

        // Rewarded Interstitial Ads
        Spacer(Modifier.padding(4.dp))
        Text("測試獎勵型插頁式（Rewarded Interstitial）", style = MaterialTheme.typography.titleMedium)
        var rewardedInterstitial by remember { mutableStateOf<RewardedInterstitialAd?>(null) }
        var rewardedInterstitialStatus by remember { mutableStateOf("未載入") }
        var lastRewardRi by remember { mutableStateOf("-") }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                rewardedInterstitialStatus = "載入中..."
                RewardedInterstitialAd.load(
                    ctx,
                    BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID,
                    AdRequest.Builder().build(),
                    object : RewardedInterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedInterstitialAd) {
                            rewardedInterstitial = ad
                            rewardedInterstitialStatus = "已載入，可顯示"
                            Log.d("AIDestinyMaster", "Test RewardedInterstitial loaded")
                            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    Log.d("AIDestinyMaster", "RewardedInterstitial dismissed")
                                    rewardedInterstitial = null
                                }
                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    Log.w("AIDestinyMaster", "RewardedInterstitial failed to show: ${adError.message}")
                                    rewardedInterstitial = null
                                }
                                override fun onAdShowedFullScreenContent() {
                                    Log.d("AIDestinyMaster", "RewardedInterstitial showed")
                                }
                            }
                        }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            rewardedInterstitialStatus = "載入失敗：${error.message}"
                            Log.w("AIDestinyMaster", "Test RewardedInterstitial failed: ${error.message}")
                            rewardedInterstitial = null
                        }
                    }
                )
            }) { Text("載入 Rewarded Interstitial") }

            Button(onClick = {
                val ad = rewardedInterstitial
                if (ad != null) {
                    ad.show(activity) { rewardItem: RewardItem ->
                        lastRewardRi = "type=${rewardItem.type}, amount=${rewardItem.amount}"
                        Log.d("AIDestinyMaster", "RewardedInterstitial earned: $lastRewardRi")
                    }
                } else {
                    rewardedInterstitialStatus = "尚未載入，請先載入"
                }
            }) { Text("顯示 Rewarded Interstitial") }
        }
        Text("Rewarded Interstitial 狀態：$rewardedInterstitialStatus / 上次回饋：$lastRewardRi")

        if (error != null) {
            Text("錯誤：$error", color = MaterialTheme.colorScheme.error)
        }
    }
}
