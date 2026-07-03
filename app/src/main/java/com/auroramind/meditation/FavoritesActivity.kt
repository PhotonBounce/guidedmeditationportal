package com.auroramind.meditation

import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

/** Favorites — the affirmation lines the user hearted in the player. */
class FavoritesActivity : AppCompatActivity() {

    private val mp by lazy { resources.displayMetrics.density }
    private fun dp(v: Int) = (v * mp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()
        val cream = ContextCompat.getColor(this, R.color.text_primary)
        val muted = ContextCompat.getColor(this, R.color.text_secondary)
        val ember = ContextCompat.getColor(this, R.color.accent_rose)
        val cardBg = ContextCompat.getColor(this, R.color.card_bg)
        val border = ContextCompat.getColor(this, R.color.card_border)

        val favs = PrefsManager(this).getAffirmationFavorites().toList()

        val scroll = ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_cosmic_gradient); isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(40), dp(24), dp(32))
        }
        col.addView(TextView(this).apply {
            text = "Favorites"; setTextColor(cream); textSize = 26f; setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = if (favs.isEmpty()) "Tap the heart in any session to save the lines that land."
            else "The affirmations that landed for you."
            setTextColor(muted); textSize = 14f; setPadding(0, dp(6), 0, dp(16))
        })

        for (line in favs) {
            val card = MaterialCardView(this).apply {
                setCardBackgroundColor(cardBg); radius = dp(14).toFloat(); cardElevation = 0f
                strokeWidth = dp(1); strokeColor = border
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { bottomMargin = dp(10) }
            }
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; setPadding(dp(16), dp(14), dp(16), dp(14))
            }
            row.addView(TextView(this).apply {
                text = "♥"; setTextColor(ember); textSize = 16f; setPadding(0, 0, dp(10), 0)
            })
            row.addView(TextView(this).apply {
                text = line; setTextColor(cream); textSize = 15f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            card.addView(row)
            col.addView(card)
        }

        scroll.addView(col)
        scroll.clipToPadding = false
        scroll.padSystemBars()
        setContentView(AuraBackground.wrap(this, scroll))
    }
}
