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
import com.aidestinymaster.app.util.AppLogger
import com.android.billingclient.api.ProductDetails

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
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val featureMap = listOf(
        "八字 Pro" to "iap_bazi_pro",
        "紫微 Pro" to "iap_ziwei_pro",
        "能量圖 Pro" to "iap_design_pro",
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
                        AppLogger.log(activity, lastMsg)
                    }
                }
            } else {
                lastMsg = "Billing result: ${result.responseCode}"
                AppLogger.log(activity, lastMsg)
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
            Button(onClick = {
                scope.launch {
                    loading = true; error = ""
                    runCatching { products = billing.queryProducts(*sampleSkus.toTypedArray()) }
                        .onFailure { t -> error = t.message ?: "未知錯誤" }
                        .also { loading = false }
                    AppLogger.log(activity, "Query Products: size=${products.size} error=$error")
                }
            }) { Text("Query Products") }
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
                lastMsg = "Restored ${purchases.size} purchases"
                AppLogger.log(activity, lastMsg)
            } }) { Text("Restore Purchases") }
        }
        if (loading) {
            Text("載入中…")
        }
        if (error.isNotEmpty()) {
            Text("錯誤：$error")
        }
        if (products.isNotEmpty()) {
            Text("Products:")
            products.forEach { pd ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val price = formatPrice(pd)
                    val period = formatPeriod(pd)
                    val label = skuToLabel(pd.productId)
                    Text((if (label.isNotEmpty()) label + " / " else "") + pd.name + " (" + pd.productId + ") · " + listOfNotBlank(price, period).joinToString(" · "))
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

private fun formatPrice(pd: ProductDetails): String {
    pd.oneTimePurchaseOfferDetails?.let { return it.formattedPrice }
    val subs = pd.subscriptionOfferDetails
    if (!subs.isNullOrEmpty()) {
        val phases = subs.first().pricingPhases.pricingPhaseList
        val base = phases.lastOrNull() ?: phases.first()
        return base.formattedPrice
    }
    return ""
}

private fun formatPeriod(pd: ProductDetails): String {
    val subs = pd.subscriptionOfferDetails
    if (!subs.isNullOrEmpty()) {
        val phases = subs.first().pricingPhases.pricingPhaseList
        val base = phases.lastOrNull() ?: phases.first()
        val p = base.billingPeriod ?: return ""
        return when {
            p.contains("Y") -> "每年"
            p.contains("M") -> "每月"
            p.contains("W") -> "每週"
            else -> "週期：$p"
        }
    }
    return "一次性"
}

private fun skuToLabel(productId: String): String = when (productId) {
    "iap_bazi_pro" -> "八字 Pro"
    "iap_ziwei_pro" -> "紫微 Pro"
    "iap_design_pro" -> "能量圖 Pro"
    "iap_astro_pro" -> "星盤 Pro"
    "sub_vip_month" -> "VIP 月訂"
    "sub_vip_year" -> "VIP 年訂"
    else -> ""
}

private fun listOfNotBlank(vararg s: String): List<String> = s.filter { it.isNotBlank() }
