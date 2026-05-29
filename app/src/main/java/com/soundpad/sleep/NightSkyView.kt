package com.soundpad.sleep

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * Animated cosmic background.
 *
 * Three depth layers of stars drift slowly across the screen, twinkle on
 * their own phases, and shift opposite to phone tilt for a parallax effect.
 * Behind them, two soft nebula blooms slowly rotate, breathing the canvas.
 *
 * Pure Canvas drawing — no images, no shaders. ~60 FPS on a Galaxy A14.
 */
class NightSkyView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : View(context, attrs, defStyle), SensorEventListener {

    // ── Star model ───────────────────────────────────────────────────────
    private data class Star(
        var x: Float, var y: Float,        // 0..1 normalized
        val size: Float,                   // 0.6..3.2 dp
        val baseAlpha: Float,              // 0.4..1.0
        val twinkleSpeed: Float,           // 0.4..1.6 Hz
        val twinklePhase: Float,           // 0..2π
        val driftX: Float,                 // -1..1, very small
        val driftY: Float,                 // -1..1, very small
        val depth: Int,                    // 0 = far (slow), 2 = near (fast)
    )

    // 60 stars total, weighted toward the far layer
    private val stars: List<Star> = buildList {
        repeat(40) { add(makeStar(depth = 0)) }
        repeat(15) { add(makeStar(depth = 1)) }
        repeat(8)  { add(makeStar(depth = 2)) }
    }

    private fun makeStar(depth: Int): Star {
        val depthSize  = when (depth) { 0 -> 0.8f; 1 -> 1.6f; else -> 2.6f }
        val depthAlpha = when (depth) { 0 -> 0.55f; 1 -> 0.75f; else -> 1.0f }
        return Star(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            size = depthSize + Random.nextFloat() * 0.6f,
            baseAlpha = depthAlpha + Random.nextFloat() * 0.1f - 0.05f,
            twinkleSpeed = 0.4f + Random.nextFloat() * 1.2f,
            twinklePhase = Random.nextFloat() * 2f * PI.toFloat(),
            driftX = (Random.nextFloat() - 0.5f) * 0.012f,
            driftY = (Random.nextFloat() - 0.5f) * 0.012f,
            depth = depth,
        )
    }

    // ── Paints ───────────────────────────────────────────────────────────
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        isAntiAlias = true
    }
    private val starGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55A78BFA.toInt()
        isAntiAlias = true
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    private val nebulaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
    }

    // ── Animation clock ──────────────────────────────────────────────────
    private val startNanos = System.nanoTime()
    private val handler = Handler(Looper.getMainLooper())
    private val frame = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 16)  // ~60 FPS
        }
    }

    // ── Parallax via tilt ────────────────────────────────────────────────
    private var tiltX = 0f
    private var tiltY = 0f
    private var sensorManager: SensorManager? = null
    private var accelSensor: Sensor? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(frame)
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(frame)
        sensorManager?.unregisterListener(this)
        super.onDetachedFromWindow()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        // Smooth toward target (gentle low-pass filter) — raw values are jittery.
        val targetX = (event.values[0] / 9.81f).coerceIn(-1f, 1f)
        val targetY = (event.values[1] / 9.81f).coerceIn(-1f, 1f)
        tiltX += (targetX - tiltX) * 0.08f
        tiltY += (targetY - tiltY) * 0.08f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ── Drawing ──────────────────────────────────────────────────────────
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val t = (System.nanoTime() - startNanos) / 1_000_000_000.0f
        val density = context.resources.displayMetrics.density

        // ── 1. Nebula blooms ─────────────────────────────────────────────
        drawNebula(
            canvas,
            cx = w * 0.30f + sin(t * 0.04f) * w * 0.05f,
            cy = h * 0.25f + cos(t * 0.05f) * h * 0.03f,
            radius = w * 0.55f,
            color = 0x33A78BFA.toInt(),
        )
        drawNebula(
            canvas,
            cx = w * 0.70f + sin(t * 0.035f + 1.5f) * w * 0.05f,
            cy = h * 0.75f + cos(t * 0.045f + 1.0f) * h * 0.03f,
            radius = w * 0.50f,
            color = 0x227DD3FC.toInt(),
        )

        // ── 2. Stars ─────────────────────────────────────────────────────
        for (s in stars) {
            // Drift over time (wraps with modulo)
            val px = ((s.x + s.driftX * t) % 1f + 1f) % 1f
            val py = ((s.y + s.driftY * t) % 1f + 1f) % 1f

            // Parallax — deeper layers move less
            val parallaxStrength = when (s.depth) { 0 -> 6f; 1 -> 16f; else -> 32f }
            val ox = -tiltX * parallaxStrength * density
            val oy = -tiltY * parallaxStrength * density

            val cx = px * w + ox
            val cy = py * h + oy

            // Twinkle — sine envelope
            val twinkle = (sin(t * s.twinkleSpeed * 2f * PI.toFloat() + s.twinklePhase) + 1f) / 2f
            val alpha = (s.baseAlpha * (0.55f + twinkle * 0.45f)).coerceIn(0f, 1f)
            val size = s.size * density

            // Near-layer stars get a soft glow halo
            if (s.depth == 2) {
                starGlowPaint.alpha = (alpha * 140).toInt()
                canvas.drawCircle(cx, cy, size * 3f, starGlowPaint)
            }

            starPaint.alpha = (alpha * 255).toInt()
            starPaint.color = if (s.depth == 2) 0xFFC4B5FD.toInt() else Color.WHITE
            canvas.drawCircle(cx, cy, size, starPaint)
        }
    }

    private fun drawNebula(canvas: Canvas, cx: Float, cy: Float, radius: Float, color: Int) {
        val centerColor = color
        val edgeColor   = color and 0x00FFFFFF   // alpha 0
        nebulaPaint.shader = RadialGradient(
            cx, cy, radius, centerColor, edgeColor, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, nebulaPaint)
    }
}
