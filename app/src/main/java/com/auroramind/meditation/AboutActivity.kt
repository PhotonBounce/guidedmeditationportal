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
 * These must stay in sync with play-store/LISTING.md and the live microsite
 * (deployed to public_html/pom via deploy-site.yml) — that's the one Play
 * Console's App content → Privacy policy field also points to.
 */
class AboutActivity : AppCompatActivity() {

    companion object {
        const val PRIVACY_URL   = "https://photon-bounce.com/pom/privacy.html"
        const val TERMS_URL     = "https://photon-bounce.com/pom/terms.html"
        const val SUPPORT_EMAIL = "support@photon-bounce.com"
        const val WEBSITE_URL   = "https://photon-bounce.com/pom/"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.clipToPadding = false
        binding.root.padSystemBars()

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
                putExtra(Intent.EXTRA_SUBJECT, "Power of Mind Support — v${BuildConfig.VERSION_NAME}")
            }
            try { startActivity(intent) }
            catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No email app installed", Toast.LENGTH_SHORT).show()
            }
        }
        // The app is free for everyone — there are no purchases to restore.
        binding.restorePurchases.visibility = android.view.View.GONE
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
