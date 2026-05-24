package com.soundpad.sleep

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.soundpad.sleep.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.privacyPolicy.setOnClickListener {
            openUrl(this, "https://yourdomain.com/privacy-policy")
        }
        binding.terms.setOnClickListener {
            openUrl(this, "https://yourdomain.com/terms")
        }
        binding.contactSupport.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@yourdomain.com")
                putExtra(Intent.EXTRA_SUBJECT, "SoundPad Support")
            }
            startActivity(intent)
        }
    }

    private fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
