package com.auroramind.meditation

import android.app.Activity
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * A standard top-left "← Back" affordance for secondary screens.
 *
 * Every secondary screen should show a visible way back (not only the system
 * gesture). Screens built programmatically add [backArrow] as the first child of
 * their content column; it simply finishes the activity, returning to wherever
 * the user came from.
 */
fun Activity.backArrow(): TextView {
    val d = resources.displayMetrics.density
    fun dp(v: Int) = (v * d).toInt()
    return TextView(this).apply {
        text = "←"
        textSize = 26f
        setTextColor(ContextCompat.getColor(this@backArrow, R.color.text_primary))
        gravity = Gravity.CENTER
        setPadding(dp(6), dp(2), dp(14), dp(10))
        isClickable = true
        isFocusable = true
        contentDescription = getString(android.R.string.cancel)
        setBackgroundResource(android.R.color.transparent)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        )
        setOnClickListener { finish() }
    }
}

/** Prepends [backArrow] to the top of a vertical LinearLayout content column. */
fun Activity.addBackArrowTo(column: LinearLayout) {
    column.addView(backArrow(), 0)
}
