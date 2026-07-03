package com.auroramind.meditation

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

/**
 * "Get help now" — crisis & addiction helplines, one tap to reach a real person.
 *
 * Uses ACTION_DIAL / ACTION_SENDTO (no CALL_PHONE permission) so it just opens
 * the dialer or messaging app with the number pre-filled. US defaults; edit
 * [helplines] to localize. This is a safety surface for a quit-habit app, not a
 * medical service — the footer makes that explicit.
 */
class ResourcesActivity : AppCompatActivity() {

    private data class Help(
        val name: String,
        val number: String,
        val desc: String,
        val sms: Boolean = false,
    )

    private val helplines = listOf(
        Help("Suicide & Crisis Lifeline", "988", "Call or text, 24/7 — free and confidential."),
        Help("SAMHSA National Helpline", "18006624357", "Treatment referral & info for substance use, 24/7."),
        Help("Quit smoking / vaping", "18007848669", "1-800-QUIT-NOW — free coaching to stop nicotine."),
        Help("Crisis Text Line", "741741", "Text HOME to reach a trained crisis counselor.", sms = true),
        Help("Emergency", "911", "If you or someone else is in immediate danger."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val gold = ContextCompat.getColor(this, R.color.accent_iris)
        val cream = ContextCompat.getColor(this, R.color.text_primary)
        val muted = ContextCompat.getColor(this, R.color.text_secondary)
        val faint = ContextCompat.getColor(this, R.color.text_tertiary)
        val cardBg = ContextCompat.getColor(this, R.color.card_bg)
        val border = ContextCompat.getColor(this, R.color.card_border)

        val scroll = ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_cosmic_gradient)
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(40), dp(24), dp(32))
        }

        col.addView(TextView(this).apply {
            text = "Get help now"
            setTextColor(cream); textSize = 26f; setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = "A craving is temporary. If it feels like too much, reach a real person — free, confidential, any time."
            setTextColor(muted); textSize = 14f
            setPadding(0, dp(6), 0, dp(20))
        })

        for (h in helplines) {
            col.addView(helpCard(h, gold, cream, muted, cardBg, border) {
                val uri = Uri.parse((if (h.sms) "sms:" else "tel:") + h.number)
                val action = if (h.sms) Intent.ACTION_SENDTO else Intent.ACTION_DIAL
                runCatching { startActivity(Intent(action, uri)) }
            })
        }

        col.addView(TextView(this).apply {
            text = "Power of Mind isn't a medical or crisis service. In an emergency, call your local emergency number."
            setTextColor(faint); textSize = 12f
            setPadding(0, dp(16), 0, 0)
        })

        scroll.addView(col)
        scroll.clipToPadding = false
        scroll.padSystemBars()
        setContentView(AuraBackground.wrap(this, scroll))
    }

    private fun helpCard(
        h: Help, gold: Int, cream: Int, muted: Int, cardBg: Int, border: Int,
        onClick: () -> Unit,
    ): MaterialCardView {
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val card = MaterialCardView(this).apply {
            setCardBackgroundColor(cardBg)
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1); strokeColor = border
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = dp(12); layoutParams = lp
            isClickable = true; isFocusable = true
            setOnClickListener { onClick() }
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        val txt = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        txt.addView(TextView(this).apply {
            text = h.name; setTextColor(cream); textSize = 16f; setTypeface(typeface, Typeface.BOLD)
        })
        txt.addView(TextView(this).apply {
            text = h.desc; setTextColor(muted); textSize = 13f; setPadding(0, dp(2), 0, 0)
        })
        row.addView(txt)
        row.addView(TextView(this).apply {
            text = if (h.sms) "Text ▸" else "Call ▸"
            setTextColor(gold); textSize = 15f; setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(12), 0, 0, 0)
        })
        card.addView(row)
        return card
    }
}
