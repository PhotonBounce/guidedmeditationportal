package com.auroramind.meditation

import android.content.Intent
import android.graphics.Typeface
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
 * The affirmation library — the heart of the "affirmations" side of the app.
 * Lists themed affirmation sets (plus any bundled spoken tracks); tapping one
 * opens [AffirmationPlayerActivity] for that theme. Built programmatically to
 * match the gold brand without a dedicated layout/adapter.
 */
class AffirmationLibraryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val gold = ContextCompat.getColor(this, R.color.accent_iris)
        val cream = ContextCompat.getColor(this, R.color.text_primary)
        val muted = ContextCompat.getColor(this, R.color.text_secondary)
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
            text = "Affirmations"
            setTextColor(cream); textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = "Spoken-style affirmation sets, tuned to where you are. Pick one and let each line land over the soundscape."
            setTextColor(muted); textSize = 14f
            setPadding(0, dp(6), 0, dp(20))
        })

        val bundled = AffirmationLibrary.list(this).size
        for (t in AffirmationContent.THEMES) {
            col.addView(themeCard(t.emoji, t.title, "${t.lines.size} affirmations", gold, cream, muted, cardBg, border, dp(0)) {
                startActivity(
                    Intent(this, AffirmationPlayerActivity::class.java)
                        .putExtra(AffirmationPlayerActivity.EXTRA_THEME, t.id)
                )
            })
        }
        if (bundled > 0) {
            col.addView(themeCard("🎧", "Your spoken tracks", "$bundled recorded", gold, cream, muted, cardBg, border, dp(0)) {
                startActivity(Intent(this, AffirmationPlayerActivity::class.java))
            })
        }

        scroll.addView(col)
        setContentView(scroll)
    }

    private fun themeCard(
        emoji: String, title: String, subtitle: String,
        gold: Int, cream: Int, muted: Int, cardBg: Int, border: Int, unused: Int,
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
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        row.addView(TextView(this).apply { text = emoji; textSize = 26f; setPadding(0, 0, dp(14), 0) })
        val txt = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        txt.addView(TextView(this).apply { text = title; setTextColor(cream); textSize = 17f; setTypeface(typeface, Typeface.BOLD) })
        txt.addView(TextView(this).apply { text = subtitle; setTextColor(muted); textSize = 13f })
        row.addView(txt)
        row.addView(TextView(this).apply { text = "▶"; setTextColor(gold); textSize = 18f })
        card.addView(row)
        return card
    }
}
