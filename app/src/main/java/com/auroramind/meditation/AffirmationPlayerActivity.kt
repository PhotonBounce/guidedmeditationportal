package com.auroramind.meditation

import android.content.res.AssetFileDescriptor
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.auroramind.meditation.databinding.ActivityAffirmationPlayerBinding

/**
 * Full-screen affirmation session: the user's habit-aware affirmation lines
 * cross-fade one at a time over a looping soundscape.
 *
 * Audio uses a plain looping [MediaPlayer] on one of the bundled [BgMusicType]
 * tracks — the same lightweight pattern as AlarmRingActivity — so no spoken
 * recordings are required yet. Opening a session credits the day toward the
 * affirmation streak via [StatsManager].
 */
class AffirmationPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAffirmationPlayerBinding
    private val handler = Handler(Looper.getMainLooper())

    private var lines: List<String> = emptyList()
    private var index = 0
    private var muted = false
    private var mediaPlayer: MediaPlayer? = null
    private var voicePlayer: MediaPlayer? = null
    private var voiceAfd: AssetFileDescriptor? = null

    private val advance = object : Runnable {
        override fun run() {
            if (lines.isEmpty()) return
            index = (index + 1) % lines.size
            crossFadeTo(lines[index])
            handler.postDelayed(this, HOLD_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAffirmationPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PrefsManager(this)
        val theme = AffirmationContent.getTheme(intent.getStringExtra(EXTRA_THEME))
        lines = theme?.lines ?: AffirmationContent.forHabit(prefs.getHabitType(), prefs.getFreedomGoal())
        if (lines.isEmpty()) lines = listOf("I am free, one breath at a time.")

        binding.affirmationText.text = lines[0]

        // Credit today's session toward the streak.
        StatsManager(this).recordSessionStart()

        startSoundscape(prefs)
        startVoiceIfAvailable(prefs)

        binding.muteBtn.setOnClickListener {
            muted = !muted
            val v = if (muted) 0f else SOUND_VOLUME
            mediaPlayer?.setVolume(v, v)
            binding.muteBtn.text = if (muted) "🔇" else "🔊"
        }
        binding.closeBtn.setOnClickListener { finish() }

        handler.postDelayed(advance, HOLD_MS)
    }

    private fun startSoundscape(prefs: PrefsManager) {
        val saved = BgMusicType.fromName(prefs.getBgMusicTrack())
        val track = if (saved == BgMusicType.NONE) BgMusicType.MEDITATION_IN_E_MAJOR else saved
        mediaPlayer = MediaPlayer.create(this, track.rawResId)?.apply {
            isLooping = true
            setVolume(SOUND_VOLUME, SOUND_VOLUME)
            start()
        }
    }

    /**
     * If the user has bundled spoken affirmation audio under assets/affirmations/,
     * play a track (habit-matched when possible) over the soundscape and show its
     * title. No audio bundled -> this no-ops and the text affirmations carry the
     * session.
     */
    private fun startVoiceIfAvailable(prefs: PrefsManager) {
        val tracks = AffirmationLibrary.list(this)
        val track = AffirmationLibrary.pick(
            tracks, prefs.getHabitType(), StatsManager(this).totalSessions()
        ) ?: return
        runCatching {
            val afd = assets.openFd(track.asset)
            voiceAfd = afd
            voicePlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
            binding.caption.text = "♪  ${track.title}"
        }
    }

    private fun crossFadeTo(text: String) {
        binding.affirmationText.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction {
                binding.affirmationText.text = text
                binding.affirmationText.animate().alpha(1f).setDuration(700).start()
            }
            .start()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        mediaPlayer?.run { if (isPlaying) stop(); release() }
        mediaPlayer = null
        voicePlayer?.run { runCatching { if (isPlaying) stop() }; release() }
        voicePlayer = null
        runCatching { voiceAfd?.close() }
        voiceAfd = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_THEME = "theme"
        private const val HOLD_MS = 6500L
        private const val SOUND_VOLUME = 0.5f
    }
}
