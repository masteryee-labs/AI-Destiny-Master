package com.aidestinymaster.app.debug

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.aidestinymaster.app.report.ReportViewModel
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.PurchaseEntity
import com.aidestinymaster.data.repository.ChartRepository
import com.aidestinymaster.data.repository.WalletRepository
import com.aidestinymaster.sync.GoogleAuthManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

@Composable
fun DebugScreen(activity: ComponentActivity) {
    val ctx = activity
    val viewModel = remember { ViewModelProvider(activity)[ReportViewModel::class.java] }
    val lastIdState by viewModel.lastId.collectAsState(null)
    val currentState by viewModel.current.collectAsState(null)

    var accountEmail by remember { mutableStateOf(GoogleAuthManager.signIn(ctx)?.email) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try { val acc = task.getResult(ApiException::class.java); accountEmail = acc?.email } catch (_: Exception) {}
    }

    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf("demo") }
    var content by remember { mutableStateOf("Hello from Compose at " + System.currentTimeMillis()) }

    // Wallet + Chart + Purchase debug states
    val walletRepo = remember { WalletRepository.from(ctx) }
    var coins by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { coins = walletRepo.ensure().coins }

    val chartRepo = remember { ChartRepository.from(ctx) }
    var lastChartId by remember { mutableStateOf<String?>(null) }

    var sku by remember { mutableStateOf("iap_demo") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Top) {
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
            Button(onClick = { launcher.launch(GoogleAuthManager.getSignInClient(ctx).signInIntent) }) { Text("Sign In") }
            Button(onClick = { GoogleAuthManager.signOut(ctx) { accountEmail = null } }) { Text("Sign Out") }
        }
        Spacer(Modifier.height(16.dp))
        Text("Last ID: ${lastIdState ?: "(none)"}")
        Spacer(Modifier.height(4.dp))
        Text("Signed in: ${accountEmail ?: "(not signed)"}")
        Spacer(Modifier.height(4.dp))
        Text("Title: ${currentState?.title ?: "-"}")
        Spacer(Modifier.height(4.dp))
        Text("Updated: ${currentState?.updatedAt ?: 0}")
        Spacer(Modifier.height(4.dp))
        Text("Summary: ${currentState?.summary ?: "-"}")

        Spacer(Modifier.height(24.dp))
        Text("Wallet Debug", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { walletRepo.earnCoins("debug", 10); coins = walletRepo.get()?.coins ?: 0 } }) { Text("Earn +10") }
            Button(onClick = { scope.launch { walletRepo.spendCoins("debug", 5); coins = walletRepo.get()?.coins ?: 0 } }) { Text("Spend -5") }
        }
        Spacer(Modifier.height(6.dp))
        Text("Coins: $coins")

        Spacer(Modifier.height(24.dp))
        Text("Chart Debug", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { lastChartId = chartRepo.create("natal", mapOf("computedJson" to "{}")) } }) { Text("Create Chart") }
        }
        Spacer(Modifier.height(6.dp))
        Text("Last Chart ID: ${lastChartId ?: "(none)"}")

        Spacer(Modifier.height(24.dp))
        Text("Purchase Debug", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val p = PurchaseEntity(sku = sku, type = "inapp", state = 1, purchaseToken = "debug", acknowledged = true, updatedAt = System.currentTimeMillis())
                    DatabaseProvider.get(ctx).purchaseDao().upsert(p)
                }
            }) { Text("Mark Entitled") }
        }
    }
}

