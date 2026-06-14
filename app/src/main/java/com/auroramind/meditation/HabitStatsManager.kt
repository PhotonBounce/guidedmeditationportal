package com.auroramind.meditation

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

/**
 * Tracks "clean time" for the quit-habit side of the app: how long since the
 * user's quit date, money saved, urges resisted, and milestone celebrations.
 *
 * Distinct from [StatsManager], which tracks the daily affirmation-session
 * streak. Here, [daysClean] is continuous time since the quit date and only
 * resets on a logged relapse — it is the hero number the dashboard shows.
 */
class HabitStatsManager(context: Context) {

    private val prefs = context.getSharedPreferences("habit_stats", Context.MODE_PRIVATE)

    // ── Quit date / clean time ──────────────────────────────────────────────
    /** Epoch millis of the current clean streak's start. 0 until the user begins. */
    fun getQuitDate(): Long = prefs.getLong(KEY_QUIT_DATE, 0L)

    /** Begin (or restart) the clean streak as of [whenMs]. */
    fun setQuitDate(whenMs: Long = System.currentTimeMillis()) =
        prefs.edit { putLong(KEY_QUIT_DATE, whenMs) }

    fun hasStarted(): Boolean = getQuitDate() > 0L

    /** Whole days elapsed since the quit date (0 if not started or in the future). */
    fun daysClean(nowMs: Long = System.currentTimeMillis()): Int {
        val quit = getQuitDate()
        if (quit <= 0L || nowMs <= quit) return 0
        return TimeUnit.MILLISECONDS.toDays(nowMs - quit).toInt()
    }

    /** Hours elapsed within the current day — for a "0d 12h" sub-label early on. */
    fun hoursCleanToday(nowMs: Long = System.currentTimeMillis()): Int {
        val quit = getQuitDate()
        if (quit <= 0L || nowMs <= quit) return 0
        return (TimeUnit.MILLISECONDS.toHours(nowMs - quit) % 24).toInt()
    }

    // ── Relapse handling ─────────────────────────────────────────────────────
    fun longestCleanDays(): Int = prefs.getInt(KEY_LONGEST_CLEAN, 0)
    fun relapseCount(): Int = prefs.getInt(KEY_RELAPSES, 0)

    /**
     * Record a relapse: bank the longest clean streak so far, increment the
     * relapse counter, and restart the clock from [nowMs]. A reset is a setback,
     * not a failure — the longest-streak record is preserved to keep momentum.
     */
    fun recordRelapse(nowMs: Long = System.currentTimeMillis()) {
        val best = maxOf(longestCleanDays(), daysClean(nowMs))
        prefs.edit {
            putInt(KEY_LONGEST_CLEAN, best)
            putInt(KEY_RELAPSES, relapseCount() + 1)
            putLong(KEY_QUIT_DATE, nowMs)
            putInt(KEY_LAST_MILESTONE, 0)
        }
    }

    // ── Money saved ────────────────────────────────────────────────────────────
    /** User's self-reported daily spend on the habit, in their local currency. */
    fun getDailyCost(): Float = prefs.getFloat(KEY_DAILY_COST, 0f)
    fun setDailyCost(amount: Float) =
        prefs.edit { putFloat(KEY_DAILY_COST, amount.coerceAtLeast(0f)) }

    /** Money not spent since the quit date, prorated by exact elapsed time. */
    fun moneySaved(nowMs: Long = System.currentTimeMillis()): Float {
        val quit = getQuitDate()
        if (quit <= 0L || nowMs <= quit) return 0f
        val elapsedDays = (nowMs - quit).toDouble() / TimeUnit.DAYS.toMillis(1)
        return (elapsedDays * getDailyCost()).toFloat()
    }

    // ── Urges resisted (panic button) ────────────────────────────────────────
    fun urgesResisted(): Int = prefs.getInt(KEY_URGES, 0)
    fun logUrgeResisted() = prefs.edit { putInt(KEY_URGES, urgesResisted() + 1) }

    // ── Milestones ─────────────────────────────────────────────────────────────
    /** The highest milestone (in days) reached at [days], or null before day 1. */
    fun milestoneReached(days: Int): Int? = MILESTONES.lastOrNull { it <= days }

    /**
     * Returns a milestone the user has newly crossed since the last celebration,
     * or null if there is nothing new. Marks it celebrated as a side effect so a
     * given milestone fires its animation exactly once.
     */
    fun consumeNewMilestone(nowMs: Long = System.currentTimeMillis()): Int? {
        val reached = milestoneReached(daysClean(nowMs)) ?: return null
        val lastCelebrated = prefs.getInt(KEY_LAST_MILESTONE, 0)
        if (reached <= lastCelebrated) return null
        prefs.edit { putInt(KEY_LAST_MILESTONE, reached) }
        return reached
    }

    companion object {
        /** Celebration thresholds in days clean. */
        val MILESTONES = listOf(1, 3, 7, 14, 30, 60, 90, 180, 365)

        private const val KEY_QUIT_DATE      = "quit_date"
        private const val KEY_LONGEST_CLEAN  = "longest_clean_days"
        private const val KEY_RELAPSES       = "relapse_count"
        private const val KEY_DAILY_COST     = "daily_cost"
        private const val KEY_URGES          = "urges_resisted"
        private const val KEY_LAST_MILESTONE = "last_milestone"
    }
}
