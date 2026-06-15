package com.auroramind.meditation

import android.content.Context
import androidx.core.content.edit

class PrefsManager(context: Context) {

    private val prefs = context.getSharedPreferences("meditation_portal_prefs", Context.MODE_PRIVATE)

    // ── Premium status ────────────────────────────────────────────────────────
    // Power of Mind is freemium: premium == an active subscription (persisted by
    // BillingManager). Free users get ads + the free affirmation theme; premium
    // removes ads and unlocks every theme. (No time-boxed trial — that doesn't
    // fit an ad-supported free tier.)
    fun isPremium(): Boolean = prefs.getBoolean(KEY_PREMIUM, false)
    fun setPremium(v: Boolean) = prefs.edit { putBoolean(KEY_PREMIUM, v) }

    private fun getFirstInstallTime(): Long {
        var time = prefs.getLong(KEY_INSTALL_TIME, 0L)
        if (time == 0L) {
            time = System.currentTimeMillis()
            prefs.edit { putLong(KEY_INSTALL_TIME, time) }
        }
        return time
    }

    // ── Volume (0.0 – 1.0) ───────────────────────────────────────────────────
    // Floor at 0.08: a stale/accidental 0 (e.g. slider dragged to the bottom in a
    // past session) must never leave a tapped meditation playing silently. Users
    // who want silence just stop playback; the slider still adjusts loudness.
    fun getVolume(): Float {
        val v = prefs.getFloat(KEY_VOLUME, 0.7f)
        return if (v < 0.08f) 0.7f else v
    }
    fun setVolume(v: Float) = prefs.edit { putFloat(KEY_VOLUME, v.coerceIn(0.08f, 1f)) }

    // ── Background Volume (0.0 – 1.0) ─────────────────────────────────────────
    fun getBgVolume(): Float = prefs.getFloat(KEY_BG_VOLUME, 0.11f)
    fun setBgVolume(v: Float) = prefs.edit { putFloat(KEY_BG_VOLUME, v.coerceIn(0f, 1f)) }

    // ── Selected Background Music Track ──────────────────────────────────────
    fun getBgMusicTrack(): String = prefs.getString(KEY_BG_MUSIC_TRACK, BgMusicType.CYMBAL_MEDITATION.name) ?: BgMusicType.CYMBAL_MEDITATION.name
    fun setBgMusicTrack(name: String) = prefs.edit { putString(KEY_BG_MUSIC_TRACK, name) }

    // ── Last played sound ────────────────────────────────────────────────────
    fun getLastSound(): SoundType =
        runCatching { SoundType.valueOf(prefs.getString(KEY_LAST_SOUND, null) ?: "") }
            .getOrDefault(SoundType.values().first())

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

    /** Returns the most-played SoundType, defaulting to the first track for new users. */
    fun getMostPlayedSound(): SoundType =
        SoundType.values()
            .maxByOrNull { getPlayCount(it) }
            ?: SoundType.values().first()

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
                if (eqIdx < 0) {
                    null
                } else {
                    val vol = part.substring(eqIdx + 1).toFloatOrNull() ?: 0.7f
                    runCatching { SoundType.valueOf(part.substring(0, eqIdx)) }.getOrNull()?.let { it to vol }
                }
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

    // ── Meditation Alarm ─────────────────────────────────────────────────────
    fun isAlarmEnabled(): Boolean = prefs.getBoolean(KEY_ALARM_ENABLED, false)
    fun setAlarmEnabled(v: Boolean) = prefs.edit { putBoolean(KEY_ALARM_ENABLED, v) }

    fun getAlarmHour(): Int = prefs.getInt(KEY_ALARM_HOUR, 7)
    fun getAlarmMinute(): Int = prefs.getInt(KEY_ALARM_MINUTE, 0)
    fun setAlarmTime(hour: Int, minute: Int) = prefs.edit {
        putInt(KEY_ALARM_HOUR, hour)
        putInt(KEY_ALARM_MINUTE, minute)
    }

    /** "TRACK" → plays a guided meditation; "TONE" → plays a synthesized wake tone. */
    fun getAlarmSourceType(): String = prefs.getString(KEY_ALARM_SOURCE_TYPE, "TONE") ?: "TONE"
    fun setAlarmSourceType(type: String) = prefs.edit { putString(KEY_ALARM_SOURCE_TYPE, type) }

