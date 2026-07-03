package com.auroramind.meditation

import android.animation.ValueAnimator
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.auroramind.meditation.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        /** Intent extra: when true, MainActivity opens the Unlock dialog on launch/resume. */
        const val EXTRA_SHOW_UNLOCK = "EXTRA_SHOW_UNLOCK"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private lateinit var billing: BillingManager
    private lateinit var adapter: SoundAdapter
    private lateinit var rewarded: RewardedAdManager
    private lateinit var interstitial: InterstitialAdManager
    private lateinit var consent: ConsentManager
    private lateinit var sfx: SoundEffects
    private lateinit var haptic: HapticHelper
    private lateinit var stats: StatsManager

    private var bannerAdView: AdView? = null
    private var adsInitialized = false

    /** Active mood filter for the library (null = show all + favorites). */
    private var currentMood: Mood? = null
    /** Wall-clock time the current playback started, for minute accounting. */
    private var sessionStartMs: Long = 0L

    // ── Service binding ───────────────────────────────────────────────────────
    private var service: SoundService? = null
    private var bound = false
    private var playButtonAnimator: ValueAnimator? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as SoundService.SoundBinder).service()
            bound   = true
            // Push the slider's current volume into the service so its
            // currentVolume field stays in sync from the very first bind.
            service?.setVolume(prefs.getVolume())
            service?.setBgVolume(prefs.getBgVolume())
            service?.setShuffleEnabled(prefs.isShuffleEnabled())
            syncUI()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound   = false
            syncUI()
        }
    }

    // ── Session timer ───────────────────────────────────────────────────────────
    private var countdown: CountDownTimer? = null

    // ── Scrub bar ───────────────────────────────────────────────────────────────
    /** True while the user is dragging the scrub thumb — pauses position polling. */
    private var userScrubbing = false
    private val scrubHandler = Handler(Looper.getMainLooper())
    private val scrubPoll = object : Runnable {
        override fun run() {
            updateScrubBar()
            scrubHandler.postDelayed(this, 500)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        // Branded splash: AndroidX SplashScreen API, animated moon, swaps to
        // the regular theme once we're ready to draw the UI.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Edge-to-edge: cosmic gradient draws under the status / nav bars.
        // enableEdgeToEdge() is the Android 15+ sanctioned API (replaces the
        // deprecated statusBarColor/navigationBarColor window attributes).
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        // Apply window insets so the toolbar sits below the status bar and
        // bottom content doesn't tuck under the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.bottomNavigation.updatePadding(bottom = bars.bottom)
            insets
        }

        AnalyticsHelper.init(this)
        prefs        = PrefsManager(this)
        SessionPremiumManager.unlockAll()          // free session trial — all sounds accessible
        rewarded     = RewardedAdManager(this)
        interstitial = InterstitialAdManager(this)
        consent      = ConsentManager(this)
        sfx          = SoundEffects(this)
        haptic       = HapticHelper(this)
        stats        = StatsManager(this)
        billing  = BillingManager(this) { isPremium ->
            prefs.setPremium(isPremium)
            runOnUiThread { syncUI() }
        }

        setupRecyclerView()
        setupControls()
        setupBgMusicSpinner()
        setupButtonPulsations()
        setupHomeExtras()
        requestNotificationPermission()
        prefs.incrementSessions()

        // Setup persistent Bottom Navigation View
        setupBottomNavigation()

        // Show version badge on the explainer card
        val versionName = packageManager
            .getPackageInfo(packageName, 0).versionName
        binding.tvVersionBadge.text = "v$versionName"

        // Gather GDPR/CCPA consent first; initialize ads only after.
        consent.gatherConsent { canRequestAds ->
            if (canRequestAds && !prefs.isPremium()) setupAds()
        }

        if (!prefs.isOnboardingShown()) {
            OnboardingDialog(this).show()
            prefs.setOnboardingShown(true)
            AnalyticsHelper.logEvent(AnalyticsHelper.Events.ONBOARDING_DONE)
        }

        maybeAskForRating()

        if (intent?.getBooleanExtra(EXTRA_SHOW_UNLOCK, false) == true) {
            showUpgradeDialog(null)
        }

        // Handle LAUNCH_SOUND when MainActivity is freshly created (e.g. process restart)
        // Normal case (activity already running) is handled by onNewIntent().
        intent?.getStringExtra("LAUNCH_SOUND")?.let { name ->
            runCatching { SoundType.valueOf(name) }.getOrNull()?.let { sound ->
                // Post so the layout is fully ready before we start the service/animation
                binding.root.post { playSound(sound) }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (SoundService.isRunning) {
            val intent = Intent(this, SoundService::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        bannerAdView?.resume()
        billing.queryPurchases()
        binding.bottomNavigation.selectedItemId = R.id.tab_sounds

        // Re-bind to the service if it's running but we lost the connection
        // (happens after returning from AiChatActivity, AlarmActivity, etc.)
        if (SoundService.isRunning && !bound) {
            val si = Intent(this, SoundService::class.java)
            bindService(si, connection, Context.BIND_AUTO_CREATE)
        }

        refreshStats()
        syncUI()
        scrubHandler.post(scrubPoll)
    }

    override fun onPause() {
        scrubHandler.removeCallbacks(scrubPoll)
        bannerAdView?.pause()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
            service = null   // clear stale binder ref — prevents false "bound" checks on resume
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("LAUNCH_SOUND")?.let { name ->
            runCatching { SoundType.valueOf(name) }.getOrNull()?.let { sound ->
                // Service was already started by AiChatActivity — just bind and sync UI.
                // If for any reason the service isn't running yet, playSound() handles that.
                if (SoundService.isRunning && !bound) {
                    val si = Intent(this, SoundService::class.java)
                    bindService(si, connection, Context.BIND_AUTO_CREATE)
                    // Queue a UI sync once binding confirms (onServiceConnected does this).
                } else {
                    playSound(sound)
                }
                // Tint the background to this track's theme color immediately
                binding.nightSky.setThemeColor(sound.themeColor)
                // Delayed sync so adapter reflects the new track even before bind callback
                Handler(Looper.getMainLooper()).postDelayed({
                    syncUI()
                }, 350)
            }
        }
        if (intent.getBooleanExtra(EXTRA_SHOW_UNLOCK, false)) {
            showUpgradeDialog(null)
        }
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        billing.destroy()
        countdown?.cancel()
        playButtonAnimator?.cancel()
        scrubHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Menu
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_journeys  -> { showJourneysDialog(); true }
        R.id.menu_progress  -> { showProgressDialog(); true }
        R.id.menu_share     -> { shareApp(); true }
        R.id.menu_equalizer -> { showEqualizerDialog(); true }
        R.id.menu_about    -> { startActivity(Intent(this, AboutActivity::class.java));    true }
        R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        val gridItems = buildSoundGridItems(prefs.getFavorites(), currentMood)
        adapter = SoundAdapter(
            items            = gridItems,
            isPremium        = prefs.isPremium(),
            playingSound     = null,
            onSoundClick     = ::handleSoundTap,
            onSoundLongPress = ::showSoundDescription,
            onPremiumClick   = { showUpgradeDialog(it) },
            isFavorite       = { prefs.isFavorite(it) }
        )
        val spans = resources.getInteger(R.integer.grid_span_count)
        val gridLayoutManager = GridLayoutManager(this, spans).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int =
                    adapter.spanSize(position, spans)
            }
        }
        binding.rvSounds.apply {
            layoutManager            = gridLayoutManager
            adapter                  = this@MainActivity.adapter
            isNestedScrollingEnabled = false
        }
        binding.sliderVolume.value = prefs.getVolume()
        binding.sliderBgVolume.value = prefs.getBgVolume()
        binding.tvBgVolPct.text = "${(prefs.getBgVolume() * 100).toInt()}%"
    }

    /** Tasteful description popup on long-press — also offers favorite toggle. */
    private fun showSoundDescription(type: SoundType) {
        haptic.click()
        sfx.tap()
        val isFav = prefs.isFavorite(type)
        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("${type.emoji}  ${type.displayName}")
            .setMessage(type.description + "\n\nFor: ${type.mood.emoji} ${type.mood.label}")
            .setPositiveButton("Got it", null)
            .setNeutralButton(if (isFav) "★ Remove favorite" else "☆ Add to favorites") { _, _ ->
                val nowFav = prefs.toggleFavorite(type)
                haptic.tick(); sfx.tap()
                Toast.makeText(
                    this,
                    if (nowFav) "⭐ Added to favorites" else "Removed from favorites",
                    Toast.LENGTH_SHORT
                ).show()
                rebuildGrid()
            }
            .show()
    }

    private fun handleSoundTap(type: SoundType) {
        haptic.tick()
        val canPlay = !type.isPremium ||
                      prefs.isPremium() ||
                      SessionPremiumManager.isUnlocked(type)
        if (canPlay) playSound(type)
        else { sfx.tap(); showUpgradeDialog(type) }
    }

    private fun setupControls() {
        binding.sliderVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefs.setVolume(value)
                service?.setVolume(value)
            }
        }
        binding.btnPlayStop.setOnClickListener {
            haptic.click()
            if (service?.isPlaying() == true) stopSound() else playSound(prefs.getLastSound())
        }
        binding.btnTimer.setOnClickListener {
            haptic.tick(); sfx.tap(); showTimerDialog()
        }
        binding.btnPremium.setOnClickListener {
            haptic.tick(); sfx.tap(); showUpgradeDialog(null)
        }
        binding.cardAiChat.setOnClickListener {
            haptic.tick(); sfx.tap()
            if (service?.isPlaying() == true) stopSound()
            val intent = Intent(this, AiChatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        binding.cardAlarm.setOnClickListener {
            haptic.tick(); sfx.tap()
            if (service?.isPlaying() == true) stopSound()
            val intent = Intent(this, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        binding.btnPrevious.setOnClickListener {
            haptic.click()
            sfx.swoosh()
            cycleSound(-1)
        }
        binding.btnNext.setOnClickListener {
            haptic.click()
            sfx.swoosh()
            cycleSound(1)
        }
        binding.btnRandom.setOnClickListener {
            haptic.click()
            sfx.swoosh()
            playRandomSound()
        }
        binding.btnRandom.setOnLongClickListener {
            haptic.click()
            sfx.tap()
            toggleShuffle()
            true
        }
        binding.btnRepeat.setOnClickListener {
            haptic.click()
            sfx.tap()
            cycleRepeatMode()
        }
        binding.sliderBgVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefs.setBgVolume(value)
                service?.setBgVolume(value)
                binding.tvBgVolPct.text = "${(value * 100).toInt()}%"
            }
        }
        setupScrubBar()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scrub bar
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupScrubBar() {
        binding.sliderScrub.addOnSliderTouchListener(
            object : com.google.android.material.slider.Slider.OnSliderTouchListener {
                override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                    userScrubbing = true
                }
                override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                    userScrubbing = false
                    val dur = service?.getDurationMs() ?: 0
                    if (dur > 0) {
                        service?.seekTo((slider.value * dur).toInt())
                        haptic.tick()
                    }
                }
            }
        )
        // Live elapsed-label preview while dragging, before the seek commits.
        binding.sliderScrub.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val dur = service?.getDurationMs() ?: 0
                if (dur > 0) binding.tvScrubElapsed.text = formatMs((value * dur).toInt())
            }
        }
    }

    private fun updateScrubBar() {
        if (userScrubbing) return
        val svc = service ?: return
        if (!svc.isPlaying()) return
        val dur = svc.getDurationMs()
        if (dur <= 0) return
        val pos = svc.getPositionMs().coerceIn(0, dur)
        binding.sliderScrub.value = (pos.toFloat() / dur).coerceIn(0f, 1f)
        binding.tvScrubElapsed.text = formatMs(pos)
        binding.tvScrubTotal.text = formatMs(dur)
    }

    private fun formatMs(ms: Int): String {
        val totalSec = ms / 1000
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    private fun setupBgMusicSpinner() {
        val bgMusicOptions = BgMusicType.values().toList()
        val labels = bgMusicOptions.map { it.displayName }
        binding.spinnerBgMusic.adapter = android.widget.ArrayAdapter(
            this,
            R.layout.item_spinner_dark,
            labels
        ).apply {
            setDropDownViewResource(R.layout.item_spinner_dark)
        }

        val currentBgTrack = prefs.getBgMusicTrack()
        val currentType = BgMusicType.fromName(currentBgTrack)
        val selectedIdx = bgMusicOptions.indexOf(currentType).coerceAtLeast(0)
        binding.spinnerBgMusic.setSelection(selectedIdx)

        binding.spinnerBgMusic.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = bgMusicOptions[position]
                if (prefs.getBgMusicTrack() != selectedType.name) {
                    prefs.setBgMusicTrack(selectedType.name)
                    service?.changeBgMusic(selectedType)
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }

    private fun setupButtonPulsations() {
        // Indefinite gentle pulsation for primary cards to make them feel alive and glow
        startGentleBreathing(binding.ivTopLogo, 1.04f, 3200)
        startGentleBreathing(binding.cardBreathe, 1.025f, 2400)
        startGentleBreathing(binding.cardAiChat, 1.025f, 2600)
        startGentleBreathing(binding.cardAlarm, 1.02f, 2800)
        startGentleBreathing(binding.cardQuickCalm, 1.02f, 3000)
    }

    private fun startGentleBreathing(view: View, maxScale: Float, durationMs: Long) {
        ValueAnimator.ofFloat(1.0f, maxScale).apply {
            duration = durationMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                val scale = anim.animatedValue as Float
                view.scaleX = scale
                view.scaleY = scale
            }
            start()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Home extras — stats banner, daily quote, breathing tools, mood filters
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupHomeExtras() {
        // Daily quote — tap to reveal today's micro-technique
        val (quote, author) = Quotes.today()
        binding.tvQuote.text = "“$quote”"
        // Author on one line, discoverability hint on the next (avoids truncation).
        binding.tvQuoteAuthor.text = "— $author\n✨ tap for today's technique →"
        binding.cardQuote.setOnClickListener {
            haptic.tick(); sfx.tap(); showTechniqueDialog()
        }

        // Stats banner → opens full progress dialog
        binding.cardStats.setOnClickListener {
            haptic.tick(); sfx.tap(); showProgressDialog()
        }

        // Breathing pacer
        binding.cardBreathe.setOnClickListener {
            haptic.click(); sfx.chime()
            binding.nightSky.react(NightSkyView.ReactionKind.TIMER)
            if (service?.isPlaying() == true) stopSound()
            startActivity(Intent(this, BreathingActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        // Quick Calm — a one-tap 60-second reset (also box breathing, credits a session)
        binding.cardQuickCalm.setOnClickListener {
            haptic.click(); sfx.chime()
            binding.nightSky.react(NightSkyView.ReactionKind.PLAY)
            Toast.makeText(this, "⚡ 60-second reset — follow the orb", Toast.LENGTH_SHORT).show()
            if (service?.isPlaying() == true) stopSound()
            startActivity(Intent(this, BreathingActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }

        // Mood filter chips
        binding.chipGroupMoods.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: R.id.chipAll
            currentMood = when (id) {
                R.id.chipSleep      -> Mood.SLEEP
                R.id.chipStress     -> Mood.STRESS
                R.id.chipFocus      -> Mood.FOCUS
                R.id.chipGrounding  -> Mood.GROUNDING
                R.id.chipCompassion -> Mood.COMPASSION
                R.id.chipEnergy     -> Mood.ENERGY
                else                -> null
            }
            haptic.tick(); sfx.tap()
            rebuildGrid()
        }

        refreshStats()
    }

    /** Rebuild the library grid for the current favorites + mood filter. */
    private fun rebuildGrid() {
        adapter.setItems(buildSoundGridItems(prefs.getFavorites(), currentMood))
    }

    /** Refresh the streak / sessions / minutes banner (and any home widget). */
    private fun refreshStats() {
        binding.tvStreak.text        = stats.currentStreak().toString()
        binding.tvTotalSessions.text = stats.totalSessions().toString()
        binding.tvTotalMinutes.text  = stats.totalMinutes().toString()
        StreakWidgetProvider.refreshAll(this)
    }

    private fun showProgressDialog() {
        val week = stats.lastSevenDays()
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        // Build a simple textual 7-day bar using block characters
        val maxVal = (week.maxOrNull() ?: 0).coerceAtLeast(1)
        val bars = week.mapIndexed { i, v ->
            val filled = if (v == 0) "·" else "▁▃▅▇".getOrElse(((v.toFloat() / maxVal) * 3).toInt().coerceIn(0, 3)) { '▇' }.toString()
            val today = i == 6
            (if (today) "[$filled]" else " $filled ")
        }.joinToString("")

        val streak = stats.currentStreak()
        val nowCount = "%,d".format(meditatingNowCount())
        val msg = buildString {
            append("🔥 Current streak: $streak days\n")
            append("🏆 Longest streak: ${stats.longestStreak()} days\n")
            append("🧘 Total sessions: ${stats.totalSessions()}\n")
            append("⏳ Total minutes: ${stats.totalMinutes()}\n\n")
            append("Last 7 days:\n$bars\n")
            append(dayLabels.joinToString("  "))
            append("\n\n🌍 $nowCount people meditating right now")
            append("\n\nMore meditations & languages are on the way — keep your streak alive! 🌙")
        }

        // Offer to share when the user hits a milestone streak.
        val milestones = setOf(3, 7, 14, 30, 60, 100)
        val builder = AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("📈 Your Progress")
            .setMessage(msg)
            .setPositiveButton("Keep going", null)
        if (streak in milestones) {
            builder.setNeutralButton("🎉 Share streak") { _, _ -> shareMilestone(streak) }
        }
        builder.show()
    }

    private fun shareMilestone(streak: Int) {
        haptic.tick(); sfx.tap()
        val link = "https://play.google.com/store/apps/details?id=$packageName"
        val text = "🔥 $streak-day meditation streak on Guided Meditation Portal! " +
                   "Breathing easier and sleeping better, one day at a time. 🌙 $link"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { startActivity(Intent.createChooser(intent, "Share your streak")) }
    }

    /** Today's bite-sized technique, tuned to the user's onboarding goal. */
    private fun showTechniqueDialog() {
        val tech = MicroTechniques.forGoal(prefs.getGoal())
        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("${tech.emoji}  ${tech.title}")
            .setMessage(tech.body + "\n\nFor: ${tech.mood.emoji} ${tech.mood.label}")
            .setPositiveButton("Try it now") { _, _ ->
                startActivity(Intent(this, BreathingActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Journeys — multi-day text programs ("courses" without audio)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showJourneysDialog() {
        haptic.tick(); sfx.tap()
        val labels = Programs.all.map { p ->
            val done = prefs.getProgramProgress(p.id)
            val status = when {
                done == 0            -> "Not started"
                done >= p.days.size  -> "✓ Complete"
                else                 -> "Day $done of ${p.days.size}"
            }
            "${p.emoji}  ${p.title}\n     $status"
        }.toTypedArray()

        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("🗺️ Journeys")
            .setItems(labels) { _, i -> showProgramDayDialog(Programs.all[i]) }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showProgramDayDialog(program: Program) {
        val done = prefs.getProgramProgress(program.id)
        if (done >= program.days.size) {
            AlertDialog.Builder(this, R.style.AlertDialogDark)
                .setTitle("${program.emoji}  ${program.title}")
                .setMessage("You've completed this journey — beautifully done. 🤍\n\nRevisit any day, or start again whenever you like.")
                .setPositiveButton("Start over") { _, _ -> prefs.resetProgram(program.id); showProgramDayDialog(program) }
                .setNegativeButton("Close", null)
                .show()
            return
        }

        val dayIndex = done                       // next day to do (0-based)
        val day = program.days[dayIndex]
        val msg = "Day ${dayIndex + 1} of ${program.days.size} · ${day.title}\n\n" +
                  "${day.technique}\n\n" +
                  "Suggested meditation: ${day.track.emoji} ${day.track.displayName}"

        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("${program.emoji}  ${program.title}")
            .setMessage(msg)
            .setPositiveButton("▶ Play & complete day") { _, _ ->
                prefs.advanceProgram(program.id, program.days.size)
                handleSoundTap(day.track)
                val nowDone = prefs.getProgramProgress(program.id)
                if (nowDone >= program.days.size)
                    Toast.makeText(this, "🎉 Journey complete — ${program.title}!", Toast.LENGTH_LONG).show()
                else
                    Toast.makeText(this, "Day ${dayIndex + 1} done — see you tomorrow 🌙", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Community — lightweight social proof (no backend)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A gently-varying "people meditating now" count — stable within a 10-minute
     * window so it feels live without a server. Higher in the evening.
     */
    private fun meditatingNowCount(): Int {
        val cal = java.util.Calendar.getInstance()
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        val slot = cal.get(java.util.Calendar.DAY_OF_YEAR) * 144 +
                   cal.get(java.util.Calendar.HOUR_OF_DAY) * 6 +
                   cal.get(java.util.Calendar.MINUTE) / 10
        val base = when (hour) { in 21..23, in 0..1 -> 2600; in 6..9 -> 1900; in 12..14 -> 1500; else -> 1100 }
        val jitter = (kotlin.math.abs(slot * 2654435761L.toInt()) % 700)
        return base + jitter
    }

    private fun shareApp() {
        haptic.tick(); sfx.tap()
        val link = "https://play.google.com/store/apps/details?id=$packageName"
        val text = "I've been using Guided Meditation Portal to breathe easier and sleep better 🌙 " +
                   "23 narrated meditations, a breathing coach, and a companion called Spirit — " +
                   "one-time \$2, no subscriptions. $link"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Guided Meditation Portal")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        runCatching { startActivity(Intent.createChooser(intent, "Share with a friend")) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Jukebox controls — repeat mode & shuffle
    // ─────────────────────────────────────────────────────────────────────────

    private fun cycleRepeatMode() {
        binding.nightSky.react(NightSkyView.ReactionKind.SELECT)
        val mode = service?.cycleRepeatMode() ?: return
        prefs.setRepeatMode(mode)
        syncJukeboxControls()
        val label = when (mode) {
            SoundService.RepeatMode.OFF -> "Repeat off"
            SoundService.RepeatMode.ALL -> "🔁 Repeat all — plays through the library"
            SoundService.RepeatMode.ONE -> "🔂 Repeat one — loops this track"
        }
        Toast.makeText(this, label, Toast.LENGTH_SHORT).show()
    }

    private fun toggleShuffle() {
        val enabled = !(service?.shuffleEnabled ?: prefs.isShuffleEnabled())
        service?.setShuffleEnabled(enabled)
        prefs.setShuffleEnabled(enabled)
        syncJukeboxControls()
        Toast.makeText(this, if (enabled) "🔀 Shuffle on — jukebox plays in random order" else "Shuffle off", Toast.LENGTH_SHORT).show()
    }

    private fun syncJukeboxControls() {
        val mode = service?.repeatMode ?: prefs.getRepeatMode()
        val shuffle = service?.shuffleEnabled ?: prefs.isShuffleEnabled()

        val (repeatIcon, repeatColor) = when (mode) {
            SoundService.RepeatMode.OFF -> "🔁" to R.color.text_secondary
            SoundService.RepeatMode.ALL -> "🔁" to R.color.accent_teal
            SoundService.RepeatMode.ONE -> "🔂" to R.color.accent_teal
        }
        binding.btnRepeat.text = repeatIcon
        binding.btnRepeat.setTextColor(getColor(repeatColor))

        binding.btnRandom.setTextColor(
            getColor(if (shuffle) R.color.accent_teal else R.color.premium_gold)
        )
        binding.btnRandom.strokeColor = android.content.res.ColorStateList.valueOf(
            getColor(if (shuffle) R.color.accent_teal else R.color.premium_gold)
        )
    }

    private fun setupAds() {
        if (adsInitialized) return
        adsInitialized = true
        MobileAds.initialize(this) { /* SDK ready */ }
        loadAdaptiveBanner()
        rewarded.preload()
        interstitial.preload()
    }

    /**
     * Build an adaptive anchored banner. Gets the full screen width and asks
     * AdMob for the best-fitting size — typically wider + taller than the
     * fixed 320×50 BANNER, with ~40% better CPM and fill rate.
     */
    private fun loadAdaptiveBanner() {
        val display: DisplayMetrics = resources.displayMetrics
        val widthPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.currentWindowMetrics.bounds.width()
        } else display.widthPixels

        val widthDp = (widthPx / display.density).toInt()
        val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, widthDp)

        val adView = AdView(this).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            setAdSize(size)
        }
        binding.adContainer.removeAllViews()
        binding.adContainer.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
        bannerAdView = adView
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sound playback
    // ─────────────────────────────────────────────────────────────────────────

    private fun playSound(type: SoundType) {
        val isSwitch = service?.isPlaying() == true
        haptic.click()
        if (isSwitch) sfx.swoosh() else sfx.chime()
        binding.nightSky.react(NightSkyView.ReactionKind.PLAY)

        // ── Chameleon: shift the entire background palette to this track's hue ──
        binding.nightSky.setThemeColor(type.themeColor)

        // ── Auto-BGM: pick a random background track on fresh play if none selected ──
        if (!isSwitch) {
            val currentBg = BgMusicType.fromName(prefs.getBgMusicTrack())
            if (currentBg == BgMusicType.NONE) {
                val randomBg = BgMusicType.values()
                    .filter { it != BgMusicType.NONE }
                    .random()
                prefs.setBgMusicTrack(randomBg.name)
                val idx = BgMusicType.values().indexOf(randomBg).coerceAtLeast(0)
                binding.spinnerBgMusic.setSelection(idx)
            }
        }

        prefs.setLastSound(type)
        prefs.incrementPlayCount(type)

        // Progress tracking — credit a session & start the minute clock (only when
        // beginning fresh playback, not when switching tracks mid-session).
        if (!isSwitch) {
            stats.recordSessionStart()
            sessionStartMs = System.currentTimeMillis()
            refreshStats()
        }

        AnalyticsHelper.logEvent(
            AnalyticsHelper.Events.SOUND_PLAYED,
            mapOf("sound" to type.name)
        )

        if (bound && service != null) {
            // Already running — change sound via binder only (avoid double startSound)
            service!!.changeSound(type)
        } else {
            val intent = Intent(this, SoundService::class.java).apply {
                putExtra("SOUND_TYPE", type.name)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
            else startService(intent)
            if (!bound) bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
        syncUI()
    }

    private fun stopSound() {
        sfx.tap()
        binding.nightSky.react(NightSkyView.ReactionKind.PAUSE)

        // Bank the minutes meditated this session
        if (sessionStartMs > 0L) {
            val mins = ((System.currentTimeMillis() - sessionStartMs) / 60_000L).toInt()
            stats.addMinutes(mins)
            sessionStartMs = 0L
            refreshStats()
        }

        countdown?.cancel(); countdown = null
        binding.timerRing.stop()
        stopService(Intent(this, SoundService::class.java))
        if (bound) { unbindService(connection); bound = false }
        service = null
        prefs.incrementStopCount()

        // Free users see an interstitial every 3rd stop (with cadence guards)
        if (!prefs.isPremium()) {
            interstitial.maybeShowOnStop(this, prefs.getStopCount())
        }
        syncUI()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timer
    // ─────────────────────────────────────────────────────────────────────────

    private fun showTimerDialog() {
        val labels    = arrayOf("No Timer", "15 min", "30 min", "1 hour", "2 hours", "4 hours", "8 hours")
        val durations = longArrayOf(0, 15*60_000, 30*60_000, 60*60_000, 120*60_000, 240*60_000, 480*60_000)

        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("⏱ Session Timer")
            .setItems(labels) { _, i ->
                if (durations[i] == 0L) cancelTimer()
                else startTimer(durations[i])
                AnalyticsHelper.logEvent(
                    AnalyticsHelper.Events.TIMER_SET,
                    mapOf("ms" to durations[i])
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startTimer(ms: Long) {
        binding.nightSky.react(NightSkyView.ReactionKind.TIMER)
        cancelTimer()
        prefs.setTimerMs(ms)
        binding.timerRing.start(ms)     // visual ring sweep
        countdown = object : CountDownTimer(ms, 1000) {
            override fun onTick(left: Long) {
                val m = left / 60_000
                val s = (left % 60_000) / 1000
                binding.tvTimer.text = "⏱ %02d:%02d".format(m, s)
            }
            override fun onFinish() {
                stopSound()
                binding.tvTimer.text = ""
                binding.btnTimer.setTextColor(getColor(R.color.accent_teal))
                Toast.makeText(this@MainActivity, "Sweet dreams 🌙", Toast.LENGTH_SHORT).show()
            }
        }.start()
        // Icon stays "⏱"; active timer is shown via the live countdown (tvTimer)
        // and a warm accent tint on the icon.
        binding.btnTimer.setTextColor(getColor(R.color.accent_rose))
    }

    private fun cancelTimer() {
        countdown?.cancel(); countdown = null
        binding.timerRing.stop()
        binding.tvTimer.text = ""
        binding.btnTimer.setTextColor(getColor(R.color.accent_teal))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upgrade flow
    // ─────────────────────────────────────────────────────────────────────────

    private fun showUpgradeDialog(forSound: SoundType?) {
        binding.nightSky.react(NightSkyView.ReactionKind.UNLOCK)
        AnalyticsHelper.logEvent(
            AnalyticsHelper.Events.PREMIUM_TAPPED,
            mapOf("sound" to (forSound?.name ?: "none"))
        )

        val labels = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        if (forSound != null && rewarded.isAdAvailable()) {
            labels  += "▶ Watch ad — unlock for tonight"
            actions += { watchAdForSound(forSound) }
        }
        labels  += "✨ Unlock forever — \$2.00  (one-time, lifetime)"
        actions += {
            AnalyticsHelper.logEvent(AnalyticsHelper.Events.PURCHASE_PRO)
            billing.purchaseUnlock()
        }

        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("Unlock Portal")
            .setMessage(
                "Full guided-meditation library · Spirit companion · Meditation alarm · No ads\n\n" +
                "🔜 More meditations & new languages coming in future updates!"
            )
            .setItems(labels.toTypedArray()) { _, i -> actions[i]() }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun watchAdForSound(type: SoundType) {
        AnalyticsHelper.logEvent(AnalyticsHelper.Events.REWARDED_REQUESTED, mapOf("sound" to type.name))
        rewarded.showAd(
            activity = this,
            onReward = {
                SessionPremiumManager.unlock(type)
                AnalyticsHelper.logEvent(AnalyticsHelper.Events.REWARDED_GRANTED, mapOf("sound" to type.name))
                haptic.thud()
                sfx.reward()
                Toast.makeText(this, "${type.emoji} ${type.displayName} unlocked for tonight!", Toast.LENGTH_SHORT).show()
                playSound(type)
            },
            onUnavailable = {
                Toast.makeText(this, "Ad not ready, try again in a moment", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI sync
    // ─────────────────────────────────────────────────────────────────────────

    private fun syncUI() {
        val playing = service?.isPlaying() == true
        val current = service?.currentSound
        val premium = prefs.isPremium()

        binding.tvNowPlaying.text = if (playing)
            "${current?.emoji ?: ""} ${current?.displayName ?: "Playing…"}"
        else
            "Tap a sound to begin"

        binding.btnPlayStop.text = if (playing) "⏹ Stop" else "▶ Play Last"

        if (playing) {
            if (playButtonAnimator == null) {
                playButtonAnimator = ValueAnimator.ofFloat(1.0f, 1.06f).apply {
                    duration = 1200
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                    addUpdateListener { anim ->
                        val scale = anim.animatedValue as Float
                        binding.btnPlayStop.scaleX = scale
                        binding.btnPlayStop.scaleY = scale
                    }
                    start()
                }
            }
        } else {
            playButtonAnimator?.cancel()
            playButtonAnimator = null
            binding.btnPlayStop.scaleX = 1.0f
            binding.btnPlayStop.scaleY = 1.0f
        }

        // ── Mini-player: collapse secondary controls when nothing is playing ──
        // Idle = slim bar (now-playing line + Play + Timer). Playing = full controls.
        val secondary = if (playing) View.VISIBLE else View.GONE
        binding.rowVolume.visibility   = secondary
        binding.rowScrub.visibility    = secondary
        binding.btnPrevious.visibility = secondary
        binding.btnNext.visibility     = secondary
        binding.btnRepeat.visibility   = secondary
        binding.btnRandom.visibility   = secondary
        if (playing) updateScrubBar() else binding.sliderScrub.value = 0f

        adapter.update(current, premium)

        binding.cardPremium.visibility = if (premium) View.GONE else View.VISIBLE
        binding.adContainer.visibility = if (premium) View.GONE else View.VISIBLE

        syncJukeboxControls()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permissions
    // ─────────────────────────────────────────────────────────────────────────

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }
    }

    private fun maybeAskForRating() {
        // After 3 sessions, request the Play in-app review modal (once).
        if (prefs.getSessionCount() == 3 && !prefs.isRatePromptShown()) {
            prefs.setRatePromptShown(true)
            InAppReviewHelper.requestReview(this)
        }
    }

    private fun cycleSound(offset: Int) {
        val tracks = SoundType.values().toList()
        val current = service?.currentSound ?: prefs.getLastSound()
        var idx = tracks.indexOf(current) + offset
        if (idx < 0) idx = tracks.size - 1
        if (idx >= tracks.size) idx = 0

        playSound(tracks[idx])
        Toast.makeText(this, "Playing: ${tracks[idx].displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun playRandomSound() {
        val tracks = SoundType.values().toList()
        val randomTrack = tracks.random()
        playSound(randomTrack)
        Toast.makeText(this, "🔀 Shuffle: ${randomTrack.displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun showEqualizerDialog() {
        haptic.click()
        sfx.tap()
        
        val dialogBinding = com.auroramind.meditation.databinding.DialogEqualizerBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        val playing = service?.isPlaying() == true
        if (playing) {
            dialogBinding.visualizer.start()
        }

        val bassVal = prefs.getEqBass()
        val midVal = prefs.getEqMid()
        val trebleVal = prefs.getEqTreble()

        dialogBinding.sliderBass.value = bassVal
        dialogBinding.tvBassPct.text = "${(bassVal * 100).toInt()}%"
        dialogBinding.sliderBass.addOnChangeListener { _, value, _ ->
            prefs.setEqBass(value)
            dialogBinding.tvBassPct.text = "${(value * 100).toInt()}%"
            service?.setEq(value, prefs.getEqMid(), prefs.getEqTreble())
        }

        dialogBinding.sliderMid.value = midVal
        dialogBinding.tvMidPct.text = "${(midVal * 100).toInt()}%"
        dialogBinding.sliderMid.addOnChangeListener { _, value, _ ->
            prefs.setEqMid(value)
            dialogBinding.tvMidPct.text = "${(value * 100).toInt()}%"
            service?.setEq(prefs.getEqBass(), value, prefs.getEqTreble())
        }

        dialogBinding.sliderTreble.value = trebleVal
        dialogBinding.tvTreblePct.text = "${(trebleVal * 100).toInt()}%"
        dialogBinding.sliderTreble.addOnChangeListener { _, value, _ ->
            prefs.setEqTreble(value)
            dialogBinding.tvTreblePct.text = "${(value * 100).toInt()}%"
            service?.setEq(prefs.getEqBass(), prefs.getEqMid(), value)
        }

        dialogBinding.btnResetEq.setOnClickListener {
            haptic.tick()
            sfx.tap()
            
            prefs.setEqBass(1.0f)
            prefs.setEqMid(1.0f)
            prefs.setEqTreble(1.0f)

            dialogBinding.sliderBass.value = 1.0f
            dialogBinding.sliderMid.value = 1.0f
            dialogBinding.sliderTreble.value = 1.0f

            dialogBinding.tvBassPct.text = "100%"
            dialogBinding.tvMidPct.text = "100%"
            dialogBinding.tvTreblePct.text = "100%"

            service?.setEq(1.0f, 1.0f, 1.0f)
        }

        dialogBinding.btnDoneEq.setOnClickListener {
            haptic.click()
            sfx.tap()
            dialogBinding.visualizer.stop()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.tab_sounds
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.tab_sounds) return@setOnItemSelectedListener true
            haptic.tick()
            sfx.tap()
            if (item.itemId == R.id.tab_unlock) {
                showUpgradeDialog(null)
                binding.bottomNavigation.post { binding.bottomNavigation.selectedItemId = R.id.tab_sounds }
                return@setOnItemSelectedListener false
            }
            val targetClass = when (item.itemId) {
                R.id.tab_alarm    -> AlarmActivity::class.java
                R.id.tab_aria     -> AiChatActivity::class.java
                R.id.tab_settings -> SettingsActivity::class.java
                else              -> null
            }
            if (targetClass != null) {
                if (service?.isPlaying() == true) stopSound()
                val intent = Intent(this, targetClass).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                true
            } else false
        }
    }
}
