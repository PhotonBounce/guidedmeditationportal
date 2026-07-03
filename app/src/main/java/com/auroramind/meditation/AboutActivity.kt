package com.auroramind.meditation

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.auroramind.meditation.databinding.ActivityAboutBinding

/**
 * About / Legal / Support screen.
 *
 * IMPORTANT: photon-bounce.com/ausis/ hosts the privacy/terms pages for a
 * DIFFERENT app ("Ausis — Sleep, Focus & Workout Sounds"). Submitting this
 * app with that URL would misrepresent our data practices to Play reviewers
 * and users. This app's own pages ship inside meditation-portal-site.zip and
 * deploy (deploy-site.yml or deploy-docs.yml) to:
 *   https://www.photon-bounce.com/guidedmeditation/privacy.html
 *   https://www.photon-bounce.com/guidedmeditation/terms.html
 * The same URL goes into Play Console → App content → Privacy policy.
 */
class AboutActivity : AppCompatActivity() {

    /** Created on first "Restore purchases" tap, reused after, destroyed with us. */
    private var billing: BillingManager? = null

    companion object {
        const val PRIVACY_URL   = "https://www.photon-bounce.com/guidedmeditation/privacy.html"
        const val TERMS_URL     = "https://www.photon-bounce.com/guidedmeditation/terms.html"
        const val SUPPORT_EMAIL = "support.meditationportal@gmail.com"
        const val WEBSITE_URL   = "https://www.photon-bounce.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // The app is always dark — force light bar icons regardless of the
            // device theme (default auto() would draw dark icons on our dark UI)
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge: keep scroll content clear of the status / nav bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

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
            // One BillingManager for the screen's lifetime — a new instance per
            // tap leaked a connected BillingClient (and this activity) each time
            val manager = billing ?: BillingManager(this) { isPremium ->
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    PrefsManager(this).setPremium(isPremium)
                    Toast.makeText(
                        this,
                        if (isPremium) "Purchases restored ✓" else "No previous purchases found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.also {
                it.onQueryFailed = {
                    runOnUiThread {
                        if (!isFinishing && !isDestroyed) {
                            Toast.makeText(
                                this,
                                "Could not reach Google Play — try again later",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
                billing = it
            }
            // First tap: the manager auto-queries once connected. Later taps re-query.
            manager.queryPurchases()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    override fun onDestroy() {
        billing?.destroy()
        billing = null
        super.onDestroy()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "No browser installed", Toast.LENGTH_SHORT).show()
        }
    }
}
