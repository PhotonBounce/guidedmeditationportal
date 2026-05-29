package com.soundpad.sleep

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.*
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle

class SoundService : Service() {

    companion object {
        const val NOTIF_ID   = 1
        const val CHANNEL_ID = "soundpad_channel"
        const val ACTION_STOP = "com.soundpad.sleep.STOP"

        /** Set by lifecycle; lets MainActivity decide whether to bind. */
        @Volatile var isRunning = false
    }

    private val engine = AudioEngine()
    private var mediaPlayer: MediaPlayer? = null
    private val binder = SoundBinder()
    @Volatile private var currentVolume: Float = 0.7f

    private val fadeHandler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null

    @Volatile var currentSound: SoundType = SoundType.WHITE_NOISE
        private set

    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var mediaSession: MediaSessionCompat

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
        initMediaSession()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "SoundPad").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onStop() { stopSelf() }
                override fun onPause() { stopSelf() }
            })
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0L, 1f)
                    .setActions(
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_PAUSE
                    )
                    .build()
            )
            isActive = true
        }
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

        // Don't start a single sound if mix mode is already active —
        // this can happen when applyMix() calls startForegroundService() after
        // doStartMix() has already been invoked on the bound service.
        if (!isMixMode) startSound(currentSound)
        updateNotification()
        return START_STICKY
    }

    private fun startSound(type: SoundType) {
        if (type.rawResId != 0) {
            // File-backed: use MediaPlayer, stop synthesizer
            engine.stop()
            releaseMediaPlayer()
            mediaPlayer = MediaPlayer.create(this, type.rawResId)?.apply {
                isLooping = true
                setVolume(currentVolume, currentVolume)
                start()
            }
        } else {
            // Synthesized: use AudioEngine, stop MediaPlayer
            releaseMediaPlayer()
            if (engine.isPlaying()) engine.changeSound(type) else engine.start(type)
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.run { if (isPlaying) stop(); release() }
        mediaPlayer = null
    }

    // ── Mix Mode — up to 3 simultaneous sound layers ─────────────────────────

    private data class MixLayer(
        val type: SoundType,
        val player: MediaPlayer?,
        val engine: AudioEngine?,
        var currentVol: Float = 0.7f
    )

    private val mixLayers = mutableListOf<MixLayer>()
    var isMixMode = false
        private set

    /** Replaces any current playback with up to 3 layered sounds, fading in over ~2 s. */
    fun startMix(layerDefs: List<Pair<SoundType, Float>>) {
        cancelFade()
        engine.stop()
        releaseMediaPlayer()
        stopAllMixLayers()
        isMixMode = true
        // Start all layers silently; fadeIn() will ramp to target volumes
        for ((type, vol) in layerDefs) {
            if (type.rawResId != 0) {
                val mp = MediaPlayer.create(this, type.rawResId)?.apply {
                    isLooping = true
                    setVolume(0f, 0f)
                    start()
                }
                mixLayers.add(MixLayer(type, mp, null, 0f))
            } else {
                val eng = AudioEngine().apply {
                    setVolume(0f)
                    start(type)
                }
                mixLayers.add(MixLayer(type, null, eng, 0f))
            }
        }
        updateNotification()
        fadeIn(layerDefs)
    }

    /** Fades out all mix layers over ~1.2 s then stops. */
    fun stopMix() {
        if (!isMixMode) return
        cancelFade()
        val capturedVols = mixLayers.map { it.type to it.currentVol }
        val steps = 15
        var step = 0
        fadeRunnable = object : Runnable {
            override fun run() {
                step++
                val progress = 1f - (step.toFloat() / steps)
                capturedVols.forEach { (type, vol) ->
                    setMixLayerVolume(type, vol * maxOf(progress, 0f))
                }
                if (step < steps) {
                    fadeHandler.postDelayed(this, 80L)
                } else {
                    stopAllMixLayers()
                    isMixMode = false
                    updateNotification()
                }
            }
        }
        fadeHandler.post(fadeRunnable!!)
    }

    fun setMixLayerVolume(type: SoundType, vol: Float) {
        mixLayers.find { it.type == type }?.apply {
            currentVol = vol
            player?.setVolume(vol, vol)
            engine?.setVolume(vol)
        }
    }

    fun getMixLayerTypes(): List<SoundType> = mixLayers.map { it.type }

    private fun fadeIn(targets: List<Pair<SoundType, Float>>) {
        val steps = 20
        var step = 0
        fadeRunnable = object : Runnable {
            override fun run() {
                if (!isMixMode) return
                step++
                val progress = step.toFloat() / steps
                targets.forEach { (type, vol) ->
                    setMixLayerVolume(type, vol * minOf(progress, 1f))
                }
                if (step < steps) fadeHandler.postDelayed(this, 100L)
            }
        }
        fadeHandler.post(fadeRunnable!!)
    }

    private fun cancelFade() {
        fadeRunnable?.let { fadeHandler.removeCallbacks(it) }
        fadeRunnable = null
    }

    private fun stopAllMixLayers() {
        for (layer in mixLayers) {
            layer.player?.runCatching { if (isPlaying) stop(); release() }
            layer.engine?.stop()
        }
        mixLayers.clear()
    }

    override fun onDestroy() {
        cancelFade()
        stopAllMixLayers()
        engine.stop()
        releaseMediaPlayer()
        abandonAudioFocus()
        wakeLock?.release()
        runCatching { mediaSession.isActive = false; mediaSession.release() }
        isRunning = false
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called from bound MainActivity
    // ─────────────────────────────────────────────────────────────────────────

    fun changeSound(type: SoundType) {
        // Switching to single sound exits mix mode
        if (isMixMode) { cancelFade(); stopAllMixLayers(); isMixMode = false }
        currentSound = type
        startSound(type)
        updateNotification()
    }

    fun setVolume(vol: Float) {
        currentVolume = vol
        engine.setVolume(vol)
        mediaPlayer?.setVolume(vol, vol)
    }
    fun isPlaying() = when {
        isMixMode -> mixLayers.isNotEmpty()
        else      -> engine.isPlaying() || mediaPlayer?.isPlaying == true
    }

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
                        AudioManager.AUDIOFOCUS_LOSS                 -> setVolume(0f)
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT       -> setVolume(0.25f * currentVolume)
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> setVolume(0.4f * currentVolume)
                        AudioManager.AUDIOFOCUS_GAIN                 -> setVolume(currentVolume)
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

        val notifTitle = if (isMixMode && mixLayers.isNotEmpty())
            "🎛️  Mix · " + mixLayers.joinToString(" + ") { it.type.emoji }
        else
            "${currentSound.emoji}  ${currentSound.displayName}"
        val notifSubtext = if (isMixMode) "Mix Studio" else currentSound.category

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notifTitle)
            .setContentText("SoundPad · Playing")
            .setSubText(notifSubtext)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_delete, "Stop", stopIntent
                )
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_ID, buildNotification())
    }
}
