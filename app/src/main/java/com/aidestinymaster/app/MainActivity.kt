package com.aidestinymaster.app

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import android.content.pm.ApplicationInfo
import com.aidestinymaster.sync.GoogleAuthManager
import com.aidestinymaster.data.db.ReportEntity
import com.aidestinymaster.data.repository.ReportRepository
import com.aidestinymaster.sync.ReportSyncBridge
import com.aidestinymaster.app.settings.SettingsScreen
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.aidestinymaster.app.report.ReportViewModel
import com.aidestinymaster.app.theme.AppTheme
import com.aidestinymaster.data.prefs.UserPrefsRepository
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import com.aidestinymaster.app.R
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.net.Uri
import com.aidestinymaster.app.ai.AiReportWorker
import com.aidestinymaster.core.ai.ModelInstaller
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private var navIntentState: MutableState<Intent?>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("AIDMModelInit", "MainActivity onCreate")
        // Ensure model assets are installed and validated on first launch; logs will be emitted from ModelInstaller
        try {
            val ok = ModelInstaller.installIfNeeded(applicationContext)
            Log.i("AIDMModelInit", "ModelInstaller.installIfNeeded result=$ok")
        } catch (t: Throwable) {
            Log.e("AIDMModelInit", "ModelInstaller call failed: ${t.message}", t)
        }
        // Preserve original intent for our own deep link handling, but prevent
        // Navigation from auto-handling it during setGraph (which was causing crashes).
        val originalIntent = intent
        val triggerE2E = originalIntent?.data?.scheme == "aidm" && originalIntent.data?.host == "e2e" && originalIntent.data?.path == "/generate"
        setIntent(Intent(this, MainActivity::class.java))
        setContent {
            val state = remember { mutableStateOf<Intent?>(originalIntent) }
            navIntentState = state
            val ctx = LocalContext.current
            val prefs = remember { UserPrefsRepository.from(ctx) }
            val settings by com.aidestinymaster.app.settings.SettingsPrefs.flow(ctx).collectAsState(initial = com.aidestinymaster.app.settings.SettingsPrefs.Settings())
            val themePref by prefs.themeFlow.collectAsState(initial = "system")
            val fontScalePref by prefs.fontScaleFlow.collectAsState(initial = "normal")
            val langPref by prefs.langFlow.collectAsState(initial = "zh-TW")
            val dark = when (themePref) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            val fontScale = when (fontScalePref) {
                "small" -> 0.9f
                "normal" -> 1.0f
                "large" -> 1.1f
                "extra" -> 1.25f
                else -> 1.0f
            }
            // Apply per-app locale without changing system language
            LaunchedEffect(langPref) {
                val tags = when (langPref) {
                    "en" -> "en"
                    else -> "zh-TW"
                }
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
            }
            AppTheme(darkTheme = dark, fontScale = fontScale) {
                // Disable splash overlay by default to avoid blur/cover feel on entry
                var splashDone by remember { mutableStateOf(true) }
                // First-launch model install gate
                var installDone by remember { mutableStateOf(false) }
                var installOk by remember { mutableStateOf(false) }
                var installError by remember { mutableStateOf<String?>(null) }
                LaunchedEffect(Unit) {
                    try {
                        Log.i("AIDMModelInit", "gate starting ModelInstaller.installIfNeeded")
                        val ok = ModelInstaller.installIfNeeded(applicationContext)
                        Log.i("AIDMModelInit", "gate ModelInstaller.installIfNeeded ok=$ok")
                        installOk = ok
                        installDone = true
                    } catch (t: Throwable) {
                        installError = t.message
                        Log.e("AIDMModelInit", "gate ModelInstaller failed: ${t.message}", t)
                        installOk = false
                        installDone = true
                    }
                }
                // Diagnostic: verify model files presence after UI composition starts
                LaunchedEffect(Unit) {
                    try {
                        val modelsDir = java.io.File(applicationContext.filesDir, "models")
                        val onnx = java.io.File(modelsDir, "tinyllama-q8.onnx")
                        val sha = java.io.File(modelsDir, "tinyllama-q8.onnx.sha256")
                        Log.i("AIDMModelInit", "diag modelsDir=${modelsDir.absolutePath} exists=${modelsDir.exists()} list=${modelsDir.list()?.joinToString()}")
                        Log.i("AIDMModelInit", "diag onnx.exists=${onnx.exists()} size=${if (onnx.exists()) onnx.length() else -1}")
                        Log.i("AIDMModelInit", "diag sha.exists=${sha.exists()} size=${if (sha.exists()) sha.length() else -1}")
                    } catch (t: Throwable) {
                        Log.e("AIDMModelInit", "diag failed: ${t.message}", t)
                    }
                }
                Box(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
                    if (installDone && installOk) {
                        com.aidestinymaster.app.nav.AppNav(this@MainActivity, intent = state.value)
                    } else {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                            Text(text = stringResource(id = R.string.app_name), style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(12.dp))
                            if (!installDone) {
                                Text("首次啟動初始化中…（模型資產解壓與校驗）")
                            } else {
                                Text("初始化失敗，請重試或回報。")
                                installError?.let { Text("錯誤：$it") }
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = {
                                    installDone = false
                                    installOk = false
                                    installError = null
                                    // Retry in background
                                    lifecycleScope.launch {
                                        try {
                                            val ok = ModelInstaller.installIfNeeded(applicationContext)
                                            installOk = ok
                                        } catch (t: Throwable) {
                                            installError = t.message
                                            installOk = false
                                        } finally {
                                            installDone = true
                                        }
                                    }
                                }) { Text(stringResource(id = R.string.retry)) }
                            }
                        }
                    }
                    // Premium splash overlay (transient)
                    // Splash overlay disabled; keep code for future enable if needed
                }
                // Debug deep link: aidm://settings?lang=en|zh-TW&reduce=0|1
                LaunchedEffect(state.value) {
                    val data = state.value?.data
                    if (data?.scheme == "aidm") {
                        when (data.host) {
                            "settings" -> {
                                val lang = data.getQueryParameter("lang")
                                val reduce = data.getQueryParameter("reduce")
                                var needRecreate = false
                                if (!lang.isNullOrBlank()) {
                                    prefs.setLang(lang)
                                    needRecreate = true
                                }
                                if (!reduce.isNullOrBlank()) {
                                    val enabled = reduce == "1" || reduce.equals("true", ignoreCase = true)
                                    prefs.setReduceMotion(enabled)
                                }
                                if (needRecreate) {
                                    // 讓語系立即生效
                                    this@MainActivity.recreate()
                                }
                            }
                            "onboarding" -> {
                                val done = data.getQueryParameter("done")
                                if (!done.isNullOrBlank()) {
                                    val enabled = done == "1" || done.equals("true", ignoreCase = true)
                                    prefs.setOnboardingDone(enabled)
                                }
                            }
                        }
                    }
                }
                // E2E: if launched with aidm://e2e/generate, auto-create a report and start AI, then deep-link to report screen
                val vm = remember { ViewModelProvider(this@MainActivity)[ReportViewModel::class.java] }
                var e2eDone by remember { mutableStateOf(false) }
                LaunchedEffect(triggerE2E) {
                    if (triggerE2E && !e2eDone) {
                        // create a simple report
                        val type = "demo"
                        val content = "Auto E2E Trigger at " + System.currentTimeMillis()
                        val repo = ReportRepository.from(this@MainActivity)
                        val id = repo.createFromAi(type, chartId = "", content = content)
                        // Mark as favorite to ensure Home has favorite items for a11y
                        com.aidestinymaster.app.report.ReportPrefs.toggleFav(this@MainActivity, id)
                        e2eDone = true
                        // enqueue AI worker (locale from prefs)
                        AiReportWorker.enqueue(this@MainActivity, id, emptyList(), mode = "default", locale = langPref, sectionsPreset = 8)
                        // ensure onboarding is marked done so NavGraph starts at Home/Report
                        UserPrefsRepository.from(this@MainActivity).setOnboardingDone(true)
                        // navigate to report via deep link
                        state.value = Intent(Intent.ACTION_VIEW, Uri.parse("aidm://report/$id"))
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navIntentState?.value = intent
    }
}

