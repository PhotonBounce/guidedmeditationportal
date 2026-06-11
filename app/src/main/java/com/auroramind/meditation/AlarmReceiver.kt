package com.auroramind.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires the scheduled Meditation Alarm and re-arms it after each ring or
 * device reboot. Launches [AlarmRingActivity] full-screen over the lock screen.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                AlarmScheduler.reschedule(context)
                ReminderScheduler.reschedule(context)
            }
            ReminderScheduler.ACTION_REMINDER -> ReminderScheduler.fireNotification(context)
            AlarmScheduler.ACTION_FIRE -> {
                val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                context.startActivity(ringIntent)

                // Re-arm for tomorrow so the alarm repeats daily.
                AlarmScheduler.reschedule(context)
            }
        }
    }
}
