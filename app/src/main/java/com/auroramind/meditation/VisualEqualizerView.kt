package com.auroramind.meditation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Animated real-time spectrum visualizer view.
 * Renders glowing bars that bounce to simulate frequency band analysis when music is playing.
 */
class VisualEqualizerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFFFB347.toInt() // Teal accent (matching mix/eq theme)
        style = Paint.Style.FILL
    }

    private var running = false
    private val numBars = 7
    private val barHeights = FloatArray(numBars) { 0.15f }
    private val targetHeights = FloatArray(numBars) { 0.15f }

    private val handler = Handler(Looper.getMainLooper())
    private val animationTick = object : Runnable {
        override fun run() {
            if (!running) return
            
            // Interpolate heights to simulate smooth audio motion
            for (i in 0 until numBars) {
                if (Math.abs(barHeights[i] - targetHeights[i]) < 0.08f) {
                    // Pick a new target height randomly
                    targetHeights[i] = Random.nextFloat() * 0.75f + 0.15f
                }
                // Smooth ease towards target
                barHeights[i] = barHeights[i] * 0.75f + targetHeights[i] * 0.25f
            }
            invalidate()
            handler.postDelayed(this, 30) // ~30fps for smooth performance
        }
    }

    fun start() {
        if (running) return
        running = true
        handler.post(animationTick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(animationTick)
        for (i in 0 until numBars) {
            barHeights[i] = 0.15f
            targetHeights[i] = 0.15f
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val barSpacing = w / (numBars * 2 - 1)
        val barWidth = barSpacing

        for (i in 0 until numBars) {
            val barHeight = h * barHeights[i]
            val left = i * 2 * barSpacing
            val top = h - barHeight
            val right = left + barWidth
            val bottom = h
            
            // Draw rounded bar
            canvas.drawRoundRect(left, top, right, bottom, barWidth / 2f, barWidth / 2f, barPaint)
        }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }
}
