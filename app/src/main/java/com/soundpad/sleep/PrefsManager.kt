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

    // ── Onboarding shown ───────────────────────────────────────────────────
    fun isOnboardingShown(): Boolean = prefs.getBoolean(KEY_ONBOARDING, false)
    fun setOnboardingShown(v: Boolean) = prefs.edit { putBoolean(KEY_ONBOARDING, v) }

    // ── Rate prompt shown ─────────────────────────────────────────────────
    fun isRatePromptShown(): Boolean = prefs.getBoolean(KEY_RATE_SHOWN, false)
    fun setRatePromptShown(v: Boolean) = prefs.edit { putBoolean(KEY_RATE_SHOWN, v) }

    // ── Free-tier interstitial cadence ────────────────────────────────────
    fun getStopCount(): Int = prefs.getInt(KEY_STOP_COUNT, 0)
    fun incrementStopCount() = prefs.edit { putInt(KEY_STOP_COUNT, getStopCount() + 1) }

    // ── User-controllable feedback ────────────────────────────────────────
    fun isHapticsEnabled(): Boolean = prefs.getBoolean(KEY_HAPTICS, true)
    fun setHapticsEnabled(v: Boolean) = prefs.edit { putBoolean(KEY_HAPTICS, v) }

    fun isUiSoundsEnabled(): Boolean = prefs.getBoolean(KEY_UI_SOUNDS, true)
    fun setUiSoundsEnabled(v: Boolean) = prefs.edit { putBoolean(KEY_UI_SOUNDS, v) }

    // ── Play history (for ARIA AI recommendations) ───────────────────────────

    /** Increment the play count for a sound type. Called every time a sound starts. */
    fun incrementPlayCount(type: SoundType) {
        val key = "play_${type.name}"
        prefs.edit { putInt(key, prefs.getInt(key, 0) + 1) }
    }

    fun getPlayCount(type: SoundType): Int = prefs.getInt("play_${type.name}", 0)

    /** Returns a map of soundType.name → play count for all sounds played at least once. */
    fun getPlayHistory(): Map<String, Int> =
        SoundType.values()
            .filter { getPlayCount(it) > 0 }
            .associate { it.name to getPlayCount(it) }

    /** Returns the most-played SoundType, defaulting to PINK_NOISE for new users. */
    fun getMostPlayedSound(): SoundType =
        SoundType.values()
            .maxByOrNull { getPlayCount(it) }
            ?: SoundType.PINK_NOISE

    // ── Saved mixes ──────────────────────────────────────────────────────────

    /** Persist a named mix. Overwrites any existing mix with the same name. */
    fun saveMix(name: String, layers: List<Pair<SoundType, Float>>) {
        val layerStr = layers.joinToString(",") { (type, vol) -> "${type.name}=${"%.2f".format(vol)}" }
        val existing = prefs.getStringSet(KEY_SAVED_MIXES, emptySet())?.toMutableSet() ?: mutableSetOf()
        existing.removeIf { it.startsWith("$name|") }
        existing.add("$name|$layerStr")
        prefs.edit { putStringSet(KEY_SAVED_MIXES, existing) }
    }

    /** Returns all saved mixes as name → list-of-(SoundType, volume). */
    fun getSavedMixes(): Map<String, List<Pair<SoundType, Float>>> {
        val set = prefs.getStringSet(KEY_SAVED_MIXES, emptySet()) ?: return emptyMap()
        return set.mapNotNull { entry ->
            val pipeIdx = entry.indexOf('|')
            if (pipeIdx < 0) return@mapNotNull null
            val name = entry.substring(0, pipeIdx)
            val layers = entry.substring(pipeIdx + 1).split(",").mapNotNull { part ->
                val eqIdx = part.indexOf('=')
                if (eqIdx < 0) return@mapNotNull null
                val vol = part.substring(eqIdx + 1).toFloatOrNull() ?: 0.7f
                runCatching { SoundType.valueOf(part.substring(0, eqIdx)) }.getOrNull()?.let { it to vol }
            }
            if (layers.isEmpty()) null else name to layers
        }.toMap()
    }

    /** Delete a saved mix by name. */
    fun deleteMix(name: String) {
        val existing = prefs.getStringSet(KEY_SAVED_MIXES, emptySet())?.toMutableSet() ?: return
        existing.removeIf { it.startsWith("$name|") }
        prefs.edit { putStringSet(KEY_SAVED_MIXES, existing) }
    }

    companion object {
        private const val KEY_PREMIUM     = "premium"
        private const val KEY_VOLUME      = "volume"
        private const val KEY_LAST_SOUND  = "last_sound"
        private const val KEY_TIMER_MS    = "timer_ms"
        private const val KEY_SESSIONS    = "sessions"
        private const val KEY_ONBOARDING  = "onboarding_shown"
        private const val KEY_RATE_SHOWN  = "rate_shown"
        private const val KEY_STOP_COUNT  = "stop_count"
        private const val KEY_HAPTICS     = "haptics_enabled"
        private const val KEY_UI_SOUNDS   = "ui_sounds_enabled"
        private const val KEY_SAVED_MIXES = "saved_mixes"
    }
}
