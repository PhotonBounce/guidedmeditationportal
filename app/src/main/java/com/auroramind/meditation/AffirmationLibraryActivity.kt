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
 * Lists themed affirmation sets; the free starter theme ("I Am Free") opens
 * immediately, the rest are locked until the user subscribes — tapping a locked
 * theme opens the paywall. Cards are rebuilt in [onResume] so the library
 * unlocks the instant a purchase completes and the user returns.
 */
class AffirmationLibraryActivity : AppCompatActivity() {

    private lateinit var col: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()
        val cream = ContextCompat.getColor(this, R.color.text_primary)
        val muted = ContextCompat.getColor(this, R.color.text_secondary)

        val scroll = ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_cosmic_gradient)
            isFillViewport = true
        }
        col = LinearLayout(this).apply {
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

        scroll.addView(col)
        setContentView(AuraBackground.wrap(this, scroll))
    }

    override fun onResume() {
        super.onResume()
        renderThemes()
    }

    private fun renderThemes() {
        val gold = ContextCompat.getColor(this, R.color.accent_iris)
        val cream = ContextCompat.getColor(this, R.color.text_primary)
        val muted = ContextCompat.getColor(this, R.color.text_secondary)
        val cardBg = ContextCompat.getColor(this, R.color.card_bg)
        val border = ContextCompat.getColor(this, R.color.card_border)

        // Drop any previously-rendered cards, keeping the two header views.
        while (col.childCount > 2) col.removeViewAt(2)

        // Favorites shortcut
        col.addView(themeCard("♥", "Favorites", "your saved lines", false, gold, cream, muted, cardBg, border) {
            startActivity(Intent(this, FavoritesActivity::class.java))
        })

        val premium = PrefsManager(this).isPremium()
        for (t in AffirmationContent.THEMES) {
            val locked = t.premium && !premium
            val subtitle = if (locked) "🔒  Premium — tap to unlock" else "${t.lines.size} affirmations"
            col.addView(themeCard(t.emoji, t.title, subtitle, locked, gold, cream, muted, cardBg, border) {
                if (locked) {
                    startActivity(
                        Intent(this, QuizActivity::class.java)
                            .putExtra(QuizActivity.EXTRA_UPGRADE_ONLY, true)
                    )
                } else {
                    startActivity(
                        Intent(this, AffirmationPlayerActivity::class.java)
                            .putExtra(AffirmationPlayerActivity.EXTRA_THEME, t.id)
                    )
                }
            })
        }

        // Bundled spoken recordings (if any) are a premium perk.
        val bundled = AffirmationLibrary.list(this).size
        if (bundled > 0 && premium) {
            col.addView(themeCard("🎧", "Your spoken tracks", "$bundled recorded", false, gold, cream, muted, cardBg, border) {
                startActivity(Intent(this, AffirmationPlayerActivity::class.java))
            })
        }
    }

    private fun themeCard(
        emoji: String, title: String, subtitle: String, locked: Boolean,
        gold: Int, cream: Int, muted: Int, cardBg: Int, border: Int,
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
        txt.addView(TextView(this).apply { text = subtitle; setTextColor(if (locked) gold else muted); textSize = 13f })
        row.addView(txt)
        row.addView(TextView(this).apply { text = if (locked) "🔒" else "▶"; setTextColor(gold); textSize = 18f })
        card.addView(row)
        return card
    }
}
