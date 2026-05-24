package com.soundpad.sleep

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import androidx.core.app.NotificationCompat

class SoundService : Service() {

    companion object {
        const val NOTIF_ID   = 1
        const val CHANNEL_ID = "soundpad_channel"
        const val ACTION_STOP = "com.soundpad.sleep.STOP"

        /** Set by lifecycle; lets MainActivity decide whether to bind. */
        @Volatile var isRunning = false
    }

    private val engine = AudioEngine()
    private val binder = SoundBinder()

    @Volatile var currentSound: SoundType = SoundType.WHITE_NOISE
        private set

    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    inner class SoundBinder : Binder() {
        fun service() = this@SoundService
    }

    override fun onBind(intent: Intent?) = binder

    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val name = intent?.getStringExtra("SOUND_TYPE") ?: currentSound.name
        currentSound = SoundType.valueOf(name)

        acquireWakeLock()
        requestAudioFocus()
        startForeground(NOTIF_ID, buildNotification())

        if (engine.isPlaying()) {
            engine.changeSound(currentSound)
        } else {
            engine.start(currentSound)
        }
        updateNotification()
        return START_STICKY
    }

    override fun onDestroy() {
        engine.stop()
        abandonAudioFocus()
        wakeLock?.release()
        isRunning = false
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called from bound MainActivity
    // ─────────────────────────────────────────────────────────────────────────

    fun changeSound(type: SoundType) {
        currentSound = type
        engine.changeSound(type)
        updateNotification()
    }

    fun setVolume(vol: Float) = engine.setVolume(vol)
    fun isPlaying() = engine.isPlaying()

    // ─────────────────────────────────────────────────────────────────────────
    // Audio focus
    // ─────────────────────────────────────────────────────────────────────────

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { change ->
                    when (change) {
                        AudioManager.AUDIOFOCUS_LOSS                 -> engine.setVolume(0f)
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT       -> engine.setVolume(0.25f)
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> engine.setVolume(0.4f)
                        AudioManager.AUDIOFOCUS_GAIN                 -> engine.setVolume(0.7f)
                    }
                }
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wake lock — keeps CPU alive so audio doesn't skip when screen is off
    // ─────────────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SoundPad::WakeLock")
        wakeLock?.acquire(8 * 60 * 60 * 1000L)   // max 8 hours
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "SoundPad Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Sleep sound playback controls"
                setShowBadge(false)
                setSound(null, null)
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, SoundService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SoundPad")
            .setContentText("${currentSound.emoji} ${currentSound.displayName}")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification())
    }
}
