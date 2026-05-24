package com.soundpad.sleep

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.soundpad.sleep.databinding.DialogOnboardingBinding

class OnboardingDialog(context: Context) {
    private val binding = DialogOnboardingBinding.inflate(LayoutInflater.from(context))
    private val dialog = AlertDialog.Builder(context)
        .setView(binding.root)
        .setCancelable(true)
        .create()

    fun show() {
        dialog.show()
    }
}