    fun getAlarmTrack(): SoundType =
        runCatching { SoundType.valueOf(prefs.getString(KEY_ALARM_TRACK, null) ?: "") }
            .getOrDefault(SoundType.values().first())
    fun setAlarmTrack(type: SoundType) = prefs.edit { putString(KEY_ALARM_TRACK, type.name) }

    fun getAlarmTone(): SynthTone =
        runCatching { SynthTone.valueOf(prefs.getString(KEY_ALARM_TONE, null) ?: "") }
            .getOrDefault(SynthTone.SOFT_CHIMES)
    fun setAlarmTone(tone: SynthTone) = prefs.edit { putString(KEY_ALARM_TONE, tone.name) }

    // ── Jukebox: repeat mode & shuffle ───────────────────────────────────────
    fun getRepeatMode(): SoundService.RepeatMode =
        runCatching { SoundService.RepeatMode.valueOf(prefs.getString(KEY_REPEAT_MODE, null) ?: "") }
            .getOrDefault(SoundService.RepeatMode.ONE)

    fun setRepeatMode(mode: SoundService.RepeatMode) = prefs.edit { putString(KEY_REPEAT_MODE, mode.name) }

    fun isShuffleEnabled(): Boolean = prefs.getBoolean(KEY_SHUFFLE, false)
    fun setShuffleEnabled(v: Boolean) = prefs.edit { putBoolean(KEY_SHUFFLE, v) }

    // ── Equalizer coefficients ──────────────────────────────────────────────
    fun getEqBass(): Float = prefs.getFloat(KEY_EQ_BASS, 1.0f)
    fun setEqBass(v: Float) = prefs.edit { putFloat(KEY_EQ_BASS, v) }

    fun getEqMid(): Float = prefs.getFloat(KEY_EQ_MID, 1.0f)
    fun setEqMid(v: Float) = prefs.edit { putFloat(KEY_EQ_MID, v) }

    fun getEqTreble(): Float = prefs.getFloat(KEY_EQ_TREBLE, 1.0f)
    fun setEqTreble(v: Float) = prefs.edit { putFloat(KEY_EQ_TREBLE, v) }

    // ── Favorites ─────────────────────────────────────────────────────────────
    fun isFavorite(type: SoundType): Boolean =
        prefs.getStringSet(KEY_FAVORITES, emptySet())?.contains(type.name) == true

