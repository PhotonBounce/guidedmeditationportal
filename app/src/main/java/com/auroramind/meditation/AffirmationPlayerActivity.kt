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
    private var interstitial: InterstitialAdManager? = null
    private lateinit var prefs: PrefsManager

    private val advance = object : Runnable {
        override fun run() {
            if (lines.isEmpty()) return
            index = (index + 1) % lines.size
            crossFadeTo(lines[index])
            updateFav()
            handler.postDelayed(this, HOLD_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()
        hideSystemBars()
        binding = ActivityAffirmationPlayerBinding.inflate(layoutInflater)
        setContentView(AuraBackground.wrap(this, binding.root))
        // Bars are hidden (zero insets), but the mute/fav/close controls hug the
        // top edge — pad the inner content so they stay tappable around a display
        // cutout and if the user swipes the bars back in.
        binding.root.padSystemBars()

        prefs = PrefsManager(this)
        val theme = AffirmationContent.getTheme(this, intent.getStringExtra(EXTRA_THEME))
        lines = theme?.lines ?: AffirmationContent.forHabit(this, prefs.getHabitType(), prefs.getFreedomGoal())
        if (lines.isEmpty()) lines = listOf(getString(R.string.aff_fallback_line))

        binding.affirmationText.text = lines[0]
        updateFav()

        // Credit today's session toward the streak.
        StatsManager(this).recordSessionStart()

        // Free tier: warm up an interstitial to show when the session is closed.
        if (!prefs.isPremium()) {
            interstitial = InterstitialAdManager(this).also { it.preload() }
        }

        startSoundscape(prefs)
        startVoiceIfAvailable(prefs)

        binding.muteBtn.setOnClickListener {
            muted = !muted
            val v = if (muted) 0f else SOUND_VOLUME
            mediaPlayer?.setVolume(v, v)
            binding.muteBtn.text = if (muted) "🔇" else "🔊"
        }
        binding.favBtn.setOnClickListener {
            if (lines.isNotEmpty()) { prefs.toggleAffirmationFavorite(lines[index]); updateFav() }
        }
        binding.closeBtn.setOnClickListener { closeSession() }

        handler.postDelayed(advance, HOLD_MS)
    }

    /** Close the session, showing a free-tier interstitial on every 2nd finish. */
    private fun closeSession() {
        val mgr = interstitial
        if (mgr != null) {
            prefs.incrementStopCount()
            if (prefs.getStopCount() % 2 == 0 && mgr.maybeShowThen(this) { finish() }) return
        }
        finish()
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

    private fun updateFav() {
        if (lines.isEmpty()) return
        binding.favBtn.text = if (prefs.isAffirmationFavorite(lines[index])) "♥" else "♡"
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
