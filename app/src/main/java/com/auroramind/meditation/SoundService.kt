package com.auroramind.meditation

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.os.*
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle

class SoundService : Service() {

    /** Jukebox repeat behaviour applied when the current track finishes. */
    enum class RepeatMode { OFF, ALL, ONE }

    companion object {
        const val NOTIF_ID   = 1
        const val CHANNEL_ID = "meditation_portal_channel"
        const val ACTION_STOP = "com.auroramind.meditation.STOP"

        /** Set by lifecycle; lets MainActivity decide whether to bind. */
        @Volatile var isRunning = false
    }

    @Volatile var repeatMode: RepeatMode = RepeatMode.ONE
        private set
    @Volatile var shuffleEnabled: Boolean = false
        private set

    private var mediaPlayer: MediaPlayer? = null
    private var bgMediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var eqBass: Float = 1.0f
    private var eqMid: Float = 1.0f
    private var eqTreble: Float = 1.0f
    private val binder = SoundBinder()
    @Volatile private var currentVolume: Float = 0.7f
    @Volatile private var currentBgVolume: Float = 0.33f
    private lateinit var prefs: PrefsManager

    @Volatile var currentSound: SoundType = SoundType.TONGLEN
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
        prefs = PrefsManager(this)
        setEq(prefs.getEqBass(), prefs.getEqMid(), prefs.getEqTreble())
        repeatMode = prefs.getRepeatMode()
        shuffleEnabled = prefs.isShuffleEnabled()
        currentBgVolume = prefs.getBgVolume()
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "Portal").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onStop() { stopPlaybackAndSelf() }
                override fun onPause() { stopPlaybackAndSelf() }
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
        if (intent == null) {
            // START_STICKY restart after process death — don't surprise the
            // user by auto-playing a default track out of nowhere
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent.action == ACTION_STOP) {
            stopPlaybackAndSelf()
            return START_NOT_STICKY
        }

        val name = intent.getStringExtra("SOUND_TYPE") ?: currentSound.name
        currentSound = runCatching { SoundType.valueOf(name) }.getOrDefault(currentSound)

        acquireWakeLock()
        requestAudioFocus()
        startForeground(NOTIF_ID, buildNotification())

        startSound(currentSound)
        updateNotification()
        return START_STICKY
    }

    private fun startSound(type: SoundType) {
        releaseMediaPlayer()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val sessionId = audioManager.generateAudioSessionId()
        mediaPlayer = runCatching {
            MediaPlayer.create(this, type.rawResId, attrs, sessionId)?.apply {
                isLooping = repeatMode == RepeatMode.ONE && !shuffleEnabled
                setVolume(currentVolume, currentVolume)
                setOnCompletionListener { onTrackFinished() }
                setOnErrorListener { _, _, _ -> true }
                start()
            }
        }.getOrNull()
        if (mediaPlayer == null) {
            mediaPlayer = runCatching {
                MediaPlayer.create(this, type.rawResId)?.apply {
                    isLooping = repeatMode == RepeatMode.ONE && !shuffleEnabled
                    setVolume(currentVolume, currentVolume)
                    setOnCompletionListener { onTrackFinished() }
                    setOnErrorListener { _, _, _ -> true }
                    start()
                }
            }.getOrNull()
        }
        attachEqualizer()
        startBgMusic(attrs, sessionId)
    }

    private fun startBgMusic(attrs: AudioAttributes, sessionId: Int) {
        val bgTrackName = prefs.getBgMusicTrack()
        val bgType = BgMusicType.fromName(bgTrackName)
        if (bgType == BgMusicType.NONE) return

        bgMediaPlayer = runCatching {
            MediaPlayer.create(this, bgType.rawResId, attrs, sessionId)?.apply {
                isLooping = true
                val bgVol = currentBgVolume * currentVolume
                setVolume(bgVol, bgVol)
                start()
            }
        }.getOrNull()

        if (bgMediaPlayer == null) {
            bgMediaPlayer = runCatching {
                MediaPlayer.create(this, bgType.rawResId)?.apply {
                    isLooping = true
                    val bgVol = currentBgVolume * currentVolume
                    setVolume(bgVol, bgVol)
                    start()
                }
            }.getOrNull()
        }
    }

    /** Jukebox auto-advance — called when a non-looping track finishes naturally. */
    private fun onTrackFinished() {
        if (repeatMode == RepeatMode.ONE && !shuffleEnabled) return // handled by MediaPlayer.isLooping

        val tracks = SoundType.values()
        if (tracks.isEmpty()) return

        val next = if (shuffleEnabled) {
            tracks.filter { it != currentSound }.randomOrNull() ?: tracks.first()
        } else {
            val idx = tracks.indexOf(currentSound)
            val nextIdx = idx + 1
            when {
                nextIdx < tracks.size -> tracks[nextIdx]
                repeatMode == RepeatMode.ALL -> tracks.first()
                else -> null // repeat OFF and end of list — stop
            }
        }

        if (next == null) {
            stopPlaybackAndSelf()
        } else {
            changeSound(next)
        }
    }

    /** Cycles OFF → ALL → ONE → OFF; persists and applies to the active player. */
    fun cycleRepeatMode(): RepeatMode {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        mediaPlayer?.isLooping = repeatMode == RepeatMode.ONE && !shuffleEnabled
        return repeatMode
    }

    fun setShuffleEnabled(enabled: Boolean) {
        shuffleEnabled = enabled
        mediaPlayer?.isLooping = repeatMode == RepeatMode.ONE && !enabled
    }

    private fun releaseMediaPlayer() {
        equalizer?.runCatching { release() }
        equalizer = null
        mediaPlayer?.run { if (isPlaying) stop(); release() }
        mediaPlayer = null
        bgMediaPlayer?.run { if (isPlaying) stop(); release() }
        bgMediaPlayer = null
    }

    /** Attaches a 3-band-mapped system Equalizer to the active MediaPlayer's audio session. */
    private fun attachEqualizer() {
        val sessionId = mediaPlayer?.audioSessionId ?: return
        equalizer = runCatching {
            Equalizer(0, sessionId).apply { enabled = true }
        }.getOrNull()
        applyEq()
    }

    /** Maps bass/mid/treble sliders (0.0–2.0, 1.0 = neutral) onto the device's EQ bands. */
    private fun applyEq() {
        val eq = equalizer ?: return
        val bands = eq.numberOfBands
        if (bands <= 0) return
        val range = eq.bandLevelRange
        val maxGain = range[1].toFloat()
        for (band in 0 until bands) {
            val third = bands / 3f
            val gainPct = when {
                band < third       -> eqBass
                band < third * 2   -> eqMid
                else               -> eqTreble
            }
            val level = ((gainPct - 1.0f) * maxGain).toInt().toShort()
            runCatching { eq.setBandLevel(band.toShort(), level) }
        }
    }

    /**
     * Full stop — releases playback, focus and the foreground notification.
     * stopSelf() alone is NOT enough while MainActivity keeps the service
     * bound: the service stays alive and audio keeps playing, so the
     * notification Stop button and headset pause appeared to do nothing.
     */
    /** Set by the bound MainActivity so its UI refreshes when playback
     *  stops from the notification, headset, playlist end, or task removal. */
    var onStoppedExternally: (() -> Unit)? = null

    // ── Sleep timer (authoritative) — survives the Activity being destroyed ──
    private val stopTimerHandler = Handler(Looper.getMainLooper())
    private var stopTimerRunnable: Runnable? = null

    fun setStopTimer(ms: Long) {
        cancelStopTimer()
        if (ms <= 0) return
        stopTimerRunnable = Runnable { stopPlaybackAndSelf() }
            .also { stopTimerHandler.postDelayed(it, ms) }
    }

    fun cancelStopTimer() {
        stopTimerRunnable?.let { stopTimerHandler.removeCallbacks(it) }
        stopTimerRunnable = null
    }

    private fun stopPlaybackAndSelf() {
        releaseMediaPlayer()
        abandonAudioFocus()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
        onStoppedExternally?.invoke()
    }

    override fun onDestroy() {
        cancelStopTimer()
        onStoppedExternally = null
        releaseMediaPlayer()
        abandonAudioFocus()
        wakeLock?.let { if (it.isHeld) it.release() }
        runCatching { mediaSession.isActive = false; mediaSession.release() }
        isRunning = false
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Called from bound MainActivity
    // ─────────────────────────────────────────────────────────────────────────

    fun changeSound(type: SoundType) {
        currentSound = type
        startSound(type)
        updateNotification()
    }

    fun setVolume(vol: Float) {
        currentVolume = vol
        mediaPlayer?.setVolume(vol, vol)
        val bgVol = currentBgVolume * currentVolume
        bgMediaPlayer?.setVolume(bgVol, bgVol)
    }

    fun setBgVolume(vol: Float) {
        currentBgVolume = vol
        val bgVol = currentBgVolume * currentVolume
        bgMediaPlayer?.setVolume(bgVol, bgVol)
    }

    fun changeBgMusic(bgMusic: BgMusicType) {
        bgMediaPlayer?.run {
            if (isPlaying) stop()
            release()
        }
        bgMediaPlayer = null

        // Only start BGM if the main meditation track is actively playing
        if (isPlaying() && bgMusic != BgMusicType.NONE) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val sessionId = audioManager.generateAudioSessionId()

            bgMediaPlayer = runCatching {
                MediaPlayer.create(this, bgMusic.rawResId, attrs, sessionId)?.apply {
                    isLooping = true
                    val bgVol = currentBgVolume * currentVolume
                    setVolume(bgVol, bgVol)
                    start()
                }
            }.getOrNull()

            if (bgMediaPlayer == null) {
                bgMediaPlayer = runCatching {
                    MediaPlayer.create(this, bgMusic.rawResId)?.apply {
                        isLooping = true
                        val bgVol = currentBgVolume * currentVolume
                        setVolume(bgVol, bgVol)
                        start()
                    }
                }.getOrNull()
            }
        }
    }

    fun isPlaying() = mediaPlayer?.isPlaying == true

    /** Current playback position in ms, or 0 if nothing is loaded. */
    fun getPositionMs(): Int = runCatching { mediaPlayer?.currentPosition ?: 0 }.getOrDefault(0)

    /** Total duration of the current track in ms, or 0 if nothing is loaded. */
    fun getDurationMs(): Int = runCatching { mediaPlayer?.duration ?: 0 }.getOrDefault(0)

    /** Scrub to an absolute position in the current track. */
    fun seekTo(positionMs: Int) {
        runCatching { mediaPlayer?.seekTo(positionMs.coerceIn(0, getDurationMs())) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Audio focus
    // ─────────────────────────────────────────────────────────────────────────

    // Apply duck/restore DIRECTLY to the player — never via setVolume(), which
    // would overwrite the user's stored volume. Permanent LOSS (another app
    // took over for good) fully stops playback instead of muting forever.
    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { change ->
        val bgVol = currentBgVolume * currentVolume
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS ->
                stopPlaybackAndSelf()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                mediaPlayer?.setVolume(0.25f * currentVolume, 0.25f * currentVolume)
                bgMediaPlayer?.setVolume(0.25f * bgVol, 0.25f * bgVol)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayer?.setVolume(0.4f * currentVolume, 0.4f * currentVolume)
                bgMediaPlayer?.setVolume(0.4f * bgVol, 0.4f * bgVol)
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(currentVolume, currentVolume)
                bgMediaPlayer?.setVolume(bgVol, bgVol)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()
            audioManager.requestAudioFocus(focusRequest!!)
        } else {
            // API 23-25 have no AudioFocusRequest — without this branch the
            // app never took focus at all on those devices
            audioManager.requestAudioFocus(
                focusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wake lock — keeps CPU alive so audio doesn't skip when screen is off
    // ─────────────────────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MeditationPortal::WakeLock")
        wakeLock?.acquire(8 * 60 * 60 * 1000L)   // max 8 hours
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Portal Playback",
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

        val notifTitle = "${currentSound.emoji}  ${currentSound.displayName}"
        val notifSubtext = currentSound.category

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(notifTitle)
            .setContentText("Portal · Playing")
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

    fun setEq(bass: Float, mid: Float, treble: Float) {
        eqBass = bass
        eqMid = mid
        eqTreble = treble
        applyEq()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopPlaybackAndSelf()
    }
}
