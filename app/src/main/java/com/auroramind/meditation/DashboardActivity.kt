package com.auroramind.meditation

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.auroramind.meditation.databinding.ActivityDashboardBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PrefsManager(this)
        habitStats = HabitStatsManager(this)
        haptic = HapticHelper(this)
        sfx = SoundEffects(this)

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

        binding.settingsBtn.setOnClickListener {
            haptic.click(); sfx.tap()
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.shareBtn.setOnClickListener {
            haptic.click(); sfx.tap()
            shareProgress()
        }

        binding.resetLink.setOnClickListener { confirmRelapse() }
    }

    override fun onResume() {
        super.onResume()
        // Starting the dashboard begins the clean clock if the quiz hasn't already.
        if (!habitStats.hasStarted()) habitStats.setQuitDate()
        refresh()
    }

    private fun refresh() {
        val days = habitStats.daysClean()
        binding.daysNumber.text = days.toString()
        binding.daysLabel.text = if (days == 1) "DAY FREE" else "DAYS FREE"

        val habit = habitDisplay(prefs.getHabitType())
        val best = maxOf(habitStats.longestCleanDays(), days)
        binding.habitSub.text = "free from $habit  ·  best: $best ${if (best == 1) "day" else "days"}"

        binding.moneyValue.text = "$" + String.format("%.2f", habitStats.moneySaved())
        binding.urgesValue.text = habitStats.urgesResisted().toString()

        val freedom = prefs.getFreedomGoal()
        binding.dashGreeting.text = if (freedom.isNotBlank()) "Toward: $freedom" else "Stay free today."

        celebrateMilestoneIfAny()
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