@Suppress("DEPRECATION")
@Composable
fun App() {
    MaterialTheme {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        val activity = ctx as ComponentActivity
        val viewModel = remember { ViewModelProvider(activity)[com.aidestinymaster.app.report.ReportViewModel::class.java] }
        val lastIdState by viewModel.lastId.collectAsState(null)
        val currentState by viewModel.current.collectAsState(null)

        val scope = rememberCoroutineScope()

        var accountEmail by remember { mutableStateOf(GoogleAuthManager.signIn(ctx)?.email) }
        val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val acc = task.getResult(ApiException::class.java)
                accountEmail = acc?.email
            } catch (_: Exception) {
                // ignore
            }
        }

        var type by remember { mutableStateOf("demo") }
        var content by remember { mutableStateOf("Hello from Compose at " + System.currentTimeMillis()) }
        var showSettings by remember { mutableStateOf(false) }

        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showSettings = false }) { Text(stringResource(id = R.string.debug)) }
                Button(onClick = { showSettings = true }) { Text(stringResource(id = R.string.settings)) }
            }
            Spacer(Modifier.height(12.dp))
            if (showSettings) {
                SettingsScreen(activity)
            } else {
                Text(stringResource(id = R.string.report_e2e_debug), style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text(stringResource(id = R.string.type)) })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text(stringResource(id = R.string.content)) })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { viewModel.create(type, content) } }) { Text(stringResource(id = R.string.create_report)) }
                    Button(onClick = { scope.launch { viewModel.push() } }, enabled = lastIdState != null) { Text(stringResource(id = R.string.push)) }
                    Button(onClick = { scope.launch { viewModel.pull() } }, enabled = lastIdState != null) { Text(stringResource(id = R.string.pull)) }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val client = GoogleAuthManager.getSignInClient(ctx)
                        launcher.launch(client.signInIntent)
                    }) { Text(stringResource(id = R.string.sign_in)) }
                    Button(onClick = {
                        GoogleAuthManager.signOut(ctx) { accountEmail = null }
                    }) { Text(stringResource(id = R.string.sign_out)) }
                }
                Spacer(Modifier.height(16.dp))
                Text(stringResource(id = R.string.last_id, lastIdState ?: "(none)"))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(id = R.string.signed_in, accountEmail ?: "(not signed)"))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(id = R.string.title, currentState?.title ?: "-"))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(id = R.string.updated, currentState?.updatedAt ?: 0))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(id = R.string.summary, currentState?.summary ?: "-"))
            }
        }
    }
}


