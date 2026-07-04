package com.auroramind.meditation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

/**
 * Schedules / cancels the single Meditation Alarm using AlarmManager,
 * driven entirely by the values persisted in [PrefsManager].
 */
object AlarmScheduler {

    const val ACTION_FIRE = "com.auroramind.meditation.ALARM_FIRE"
    const val ACTION_SNOOZE = "com.auroramind.meditation.ALARM_SNOOZE"
    private const val REQUEST_CODE = 4242
    private const val SNOOZE_REQUEST_CODE = 4243

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_FIRE }
        return PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Distinct request code + action: reschedule()/cancel() of the daily alarm
    // must never cancel a pending snooze (opening AlarmActivity re-arms daily)
    private fun snoozePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_SNOOZE }
        return PendingIntent.getBroadcast(
            context, SNOOZE_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** (Re)schedules the alarm for the next occurrence of the saved time, if enabled. */
    fun reschedule(context: Context) {
        val prefs = PrefsManager(context)
        if (!prefs.isAlarmEnabled()) {
            cancel(context)
            return
        }
        schedule(context, prefs.getAlarmHour(), prefs.getAlarmMinute())
    }

    /** Schedules the next firing for the given wall-clock time (today or tomorrow). */
    fun schedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextOccurrence(hour, minute)

        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        val pi = pendingIntent(context)
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    /** One-shot re-fire after [minutes] — used for the Snooze button. */
    fun scheduleSnooze(context: Context, minutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val pi = snoozePendingIntent(context)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
        alarmManager.cancel(snoozePendingIntent(context))
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
