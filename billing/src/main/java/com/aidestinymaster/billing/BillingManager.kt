package com.aidestinymaster.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class BillingManager private constructor(private val context: Context) : PurchasesUpdatedListener {
    private var client: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases()
        .setListener(this)
        .build()

    private val listeners = mutableListOf<(BillingResult, List<Purchase>?) -> Unit>()

    fun startConnection(onReady: (() -> Unit)? = null) {
        if (client.isReady) { BillingLogger.log(context, "Billing already ready"); onReady?.invoke(); return }
        BillingLogger.log(context, "Billing startConnection")
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() { BillingLogger.log(context, "Billing disconnected") }
            override fun onBillingSetupFinished(result: BillingResult) {
                BillingLogger.log(context, "Billing setup finished: code=${'$'}{result.responseCode}")
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady?.invoke()
            }
        })
    }

    suspend fun queryProducts(vararg ids: String): List<ProductDetails> = withContext(Dispatchers.IO) {
        if (!client.isReady) suspendCancellableCoroutine { cont -> startConnection { cont.resume(Unit) } }
        BillingLogger.log(context, "queryProducts ids=${'$'}{ids.joinToString()}")
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(ids.map { id -> QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(BillingClient.ProductType.INAPP).build() })
            .build()
        val res = client.queryProductDetails(params)
        BillingLogger.log(context, "queryProducts result code=${'$'}{res.billingResult.responseCode} count=${'$'}{res.productDetailsList?.size ?: 0}")
        if (res.billingResult.responseCode == BillingClient.BillingResponseCode.OK) res.productDetailsList ?: emptyList() else emptyList()
    }

    fun launchPurchase(activity: Activity, product: ProductDetails, offerToken: String? = null) {
        BillingLogger.log(context, "launchPurchase productId=${'$'}{product.productId}")
        val offer = product.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: offerToken
        val flowParams = if (offer != null) {
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .setOfferToken(offer)
                        .build()
                )).build()
        } else {
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product)
                        .build()
                )).build()
        }
        client.launchBillingFlow(activity, flowParams)
    }

    suspend fun queryPurchases(): List<Purchase> = withContext(Dispatchers.IO) {
        if (!client.isReady) suspendCancellableCoroutine { cont -> startConnection { cont.resume(Unit) } }
        BillingLogger.log(context, "queryPurchases (INAPP)")
        val res = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        BillingLogger.log(context, "queryPurchases result code=${'$'}{res.billingResult.responseCode} count=${'$'}{res.purchasesList?.size ?: 0}")
        if (res.billingResult.responseCode == BillingClient.BillingResponseCode.OK) res.purchasesList ?: emptyList() else emptyList()
    }

    suspend fun queryPurchasesAsync(): List<Purchase> = withContext(Dispatchers.IO) {
        if (!client.isReady) suspendCancellableCoroutine { cont -> startConnection { cont.resume(Unit) } }
        BillingLogger.log(context, "queryPurchasesAsync (INAPP+SUBS)")
        val inapp = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        val subs = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        BillingLogger.log(context, "queryPurchasesAsync inapp=${'$'}{inapp.purchasesList?.size ?: 0} subs=${'$'}{subs.purchasesList?.size ?: 0}")
        val l1 = if (inapp.billingResult.responseCode == BillingClient.BillingResponseCode.OK) inapp.purchasesList ?: emptyList() else emptyList()
        val l2 = if (subs.billingResult.responseCode == BillingClient.BillingResponseCode.OK) subs.purchasesList ?: emptyList() else emptyList()
        l1 + l2
    }

    fun addListener(cb: (BillingResult, List<Purchase>?) -> Unit) { listeners += cb }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        BillingLogger.log(context, "onPurchasesUpdated code=${'$'}{result.responseCode} count=${'$'}{purchases?.size ?: 0}")
        listeners.forEach { it(result, purchases) }
    }

    suspend fun acknowledgeIfNeeded(purchase: Purchase): Boolean = withContext(Dispatchers.IO) {
        if (purchase.isAcknowledged) return@withContext true
        if (!client.isReady) suspendCancellableCoroutine { cont -> startConnection { cont.resume(Unit) } }
        BillingLogger.log(context, "acknowledge purchaseToken=${'$'}{purchase.purchaseToken.take(8)}...")
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        val res = client.acknowledgePurchase(params)
        BillingLogger.log(context, "acknowledge result code=${'$'}{res.responseCode}")
        res.responseCode == BillingClient.BillingResponseCode.OK
    }

    companion object {
        @Volatile private var inst: BillingManager? = null
        fun from(context: Context): BillingManager = inst ?: synchronized(this) { inst ?: BillingManager(context.applicationContext).also { inst = it } }
    }
}
