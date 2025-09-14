package com.aidestinymaster.app.ads

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

object RewardedAdsManager {
    private const val TAG = "RewardedAdsMgr"
    private const val PREF = "rewarded_ads_prefs"
    private const val KEY_LAST_SHOWN_AT = "last_shown_at"
    private const val KEY_DAILY_COUNT_PREFIX = "daily_count_" // + yyyyMMdd

    // Config
    private const val MIN_COOLDOWN_MS: Long = 5 * 60 * 1000 // 5 minutes
    private const val DAILY_MAX: Int = 10

    private fun prefs(ctx: Context): SharedPreferences = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    private fun todayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())

    fun canShow(ctx: Context): Pair<Boolean, String?> {
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        val last = p.getLong(KEY_LAST_SHOWN_AT, 0L)
        if (now - last < MIN_COOLDOWN_MS) {
            val remain = ((MIN_COOLDOWN_MS - (now - last)) / 1000).toInt()
            return false to "冷卻中，請 ${remain} 秒後再試"
        }
        val daily = p.getInt(KEY_DAILY_COUNT_PREFIX + todayKey(), 0)
        if (daily >= DAILY_MAX) return false to "今日觀看次數已達上限"
        return true to null
    }

    fun recordShown(ctx: Context) {
        val p = prefs(ctx)
        val key = KEY_DAILY_COUNT_PREFIX + todayKey()
        val daily = p.getInt(key, 0) + 1
        p.edit().putLong(KEY_LAST_SHOWN_AT, System.currentTimeMillis()).putInt(key, daily).apply()
    }

    // Exposed helpers for UI
    fun getDailyCount(ctx: Context): Int = prefs(ctx).getInt(KEY_DAILY_COUNT_PREFIX + todayKey(), 0)
    fun getDailyMax(): Int = DAILY_MAX
    fun getCooldownRemainingSeconds(ctx: Context): Int {
        val now = System.currentTimeMillis()
        val last = prefs(ctx).getLong(KEY_LAST_SHOWN_AT, 0L)
        val remainMs = MIN_COOLDOWN_MS - (now - last)
        return if (remainMs > 0) (remainMs / 1000).toInt() else 0
    }
    fun getMinCooldownSeconds(): Int = (MIN_COOLDOWN_MS / 1000).toInt()

    fun loadRewardedAd(
        context: Context,
        adUnitId: String,
        onLoaded: (RewardedAd) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded loaded")
                    onLoaded(ad)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed: ${error.message}")
                    onFailed(error.message ?: "load failed")
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity,
        ad: RewardedAd,
        onUserEarnedReward: (RewardItem) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        try {
            ad.show(activity) { rewardItem ->
                onUserEarnedReward(rewardItem)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "show failed: ${t.message}")
            onFailed(t.message ?: "show failed")
        }
    }
}
