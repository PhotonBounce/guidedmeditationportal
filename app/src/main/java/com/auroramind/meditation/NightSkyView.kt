package com.auroramind.meditation

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
 * Animated cosmic "portal" background.
 *
 * Three depth layers of stars drift slowly across the screen, twinkle on
 * their own phases, and shift opposite to phone tilt for a parallax effect.
 * Behind them, two soft nebula blooms slowly rotate, breathing the canvas.
 * A slow-breathing "portal" ring pulses outward from center — a gentle visual
 * pacing cue echoing guided-breath rhythm — and soft drifting orbs (lotus /
 * moonlight tones) float past for extra depth and calm.
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
        color = 0x55E91E8C.toInt()
        isAntiAlias = true
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.NORMAL)
    }
    private val nebulaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
    }
    private val portalRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = 0x66E91E8C.toInt()
    }
    private val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
    }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }

    // ── Soft drifting orbs (lotus / moonlight motes) ─────────────────────
    private data class Orb(
        var x: Float, var y: Float,   // 0..1 normalized
        val size: Float,              // dp radius
        val color: Int,
        val driftX: Float,
        val driftY: Float,
        val pulseSpeed: Float,
        val pulsePhase: Float,
    )

    private val orbs: List<Orb> = buildList {
        repeat(6) {
            add(
                Orb(
                    x = Random.nextFloat(),
                    y = Random.nextFloat(),
                    size = 28f + Random.nextFloat() * 36f,
                    color = if (it % 2 == 0) 0x33FF80C8.toInt() else 0x33FFD6EC.toInt(),
                    driftX = (Random.nextFloat() - 0.5f) * 0.018f,
                    driftY = (Random.nextFloat() - 0.5f) * 0.018f,
                    pulseSpeed = 0.15f + Random.nextFloat() * 0.25f,
                    pulsePhase = Random.nextFloat() * 2f * PI.toFloat(),
                )
            )
        }
    }

    /** Breath cycle length in seconds — mirrors a calm 4s-in / 4s-out pace. */
    private val breathPeriodSec = 8f

    // ── Reactive "energy" pulses ──────────────────────────────────────────
    // The background subtly flares — brighter rings, warmer nebula tint, and
    // a soft expanding ripple — whenever the user does something meaningful
    // (taps play, picks a sound, opens chat, unlocks, etc). Decays smoothly
    // back to the resting cosmic state so it never feels jarring.
    private data class Ripple(
        val x: Float, val y: Float,       // 0..1 normalized origin
        val color: Int,
        val startedAt: Float,             // seconds, relative to view clock
        val life: Float = 2.6f,           // seconds to fully fade
    )

    private val ripples = ArrayDeque<Ripple>()
    @Volatile private var pulseEnergy = 0f          // 0..1, decays each frame
    @Volatile private var pulseTint = 0xE91E8C      // current accent tint, blends toward this

    // ── Chameleon theme color — smoothly lerps toward the playing track's hue ──
    @Volatile private var chameleonTarget = 0xE91E8C   // RRGGBB — target color
    @Volatile private var chameleonCurrent = 0xE91E8C  // RRGGBB — interpolated current
    @Volatile private var chameleonSpeed = 0.012f      // lerp factor per frame (~0.7s to blend)

    /** Kinds of user action the background can react to — each gets its own hue & punch. */
    enum class ReactionKind(val tint: Int, val punch: Float) {
        PLAY(0xE91E8C, 0.9f),       // deep pink — starting a soundscape
        PAUSE(0x9C4A7A, 0.45f),     // muted rose — settling down
        SELECT(0xFF80C8, 0.7f),     // soft pink — choosing a track
        CHAT(0xF06292, 0.65f),      // warm pink — talking with Spirit
        UNLOCK(0xFFD27D, 1.0f),     // warm gold — celebratory, the big moment
        TIMER(0x67E8C9, 0.55f),     // teal — calm focus cue
    }

    /**
     * Call from the host screen whenever the user does something noteworthy.
     * Spawns a ripple at an optional normalized origin (defaults to the portal
     * center) and nudges the whole canvas toward that action's accent color
     * for a few seconds — shapes, glows and rings all answer back gently.
     */
    fun react(kind: ReactionKind, originX: Float = 0.5f, originY: Float = 0.42f) {
        val now = (System.nanoTime() - startNanos) / 1_000_000_000.0f
        pulseEnergy = (pulseEnergy + kind.punch).coerceAtMost(1.6f)
        pulseTint = kind.tint
        ripples.addLast(Ripple(originX, originY, kind.tint, now))
        while (ripples.size > 5) ripples.removeFirst()
    }

    /**
     * Smoothly shift the entire background color to match the playing track.
     * The nebula, portal rings and orb tints all lerp to [color] over ~0.7 s.
     * @param color ARGB int (typically from SoundType.themeColor)
     */
    fun setThemeColor(color: Int) {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        chameleonTarget = (r shl 16) or (g shl 8) or b
        // Also use as a pulse-tint so the ripple rings get the new hue
        pulseTint = chameleonTarget
        pulseEnergy = (pulseEnergy + 0.5f).coerceAtMost(1.6f)
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

        // ── Advance chameleon lerp each frame ────────────────────────────
        run {
            val tr = (chameleonTarget shr 16) and 0xFF
            val tg = (chameleonTarget shr 8) and 0xFF
            val tb = chameleonTarget and 0xFF
            val cr = (chameleonCurrent shr 16) and 0xFF
            val cg = (chameleonCurrent shr 8) and 0xFF
            val cb = chameleonCurrent and 0xFF
            val nr = (cr + (tr - cr) * chameleonSpeed).toInt().coerceIn(0, 255)
            val ng = (cg + (tg - cg) * chameleonSpeed).toInt().coerceIn(0, 255)
            val nb = (cb + (tb - cb) * chameleonSpeed).toInt().coerceIn(0, 255)
            chameleonCurrent = (nr shl 16) or (ng shl 8) or nb
        }
        val chR = (chameleonCurrent shr 16) and 0xFF
        val chG = (chameleonCurrent shr 8) and 0xFF
        val chB = chameleonCurrent and 0xFF

        // Decay the reactive "energy" toward rest — exponential ease, frame-rate independent enough at ~60fps
        if (pulseEnergy > 0.001f) pulseEnergy *= 0.965f else pulseEnergy = 0f
        val energy = pulseEnergy.coerceIn(0f, 1f)
        val tintR = (pulseTint shr 16) and 0xFF
        val tintG = (pulseTint shr 8) and 0xFF
        val tintB = pulseTint and 0xFF

        fun blend(base: Int, amount: Float): Int {
            val a = (base shr 24) and 0xFF
            val r = (base shr 16) and 0xFF
            val g = (base shr 8) and 0xFF
            val b = base and 0xFF
            val nr = (r + (tintR - r) * amount).toInt().coerceIn(0, 255)
            val ng = (g + (tintG - g) * amount).toInt().coerceIn(0, 255)
            val nb = (b + (tintB - b) * amount).toInt().coerceIn(0, 255)
            return (a shl 24) or (nr shl 16) or (ng shl 8) or nb
        }

        // ── 1. Nebula blooms — drift in shape AND tint toward the chameleon color ──
        // Build ARGB colors from the current chameleon hue
        val nebula1Color = (0x33 shl 24) or chameleonCurrent
        val nebula2Color = (0x22 shl 24) or (
            ((chR / 2).coerceIn(0,255) shl 16) or
            (((chG + 127) / 2).coerceIn(0,255) shl 8) or
            ((chB / 2 + 127).coerceIn(0,255))
        )
        val shapeWobble = 1f + energy * 0.18f
        drawNebula(
            canvas,
            cx = w * 0.30f + sin(t * 0.04f) * w * (0.05f + energy * 0.04f),
            cy = h * 0.25f + cos(t * 0.05f) * h * 0.03f,
            radius = w * 0.55f * shapeWobble,
            color = blend(nebula1Color, energy * 0.6f),
        )
        drawNebula(
            canvas,
            cx = w * 0.70f + sin(t * 0.035f + 1.5f) * w * (0.05f + energy * 0.05f),
            cy = h * 0.75f + cos(t * 0.045f + 1.0f) * h * 0.03f,
            radius = w * 0.50f * shapeWobble,
            color = blend(nebula2Color, energy * 0.6f),
        )

        // ── 2. Breathing portal rings — flare brighter & quicken with energy ──
        val breathSpeed = 1f + energy * 0.6f
        val breathT = ((t * breathSpeed) % breathPeriodSec) / breathPeriodSec   // 0..1
        val portalCx = w * 0.5f
        val portalCy = h * 0.42f
        val maxRingRadius = min(w, h) * 0.52f * (1f + energy * 0.12f)
        val ringBaseColor = (0x66 shl 24) or chameleonCurrent
        val ringTint = blend(ringBaseColor, energy)
        for (ring in 0 until 3) {
            val phase = ((breathT + ring / 3f) % 1f)
            val radius = maxRingRadius * (0.18f + phase * 0.82f)
            val fade = (1f - phase).coerceIn(0f, 1f)
            portalRingPaint.strokeWidth = (1.2f + (1f - phase) * (1.8f + energy * 2.2f)) * density
            portalRingPaint.color = ringTint
            portalRingPaint.alpha = ((fade * 70) + energy * 90).toInt().coerceAtMost(255)
            canvas.drawCircle(portalCx, portalCy, radius, portalRingPaint)
        }

        // ── 2b. Reactive ripples — soft expanding rings spawned by user actions ──
        val expired = mutableListOf<Ripple>()
        for (r in ripples) {
            val age = t - r.startedAt
            if (age > r.life) { expired.add(r); continue }
            val progress = (age / r.life).coerceIn(0f, 1f)
            val radius = min(w, h) * (0.06f + progress * 0.62f)
            val fade = (1f - progress)
            ripplePaint.color = r.color
            ripplePaint.strokeWidth = (1f + (1f - progress) * 3f) * density
            ripplePaint.alpha = (fade * 130).toInt()
            canvas.drawCircle(r.x * w, r.y * h, radius, ripplePaint)
        }
        ripples.removeAll(expired.toSet())

        // ── 3. Drifting orbs (lotus / moonlight motes) ───────────────────
        for (o in orbs) {
            val px = ((o.x + o.driftX * t) % 1f + 1f) % 1f
            val py = ((o.y + o.driftY * t) % 1f + 1f) % 1f
            val pulse = (sin(t * o.pulseSpeed * 2f * PI.toFloat() + o.pulsePhase) + 1f) / 2f
            val r = (o.size * (0.7f + pulse * 0.3f + energy * 0.25f)) * density
            orbPaint.color = blend(o.color, energy * 0.45f)
            orbPaint.alpha = (60 + pulse * 60 + energy * 50).toInt().coerceAtMost(255)
            canvas.drawCircle(px * w, py * h, r, orbPaint)
        }

        // ── 4. Stars ─────────────────────────────────────────────────────
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
            starPaint.color = if (s.depth == 2) 0xFFFF80C8.toInt() else Color.WHITE
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
