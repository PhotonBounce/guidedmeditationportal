package com.auroramind.meditation

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.auroramind.meditation.databinding.DialogOnboardingBinding

/**
 * First-launch welcome dialog. Briefly explains what Guided Meditation Portal does and the
 * freemium model. Shows once — gated by PrefsManager.isOnboardingShown().
 */
class OnboardingDialog(context: Context) {

    private val binding = DialogOnboardingBinding.inflate(LayoutInflater.from(context))

    private val dialog = AlertDialog.Builder(context, R.style.AlertDialogDark)
        .setView(binding.root)
        .setCancelable(false)
        .create()

    init {
        val sfx = SoundEffects(context)
        val haptic = HapticHelper(context)
        val prefs = PrefsManager(context)
        binding.onboardingOk.setOnClickListener {
            haptic.click()
            sfx.tap()
            // Persist the selected goal so recommendations & Spirit can personalize.
            val goal = when (binding.onboardingGoals.checkedChipId) {
                R.id.goalSleep      -> Mood.SLEEP
                R.id.goalStress     -> Mood.STRESS
                R.id.goalFocus      -> Mood.FOCUS
                R.id.goalGrounding  -> Mood.GROUNDING
                R.id.goalCompassion -> Mood.COMPASSION
                else                -> null
            }
            goal?.let { prefs.setGoal(it) }
            dialog.dismiss()
        }
    }

    fun show() {
        dialog.show()
    }
}
