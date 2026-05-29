package com.soundpad.sleep

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.soundpad.sleep.databinding.DialogOnboardingBinding

/**
 * First-launch welcome dialog. Briefly explains what SoundPad does and the
 * freemium model. Shows once — gated by PrefsManager.isOnboardingShown().
 */
class OnboardingDialog(context: Context) {

    private val binding = DialogOnboardingBinding.inflate(LayoutInflater.from(context))

    private val dialog = AlertDialog.Builder(context, R.style.AlertDialogDark)
        .setView(binding.root)
        .setCancelable(false)
        .create()

    init {
        binding.onboardingOk.setOnClickListener { dialog.dismiss() }
    }

    fun show() {
        dialog.show()
    }
}
