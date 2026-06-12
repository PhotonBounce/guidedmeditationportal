package com.auroramind.meditation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * Fires the scheduled Meditation Alarm and re-arms it after each ring or
 * device reboot. On Android 10+ a BroadcastReceiver cannot launch an
 * activity directly, so the ring screen is delivered via a full-screen
 * intent notification (USE_FULL_SCREEN_INTENT).
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val RING_CHANNEL_ID = "meditation_alarm_ring"
        private const val RING_NOTIFICATION_ID = 4201
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                AlarmScheduler.reschedule(context)
                ReminderScheduler.reschedule(context)
            }
            ReminderScheduler.ACTION_REMINDER -> ReminderScheduler.fireNotification(context)
            AlarmScheduler.ACTION_FIRE -> {
                showRingScreen(context)
                // Re-arm for tomorrow so the alarm repeats daily.
                AlarmScheduler.reschedule(context)
            }
        }
    }

    private fun showRingScreen(context: Context) {
        val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val fullScreenPi = PendingIntent.getActivity(
            context, RING_NOTIFICATION_ID, ringIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(
                    RING_CHANNEL_ID,
                    "Meditation Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Rings when your scheduled meditation alarm fires"
                    setBypassDnd(true)
                }
            )
        }

        val notification = NotificationCompat.Builder(context, RING_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Meditation Alarm")
            .setContentText("Time to wake gently — tap to open")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        nm.notify(RING_NOTIFICATION_ID, notification)

        // Direct launch still works when the app is in the foreground or on
        // older Android versions; harmless no-op when blocked.
        runCatching { context.startActivity(ringIntent) }
    }
}
