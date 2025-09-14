package com.google.android.gms.ads.rewardedinterstitial

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem

open class RewardedInterstitialAd {
    var fullScreenContentCallback: FullScreenContentCallback? = null
    fun show(activity: Activity, onUserEarnedReward: (RewardItem) -> Unit) {}

    companion object {
        fun load(
            context: Context,
            adUnitId: String,
            request: AdRequest,
            callback: RewardedInterstitialAdLoadCallback
        ) {
            callback.onAdFailedToLoad(LoadAdError("stub"))
        }
    }
}

abstract class RewardedInterstitialAdLoadCallback {
    open fun onAdLoaded(ad: RewardedInterstitialAd) {}
    open fun onAdFailedToLoad(error: LoadAdError) {}
}
