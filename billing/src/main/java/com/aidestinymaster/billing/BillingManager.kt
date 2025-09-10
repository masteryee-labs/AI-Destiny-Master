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
        if (client.isReady) { onReady?.invoke(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() { }
            override fun onBillingSetupFinished(result: BillingResult) { if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady?.invoke() }
        })
    }

    suspend fun queryProducts(vararg ids: String): List<ProductDetails> = withContext(Dispatchers.IO) {
        if (!client.isReady) suspendCancellableCoroutine { cont -> startConnection { cont.resume(Unit) } }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(ids.map { id -> QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(BillingClient.ProductType.INAPP).build() })
            .build()
        val res = client.queryProductDetails(params)
        if (res.billingResult.responseCode == BillingClient.BillingResponseCode.OK) res.productDetailsList ?: emptyList() else emptyList()
    }

    fun launchPurchase(activity: Activity, product: ProductDetails, offerToken: String? = null) {
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
        val res = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build())
        if (res.billingResult.responseCode == BillingClient.BillingResponseCode.OK) res.purchasesList ?: emptyList() else emptyList()
    }

    fun addListener(cb: (BillingResult, List<Purchase>?) -> Unit) { listeners += cb }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        listeners.forEach { it(result, purchases) }
    }

    suspend fun acknowledgeIfNeeded(purchase: Purchase): Boolean = withContext(Dispatchers.IO) {
        if (purchase.isAcknowledged) return@withContext true
        if (!client.isReady) suspendCancellableCoroutine { cont -> startConnection { cont.resume(Unit) } }
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        val res = client.acknowledgePurchase(params)
        res.responseCode == BillingClient.BillingResponseCode.OK
    }

    companion object {
        @Volatile private var inst: BillingManager? = null
        fun from(context: Context): BillingManager = inst ?: synchronized(this) { inst ?: BillingManager(context.applicationContext).also { inst = it } }
    }
}
