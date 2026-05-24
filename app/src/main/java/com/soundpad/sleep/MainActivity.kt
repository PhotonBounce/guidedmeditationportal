package com.soundpad.sleep

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.*
import com.soundpad.sleep.databinding.ActivityMainBinding

import android.view.Menu
import android.view.MenuItem

class MainActivity : AppCompatActivity() {
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PrefsManager
    private lateinit var billing: BillingManager
    private lateinit var adapter: SoundAdapter

    // ── Service binding ───────────────────────────────────────────────────────
    private var service: SoundService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as SoundService.SoundBinder).service()
            bound   = true
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
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs   = PrefsManager(this)
        billing = BillingManager(this) { isPremium ->
            prefs.setPremium(isPremium)
            runOnUiThread { syncUI() }
        }

        setupRecyclerView()
        setupControls()
        setupAds()
        requestNotificationPermission()
        prefs.incrementSessions()
        if (!prefs.isOnboardingShown()) {
            val onboarding = OnboardingDialog(this)
            onboarding.show()
            prefs.setOnboardingShown(true)
        }
        maybeAskForRating()
    }

    override fun onStart() {
        super.onStart()
        // Bind only if service is already alive (don't auto-create)
        if (SoundService.isRunning) {
            val intent = Intent(this, SoundService::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onResume() {
        super.onResume()
        billing.queryPurchases()
        syncUI()
    }

    override fun onStop() {
        super.onStop()
        if (bound) { unbindService(connection); bound = false }
    }

    override fun onDestroy() {
        billing.destroy()
        countdown?.cancel()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        adapter = SoundAdapter(
            sounds         = SoundType.values().toList(),
            isPremium      = prefs.isPremium(),
            playingSound   = null,
            onSoundClick   = ::playSound,
            onPremiumClick = ::showUpgradeDialog
        )
        binding.rvSounds.apply {
            layoutManager          = GridLayoutManager(this@MainActivity, 2)
            adapter                = this@MainActivity.adapter
            isNestedScrollingEnabled = false
        }
        binding.sliderVolume.value = prefs.getVolume()
    }

    private fun setupControls() {
        // Volume
        binding.sliderVolume.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                prefs.setVolume(value)
                service?.setVolume(value)
            }
        }

        // Play / Stop
        binding.btnPlayStop.setOnClickListener {
            if (service?.isPlaying() == true) stopSound() else {
                playSound(prefs.getLastSound())
            }
        }

        // Timer
        binding.btnTimer.setOnClickListener { showTimerDialog() }

        // Premium card CTA
        binding.btnPremium.setOnClickListener { showUpgradeDialog() }

        // Premium card subscription CTA
        binding.btnSubscribe.setOnClickListener { showSubscribeDialog() }
    }

    private fun setupAds() {
        if (prefs.isPremium()) {
            binding.adView.visibility = View.GONE
            return
        }
        MobileAds.initialize(this)
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sound playback
    // ─────────────────────────────────────────────────────────────────────────

    private fun playSound(type: SoundType) {
        prefs.setLastSound(type)
        val intent = Intent(this, SoundService::class.java).apply {
            putExtra("SOUND_TYPE", type.name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        if (!bound) {
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } else {
            service?.changeSound(type)
        }
        syncUI()
    }

    private fun stopSound() {
        countdown?.cancel()
        countdown = null
        stopService(Intent(this, SoundService::class.java))
        if (bound) { unbindService(connection); bound = false }
        service = null
        syncUI()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Timer
    // ─────────────────────────────────────────────────────────────────────────

    private fun showTimerDialog() {
        val labels = arrayOf("No Timer", "15 min", "30 min", "1 hour", "2 hours", "4 hours", "8 hours")
        val durations = longArrayOf(0, 15*60_000, 30*60_000, 60*60_000, 120*60_000, 240*60_000, 480*60_000)

        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("⏱ Sleep Timer")
            .setItems(labels) { _, i ->
                if (durations[i] == 0L) cancelTimer()
                else startTimer(durations[i])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startTimer(ms: Long) {
        cancelTimer()
        prefs.setTimerMs(ms)
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
        binding.tvTimer.text = ""
        binding.btnTimer.text = "⏱ Timer"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upgrade dialogs
    // ─────────────────────────────────────────────────────────────────────────

    private fun showUpgradeDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("✨ SoundPad Pro")
            .setMessage(
                "Unlock everything:\n\n" +
                "• All 14 sounds (noise colors, nature, synthetics)\n" +
                "• Remove ads forever\n" +
                "• Sleep timer up to 8 hours\n\n" +
                "One-time purchase · \$3.99"
            )
            .setPositiveButton("Unlock — \$3.99") { _, _ -> billing.purchasePro() }
            .setNeutralButton("Subscribe \$1.99/mo") { _, _ -> billing.purchaseUltimate() }
            .setNegativeButton("Not Now", null)
            .show()
    }

    private fun showSubscribeDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("🌟 SoundPad Ultimate")
            .setMessage(
                "Everything in Pro, plus:\n\n" +
                "• Sound Mixer (blend 2 sounds)\n" +
                "• Auto-start schedule\n" +
                "• Unlimited saved presets\n\n" +
                "\$1.99 / month · Cancel anytime"
            )
            .setPositiveButton("Subscribe — \$1.99/mo") { _, _ -> billing.purchaseUltimate() }
            .setNegativeButton("No Thanks", null)
            .show()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI sync
    // ─────────────────────────────────────────────────────────────────────────

    private fun syncUI() {
        val playing     = service?.isPlaying() == true
        val current     = service?.currentSound
        val premium     = prefs.isPremium()

        // Now-playing banner
        binding.tvNowPlaying.text = if (playing)
            "${current?.emoji ?: ""} ${current?.displayName ?: "Playing…"}"
        else
            "Tap a sound to begin"

        // Play/Stop button
        binding.btnPlayStop.text = if (playing) "⏹ Stop" else "▶ Play Last"

        // Sound cards
        adapter.update(current, premium)

        // Premium card visibility
        binding.cardPremium.visibility = if (premium) View.GONE else View.VISIBLE

        // Ad visibility
        if (premium) binding.adView.visibility = View.GONE
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Permissions & rating prompt
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
        // Ask for rating after 3 sessions (a simple organic prompt)
        if (prefs.getSessionCount() == 3) {
            AlertDialog.Builder(this, R.style.AlertDialogDark)
                .setTitle("Enjoying SoundPad? 🌙")
                .setMessage("If it's helping you sleep, a quick rating means the world to us!")
                .setPositiveButton("⭐ Rate Us") { _, _ ->
                    runCatching {
                        startActivity(
                            Intent(android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("market://details?id=$packageName"))
                        )
                    }
                }
                .setNegativeButton("Maybe Later", null)
                .show()
        }
    }
}
