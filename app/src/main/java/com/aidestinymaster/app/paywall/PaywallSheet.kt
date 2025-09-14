package com.aidestinymaster.app.paywall

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aidestinymaster.billing.BillingManager
import com.aidestinymaster.billing.EntitlementEvents
import kotlinx.coroutines.launch
import com.android.billingclient.api.ProductDetails
import com.aidestinymaster.app.util.AppLogger
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.AlertDialog
import android.content.Intent
import android.provider.Settings
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.ConnectionResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    activity: ComponentActivity,
    visible: Boolean,
    onDismiss: () -> Unit,
    onPurchased: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val billing = remember { BillingManager.from(activity) }
    val sheetState = rememberModalBottomSheetState()
    val productsState = remember { mutableStateOf<List<com.android.billingclient.api.ProductDetails>>(emptyList()) }
    val loading = remember { mutableStateOf(false) }
    val error = remember { mutableStateOf("") }
    val purchasing = remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val showErrorDialog = remember { mutableStateOf(false) }
    val lastAttempt = remember { mutableStateOf<ProductDetails?>(null) }
    val lastBillingCode = remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(visible) {
        if (visible) {
            // 嘗試載入商品清單
            scope.launch {
                runCatching {
                    loading.value = true
                    error.value = ""
                    billing.startConnection { /* ready */ }
                    val skus = listOf(
                        "iap_bazi_pro",
                        "iap_ziwei_pro",
                        "iap_design_pro",
                        "iap_astro_pro",
                        "sub_vip_month",
                        "sub_vip_year"
                    )
                    productsState.value = billing.queryProducts(*skus.toTypedArray())
                    AppLogger.log(activity, "Paywall loaded products: ${'$'}{productsState.value.map { it.productId }}")
                }
                .onFailure { t -> error.value = t.message ?: "未知錯誤" }
                .also { loading.value = false }
            }
        }
    }

    // When error occurs, show snackbar with message
    LaunchedEffect(error.value) {
        val msg = error.value
        if (msg.isNotEmpty()) {
            snackbar.showSnackbar(message = msg, duration = SnackbarDuration.Short)
        }
    }

    // 監聽購買回呼，成功時通知呼叫端並關閉
    LaunchedEffect(Unit) {
        billing.addListener { result, purchases ->
            if (result.responseCode == com.android.billingclient.api.BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
                // 通知全域權益變更
                EntitlementEvents.emitChanged()
                AppLogger.log(activity, "Purchase success: ${'$'}{purchases.map { it.products }}")
                onPurchased()
                onDismiss()
                purchasing.value = false
            } else if (result.responseCode != com.android.billingclient.api.BillingClient.BillingResponseCode.OK) {
                val reason = mapBillingCode(result.responseCode)
                error.value = "購買失敗（${'$'}reason），代碼：${'$'}{result.responseCode}"
                AppLogger.log(activity, "Purchase failed: code=${'$'}{result.responseCode}")
                purchasing.value = false
                showErrorDialog.value = true
                lastBillingCode.value = result.responseCode
            }
        }
    }

    if (visible) {
        ModalBottomSheet(
            onDismissRequest = { onDismiss() },
            sheetState = sheetState,
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("解鎖完整體驗")
                Text("選擇解鎖方式：單次深度或 VIP 訂閱")

                val products = productsState.value
                if (loading.value) {
                    Text("載入商品中…")
                } else if (products.isEmpty()) {
                    Text("尚未取得商品，請確認 Play Console 設定或重試")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                runCatching {
                                    loading.value = true
                                    error.value = ""
                                    val skus = listOf(
                                        "iap_bazi_pro",
                                        "iap_ziwei_pro",
                                        "iap_design_pro",
                                        "iap_astro_pro",
                                        "sub_vip_month",
                                        "sub_vip_year"
                                    )
                                    productsState.value = billing.queryProducts(*skus.toTypedArray())
                                }
                                .onFailure { t -> error.value = t.message ?: "未知錯誤" }
                                .also { loading.value = false }
                            }
                        }) { Text("重新整理商品") }
                        Button(onClick = onDismiss) { Text("關閉") }
                    }
                } else {
                    products.forEach { pd ->
                        val price = formatPrice(pd)
                        val period = formatPeriod(pd)
                        val label = skuToLabel(pd.productId)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(if (label.isNotEmpty()) "${'$'}label（${'$'}{pd.name}）" else pd.name)
                                val desc = pd.description.takeIf { it.isNotBlank() } ?: pd.productId
                                Text(desc)
                                Text(listOfNotBlank(price, period).joinToString(" · "))
                            }
                            Button(onClick = {
                                purchasing.value = true
                                error.value = ""
                                lastAttempt.value = pd
                                billing.launchPurchase(activity, pd)
                            }, enabled = !purchasing.value) { Text(if (purchasing.value) "處理中..." else "購買") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onDismiss) { Text("關閉") }
                    }
                }
                if (error.value.isNotEmpty()) Text("錯誤：${'$'}{error.value}")
            }
            SnackbarHost(hostState = snackbar)
            if (showErrorDialog.value) {
                val playSvcStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(activity)
                AlertDialog(
                    onDismissRequest = { showErrorDialog.value = false },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                val pd = lastAttempt.value
                                if (pd != null) {
                                    purchasing.value = true
                                    error.value = ""
                                    billing.launchPurchase(activity, pd)
                                }
                                showErrorDialog.value = false
                            }) { Text("重新嘗試購買") }
                            Button(onClick = {
                                // 開啟網路設定
                                runCatching {
                                    activity.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                                }
                            }) { Text("網路設定") }
                            Button(onClick = {
                                // 回報問題（寄送 Email）
                                val mail = Intent(Intent.ACTION_SENDTO).apply {
                                    data = android.net.Uri.parse("mailto:")
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support@aidestinymaster.app"))
                                    putExtra(Intent.EXTRA_SUBJECT, "內購問題回報")
                                    putExtra(Intent.EXTRA_TEXT, "錯誤訊息：${'$'}{error.value}\n請簡述操作步驟與發生時間。")
                                }
                                runCatching { activity.startActivity(mail) }
                            }) { Text("回報問題") }
                            if (playSvcStatus != ConnectionResult.SUCCESS) {
                                Button(onClick = {
                                    // 嘗試以官方解決方案對話框／或跳轉 Play 商店
                                    val gaa = GoogleApiAvailability.getInstance()
                                    val pi = gaa.getErrorResolutionPendingIntent(activity, playSvcStatus, 0)
                                    if (pi != null) {
                                        runCatching { pi.send() }
                                            .onFailure {
                                                runCatching {
                                                    val uri = android.net.Uri.parse("market://details?id=com.google.android.gms")
                                                    activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                }
                                            }
                                    } else {
                                        runCatching {
                                            val uri = android.net.Uri.parse("market://details?id=com.google.android.gms")
                                            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                        }
                                    }
                                }) { Text("更新 Play 服務") }
                            }
                        }
                    },
                    dismissButton = { Button(onClick = { showErrorDialog.value = false }) { Text("關閉") } },
                    title = { Text("購買失敗") },
                    text = {
                        val base = error.value.ifEmpty { "發生未知錯誤，請稍後重試" }
                        if (playSvcStatus != ConnectionResult.SUCCESS) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(base)
                                Text("Play 服務狀態：" + mapPlayServicesStatus(playSvcStatus))
                                // 補充建議：若同時遇到 SERVICE_UNAVAILABLE 或 DEVELOPER_ERROR
                                lastBillingCode.value?.let { code ->
                                    val extra = when (code) {
                                        com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "服務暫時不可用，請先更新 Play 服務或稍後重試"
                                        com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "可能為測試環境或參數設定問題，請稍後重試或聯絡客服"
                                        else -> null
                                    }
                                    if (extra != null) Text(extra)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(base)
                                lastBillingCode.value?.let { code ->
                                    val extra = when (code) {
                                        com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "服務暫時不可用，請先更新 Play 服務或稍後重試"
                                        com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "可能為測試環境或參數設定問題，請稍後重試或聯絡客服"
                                        else -> null
                                    }
                                    if (extra != null) Text(extra)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

private fun mapPlayServicesStatus(code: Int): String = when (code) {
    ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Play 服務需更新"
    ConnectionResult.SERVICE_MISSING -> "未安裝 Play 服務"
    ConnectionResult.SERVICE_DISABLED -> "Play 服務已停用"
    ConnectionResult.NETWORK_ERROR -> "網路錯誤，請檢查連線"
    ConnectionResult.SERVICE_INVALID -> "Play 服務無效或損壞"
    ConnectionResult.API_UNAVAILABLE -> "API 不可用"
    ConnectionResult.RESOLUTION_REQUIRED -> "需要修復（將顯示提示）"
    else -> "狀態碼：$code"
}

private fun mapBillingCode(code: Int): String = when (code) {
    com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED -> "使用者取消"
    com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "服務不可用（網路或 Play 服務異常）"
    com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "裝置不支援結帳"
    com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "商品不可購買（上架/地區/帳號）"
    com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "開發者錯誤（參數/設定）"
    com.android.billingclient.api.BillingClient.BillingResponseCode.ERROR -> "未知錯誤"
    com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "已擁有此商品"
    com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "尚未擁有此商品"
    else -> "其他"
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
            else -> "週期：${'$'}p"
        }
    }
    return "一次性"
}

private fun skuToLabel(productId: String): String {
    return when (productId) {
        "iap_bazi_pro" -> "八字 Pro"
        "iap_ziwei_pro" -> "紫微 Pro"
        "iap_design_pro" -> "能量圖 Pro"
        "iap_astro_pro" -> "星盤 Pro"
        "sub_vip_month" -> "VIP 月訂"
        "sub_vip_year" -> "VIP 年訂"
        else -> ""
    }
}

private fun listOfNotBlank(vararg s: String): List<String> = s.filter { it.isNotBlank() }
