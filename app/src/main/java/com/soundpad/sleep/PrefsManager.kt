    // ── Onboarding shown ───────────────────────────────────────────────────
    fun isOnboardingShown(): Boolean = prefs.getBoolean(KEY_ONBOARDING, false)
    fun setOnboardingShown(v: Boolean) = prefs.edit { putBoolean(KEY_ONBOARDING, v) }
package com.soundpad.sleep

import android.content.Context
import androidx.core.content.edit

class PrefsManager(context: Context) {

    private val prefs = context.getSharedPreferences("soundpad_prefs", Context.MODE_PRIVATE)

    // ── Premium status ────────────────────────────────────────────────────────
    fun isPremium(): Boolean = prefs.getBoolean(KEY_PREMIUM, false)
    fun setPremium(v: Boolean) = prefs.edit { putBoolean(KEY_PREMIUM, v) }

    // ── Volume (0.0 – 1.0) ───────────────────────────────────────────────────
    fun getVolume(): Float = prefs.getFloat(KEY_VOLUME, 0.7f)
    fun setVolume(v: Float) = prefs.edit { putFloat(KEY_VOLUME, v) }

    // ── Last played sound ────────────────────────────────────────────────────
    fun getLastSound(): SoundType =
        runCatching { SoundType.valueOf(prefs.getString(KEY_LAST_SOUND, null) ?: "") }
            .getOrDefault(SoundType.PINK_NOISE)

    fun setLastSound(t: SoundType) = prefs.edit { putString(KEY_LAST_SOUND, t.name) }

    // ── Timer duration (ms, 0 = off) ─────────────────────────────────────────
    fun getTimerMs(): Long = prefs.getLong(KEY_TIMER_MS, 30 * 60 * 1000L)
    fun setTimerMs(ms: Long) = prefs.edit { putLong(KEY_TIMER_MS, ms) }

    // ── Rate-us prompt: shown count ───────────────────────────────────────────
    fun getSessionCount(): Int = prefs.getInt(KEY_SESSIONS, 0)
    fun incrementSessions() = prefs.edit { putInt(KEY_SESSIONS, getSessionCount() + 1) }

    companion object {
        private const val KEY_PREMIUM   = "premium"
        private const val KEY_VOLUME    = "volume"
        private const val KEY_LAST_SOUND = "last_sound"
        private const val KEY_TIMER_MS  = "timer_ms"
        private const val KEY_SESSIONS  = "sessions"
        private const val KEY_ONBOARDING = "onboarding_shown"
    }
}
