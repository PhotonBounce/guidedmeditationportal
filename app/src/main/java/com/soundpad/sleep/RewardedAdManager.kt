package com.soundpad.sleep

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Wraps Google AdMob rewarded ads. Auto-preloads, so when the user taps
 * "watch ad to unlock", the ad is ready instantly. Falls back gracefully
 * if no fill is available.
 *
 * REPLACE the unit ID below with your real one from AdMob console before
 * publishing. The current value is Google's official rewarded test unit.
 */
class RewardedAdManager(private val context: Context) {

    companion object {
        private const val TAG = "SoundPadRewarded"
        // Centralized in BuildConfig (see app/build.gradle) — swap there.
        private val UNIT_ID get() = BuildConfig.ADMOB_REWARDED_UNIT_ID
    }

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    /** Preload an ad. Safe to call repeatedly — no-ops if already loading or loaded. */
    fun preload() {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        RewardedAd.load(
            context, UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded load failed: ${error.message}")
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    /** Show the loaded ad. Calls [onReward] only on successful completion. */
    fun showAd(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            onUnavailable()
            preload()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                preload()   // queue up the next one
            }
        }
        ad.show(activity) { _: RewardItem ->
            onReward()
        }
    }

    fun isAdAvailable(): Boolean = rewardedAd != null
}
