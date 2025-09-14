package com.google.android.gms.ads

import android.content.Context
import android.view.View

class AdRequest {
    class Builder {
        fun build(): AdRequest = AdRequest()
    }
}

class AdSize {
    companion object {
        val BANNER = AdSize()
    }
}

open class AdView(context: Context) : View(context) {
    var adUnitId: String = ""
    fun setAdSize(@Suppress("unused") size: AdSize) {}
    fun loadAd(@Suppress("unused") request: AdRequest) {}
}

open class FullScreenContentCallback {
    open fun onAdDismissedFullScreenContent() {}
    open fun onAdFailedToShowFullScreenContent(@Suppress("unused") adError: AdError) {}
    open fun onAdShowedFullScreenContent() {}
}

open class AdError(val message: String = "stub")
open class LoadAdError(val message: String = "stub")

object MobileAds {
    fun initialize(@Suppress("unused") context: Context, @Suppress("unused") callback: (InitializationStatus) -> Unit = {}) {}
    fun getInitializationStatus(): InitializationStatus? = InitializationStatus()
}

class InitializationStatus {
    val adapterStatusMap: Map<String, com.google.android.gms.ads.initialization.AdapterStatus> = emptyMap()
}
