package com.auroramind.meditation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.auroramind.meditation.databinding.ActivityAboutBinding

/**
 * About / Legal / Support screen.
 *
 * IMPORTANT: photon-bounce.com/ausis/ hosts the privacy/terms pages for a
 * DIFFERENT app ("Ausis — Sleep, Focus & Workout Sounds"). Submitting this
 * app with that URL would misrepresent our data practices to Play reviewers
 * and users. This app's own pages live in their own path:
 * Upload docs/meditation-portal/privacy.html → https://www.photon-bounce.com/meditation-portal/privacy.html
 *         docs/meditation-portal/terms.html  → https://www.photon-bounce.com/meditation-portal/terms.html
 */
class AboutActivity : AppCompatActivity() {

    companion object {
        const val PRIVACY_URL   = "https://www.photon-bounce.com/meditation-portal/privacy.html"
        const val TERMS_URL     = "https://www.photon-bounce.com/meditation-portal/terms.html"
        const val SUPPORT_EMAIL = "support.meditationportal@gmail.com"
        const val WEBSITE_URL   = "https://www.photon-bounce.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.about_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.btnAboutBack.setOnClickListener { finish() }

        binding.aboutVersion.text = getString(R.string.about_version, BuildConfig.VERSION_NAME)

        binding.privacyPolicy.setOnClickListener { openUrl(PRIVACY_URL) }
        binding.terms.setOnClickListener         { openUrl(TERMS_URL) }
        binding.visitWebsite.setOnClickListener  { openUrl(WEBSITE_URL) }
        binding.contactSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$SUPPORT_EMAIL")
                putExtra(Intent.EXTRA_SUBJECT, "Portal Support — v${BuildConfig.VERSION_NAME}")
            }
            try { startActivity(intent) }
            catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No email app installed", Toast.LENGTH_SHORT).show()
            }
        }
        binding.restorePurchases.setOnClickListener {
            Toast.makeText(this, "Checking for previous purchases…", Toast.LENGTH_SHORT).show()
            // Trigger a fresh purchase query via a lightweight BillingManager instance
            BillingManager(this) { isPremium ->
                runOnUiThread {
                    PrefsManager(this).setPremium(isPremium)
                    Toast.makeText(
                        this,
                        if (isPremium) "Purchases restored ✓" else "No previous purchases found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No browser installed", Toast.LENGTH_SHORT).show()
        }
    }
}
