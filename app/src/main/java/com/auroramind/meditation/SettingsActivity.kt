package com.auroramind.meditation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        goEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)
        sfx = SoundEffects(this)
        haptic = HapticHelper(this)

        binding.root.getChildAt(0).padSystemBars()

        binding.btnBack.setOnClickListener { finish() }

        supportActionBar?.title = getString(R.string.settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnLanguage.setOnClickListener {
            haptic.tick(); sfx.tap()
            LocaleManager.showPicker(this)
        }

        // The app is free for everyone — there's no subscription to manage.
        binding.btnManageSubscription.visibility = View.GONE

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
            val text = "I've been using Power of Mind to break free from a habit that was holding me back 🔥 " +
                       "daily audio affirmations, a clean-day streak, and a panic button for cravings. $link"
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Power of Mind")
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
}
