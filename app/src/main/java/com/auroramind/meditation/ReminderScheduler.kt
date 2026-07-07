package com.auroramind.meditation

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

/**
 * Schedules a gentle daily check-in reminder notification — the single most
 * effective retention lever after streaks. A quiet, tappable notification
 * that opens the app, no audio or full-screen ring involved.
 */
object ReminderScheduler {

    const val ACTION_REMINDER = "com.auroramind.meditation.DAILY_REMINDER"
    private const val REQUEST_CODE = 7373
    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIF_ID = 9001

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_REMINDER }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** (Re)schedule based on saved prefs. Cancels if the reminder is disabled. */
    fun reschedule(context: Context) {
        val prefs = PrefsManager(context)
        if (!prefs.isReminderEnabled()) { cancel(context); return }
        schedule(context, prefs.getReminderHour(), prefs.getReminderMinute())
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextOccurrence(hour, minute)
        // Inexact is fine (and battery-friendly) for a soft reminder.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent(context))
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(context))
    }

    /** Called from AlarmReceiver when the reminder fires — posts the notification + re-arms. */
    fun fireNotification(context: Context) {
        ensureChannel(context)

        val stats = StatsManager(context)
        val streak = stats.currentStreak()

        // Every ping teaches a quick technique tuned to the user's goal — turns a
        // nag into a genuinely useful 30-second practice (no audio required).
        val goal = PrefsManager(context).getGoal()
        val tech = MicroTechniques.forGoal(goal)
        val streakLine = if (streak > 0) "🔥 $streak-day streak — keep it alive.\n\n" else ""
        val body = "$streakLine${tech.emoji} ${tech.title}\n${tech.body}"

        val openIntent = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tab_sounds)
            .setContentTitle("Stay strong 💪  ·  ${tech.title}")
            .setContentText(tech.teaser)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        runCatching {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIF_ID, notif)
        }

        // Re-arm for tomorrow
        reschedule(context)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID, "Daily reminder",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "A gentle daily nudge to stay on track" }
                )
            }
        }
    }

    private fun nextOccurrence(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) target.add(Calendar.DAY_OF_MONTH, 1)
        return target.timeInMillis
    }
}
