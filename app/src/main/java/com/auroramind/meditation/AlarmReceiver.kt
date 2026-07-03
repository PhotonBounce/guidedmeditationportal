package com.auroramind.meditation

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat

/**
 * Fires the scheduled Meditation Alarm and re-arms it after each ring.
 * (Boot re-arm lives in the separate, exported [BootReceiver] — this one is
 * NOT exported so no third-party app can spoof-trigger the alarm.)
 * On Android 10+ a BroadcastReceiver cannot launch an activity directly, so
 * the ring screen is delivered via a full-screen intent notification
 * (USE_FULL_SCREEN_INTENT).
 */
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val RING_CHANNEL_ID = "meditation_alarm_ring"
        const val RING_NOTIFICATION_ID = 4201   // cancelled by AlarmRingActivity
    }

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ReminderScheduler.ACTION_REMINDER -> ReminderScheduler.fireNotification(context)
            AlarmScheduler.ACTION_FIRE -> {
                showRingScreen(context)
                // Re-arm for tomorrow so the alarm repeats daily.
                AlarmScheduler.reschedule(context)
            }
            // Snooze one-shot: ring again but DON'T touch the daily schedule
            AlarmScheduler.ACTION_SNOOZE -> showRingScreen(context)
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
                    // Alarm-attributed channel sound: if the full-screen intent is
                    // blocked (Android 14+ FSI permission revoked), the alarm still
                    // makes noise on the alarm stream
                    setSound(
                        Settings.System.DEFAULT_ALARM_ALERT_URI,
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
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
            .build()

        nm.notify(RING_NOTIFICATION_ID, notification)

        // Direct launch still works when the app is in the foreground or on
        // older Android versions; harmless no-op when blocked.
        runCatching { context.startActivity(ringIntent) }
    }
}
