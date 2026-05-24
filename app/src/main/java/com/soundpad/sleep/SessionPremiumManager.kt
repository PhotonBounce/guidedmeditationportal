// This is a stub for session-based premium unlock. Integrate with PrefsManager and RewardedAdManager.
package com.soundpad.sleep

import android.content.Context

object SessionPremiumManager {
    private const val KEY_SESSION_PREMIUM = "session_premium"

    fun isSessionPremium(context: Context): Boolean {
        val prefs = context.getSharedPreferences("soundpad_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SESSION_PREMIUM, false)
    }

    fun setSessionPremium(context: Context, value: Boolean) {
        val prefs = context.getSharedPreferences("soundpad_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SESSION_PREMIUM, value).apply()
    }

    fun clearSessionPremium(context: Context) {
        setSessionPremium(context, false)
    }
}
