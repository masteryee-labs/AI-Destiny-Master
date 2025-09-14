package com.google.android.gms.ads.rewarded

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError

class RewardItem(val type: String = "stub", val amount: Int = 0)

open class RewardedAd {
    var fullScreenContentCallback: FullScreenContentCallback? = null
    fun show(activity: Activity, onUserEarnedReward: (RewardItem) -> Unit) {}

    companion object {
        fun load(
            context: Context,
            adUnitId: String,
            request: AdRequest,
            callback: RewardedAdLoadCallback
        ) {
            callback.onAdFailedToLoad(LoadAdError("stub"))
        }
    }
}

abstract class RewardedAdLoadCallback {
    open fun onAdLoaded(ad: RewardedAd) {}
    open fun onAdFailedToLoad(error: LoadAdError) {}
}
