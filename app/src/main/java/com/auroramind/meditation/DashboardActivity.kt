package com.auroramind.meditation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.auroramind.meditation.databinding.ActivityDashboardBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Home screen for the quit-habit + affirmations app.
 *
 * Shows the hero "days free" count, money saved, and urges beaten from
 * [HabitStatsManager], plus the two core actions: play today's affirmation and
 * the panic button (urge surfing via [BreathingActivity]). Reached after the
 * onboarding quiz; numbers refresh in [onResume] so they stay live.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var prefs: PrefsManager
    private lateinit var habitStats: HabitStatsManager
    private lateinit var haptic: HapticHelper
    private lateinit var sfx: SoundEffects
    private lateinit var billing: BillingManager
    private var bannerAdView: AdView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(AuraBackground.wrap(this, binding.root))
        // Inset the content only — the aura backdrop keeps painting under the bars.
        binding.root.padSystemBars()

        prefs = PrefsManager(this)
        habitStats = HabitStatsManager(this)
        haptic = HapticHelper(this)
        sfx = SoundEffects(this)
        // Re-verify entitlement on the home screen so the banner reflects an
        // active/lapsed subscription even when the user skips the quiz on launch.
        billing = BillingManager(this) { _ -> runOnUiThread { applyEntitlement() } }

        // Today's affirmation — opens the affirmation library to browse sets.
        binding.btnAffirmation.setOnClickListener {
            haptic.click(); sfx.tap()
            startActivity(Intent(this, AffirmationLibraryActivity::class.java))
        }

        // Panic button — ride out the urge with a paced breath, and bank the win.
        binding.btnPanic.setOnClickListener {
            haptic.click(); sfx.tap()
            habitStats.logUrgeResisted()
            startActivity(Intent(this, BreathingActivity::class.java))
        }

        binding.languageBtn.setOnClickListener {
            haptic.click(); sfx.tap()
            LocaleManager.showPicker(this)
        }

        binding.settingsBtn.setOnClickListener {
            haptic.click(); sfx.tap()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.shareBtn.setOnClickListener {
            haptic.click(); sfx.tap()
            startActivity(Intent(this, ShareActivity::class.java))
        }

        binding.helpLink.setOnClickListener {
            haptic.click(); sfx.tap()
            startActivity(Intent(this, ResourcesActivity::class.java))
        }

        binding.insightsLink.setOnClickListener {
            haptic.click(); sfx.tap()
            startActivity(Intent(this, InsightsActivity::class.java))
        }

        binding.resetLink.setOnClickListener { confirmRelapse() }
    }

    override fun onResume() {
        super.onResume()
        // Starting the dashboard begins the clean clock if the quiz hasn't already.
        if (!habitStats.hasStarted()) habitStats.setQuitDate()
        refresh()
        applyEntitlement()
        bannerAdView?.resume()
    }

    override fun onPause() {
        bannerAdView?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        bannerAdView?.destroy()
        bannerAdView = null
        billing.destroy()
        super.onDestroy()
    }

    /** Show the free-tier banner; remove it the instant the user is premium. */
    private fun applyEntitlement() {
        if (PrefsManager(this).isPremium()) removeBanner() else loadBannerIfNeeded()
    }

    private fun loadBannerIfNeeded() {
        if (bannerAdView != null) return
        val display = resources.displayMetrics
        val widthPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).currentWindowMetrics.bounds.width()
        else display.widthPixels
        val widthDp = (widthPx / display.density).toInt()
        val size = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, widthDp)
        val adView = AdView(this).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_UNIT_ID
            setAdSize(size)
        }
        binding.adContainer.removeAllViews()
        binding.adContainer.addView(adView)
        runCatching { adView.loadAd(AdRequest.Builder().build()) }
        bannerAdView = adView
    }

    private fun removeBanner() {
        bannerAdView?.destroy()
        bannerAdView = null
        binding.adContainer.removeAllViews()
    }

    private fun refresh() {
        val days = habitStats.daysClean()
        binding.daysNumber.text = days.toString()
        binding.daysLabel.text = getString(if (days == 1) R.string.dash_day_free else R.string.dash_days_free)

        val habit = habitDisplay(prefs.getHabitType())
        val best = maxOf(habitStats.longestCleanDays(), days)
        binding.habitSub.text = "free from $habit  ·  best: $best ${if (best == 1) "day" else "days"}"

        // Milestone progress ring + caption.
        val next = HabitStatsManager.MILESTONES.firstOrNull { it > days } ?: 365
        binding.ring.setProgress(if (next > 0) days.toFloat() / next else 1f)
        val left = next - days
        binding.nextMilestone.text = "✦ $left ${if (left == 1) "day" else "days"} to your $next-day milestone"

        // Last-7-days streak strip.
        renderStreak(days.coerceIn(0, 7))

        binding.moneyValue.text = "$" + String.format("%.2f", habitStats.moneySaved())
        binding.urgesValue.text = habitStats.urgesResisted().toString()

        // "Your why" card — the freedom goal captured in the quiz.
        val freedom = prefs.getFreedomGoal()
        binding.dashGreeting.text = getString(R.string.dash_greeting)
        if (freedom.isNotBlank()) {
            binding.whyText.text = freedom
            binding.whyCard.visibility = View.VISIBLE
        } else {
            binding.whyCard.visibility = View.GONE
        }

        celebrateMilestoneIfAny()
    }

    private fun renderStreak(filled: Int) {
        val strip = binding.streakStrip
        strip.removeAllViews()
        val d = resources.displayMetrics.density
        val size = (16 * d).toInt()
        val gap = (8 * d).toInt()
        for (i in 0 until 7) {
            val dot = View(this)
            val lp = LinearLayout.LayoutParams(size, size)
            if (i > 0) lp.marginStart = gap
            dot.layoutParams = lp
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                if (i < filled) {
                    setColor(Color.parseColor("#FFB347"))
                } else {
                    setColor(Color.parseColor("#22FFFFFF"))
                    setStroke((1 * d).toInt(), Color.parseColor("#3A2A18"))
                }
            }
            strip.addView(dot)
        }
    }

    private fun celebrateMilestoneIfAny() {
        val milestone = habitStats.consumeNewMilestone() ?: return
        sfx.chime()
        startActivity(
            Intent(this, MilestoneActivity::class.java)
                .putExtra(MilestoneActivity.EXTRA_DAYS, milestone)
        )
    }

    private fun shareProgress() {
        val days = habitStats.daysClean()
        val habit = habitDisplay(prefs.getHabitType())
        val money = "$" + String.format("%.2f", habitStats.moneySaved())
        val text = "🔥 $days ${if (days == 1) "day" else "days"} free from $habit with Power of Mind — " +
            "$money saved and counting. Break free, one clean day at a time."
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Share your progress"
            )
        )
    }

    private fun confirmRelapse() {
        AlertDialog.Builder(this, R.style.AlertDialogDark)
            .setTitle("Reset your counter?")
            .setMessage("A slip isn't a failure — it's data. We'll keep your best streak and start the clock again from now.")
            .setPositiveButton("Reset") { _, _ ->
                habitStats.recordRelapse()
                refresh()
                Toast.makeText(this, "Fresh start. Day one begins now.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun habitDisplay(key: String): String = when (key) {
        "vaping" -> "vaping"
        "smoking" -> "smoking"
        "social_media" -> "social media"
        "doomscrolling" -> "doomscrolling"
        "alcohol" -> "alcohol"
        else -> "the habit"
    }
}
