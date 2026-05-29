package com.soundpad.sleep

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.soundpad.sleep.databinding.ActivitySettingsBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)

        supportActionBar?.title = getString(R.string.settings_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.switchHaptics.isChecked = prefs.isHapticsEnabled()
        binding.switchHaptics.setOnCheckedChangeListener { _, on ->
            prefs.setHapticsEnabled(on)
            if (on) HapticHelper(this).tick()
        }

        binding.switchUiSounds.isChecked = prefs.isUiSoundsEnabled()
        binding.switchUiSounds.setOnCheckedChangeListener { _, on ->
            prefs.setUiSoundsEnabled(on)
            if (on) SoundEffects(this).tap()
        }

        binding.btnManageSubscription.setOnClickListener {
            // Deep-link to Play subscription management for our package
            val uri = Uri.parse("https://play.google.com/store/account/subscriptions?package=$packageName")
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                .onFailure { Toast.makeText(this, "Play Store not available", Toast.LENGTH_SHORT).show() }
        }

        binding.btnResetConsent.setOnClickListener {
            ConsentManager(this).reset()
            Toast.makeText(this, R.string.settings_consent_reset_done, Toast.LENGTH_SHORT).show()
        }

        binding.btnReplayOnboarding.setOnClickListener {
            prefs.setOnboardingShown(false)
            Toast.makeText(this, R.string.settings_onboarding_reset_done, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
