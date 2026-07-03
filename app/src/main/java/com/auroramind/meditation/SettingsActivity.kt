package com.auroramind.meditation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.auroramind.meditation.databinding.ActivitySettingsBinding

/**
 * User preferences:
 *   - Haptics on/off
 *   - UI sounds on/off
 *   - Manage subscription (deep-links to Play subscriptions)
 *   - Reset ad-consent (re-shows UMP form next launch)
 *   - Replay onboarding
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PrefsManager
    private lateinit var sfx: SoundEffects
    private lateinit var haptic: HapticHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)
        sfx = SoundEffects(this)
        haptic = HapticHelper(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.root.getChildAt(0).updatePadding(top = bars.top)
            binding.bottomNavigation.updatePadding(bottom = bars.bottom)
            insets
        }

        supportActionBar?.title = getString(R.string.settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupBottomNavigation()

        binding.switchHaptics.isChecked = prefs.isHapticsEnabled()
        binding.switchHaptics.setOnCheckedChangeListener { _, on ->
            prefs.setHapticsEnabled(on)
            if (on) haptic.tick()
        }

        binding.switchUiSounds.isChecked = prefs.isUiSoundsEnabled()
        binding.switchUiSounds.setOnCheckedChangeListener { _, on ->
            prefs.setUiSoundsEnabled(on)
            if (on) sfx.tap()
        }

        // ── Daily reminder ──────────────────────────────────────────────
        updateReminderTimeLabel()
        binding.switchReminder.isChecked = prefs.isReminderEnabled()
        binding.switchReminder.setOnCheckedChangeListener { _, on ->
            prefs.setReminderEnabled(on)
            if (on) {
                haptic.tick()
                ReminderScheduler.reschedule(this)
                Toast.makeText(this, "Daily reminder on ✓", Toast.LENGTH_SHORT).show()
            } else {
                ReminderScheduler.cancel(this)
            }
        }
        binding.btnReminderTime.setOnClickListener {
            haptic.tick(); sfx.tap()
            android.app.TimePickerDialog(
                this,
                { _, hour, minute ->
                    prefs.setReminderTime(hour, minute)
                    updateReminderTimeLabel()
                    if (prefs.isReminderEnabled()) ReminderScheduler.reschedule(this)
                },
                prefs.getReminderHour(), prefs.getReminderMinute(), false
            ).show()
        }

        // ── Share ───────────────────────────────────────────────────────
        binding.btnShare.setOnClickListener {
            haptic.tick(); sfx.tap()
            val link = "https://play.google.com/store/apps/details?id=$packageName"
            val text = "I've been using Guided Meditation Portal to breathe easier and sleep better 🌙 " +
                       "23 narrated meditations, a breathing coach, and a companion called Spirit — " +
                       "one-time \$2, no subscriptions. $link"
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Guided Meditation Portal")
                putExtra(Intent.EXTRA_TEXT, text)
            }
            runCatching { startActivity(Intent.createChooser(share, "Share with a friend")) }
        }

        binding.btnManageSubscription.setOnClickListener {
            haptic.tick()
            sfx.tap()
            // Deep-link to Play subscription management for our package
            val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                .onFailure { Toast.makeText(this, "Play Store not available", Toast.LENGTH_SHORT).show() }
        }

        binding.btnResetConsent.setOnClickListener {
            haptic.tick()
            sfx.tap()
            ConsentManager(this).reset()
            Toast.makeText(this, R.string.settings_consent_reset_done, Toast.LENGTH_SHORT).show()
        }

        binding.btnReplayOnboarding.setOnClickListener {
            haptic.tick()
            sfx.tap()
            prefs.setOnboardingShown(false)
            Toast.makeText(this, R.string.settings_onboarding_reset_done, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateReminderTimeLabel() {
        val h = prefs.getReminderHour()
        val m = prefs.getReminderMinute()
        val ampm = if (h < 12) "AM" else "PM"
        val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
        binding.btnReminderTime.text = "Reminder time: %d:%02d %s".format(h12, m, ampm)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.tab_settings
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.tab_settings
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.tab_settings) return@setOnItemSelectedListener true
            haptic.tick()
            sfx.tap()
            val targetClass = when (item.itemId) {
                R.id.tab_sounds   -> MainActivity::class.java
                R.id.tab_alarm    -> AlarmActivity::class.java
                R.id.tab_aria     -> AiChatActivity::class.java
                R.id.tab_unlock   -> MainActivity::class.java
                else              -> null
            }
            if (targetClass != null) {
                val intent = Intent(this, targetClass).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    if (item.itemId == R.id.tab_unlock) {
                        putExtra(MainActivity.EXTRA_SHOW_UNLOCK, true)
                    }
                }
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                true
            } else false
        }
    }
}
