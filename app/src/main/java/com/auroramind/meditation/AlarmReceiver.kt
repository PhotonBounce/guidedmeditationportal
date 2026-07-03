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
        // v2: the original "meditation_alarm_ring" channel shipped without a
        // sound and channel attributes are immutable once created — new IDs
        // (and deleting the old channel) are the only way to migrate updaters.
        private const val RING_CHANNEL_ID = "meditation_alarm_ring_v2"        // silent — ring screen supplies audio
        private const val FALLBACK_CHANNEL_ID = "meditation_alarm_fallback"   // audible — used when FSI is revoked
        private const val LEGACY_CHANNEL_ID = "meditation_alarm_ring"
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
        // Can the ring screen actually appear? (FSI permission revocable on 34+)
        val canShowRingScreen = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            nm.canUseFullScreenIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.deleteNotificationChannel(LEGACY_CHANNEL_ID)
            // Silent channel: AlarmRingActivity is the sole audio source, so the
            // notification itself must not blast the system klaxon over it
            nm.createNotificationChannel(
                NotificationChannel(
                    RING_CHANNEL_ID,
                    "Meditation Alarm",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Shows the wake-up screen when your meditation alarm fires"
                    setBypassDnd(true)
                    setSound(null, null)
                }
            )
            // Audible fallback: only used when the full-screen intent is blocked
            // (Android 14+ permission revoked) and our ring screen can't appear
            nm.createNotificationChannel(
                NotificationChannel(
                    FALLBACK_CHANNEL_ID,
                    "Meditation Alarm (sound fallback)",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Rings audibly when the full-screen alarm is not allowed"
                    setBypassDnd(true)
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

        val channelId = if (canShowRingScreen) RING_CHANNEL_ID else FALLBACK_CHANNEL_ID
        val notification = NotificationCompat.Builder(context, channelId)
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
