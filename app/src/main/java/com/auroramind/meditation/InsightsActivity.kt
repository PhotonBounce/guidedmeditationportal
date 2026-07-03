package com.auroramind.meditation

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView

/**
 * Insights — at-a-glance stats (days free, money saved, urges beaten, best
 * streak) and a simple savings chart, all from on-device data. Built
 * programmatically over the animated aura background.
 */
class InsightsActivity : AppCompatActivity() {

    private val mp by lazy { resources.displayMetrics.density }
    private fun dp(v: Int) = (v * mp).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()
        val habit = HabitStatsManager(this)
        val cream = ContextCompat.getColor(this, R.color.text_primary)
        val muted = ContextCompat.getColor(this, R.color.text_secondary)
        val gold = ContextCompat.getColor(this, R.color.accent_iris)

        val days = habit.daysClean()
        val saved = habit.moneySaved()
        val best = maxOf(habit.longestCleanDays(), days)
        val urges = habit.urgesResisted()

        val scroll = ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_cosmic_gradient)
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(40), dp(24), dp(32))
        }
        col.addView(TextView(this).apply {
            text = "Insights"; setTextColor(cream); textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = "📅  Open streak calendar  ›"
            setTextColor(gold); textSize = 14f
            setPadding(0, dp(12), 0, dp(2))
            isClickable = true; isFocusable = true
            setOnClickListener { startActivity(Intent(this@InsightsActivity, CalendarActivity::class.java)) }
        })

        col.addView(statRow(listOf("$days" to "days free", "$" + String.format("%.0f", saved) to "saved")))
        col.addView(statRow(listOf("$urges" to "urges beaten", "$best" to "best streak")))

        col.addView(TextView(this).apply {
            text = "Money saved, building daily"; setTextColor(muted); textSize = 13f
            setPadding(0, dp(20), 0, dp(8))
        })
        col.addView(barChart())

        scroll.addView(col)
        scroll.clipToPadding = false
        scroll.padSystemBars()
        setContentView(AuraBackground.wrap(this, scroll))
    }

    private fun statRow(items: List<Pair<String, String>>): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) }
        }
        items.forEachIndexed { i, (big, small) ->
            val card = MaterialCardView(this).apply {
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.card_bg))
                radius = dp(14).toFloat(); cardElevation = 0f
                strokeWidth = dp(1); strokeColor = ContextCompat.getColor(context, R.color.card_border)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { if (i > 0) marginStart = dp(10) }
            }
            val inner = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setPadding(dp(16), dp(18), dp(16), dp(18))
            }
            inner.addView(TextView(this).apply {
                text = big; setTextColor(ContextCompat.getColor(context, R.color.accent_iris))
                textSize = 28f; setTypeface(typeface, Typeface.BOLD)
            })
            inner.addView(TextView(this).apply {
                text = small; setTextColor(ContextCompat.getColor(context, R.color.text_secondary)); textSize = 12f
            })
            card.addView(inner)
            row.addView(card)
        }
        return row
    }

    private fun barChart(): View {
        val chart = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(124))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = dp(14).toFloat()
                setColor(Color.parseColor("#241910")); setStroke(dp(1), Color.parseColor("#3A2A18"))
            }
        }
        val maxH = dp(96)
        for (i in 0 until 7) {
            val frac = (i + 1) / 7f
            val bar = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, (maxH * frac).toInt(), 1f)
                    .apply { if (i > 0) marginStart = dp(8) }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(dp(4).toFloat(), dp(4).toFloat(), dp(4).toFloat(), dp(4).toFloat(), 0f, 0f, 0f, 0f)
                    colors = intArrayOf(Color.parseColor("#FFC95E"), Color.parseColor("#E8742B"))
                    orientation = GradientDrawable.Orientation.TOP_BOTTOM
                }
            }
            chart.addView(bar)
        }
        return chart
    }
}
