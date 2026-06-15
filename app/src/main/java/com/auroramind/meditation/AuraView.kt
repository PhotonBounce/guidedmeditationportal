package com.auroramind.meditation

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated brand background — the warm cosmic gradient with slow-drifting gold
 * glow orbs and rising embers. A lighter cousin of the splash canvas, meant to
 * sit behind any screen (the screen's own background is cleared by
 * [AuraBackground.wrap]). Runs ~30fps and stops drawing when off-window.
 */
class AuraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val handler = Handler(Looper.getMainLooper())
    private val startNanos = System.nanoTime()
    private val frame = object : Runnable {
        override fun run() { invalidate(); handler.postDelayed(this, 33) }
    }

    private val bgPaint = Paint()
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(46f, BlurMaskFilter.Blur.NORMAL)
    }
    private val emberPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private data class Ember(
        val nx: Float, val r: Float, val speed: Float,
        val phase: Float, val drift: Float, val alpha: Float,
    )

    private val embers: List<Ember> = buildList {
        val rng = Random(11)
        repeat(22) {
            add(
                Ember(
                    rng.nextFloat(),
                    1.2f + rng.nextFloat() * 2.6f,
                    0.012f + rng.nextFloat() * 0.03f,
                    rng.nextFloat() * 6.28f,
                    (rng.nextFloat() - 0.5f) * 0.06f,
                    0.25f + rng.nextFloat() * 0.5f,
                )
            )
        }
    }

    private data class Orb(val nx: Float, val ny: Float, val r: Float, val color: Int, val dx: Float, val dy: Float)
    private val orbs: List<Orb> = listOf(
        Orb(0.20f, 0.16f, 150f, Color.argb(46, 255, 201, 94), 0.006f, 0.004f),
        Orb(0.82f, 0.68f, 184f, Color.argb(40, 232, 116, 43), -0.005f, -0.006f),
        Orb(0.50f, 0.94f, 160f, Color.argb(32, 255, 179, 71), 0.004f, -0.003f),
    )

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(frame)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacksAndMessages(null)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val t = (System.nanoTime() - startNanos) / 1_000_000_000f
        val d = resources.displayMetrics.density

        // Warm vertical cosmic gradient (matches the rest of the app).
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(0xFF140D08.toInt(), 0xFF1D130B.toInt(), 0xFF241910.toInt()),
            floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Soft drifting glow orbs.
        for (o in orbs) {
            val px = ((o.nx + o.dx * t) % 1.3f + 1.3f) % 1.3f * w
            val py = ((o.ny + o.dy * t) % 1.3f + 1.3f) % 1.3f * h
            glowPaint.color = o.color
            canvas.drawCircle(px, py, o.r * d, glowPaint)
        }

        // Rising embers with a gentle twinkle.
        for (e in embers) {
            val py = (1f - (e.speed * t) % 1f) * h
            val px = ((e.nx + e.drift * t) % 1f + 1f) % 1f * w
            val tw = (sin(t * 1.5f + e.phase) + 1f) / 2f
            emberPaint.color = Color.argb(
                ((e.alpha * (0.4f + tw * 0.6f)) * 255f).toInt().coerceIn(0, 255),
                255, 196, 120,
            )
            canvas.drawCircle(px, py, e.r * d, emberPaint)
        }
    }
}

/** Wraps a screen's content with an animated [AuraView] behind it. */
object AuraBackground {
    fun wrap(context: Context, content: View): View {
        content.background = null
        val frame = FrameLayout(context)
        frame.addView(
            AuraView(context),
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        frame.addView(
            content,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        return frame
    }
}
