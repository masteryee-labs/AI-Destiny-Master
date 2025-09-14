package com.google.android.gms.ads.interstitial

import android.content.Context
import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError

open class InterstitialAd {
    var fullScreenContentCallback: FullScreenContentCallback? = null
    fun show(activity: Activity) {}

    companion object {
        fun load(
            context: Context,
            adUnitId: String,
            request: AdRequest,
            callback: InterstitialAdLoadCallback
        ) {
            callback.onAdFailedToLoad(LoadAdError("stub"))
        }
    }
}

abstract class InterstitialAdLoadCallback {
    open fun onAdLoaded(ad: InterstitialAd) {}
    open fun onAdFailedToLoad(error: LoadAdError) {}
}
