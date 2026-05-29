package com.soundpad.sleep

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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.soundpad.sleep.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private lateinit var billing: BillingManager
    private lateinit var adapter: SoundAdapter
    private lateinit var rewarded: RewardedAdManager
    private lateinit var interstitial: InterstitialAdManager
    private lateinit var consent: ConsentManager
    private lateinit var sfx: SoundEffects
    private lateinit var haptic: HapticHelper

    private var bannerAdView: AdView? = null
    private var adsInitialized = false

    // ── Service binding ───────────────────────────────────────────────────────
    private var service: SoundService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as SoundService.SoundBinder).service()
            bound   = true
            // Push the slider's current volume into the service so its
            // currentVolume field stays in sync from the very first bind.
            service?.setVolume(prefs.getVolume())
            syncUI()
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound   = false
            syncUI()
        }
    }

    // ── Sleep timer ───────────────────────────────────────────────────────────
    private var countdown: CountDownTimer? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        // Branded splash: AndroidX SplashScreen API, animated moon, swaps to
        // the regular theme once we're ready to draw the UI.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Edge-to-edge: cosmic gradient draws under the status / nav bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Apply window insets so the toolbar sits below the status bar and
        // bottom content doesn't tuck under the navigation bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = bars.top)
            binding.adContainer.updatePadding(bottom = bars.bottom)
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
        billing  = BillingManager(this) { isPremium ->
            prefs.setPremium(isPremium)
            runOnUiThread { syncUI() }
        }

        setupRecyclerView()
        setupControls()
        requestNotificationPermission()
        prefs.incrementSessions()

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
        syncUI()
    }

    override fun onPause() {
        bannerAdView?.pause()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (bound) { unbindService(connection); bound = false }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("LAUNCH_SOUND")?.let { name ->
            runCatching { SoundType.valueOf(name) }.getOrNull()?.let { playSound(it) }
        }
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        billing.destroy()
        countdown?.cancel()
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
        R.id.menu_about    -> { startActivity(Intent(this, AboutActivity::class.java));    true }
        R.id.menu_settings -> { startActivity(Intent(this, SettingsActivity::class.java)); true }
        else -> super.onOptionsItemSelected(item)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        val gridItems = buildSoundGridItems()
        adapter = SoundAdapter(
            items            = gridItems,
            isPremium        = prefs.isPremium(),
            playingSound     = null,
            onSoundClick     = ::handleSoundTap,
            onSoundLongPress = ::showSoundDescription,
            onPremiumClick   = { showUpgradeDialog(it) }
        )
        val spans = 2
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
    }

    /** Tasteful description popup on long-press — uses the existing description field. */
    private fun showSoundDescription(type: SoundType) {
        haptic.click()
        sfx.tap()
        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("${type.emoji}  ${type.displayName}")
            .setMessage(type.description + "\n\nCategory: ${type.category}")
            .setPositiveButton("Got it", null)
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
        binding.btnSubscribe.setOnClickListener {
            haptic.tick(); sfx.tap(); showSubscribeDialog()
        }
        binding.cardAiChat.setOnClickListener {
            haptic.tick()
            startActivity(Intent(this, AiChatActivity::class.java))
        }
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

        prefs.setLastSound(type)
        prefs.incrementPlayCount(type)
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
            .setTitle("⏱ Sleep Timer")
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
                binding.btnTimer.text = "⏱ Timer"
                Toast.makeText(this@MainActivity, "Sweet dreams 🌙", Toast.LENGTH_SHORT).show()
            }
        }.start()
        binding.btnTimer.text = "Cancel Timer"
    }

    private fun cancelTimer() {
        countdown?.cancel(); countdown = null
        binding.timerRing.stop()
        binding.tvTimer.text = ""
        binding.btnTimer.text = "⏱ Timer"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upgrade flow
    // ─────────────────────────────────────────────────────────────────────────

    private fun showUpgradeDialog(forSound: SoundType?) {
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
        labels  += "✨ Unlock forever — \$3.99"
        actions += {
            AnalyticsHelper.logEvent(AnalyticsHelper.Events.PURCHASE_PRO)
            billing.purchasePro()
        }
        labels  += "🌟 Monthly — \$1.99 / month"
        actions += {
            AnalyticsHelper.logEvent(AnalyticsHelper.Events.PURCHASE_MONTHLY)
            billing.purchaseUltimate()
        }
        labels  += "💎 Yearly — \$14.99 / year (save 37%)"
        actions += {
            AnalyticsHelper.logEvent(AnalyticsHelper.Events.PURCHASE_YEARLY)
            billing.purchaseYearly()
        }

        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("Unlock ZenPulse")
            .setItems(labels.toTypedArray()) { _, i -> actions[i]() }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun showSubscribeDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("🌟 ZenPulse Ultimate")
            .setMessage(
                "Unlock everything with a subscription:\n\n" +
                "• All premium sounds (synthesized + ambient + energy)\n" +
                "• Remove ads forever\n" +
                "• Sleep timer up to 8 hours\n" +
                "• Cancel anytime\n\n" +
                "Pick your plan:"
            )
            .setPositiveButton("Yearly · \$14.99") { _, _ ->
                AnalyticsHelper.logEvent(AnalyticsHelper.Events.PURCHASE_YEARLY)
                billing.purchaseYearly()
            }
            .setNeutralButton("Monthly · \$1.99") { _, _ ->
                AnalyticsHelper.logEvent(AnalyticsHelper.Events.PURCHASE_MONTHLY)
                billing.purchaseUltimate()
            }
            .setNegativeButton("No Thanks", null)
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

        adapter.update(current, premium)

        binding.cardPremium.visibility = if (premium) View.GONE else View.VISIBLE
        binding.adContainer.visibility = if (premium) View.GONE else View.VISIBLE
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
}
