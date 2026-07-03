package com.auroramind.meditation

import android.app.ActivityManager
import android.app.KeyguardManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager
import android.os.Process
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
        // Will the ring screen ACTUALLY appear? Three requirements:
        //  1. FSI permission not revoked (Android 14+)
        //  2. The system only auto-launches a full-screen intent when the device
        //     is locked / screen off; with the screen on and unlocked it degrades
        //     to a heads-up card — UNLESS our own app is foregrounded, where the
        //     direct startActivity() below succeeds instead.
        val fsiAllowed = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            nm.canUseFullScreenIntent()
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val screenAwakeUnlocked = pm.isInteractive && !km.isKeyguardLocked
        val appInForeground = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
            .runningAppProcesses?.any {
                it.pid == Process.myPid() &&
                it.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            } == true
        val canShowRingScreen = (fsiAllowed && !screenAwakeUnlocked) || appInForeground
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
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Meditation Alarm")
            .setContentText("Time to wake gently — tap to open")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setAutoCancel(true)

        // Pre-O has no channels — the audible fallback needs the sound on the
        // builder (ignored on O+ where the channel's sound applies instead)
        if (channelId == FALLBACK_CHANNEL_ID) {
            @Suppress("DEPRECATION")
            builder.setSound(Settings.System.DEFAULT_ALARM_ALERT_URI, AudioManager.STREAM_ALARM)
        }

        nm.notify(RING_NOTIFICATION_ID, builder.build())

        // Direct launch works when our app is in the foreground (background
        // activity starts are blocked on Android 10+).
        if (appInForeground) runCatching { context.startActivity(ringIntent) }
    }
}
