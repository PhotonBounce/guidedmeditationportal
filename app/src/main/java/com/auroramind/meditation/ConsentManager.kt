package com.auroramind.meditation

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * Wraps Google's User Messaging Platform (UMP) for GDPR / CCPA consent.
 *
 * Without this, AdMob can ONLY serve non-personalized ads in EU/UK regions —
 * roughly 30% lower CPM. The consent form takes about 2 seconds for users
 * outside the EU (auto-passes through) and shows a one-time choice screen
 * for EU users.
 *
 * To test the EU experience while developing on a non-EU device, uncomment
 * the ConsentDebugSettings block and add your test device hash.
 */
class ConsentManager(private val activity: Activity) {

    companion object {
        private const val TAG = "MeditationPortalConsent"
    }

    private val info: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(activity)

    /** True once the user has either consented or no consent is required. */
    fun canRequestAds(): Boolean = info.canRequestAds()

    /**
     * Request consent (silently if not in EU; shows form if required).
     * Calls [onComplete] with true when ads can be requested.
     */
    fun gatherConsent(onComplete: (canRequestAds: Boolean) -> Unit) {
        val params = ConsentRequestParameters.Builder()
            // .setConsentDebugSettings(
            //     ConsentDebugSettings.Builder(activity)
            //         .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            //         .addTestDeviceHashedId("TEST-DEVICE-HASH-HERE")
            //         .build()
            // )
            .build()

        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.message}")
                    }
                    onComplete(info.canRequestAds())
                }
            },
            { requestError ->
                Log.w(TAG, "Consent info update failed: ${requestError.message}")
                // If the consent check itself fails, fall back to the user's
                // previously stored consent (defaults to "can request ads"
                // outside the EU).
                onComplete(info.canRequestAds())
            }
        )
    }

    /** Force-reset consent (for support requests / "change my ad preferences"). */
    fun reset() = info.reset()
}
