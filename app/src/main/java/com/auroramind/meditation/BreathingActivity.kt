package com.auroramind.meditation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.SystemClock
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import kotlin.math.*

/**
 * BreathingActivity — an interactive guided-breath pacer.
 *
 * A glowing orb expands and contracts through a 4-4-4-4 "box breathing" cycle
 * (Inhale · Hold · Exhale · Hold), with on-screen phase labels and a soft
 * haptic tick at each phase change. This is a table-stakes feature users
 * expect from premium meditation apps — and a perfect <60s "quick win".
 *
 * Pure Canvas — no assets. Counts as a session toward the user's streak.
 */
class BreathingActivity : AppCompatActivity() {

    private lateinit var view: BreathingView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#140D08"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        view = BreathingView(this)
        root.addView(view, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Title + subtitle
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, (72 * resources.displayMetrics.density).toInt(), pad, 0)
        }
        header.addView(TextView(this).apply {
            text = "Box Breathing"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
        })
        header.addView(TextView(this).apply {
            text = "Follow the orb · 4 seconds each phase"
            setTextColor(Color.parseColor("#FFE6A8"))
            textSize = 13f
            gravity = Gravity.CENTER
            val t = (6 * resources.displayMetrics.density).toInt()
            setPadding(0, t, 0, 0)
        })
        root.addView(header, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP
        ))

        // Done button
        val done = MaterialButton(this).apply {
            text = "Done"
            cornerRadius = (24 * resources.displayMetrics.density).toInt()
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFC95E"))
            setTextColor(Color.WHITE)
            setOnClickListener { finish() }
        }
        val lp = FrameLayout.LayoutParams(
            (160 * resources.displayMetrics.density).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply { bottomMargin = (48 * resources.displayMetrics.density).toInt() }
        root.addView(done, lp)

        setContentView(root)

        // Credit a session toward the streak
        StatsManager(this).recordSessionStart()
    }

    override fun onDestroy() {
        view.stop()
        super.onDestroy()
    }
}

/** The animated breathing orb + phase label. */
private class BreathingView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val haptic = HapticHelper(context)
    private val sfx = SoundEffects(context)

    // 4-4-4-4 box breathing
    private val phaseMs = 4000L
    private val phases = arrayOf("Inhale", "Hold", "Exhale", "Hold")
    private var lastPhase = -1

    private val startTime = SystemClock.elapsedRealtime()

    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(40f, BlurMaskFilter.Blur.NORMAL)
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(120, 255, 230, 168)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
    }
    private val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFE6A8")
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { invalidate() }
        start()
    }

    fun stop() { animator.cancel() }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val d = resources.displayMetrics.density
        val cx = w / 2f; val cy = h / 2f

        val elapsed = SystemClock.elapsedRealtime() - startTime
        val cycleMs = phaseMs * 4
        val inCycle = elapsed % cycleMs
        val phaseIdx = (inCycle / phaseMs).toInt().coerceIn(0, 3)
        val phaseProgress = (inCycle % phaseMs).toFloat() / phaseMs   // 0..1
        val secLeft = 4 - (inCycle % phaseMs / 1000).toInt()

        // Haptic tick on phase change
        if (phaseIdx != lastPhase) {
            lastPhase = phaseIdx
            haptic.tick()
            if (phaseIdx == 1) {
                sfx.breathingTop()
            } else if (phaseIdx == 3) {
                sfx.breathingBottom()
            }
        }

        // Scale: inhale grows 0.4→1.0; hold stays; exhale shrinks 1.0→0.4; hold stays
        val scale = when (phaseIdx) {
            0 -> 0.4f + 0.6f * easeInOut(phaseProgress)   // inhale
            1 -> 1.0f                                      // hold (full)
            2 -> 1.0f - 0.6f * easeInOut(phaseProgress)   // exhale
            else -> 0.4f                                   // hold (empty)
        }

        val maxR = min(w, h) * 0.28f
        val r = maxR * scale

        // Outer guide ring (full size)
        canvas.drawCircle(cx, cy, maxR, ringPaint)

        // Glow halo
        glowPaint.color = Color.argb(110, 255, 201, 94)
        canvas.drawCircle(cx, cy, r * 1.05f, glowPaint)

        // Orb with radial gradient
        orbPaint.shader = RadialGradient(
            cx, cy, r.coerceAtLeast(1f),
            Color.parseColor("#FFE6A8"), Color.parseColor("#FFC95E"),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, orbPaint)

        // Phase label + countdown
        labelPaint.textSize = 26f * d
        canvas.drawText(phases[phaseIdx], cx, cy - 14f * d, labelPaint)
        countPaint.textSize = 40f * d
        canvas.drawText(secLeft.coerceAtLeast(1).toString(), cx, cy + 30f * d, countPaint)
    }

    private fun easeInOut(t: Float): Float =
        if (t < 0.5f) 2f * t * t else -1f + (4f - 2f * t) * t
}
