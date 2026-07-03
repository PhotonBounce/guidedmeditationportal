package com.auroramind.meditation

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.auroramind.meditation.databinding.ActivityAlarmRingBinding

/**
 * Full-screen ringing UI shown when the Meditation Alarm fires. Plays the
 * chosen guided track (MediaPlayer) or gentle synthesized tone (AudioEngine)
 * and lets the user dismiss or snooze for ten minutes.
 */
class AlarmRingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmRingBinding
    private lateinit var prefs: PrefsManager

    private var mediaPlayer: MediaPlayer? = null
    private var audioEngine: AudioEngine? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // The app is always dark — force light bar icons regardless of the
            // device theme (default auto() would draw dark icons on our dark UI)
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        binding = ActivityAlarmRingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)
        // Hardware volume keys control the ALARM stream on this screen
        volumeControlStream = AudioManager.STREAM_ALARM

        // Edge-to-edge: keep Dismiss / Snooze clear of the system bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        showOverLockScreen()
        acquireWakeLock()
        bindLabels()
        startRinging()

        binding.btnDismiss.setOnClickListener {
            stopRinging()
            finish()
        }
        binding.btnSnooze.setOnClickListener {
            stopRinging()
            AlarmScheduler.scheduleSnooze(this, 10)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun showOverLockScreen() {
        // setShowWhenLocked/setTurnScreenOn only exist on API 27+ — calling
        // them on API 23-26 crashes with NoSuchMethodError when the alarm fires
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    @Suppress("DEPRECATION")
    private fun acquireWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "MeditationPortal::AlarmWakeLock"
        )
        wakeLock?.acquire(5 * 60 * 1000L)
    }

    private fun bindLabels() {
        if (prefs.getAlarmSourceType() == "TRACK") {
            val track = prefs.getAlarmTrack()
            binding.ringEmoji.text = track.emoji
            binding.ringSubtitle.text = "Waking you with ${track.displayName}"
        } else {
            val tone = prefs.getAlarmTone()
            binding.ringEmoji.text = tone.emoji
            binding.ringSubtitle.text = "A gentle ${tone.displayName.lowercase()} to start your day"
        }
    }

    private fun startRinging() {
        if (prefs.getAlarmSourceType() == "TRACK") {
            val track = prefs.getAlarmTrack()
            // Play on the ALARM stream — MediaPlayer.create() defaults to the
            // media stream, which is silent when media volume is turned down
            mediaPlayer = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(
                        this@AlarmRingActivity,
                        Uri.parse("android.resource://$packageName/${track.rawResId}")
                    )
                    isLooping = true
                    prepare()
                    start()
                }
            }.getOrNull()
        } else {
            audioEngine = AudioEngine().apply {
                start(prefs.getAlarmTone(), AudioAttributes.USAGE_ALARM)
            }
        }
    }

    private fun stopRinging() {
        mediaPlayer?.run { if (isPlaying) stop(); release() }
        mediaPlayer = null
        audioEngine?.stop()
        audioEngine = null
        wakeLock?.run { if (isHeld) release() }
        wakeLock = null
        // Remove the full-screen-intent notification — otherwise it lingers
        // after dismissal and tapping it re-rings the alarm.
        androidx.core.app.NotificationManagerCompat.from(this)
            .cancel(AlarmReceiver.RING_NOTIFICATION_ID)
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }
}
