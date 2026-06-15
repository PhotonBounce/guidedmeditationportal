package com.auroramind.meditation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * A circular progress ring for the dashboard hero — a gold arc over a faint
 * track, showing how far the user is toward their next clean-time milestone.
 * Call [setProgress] (0..1); it animates from the current value.
 */
class RingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val d = resources.displayMetrics.density
    private var progress = 0f
    private var animator: ValueAnimator? = null
    private val rect = RectF()

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 9f * d
        color = Color.parseColor("#3A2A18")
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 9f * d
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#FFC95E")
    }

    fun setProgress(p: Float) {
        val target = p.coerceIn(0f, 1f)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 850
            interpolator = DecelerateInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val pad = arc.strokeWidth / 2f + 2f * d
        rect.set(pad, pad, width - pad, height - pad)
        canvas.drawArc(rect, -90f, 360f, false, track)
        if (progress > 0f) canvas.drawArc(rect, -90f, 360f * progress, false, arc)
    }
}
