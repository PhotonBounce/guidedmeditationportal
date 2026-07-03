package com.auroramind.meditation

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Streak calendar — a 5×7 month grid where every clean day glows gold and today
 * is ringed. Pure on-device data from [HabitStatsManager]. Built over the
 * animated aura background.
 */
class CalendarActivity : AppCompatActivity() {

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
        val cells = 35
        val filled = days.coerceIn(0, cells)

        val scroll = ScrollView(this).apply {
            setBackgroundResource(R.drawable.bg_cosmic_gradient)
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(40), dp(24), dp(32))
        }
        col.addView(TextView(this).apply {
            text = "Your calendar"; setTextColor(cream); textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = "Every gold day is a day you stayed free."
            setTextColor(muted); textSize = 14f; setPadding(0, dp(6), 0, dp(18))
        })

        // Weekday header
        val head = weekRow()
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { w ->
            head.addView(TextView(this).apply {
                text = w; gravity = Gravity.CENTER; setTextColor(muted); textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }
        col.addView(head)

        // 5 rows × 7 cells
        var idx = 0
        repeat(5) {
            val row = weekRow().apply {
                (layoutParams as LinearLayout.LayoutParams).topMargin = dp(6)
            }
            repeat(7) {
                val on = idx >= cells - filled
                val today = idx == cells - 1
                val cell = TextView(this).apply {
                    val lp = LinearLayout.LayoutParams(0, dp(34), 1f)
                    if (it > 0) lp.marginStart = dp(6)
                    layoutParams = lp
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = dp(7).toFloat()
                        if (on) {
                            colors = intArrayOf(Color.parseColor("#FFE6A8"), Color.parseColor("#FFB347"))
                            orientation = GradientDrawable.Orientation.TOP_BOTTOM
                        } else {
                            setColor(Color.parseColor("#14FFFFFF"))
                            setStroke(dp(1), Color.parseColor("#3A2A18"))
                        }
                        if (today) setStroke(dp(2), gold)
                    }
                }
                row.addView(cell)
                idx++
            }
            col.addView(row)
        }

        col.addView(TextView(this).apply {
            text = "🔥 $days-day streak · keep it glowing"
            setTextColor(gold); textSize = 13f; setPadding(0, dp(18), 0, 0)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        scroll.addView(col)
        scroll.clipToPadding = false
        scroll.padSystemBars()
        setContentView(AuraBackground.wrap(this, scroll))
    }

    private fun weekRow() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
