package com.aidestinymaster.app.paywall

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
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
import com.aidestinymaster.billing.BillingManager
import kotlinx.coroutines.launch

@Composable
fun PaywallScreen(activity: ComponentActivity) {
    val repo = PurchaseRepository.from(activity)
    val scope = rememberCoroutineScope()
    var sku by remember { mutableStateOf("iap_bazi_pro") }
    var entitled by remember { mutableStateOf<Boolean?>(null) }
    var activeCount by remember { mutableStateOf(0) }
    var products by remember { mutableStateOf<List<com.android.billingclient.api.ProductDetails>>(emptyList()) }
    val sampleSkus = listOf("iap_bazi_pro", "iap_ziwei_pro", "iap_design_pro", "iap_astro_pro", "android.test.purchased")
    val billing = remember { BillingManager.from(activity) }
    var lastMsg by remember { mutableStateOf("") }
    val featureMap = listOf(
        "八字 Pro" to "iap_bazi_pro",
        "紫微 Pro" to "iap_ziwei_pro",
        "人設 Pro" to "iap_design_pro",
        "星盤 Pro" to "iap_astro_pro",
        "VIP 月" to "sub_vip_month",
        "VIP 年" to "sub_vip_year"
    )

    LaunchedEffect(Unit) {
        billing.addListener { result, purchases ->
            if (result.responseCode == com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { p ->
                    scope.launch {
                        val acked = billing.acknowledgeIfNeeded(p)
                        val now = System.currentTimeMillis()
                        DatabaseProvider.get(activity).purchaseDao().upsert(
                            PurchaseEntity(
                                sku = p.products.firstOrNull() ?: sku,
                                type = if (p.products.isNotEmpty()) "inapp" else "unknown",
                                state = p.purchaseState,
                                purchaseToken = p.purchaseToken,
                                acknowledged = acked || p.isAcknowledged,
                                updatedAt = now
                            )
                        )
                        entitled = repo.isEntitled(p.products.firstOrNull() ?: sku)
                        lastMsg = "Handled purchase: ${p.products.joinToString()} ack=$acked"
                    }
                }
            } else {
                lastMsg = "Billing result: ${result.responseCode}"
            }
        }
    }

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
            Button(onClick = { billing.startConnection { /* ready */ } }) { Text("Init Billing") }
            Button(onClick = { scope.launch { products = billing.queryProducts(*sampleSkus.toTypedArray()) } }) { Text("Query Products") }
            Button(onClick = { scope.launch {
                val purchases = billing.queryPurchasesAsync()
                purchases.forEach { p ->
                    val acked = billing.acknowledgeIfNeeded(p)
                    DatabaseProvider.get(activity).purchaseDao().upsert(
                        PurchaseEntity(
                            sku = p.products.firstOrNull() ?: sku,
                            type = if (p.products.isNotEmpty()) "inapp" else "unknown",
                            state = p.purchaseState,
                            purchaseToken = p.purchaseToken,
                            acknowledged = acked || p.isAcknowledged,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
                activeCount = repo.getActive().size
                entitled = repo.isEntitled(sku)
                lastMsg = "Restored ${'$'}{purchases.size} purchases"
            } }) { Text("Restore Purchases") }
        }
        if (products.isNotEmpty()) {
            Text("Products:")
            products.forEach { pd ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(pd.name + " (" + pd.productId + ")")
                    Button(onClick = { billing.launchPurchase(activity, pd) }) { Text("Buy") }
                }
            }
        }
        if (entitled != null) Text("Entitled: $entitled")
        Text("Active purchases: $activeCount")
        if (lastMsg.isNotEmpty()) Text("Last: $lastMsg")
        Spacer(Modifier.height(8.dp))
        Text("快速選擇功能 → 對應 SKU：")
        featureMap.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, id) ->
                    Button(onClick = { sku = id }) { Text("$label / $id") }
                }
            }
        }
    }
}
