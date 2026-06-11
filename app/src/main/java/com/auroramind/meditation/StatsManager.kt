package com.auroramind.meditation

import android.content.Context
import androidx.core.content.edit
import java.util.concurrent.TimeUnit

/**
 * Tracks the user's practice progress — the single biggest retention lever in
 * meditation apps (Insight Timer reaches ~16% D30 retention largely through
 * streaks + progress gamification).
 *
 * Records:
 *   • current streak (consecutive days practiced, with a 1-day forgiving grace)
 *   • longest streak
 *   • total completed sessions
 *   • total minutes meditated
 *   • last practice day (epoch-day)
 *
 * A "session" is credited the first time a track is played on a given calendar
 * day; minutes accumulate as sessions complete.
 */
class StatsManager(context: Context) {

    private val prefs = context.getSharedPreferences("meditation_stats", Context.MODE_PRIVATE)

    private fun epochDay(timeMs: Long = System.currentTimeMillis()): Long =
        TimeUnit.MILLISECONDS.toDays(timeMs)

    // ── Getters ───────────────────────────────────────────────────────────────
    fun currentStreak(): Int  = run { refreshStreak(); prefs.getInt(KEY_STREAK, 0) }
    fun longestStreak(): Int  = prefs.getInt(KEY_LONGEST, 0)
    fun totalSessions(): Int  = prefs.getInt(KEY_SESSIONS, 0)
    fun totalMinutes(): Int   = prefs.getInt(KEY_MINUTES, 0)
    fun sessionsToday(): Int  =
        if (prefs.getLong(KEY_LAST_DAY, -1) == epochDay()) prefs.getInt(KEY_TODAY_COUNT, 0) else 0

    /** Sessions completed in each of the last 7 days (oldest → today). */
    fun lastSevenDays(): IntArray {
        val today = epochDay()
        return IntArray(7) { i ->
            val day = today - (6 - i)
            prefs.getInt("$KEY_DAY_PREFIX$day", 0)
        }
    }

    // ── Recording ──────────────────────────────────────────────────────────────

    /** Call when a meditation starts. Credits a session and advances the streak. */
    fun recordSessionStart() {
        val today = epochDay()
        val lastDay = prefs.getLong(KEY_LAST_DAY, -1)

        var streak = prefs.getInt(KEY_STREAK, 0)
        when {
            lastDay == today      -> { /* already practiced today — streak unchanged */ }
            lastDay == today - 1  -> streak += 1                 // consecutive day
            lastDay == today - 2  -> streak += 1                 // forgiving 1-day grace
            else                  -> streak = 1                  // streak reset / first ever
        }

        val longest = maxOf(prefs.getInt(KEY_LONGEST, 0), streak)
        val todayCount = (if (lastDay == today) prefs.getInt(KEY_TODAY_COUNT, 0) else 0) + 1

        prefs.edit {
            putInt(KEY_STREAK, streak)
            putInt(KEY_LONGEST, longest)
            putLong(KEY_LAST_DAY, today)
            putInt(KEY_TODAY_COUNT, todayCount)
            putInt(KEY_SESSIONS, prefs.getInt(KEY_SESSIONS, 0) + 1)
            putInt("$KEY_DAY_PREFIX$today", prefs.getInt("$KEY_DAY_PREFIX$today", 0) + 1)
        }
    }

    /** Call to add elapsed listening minutes (e.g. on stop / timer finish). */
    fun addMinutes(minutes: Int) {
        if (minutes <= 0) return
        prefs.edit { putInt(KEY_MINUTES, prefs.getInt(KEY_MINUTES, 0) + minutes) }
    }

    /**
     * If the user has missed more than the grace window, collapse the streak to 0
     * so the displayed value is honest the next time they open the app.
     */
    private fun refreshStreak() {
        val today = epochDay()
        val lastDay = prefs.getLong(KEY_LAST_DAY, -1)
        if (lastDay >= 0 && today - lastDay > 2) {
            prefs.edit { putInt(KEY_STREAK, 0) }
        }
    }

    companion object {
        private const val KEY_STREAK      = "streak"
        private const val KEY_LONGEST     = "longest_streak"
        private const val KEY_SESSIONS    = "total_sessions"
        private const val KEY_MINUTES     = "total_minutes"
        private const val KEY_LAST_DAY    = "last_day"
        private const val KEY_TODAY_COUNT = "today_count"
        private const val KEY_DAY_PREFIX  = "day_"
    }
}
