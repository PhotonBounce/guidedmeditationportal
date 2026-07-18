package com.auroramind.meditation

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.content.ContextCompat
import com.auroramind.meditation.databinding.ActivityQuizBinding

/**
 * Onboarding assessment quiz.
 *
 * A short series of questions that personalize the experience. The app is free
 * for everyone (ad-supported) — there is no paywall. Completing the quiz records
 * the user's habit profile (via [PrefsManager]), starts the clean-time clock
 * (via [HabitStatsManager]), and goes straight to the dashboard.
 */
class QuizActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizBinding
    private lateinit var prefs: PrefsManager
    private lateinit var habitStats: HabitStatsManager
    private lateinit var haptic: HapticHelper
    private lateinit var sfx: SoundEffects

    private var stepIndex = 0

    private val singleAnswers = HashMap<Int, String>()
    private val multiAnswers = HashMap<Int, MutableSet<String>>()

    private data class Step(
        val title: String,
        val subtitle: String,
        val options: List<String>,
        val multi: Boolean = false,
        val freeText: Boolean = false,
    )

    // Indices are referenced when persisting: 0 habit, 2 triggers, 3 cost, 4 freedom.
    private val steps = listOf(
        Step(
            "What are you breaking free from?",
            "Pick the one that fits best — your plan is built around it.",
            listOf("Vaping", "Smoking", "Social media", "Doomscrolling", "Alcohol", "Something else"),
        ),
        Step(
            "How long have you been trying to quit?",
            "There's no wrong answer. This just sets the right pace.",
            listOf("I'm just starting", "Less than a month", "1–6 months", "Over 6 months"),
        ),
        Step(
            "When do the urges hit hardest?",
            "Choose all that apply — affirmations will target these moments.",
            listOf("Stress", "Boredom", "Loneliness", "After meals", "Social situations", "Mornings", "Late nights"),
            multi = true,
        ),
        Step(
            "Roughly what does the habit cost you a day?",
            "We'll turn every clean day into money saved.",
            listOf("Under $5", "$5–$10", "$10–$20", "Over $20"),
        ),
        Step(
            "What will freedom feel like?",
            "One line, in your own words. We'll weave it into your affirmations.",
            emptyList(),
            freeText = true,
        ),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(AuraBackground.wrap(this, binding.root))
        // Inset the content only (aura stays full-bleed); ime=true keeps the
        // free-text step clear of the keyboard on API 30+ where adjustResize
        // is ignored for edge-to-edge windows.
        binding.root.padSystemBars(ime = true)

        prefs = PrefsManager(this)
        habitStats = HabitStatsManager(this)
        haptic = HapticHelper(this)
        sfx = SoundEffects(this)

        // The app is free for everyone (ad-supported) — no paywall. Hide any
        // leftover paywall/subscription views from the layout.
        binding.paywallGroup.visibility = View.GONE
        binding.previewLink.visibility = View.GONE
        binding.restoreLink.visibility = View.GONE

        binding.btnContinue.setOnClickListener {
            haptic.click(); sfx.tap()
            onContinue()
        }

        renderStep()
    }

    private fun renderStep() {
        val step = steps[stepIndex]
        binding.progressText.text = "STEP ${stepIndex + 1} OF ${steps.size}"
        binding.questionTitle.text = step.title
        binding.questionSubtitle.text = step.subtitle

        binding.optionsRadioGroup.visibility = if (!step.multi && !step.freeText) View.VISIBLE else View.GONE
        binding.optionsCheckContainer.visibility = if (step.multi) View.VISIBLE else View.GONE
        binding.freedomInput.visibility = if (step.freeText) View.VISIBLE else View.GONE

        // Reset the radio group's checked state. RadioGroup.removeAllViews()
        // does NOT clear the remembered checkedRadioButtonId, so without this
        // the group keeps returning the id of a button removed on a previous
        // step. On a later step where the user hasn't picked anything, that
        // stale id slips past the "nothing selected" guard in onContinue(),
        // then findViewById(staleId) returns null → NullPointerException on
        // getText(). Clearing it here makes every step start truly unselected.
        binding.optionsRadioGroup.removeAllViews()
        binding.optionsRadioGroup.clearCheck()
        binding.optionsCheckContainer.removeAllViews()

        val accent = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_iris))
        val textColor = ContextCompat.getColor(this, R.color.text_primary)
        val pad = dp(12)

        if (step.multi) {
            step.options.forEach { opt ->
                val cb = AppCompatCheckBox(this).apply {
                    text = opt
                    textSize = 16f
                    setTextColor(textColor)
                    setPadding(dp(8), pad, dp(8), pad)
                    buttonTintList = accent
                    isChecked = multiAnswers[stepIndex]?.contains(opt) == true
                }
                binding.optionsCheckContainer.addView(cb)
            }
        } else if (!step.freeText) {
            step.options.forEach { opt ->
                val rb = AppCompatRadioButton(this).apply {
                    id = View.generateViewId()
                    text = opt
                    textSize = 16f
                    setTextColor(textColor)
                    setPadding(dp(8), pad, dp(8), pad)
                    buttonTintList = accent
                }
                binding.optionsRadioGroup.addView(rb)
                if (singleAnswers[stepIndex] == opt) binding.optionsRadioGroup.check(rb.id)
            }
        }

        binding.btnContinue.text = if (stepIndex == steps.lastIndex) "See my plan" else "Continue"
    }

    private fun onContinue() {
        val step = steps[stepIndex]
        when {
            step.freeText -> { /* optional — read at persist time */ }
            step.multi -> {
                val selected = collectChecked()
                if (selected.isEmpty()) {
                    Toast.makeText(this, "Pick at least one to continue", Toast.LENGTH_SHORT).show()
                    return
                }
                multiAnswers[stepIndex] = selected
            }
            else -> {
                val checkedId = binding.optionsRadioGroup.checkedRadioButtonId
                // Scope the lookup to the radio group (not the whole root) and
                // null-check the result: a stale or removed id must never reach
                // getText() on a null view.
                val selected = if (checkedId != View.NO_ID)
                    binding.optionsRadioGroup.findViewById<RadioButton>(checkedId) else null
                if (selected == null) {
                    Toast.makeText(this, "Pick one to continue", Toast.LENGTH_SHORT).show()
                    return
                }
                singleAnswers[stepIndex] = selected.text.toString()
            }
        }

        if (stepIndex < steps.lastIndex) {
            stepIndex++
            renderStep()
        } else {
            // No paywall — personalize, then straight into the free app.
            persistProfile()
            goToDashboard()
        }
    }

    private fun collectChecked(): MutableSet<String> {
        val set = mutableSetOf<String>()
        for (i in 0 until binding.optionsCheckContainer.childCount) {
            val child = binding.optionsCheckContainer.getChildAt(i)
            if (child is AppCompatCheckBox && child.isChecked) set.add(child.text.toString())
        }
        return set
    }

    private fun persistProfile() {
        prefs.setHabitType(habitKey(singleAnswers[0]))
        prefs.setTriggers(multiAnswers[2] ?: emptySet())
        prefs.setFreedomGoal(binding.freedomInput.text.toString().trim())
        habitStats.setQuitDate()
        habitStats.setDailyCost(costPerDay(singleAnswers[3]))
        prefs.setQuizCompleted(true)
    }

    private fun goToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun habitKey(label: String?): String = when (label) {
        "Vaping" -> "vaping"
        "Smoking" -> "smoking"
        "Social media" -> "social_media"
        "Doomscrolling" -> "doomscrolling"
        "Alcohol" -> "alcohol"
        else -> "other"
    }

    private fun costPerDay(label: String?): Float = when (label) {
        "Under $5" -> 3f
        "$5–$10" -> 7f
        "$10–$20" -> 15f
        "Over $20" -> 25f
        else -> 0f
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        /** Retained for compatibility; the app has no paywall, so it is unused. */
        const val EXTRA_UPGRADE_ONLY = "upgrade_only"
    }
}
