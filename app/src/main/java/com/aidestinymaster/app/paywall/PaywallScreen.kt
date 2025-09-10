package com.aidestinymaster.app.paywall

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.PurchaseEntity
import com.aidestinymaster.data.repository.PurchaseRepository
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(activity: ComponentActivity) {
    val repo = PurchaseRepository.from(activity)
    val scope = rememberCoroutineScope()
    var sku by remember { mutableStateOf("iap_demo") }
    var entitled by remember { mutableStateOf<Boolean?>(null) }
    var activeCount by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Paywall", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { scope.launch { entitled = repo.isEntitled(sku) } }) { Text("Check Entitled") }
            Button(onClick = { scope.launch {
                val p = PurchaseEntity(sku, type = "inapp", state = 1, purchaseToken = "debug", acknowledged = true, updatedAt = System.currentTimeMillis())
                DatabaseProvider.get(activity).purchaseDao().upsert(p)
            } }) { Text("Mark Entitled") }
            Button(onClick = { scope.launch { activeCount = repo.getActive().size } }) { Text("Restore Active") }
        }
        if (entitled != null) Text("Entitled: $entitled")
        Text("Active purchases: $activeCount")
    }
}