    fun toggleFavorite(type: SoundType): Boolean {
        val set = prefs.getStringSet(KEY_FAVORITES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val nowFavorite = if (set.contains(type.name)) { set.remove(type.name); false }
                          else { set.add(type.name); true }
        prefs.edit { putStringSet(KEY_FAVORITES, set) }
        return nowFavorite
    }

    /** Favorite tracks in enum declaration order. */
    fun getFavorites(): List<SoundType> {
        val set = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: return emptyList()
        return SoundType.values().filter { set.contains(it.name) }
    }

    // ── Primary goal (captured during onboarding) ──────────────────────────────
    fun getGoal(): Mood? =
        prefs.getString(KEY_GOAL, null)?.let { runCatching { Mood.valueOf(it) }.getOrNull() }
    fun setGoal(mood: Mood) = prefs.edit { putString(KEY_GOAL, mood.name) }

    // ── Quit-habit profile (captured during the onboarding quiz) ────────────────
    // Identity/preferences for the quit-habit side. Numeric "clean time" state
    // (quit date, daily cost, relapses) lives in HabitStatsManager instead.

    /** The habit the user is breaking free from, e.g. "vaping" or "doomscrolling". */
    fun getHabitType(): String = prefs.getString(KEY_HABIT_TYPE, "") ?: ""
    fun setHabitType(type: String) = prefs.edit { putString(KEY_HABIT_TYPE, type) }

    /** The user's selected triggers, e.g. "stress", "boredom", "social". */
    fun getTriggers(): Set<String> = prefs.getStringSet(KEY_TRIGGERS, emptySet()) ?: emptySet()
    fun setTriggers(triggers: Set<String>) = prefs.edit { putStringSet(KEY_TRIGGERS, triggers) }

    /** Free-text "what freedom means to me" — personalizes affirmation selection. */
    fun getFreedomGoal(): String = prefs.getString(KEY_FREEDOM_GOAL, "") ?: ""
    fun setFreedomGoal(text: String) = prefs.edit { putString(KEY_FREEDOM_GOAL, text) }

    /** True once the onboarding quiz has been completed (gates the hard paywall). */
    fun isQuizCompleted(): Boolean = prefs.getBoolean(KEY_QUIZ_DONE, false)
    fun setQuizCompleted(v: Boolean) = prefs.edit { putBoolean(KEY_QUIZ_DONE, v) }

    // ── Journeys (multi-day programs) ───────────────────────────────────────────
    /** Days completed for a program (0 = not started, N = N days done). */
    fun getProgramProgress(programId: String): Int =
        prefs.getInt("$KEY_PROGRAM_PREFIX$programId", 0)

    /** Advance a program by one completed day, capped at its length. */
    fun advanceProgram(programId: String, totalDays: Int) {
        val next = (getProgramProgress(programId) + 1).coerceAtMost(totalDays)
        prefs.edit { putInt("$KEY_PROGRAM_PREFIX$programId", next) }
    }

    fun resetProgram(programId: String) =
        prefs.edit { putInt("$KEY_PROGRAM_PREFIX$programId", 0) }

    // ── Daily reminder notification ─────────────────────────────────────────────
    fun isReminderEnabled(): Boolean = prefs.getBoolean(KEY_REMINDER_ON, false)
    fun setReminderEnabled(v: Boolean) = prefs.edit { putBoolean(KEY_REMINDER_ON, v) }

    fun getReminderHour(): Int = prefs.getInt(KEY_REMINDER_HOUR, 20)
    fun getReminderMinute(): Int = prefs.getInt(KEY_REMINDER_MINUTE, 0)
    fun setReminderTime(hour: Int, minute: Int) = prefs.edit {
        putInt(KEY_REMINDER_HOUR, hour)
        putInt(KEY_REMINDER_MINUTE, minute)
    }

    companion object {
        private const val KEY_PREMIUM     = "premium"
        private const val KEY_VOLUME      = "volume"
        private const val KEY_BG_VOLUME   = "bg_volume"
        private const val KEY_BG_MUSIC_TRACK = "bg_music_track"
        private const val KEY_LAST_SOUND  = "last_sound"
        private const val KEY_TIMER_MS    = "timer_ms"
        private const val KEY_SESSIONS    = "sessions"
        private const val KEY_ONBOARDING  = "onboarding_shown"
        private const val KEY_RATE_SHOWN  = "rate_shown"
        private const val KEY_STOP_COUNT  = "stop_count"
        private const val KEY_HAPTICS     = "haptics_enabled"
        private const val KEY_UI_SOUNDS   = "ui_sounds_enabled"
        private const val KEY_SAVED_MIXES = "saved_mixes"
        private const val KEY_INSTALL_TIME = "install_time"
        private const val KEY_EQ_BASS      = "eq_bass"
        private const val KEY_EQ_MID       = "eq_mid"
        private const val KEY_EQ_TREBLE    = "eq_treble"
        private const val KEY_REPEAT_MODE  = "repeat_mode"
        private const val KEY_SHUFFLE      = "shuffle_enabled"
        private const val KEY_ALARM_ENABLED     = "alarm_enabled"
        private const val KEY_ALARM_HOUR        = "alarm_hour"
        private const val KEY_ALARM_MINUTE      = "alarm_minute"
        private const val KEY_ALARM_SOURCE_TYPE = "alarm_source_type"
        private const val KEY_ALARM_TRACK       = "alarm_track"
        private const val KEY_ALARM_TONE        = "alarm_tone"
        private const val KEY_FAVORITES         = "favorites"
        private const val KEY_GOAL              = "primary_goal"
        private const val KEY_HABIT_TYPE        = "habit_type"
        private const val KEY_TRIGGERS          = "habit_triggers"
        private const val KEY_FREEDOM_GOAL      = "freedom_goal"
        private const val KEY_QUIZ_DONE         = "quiz_completed"
        private const val KEY_REMINDER_ON       = "reminder_enabled"
        private const val KEY_REMINDER_HOUR     = "reminder_hour"
        private const val KEY_REMINDER_MINUTE   = "reminder_minute"
        private const val KEY_PROGRAM_PREFIX    = "program_"
    }
}
