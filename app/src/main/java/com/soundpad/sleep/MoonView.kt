package com.soundpad.sleep

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/**
 * Breathing crescent moon. Scales 0.97 ↔ 1.03 over ~4 seconds with a soft
 * gold halo that breathes in counter-phase. Sets the meditative mood at the
 * top of the screen.
 */
class MoonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle) {

    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80F5C842.toInt()
        maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }
    private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5C842.toInt()
    }
    private val cutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0B0B1E.toInt()    // same as cosmic_indigo_top
    }

    private val startNanos = System.nanoTime()
    private val handler = Handler(Looper.getMainLooper())
    private val frame = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 33)   // 30 FPS is enough for breathing
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(frame)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(frame)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val t = (System.nanoTime() - startNanos) / 1_000_000_000.0f
        val breath = sin(t * 2f * PI.toFloat() / 4f).toFloat()    // 4s period
        val moonScale = 1f + breath * 0.03f
        val haloScale = 1f - breath * 0.06f

        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) / 2.5f

        // ── Halo ─────────────────────────────────────────────────────────
        haloPaint.alpha = (110 + breath * 60).toInt().coerceIn(40, 200)
        canvas.drawCircle(cx, cy, radius * 1.6f * haloScale, haloPaint)

        // ── Crescent moon (filled disc with offset cutout) ───────────────
        canvas.drawCircle(cx, cy, radius * moonScale, moonPaint)
        // Offset cutout creates the crescent
        canvas.drawCircle(
            cx + radius * 0.30f, cy - radius * 0.10f,
            radius * 0.95f * moonScale,
            cutoutPaint
        )
    }
}
