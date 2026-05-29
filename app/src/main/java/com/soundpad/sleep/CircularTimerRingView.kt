package com.soundpad.sleep

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View

/**
 * Glowing arc that sweeps around a button as a countdown progresses.
 *
 * Call [start] with a total duration; the view animates a clockwise arc
 * shrinking from 360° down to 0 over the remaining time, with a soft glow
 * at the leading tip. Call [stop] to clear.
 */
class CircularTimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF7DD3FC.toInt()    // aurora cyan
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val ringGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x807DD3FC.toInt()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.NORMAL)
    }

    private var totalMs = 0L
    private var startedAt = 0L
    private var running = false

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            invalidate()
            handler.postDelayed(this, 16)
        }
    }

    fun start(durationMs: Long) {
        totalMs = durationMs
        startedAt = System.currentTimeMillis()
        running = true
        handler.post(tick)
        invalidate()
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        if (!running || totalMs <= 0L) return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val density = context.resources.displayMetrics.density
        val strokeWidth = 4f * density
        ringPaint.strokeWidth = strokeWidth
        ringGlowPaint.strokeWidth = strokeWidth * 2.5f

        val inset = strokeWidth * 1.5f
        val rect = RectF(inset, inset, w - inset, h - inset)

        val elapsed = System.currentTimeMillis() - startedAt
        val frac = ((totalMs - elapsed).toFloat() / totalMs).coerceIn(0f, 1f)
        val sweep = 360f * frac

        // Draw the dim background full ring
        ringPaint.alpha = 30
        canvas.drawArc(rect, 0f, 360f, false, ringPaint)

        // Glow underlay
        ringGlowPaint.alpha = 180
        canvas.drawArc(rect, -90f, sweep, false, ringGlowPaint)

        // Bright foreground sweep
        ringPaint.alpha = 255
        canvas.drawArc(rect, -90f, sweep, false, ringPaint)
    }
}
