package com.aidestinymaster.app

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.ads.MobileAds
import android.content.pm.ApplicationInfo
import com.aidestinymaster.sync.GoogleAuthManager
import com.aidestinymaster.data.db.ReportEntity
import com.aidestinymaster.data.repository.ReportRepository
import com.aidestinymaster.sync.ReportSyncBridge
import com.aidestinymaster.app.settings.SettingsScreen
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.aidestinymaster.app.report.ReportViewModel

class MainActivity : ComponentActivity() {
    private var navIntentState: MutableState<Intent?>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // In debug builds, initialize MobileAds explicitly to test ads without provider auto-init
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            try {
                MobileAds.initialize(this) { status ->
                    Log.d("AIDestinyMaster", "MobileAds initialized. Adapters=${status.adapterStatusMap.keys}")
                }
            } catch (t: Throwable) {
                Log.w("AIDestinyMaster", "MobileAds init failed in debug: ${t.message}")
            }
        }
        // Preserve original intent for our own deep link handling, but prevent
        // Navigation from auto-handling it during setGraph (which was causing crashes).
        val originalIntent = intent
        setIntent(Intent(this, MainActivity::class.java))
        setContent {
            val state = remember { mutableStateOf<Intent?>(originalIntent) }
            navIntentState = state
            com.aidestinymaster.app.nav.AppNav(this, intent = state.value)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        navIntentState?.value = intent
    }
}

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

        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Top) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { showSettings = false }) { Text("Debug") }
                Button(onClick = { showSettings = true }) { Text("Settings") }
            }
            Spacer(Modifier.height(12.dp))
            if (showSettings) {
                SettingsScreen(activity)
            } else {
                Text("Report E2E Debug", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type") })
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Content") })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { scope.launch { viewModel.create(type, content) } }) { Text("Create Report") }
                    Button(onClick = { scope.launch { viewModel.push() } }, enabled = lastIdState != null) { Text("Push") }
                    Button(onClick = { scope.launch { viewModel.pull() } }, enabled = lastIdState != null) { Text("Pull") }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val client = GoogleAuthManager.getSignInClient(ctx)
                        launcher.launch(client.signInIntent)
                    }) { Text("Sign In") }
                    Button(onClick = {
                        GoogleAuthManager.signOut(ctx) { accountEmail = null }
                    }) { Text("Sign Out") }
                }
                Spacer(Modifier.height(16.dp))
                Text("Last ID: ${lastIdState ?: "(none)"}")
                Spacer(Modifier.height(8.dp))
                Text("Signed in: ${accountEmail ?: "(not signed)"}")
                Spacer(Modifier.height(8.dp))
                Text("Title: ${currentState?.title ?: "-"}")
                Spacer(Modifier.height(4.dp))
                Text("Updated: ${currentState?.updatedAt ?: 0}")
                Spacer(Modifier.height(4.dp))
                Text("Summary: ${currentState?.summary ?: "-"}")
            }
        }
    }
}


