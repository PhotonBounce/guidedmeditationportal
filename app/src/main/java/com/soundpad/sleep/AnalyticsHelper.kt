package com.soundpad.sleep

import android.content.Context
import android.util.Log

/**
 * Lightweight, dependency-free analytics shim.
 *
 * Logs events to Logcat in debug builds. When you're ready, swap the body of
 * `logEvent` for Firebase Analytics or any provider:
 *
 *   1. Add to build.gradle:
 *        implementation platform('com.google.firebase:firebase-bom:32.7.0')
 *        implementation 'com.google.firebase:firebase-analytics-ktx'
 *      plus the Google Services Gradle plugin and a google-services.json.
 *   2. Replace the body with FirebaseAnalytics.getInstance(ctx).logEvent(...).
 *
 * This shim never crashes, so ship-blocking events like sound_played still flow.
 */
object AnalyticsHelper {
    private const val TAG = "SoundPadAnalytics"
    private var initialized = false

    fun init(context: Context) {
        initialized = true
    }

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        if (!initialized) return
        if (BuildConfig.DEBUG) Log.d(TAG, "$name $params")
    }

    // Convenience event names — keep canonical so dashboards stay clean.
    object Events {
        const val SOUND_PLAYED       = "sound_played"
        const val PREMIUM_TAPPED     = "premium_tapped"
        const val PURCHASE_PRO       = "purchase_pro"
        const val PURCHASE_MONTHLY   = "purchase_monthly"
        const val PURCHASE_YEARLY    = "purchase_yearly"
        const val REWARDED_REQUESTED = "rewarded_requested"
        const val REWARDED_GRANTED   = "rewarded_granted"
        const val TIMER_SET          = "timer_set"
        const val ONBOARDING_DONE    = "onboarding_done"
    }
}
