// This is a stub for rewarded ad logic. Integrate with MainActivity and SoundAdapter for session-based premium unlock.
package com.soundpad.sleep

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun loadAd(onLoaded: (() -> Unit)? = null) {
        if (isLoading || rewardedAd != null) return
        isLoading = true
        RewardedAd.load(
            context,
            "ca-app-pub-3940256099942544/5224354917", // Replace with your real Ad Unit ID
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    onLoaded?.invoke()
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    fun showAd(activity: Activity, onReward: () -> Unit) {
        rewardedAd?.show(activity) { _: RewardItem ->
            onReward()
            rewardedAd = null
        }
    }

    fun isAdAvailable(): Boolean = rewardedAd != null
}
