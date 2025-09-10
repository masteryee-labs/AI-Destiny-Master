package com.aidestinymaster.app.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun SettingsScreen(activity: androidx.activity.ComponentActivity) {
    val viewModel = remember { ViewModelProvider(activity)[SettingsViewModel::class.java] }
    val ctx = activity
    val syncEnabled by viewModel.syncEnabled.collectAsState()
    val email by viewModel.email.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val acc = task.getResult(ApiException::class.java)
            viewModel.onSignedIn(acc?.email)
        } catch (_: Exception) { viewModel.onSignedIn(null) }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("設定", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("登入狀態：${email ?: "未登入"}")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { launcher.launch(GoogleAuthManager.getSignInClient(ctx).signInIntent) }) { Text("Sign In") }
            Button(onClick = { GoogleAuthManager.signOut(ctx) { viewModel.onSignedIn(null) } }) { Text("Sign Out") }
            Button(onClick = { com.aidestinymaster.sync.SyncBatchScheduler.scheduleNow(ctx) }) { Text("立即同步") }
            Button(onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("aidm://onboarding?from=settings"))
                ctx.startActivity(intent)
            }) { Text("前往導覽") }
            Button(onClick = {
                androidx.lifecycle.lifecycleScope.launchWhenStarted(activity.lifecycle) {
                    com.aidestinymaster.app.prefs.UserPrefs.setOnboardingDone(ctx, false)
                }
            }) { Text("重置導覽") }
        }
        val scope = rememberCoroutineScope()
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("啟用同步")
            Switch(checked = syncEnabled, onCheckedChange = { enabled ->
                scope.launch { viewModel.setSyncEnabled(enabled) }
            })
        }
        if (error != null) {
            Text("錯誤：$error", color = MaterialTheme.colorScheme.error)
        }
    }
}
