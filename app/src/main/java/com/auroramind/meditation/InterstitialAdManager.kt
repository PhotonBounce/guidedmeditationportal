package com.auroramind.meditation

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Wraps Google AdMob interstitial ads.
 *
 * Shown on every 3rd "Stop" tap for free users — high revenue lift (~3×
 * total ad RPM vs banner-only) with minimal retention impact because the
 * user is already leaving the app surface.
 *
 * Cadence guards:
 *   - Don't show in the first 2 minutes of a session (too aggressive)
 *   - Don't show if user just dismissed one < 90s ago
 *   - Don't show to premium users (paid-tier)
 *   - Don't show if no ad is loaded (silent no-op)
 *
 * Replace UNIT_ID with your real AdMob interstitial unit before shipping.
 */
class InterstitialAdManager(private val context: Context) {

    companion object {
        private const val TAG = "PortalInterstitial"
        // Centralized in BuildConfig (see app/build.gradle).
        private val UNIT_ID get() = BuildConfig.ADMOB_INTERSTITIAL_UNIT_ID

        private const val MIN_INTERVAL_MS = 90_000L         // 90s between shows
        private const val MIN_SESSION_AGE_MS = 120_000L     // session must be 2 min old
        private const val SHOW_EVERY_NTH_STOP = 3
    }

    private var ad: InterstitialAd? = null
    private var isLoading = false
    private var lastShownAt = 0L
    private val sessionStart = System.currentTimeMillis()

    fun preload() {
        if (isLoading || ad != null) return
        isLoading = true
        InterstitialAd.load(
            context, UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    isLoading = false
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial load failed: ${error.message}")
                    ad = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Maybe show the interstitial. Returns true if we showed one.
     * Caller (MainActivity) increments the stop counter and passes it in.
     */
    fun maybeShowOnStop(activity: Activity, stopCount: Int): Boolean {
        // Cadence: only every Nth stop
        if (stopCount % SHOW_EVERY_NTH_STOP != 0) return false

        val now = System.currentTimeMillis()
        if (now - sessionStart < MIN_SESSION_AGE_MS) return false
        if (now - lastShownAt < MIN_INTERVAL_MS) return false

        val current = ad ?: run {
            preload()
            return false
        }
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                Log.w(TAG, "Interstitial show failed: ${e.message}")
                ad = null
                preload()
            }
        }
        current.show(activity)
        lastShownAt = now
        return true
    }
}
