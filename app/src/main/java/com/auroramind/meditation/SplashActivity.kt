package com.auroramind.meditation

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.os.*
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.*

/**
 * SplashActivity — the "sick opener" the user asked for.
 *
 * Renders a full-screen procedural 3D animation directly onto a Canvas View:
 *  • 3-layer parallax star field (depth-based drift on each frame)
 *  • Rotating 3D wireframe icosphere (perspective-projected onto 2D canvas)
 *  • Expanding "portal" pulse rings driven by a breath timer
 *  • Soft drifting neon orbs
 *  • App name reveal with letter-by-letter glow fade-in
 *  • Tagline fade-in after the title
 *
 * Plays a short synthesised chime via AudioEngine, then auto-advances to
 * MainActivity after SPLASH_DURATION_MS milliseconds.
 *
 * No images, no shaders beyond RadialGradient — pure Canvas.
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION_MS = 3200L
        private const val TITLE_APPEAR_MS    = 600L
        private const val TAGLINE_APPEAR_MS  = 1400L
    }

    private lateinit var canvas: SplashCanvas
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Full-screen immersive: no status bar, no nav bar
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        canvas = SplashCanvas(this)
        setContentView(canvas)

        // Play chime sound effect
        try { SoundEffects(this).chime() } catch (_: Exception) {}

        // Advance after the animation finishes — into the quit-habit + affirmations
        // flow: the onboarding quiz on first run, the dashboard on later launches.
        handler.postDelayed({
            val next = if (PrefsManager(this).isQuizCompleted())
                DashboardActivity::class.java else QuizActivity::class.java
            startActivity(Intent(this, next))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }, SPLASH_DURATION_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        canvas.stop()
        super.onDestroy()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SplashCanvas — the rendering heart of the splash screen
// ─────────────────────────────────────────────────────────────────────────────

private class SplashCanvas(context: Context) : View(context) {

    private val startNanos = System.nanoTime()
    private val handler    = Handler(Looper.getMainLooper())
    private val frameRunner = object : Runnable {
        override fun run() { invalidate(); handler.postDelayed(this, 16) }
    }

    // ── Colors ───────────────────────────────────────────────────────────────
    private val PINK   = Color.parseColor("#E91E8C")
    private val PINK_L = Color.parseColor("#FF80C8")
    private val TEAL   = Color.parseColor("#5EEAD4")
    private val DARK   = Color.parseColor("#0B0B1E")

    // ── Paints ───────────────────────────────────────────────────────────────
    private val bgPaint  = Paint().apply { color = DARK }
    private val wirePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 1.4f
        strokeCap   = Paint.Cap.ROUND
        color       = Color.argb(160, 233, 30, 140)   // translucent pink
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.NORMAL)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.create("sans-serif-light", Typeface.BOLD)
        letterSpacing = 0.1f
    }
    private val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface  = Typeface.create("sans-serif-light", Typeface.NORMAL)
        letterSpacing = 0.15f
    }
    private val nebulaPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── App logo (the hero of the splash) ─────────────────────────────────────
    private val logo = BitmapFactory.decodeResource(resources, R.drawable.appicon)
    private val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val logoGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        maskFilter = BlurMaskFilter(34f, BlurMaskFilter.Blur.NORMAL)
    }
    private val clipPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ── 3-D Icosphere wireframe ───────────────────────────────────────────────
    // Generated as a lat/lon sphere (8 lat lines × 16 lon lines = 128 points)
    private data class V3(val x: Float, val y: Float, val z: Float)

    private val sphereVerts: List<V3>
    private val sphereEdgesLat: List<Pair<Int, Int>>   // along latitude
    private val sphereEdgesLon: List<Pair<Int, Int>>   // along longitude

    init {
        val latSteps = 8
        val lonSteps = 16
        val verts = mutableListOf<V3>()
        // top pole
        verts.add(V3(0f, 1f, 0f))
        for (lat in 1 until latSteps) {
            val phi = PI.toFloat() * lat / latSteps        // 0..π
            val y   = cos(phi)
            val r   = sin(phi)
            for (lon in 0 until lonSteps) {
                val theta = 2f * PI.toFloat() * lon / lonSteps
                verts.add(V3(r * cos(theta), y, r * sin(theta)))
            }
        }
        // bottom pole
        verts.add(V3(0f, -1f, 0f))
        sphereVerts = verts

        // latitude ring edges (connect consecutive verts in the same lat band)
        val latEdges = mutableListOf<Pair<Int, Int>>()
        for (lat in 1 until latSteps) {
            val base = 1 + (lat - 1) * lonSteps
            for (lon in 0 until lonSteps) {
                latEdges.add(base + lon to base + (lon + 1) % lonSteps)
            }
        }
        sphereEdgesLat = latEdges

        // longitude column edges (connect across lat bands)
        val lonEdges = mutableListOf<Pair<Int, Int>>()
        // top pole → first ring
        for (lon in 0 until lonSteps) lonEdges.add(0 to 1 + lon)
        // ring-to-ring
        for (lat in 1 until latSteps - 1) {
            val base  = 1 + (lat - 1) * lonSteps
            val base2 = base + lonSteps
            for (lon in 0 until lonSteps) lonEdges.add(base + lon to base2 + lon)
        }
        // last ring → bottom pole
        val lastBase = 1 + (latSteps - 2) * lonSteps
        for (lon in 0 until lonSteps) lonEdges.add(lastBase + lon to sphereVerts.lastIndex)
        sphereEdgesLon = lonEdges
    }

    // ── Parallax star field (3 depth layers) ─────────────────────────────────
    private data class Star(
        val nx: Float, val ny: Float,
        val size: Float,
        val alpha: Float,
        val depth: Int,       // 0 = far, 2 = near
        val twinklePhase: Float,
        val twinkleHz: Float,
    )

    private val stars: List<Star> = buildList {
        val rng = kotlin.random.Random(42)
        repeat(50) { add(Star(rng.nextFloat(), rng.nextFloat(), 0.9f + rng.nextFloat() * 0.5f, 0.5f + rng.nextFloat() * 0.4f, 0, rng.nextFloat() * 6.28f, 0.4f + rng.nextFloat() * 0.8f)) }
        repeat(20) { add(Star(rng.nextFloat(), rng.nextFloat(), 1.5f + rng.nextFloat() * 0.8f, 0.7f + rng.nextFloat() * 0.3f, 1, rng.nextFloat() * 6.28f, 0.5f + rng.nextFloat() * 1.0f)) }
        repeat(10) { add(Star(rng.nextFloat(), rng.nextFloat(), 2.0f + rng.nextFloat() * 1.0f, 0.9f + rng.nextFloat() * 0.1f, 2, rng.nextFloat() * 6.28f, 0.3f + rng.nextFloat() * 0.6f)) }
    }

    // ── Drifting orbs ─────────────────────────────────────────────────────────
    private data class Orb(val nx: Float, val ny: Float, val r: Float, val color: Int, val dx: Float, val dy: Float, val phase: Float, val hz: Float)
    private val orbs: List<Orb> = buildList {
        val rng = kotlin.random.Random(7)
        val colors = listOf(
            Color.argb(50, 233, 30, 140),
            Color.argb(40, 255, 128, 200),
            Color.argb(35, 94, 234, 212),
            Color.argb(45, 255, 215, 0),
        )
        repeat(6) { i ->
            add(Orb(rng.nextFloat(), rng.nextFloat(),
                30f + rng.nextFloat() * 50f,
                colors[i % colors.size],
                (rng.nextFloat() - 0.5f) * 0.025f,
                (rng.nextFloat() - 0.5f) * 0.025f,
                rng.nextFloat() * 6.28f,
                0.1f + rng.nextFloat() * 0.2f))
        }
    }

    // ── Animation state ───────────────────────────────────────────────────────
    private var titleAlpha  = 0f
    private var taglineAlpha = 0f
    private var titleAnimator: ValueAnimator? = null
    private var taglineAnimator: ValueAnimator? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(frameRunner)

        // Title fade-in after TITLE_APPEAR_MS
        handler.postDelayed({
            titleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 900
                interpolator = DecelerateInterpolator(1.5f)
                addUpdateListener { titleAlpha = it.animatedValue as Float }
                start()
            }
        }, 600)

        // Tagline fade-in
        handler.postDelayed({
            taglineAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 700
                interpolator = DecelerateInterpolator()
                addUpdateListener { taglineAlpha = it.animatedValue as Float }
                start()
            }
        }, 1400)
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        titleAnimator?.cancel()
        taglineAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat();  val h = height.toFloat()
        if (w == 0f || h == 0f) return

        val t = (System.nanoTime() - startNanos) / 1_000_000_000.0f
        val d = context.resources.displayMetrics.density

        // ── Background gradient ───────────────────────────────────────────
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        nebulaPaint.shader = RadialGradient(
            w * 0.5f, h * 0.45f, h * 0.65f,
            Color.argb(55, 233, 30, 140),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, w, h, nebulaPaint)

        // ── Drifting orbs ─────────────────────────────────────────────────
        for (orb in orbs) {
            val px = ((orb.nx + orb.dx * t) % 1f + 1f) % 1f * w
            val py = ((orb.ny + orb.dy * t) % 1f + 1f) % 1f * h
            val pulse = (sin(t * orb.hz * 2f * PI.toFloat() + orb.phase) + 1f) / 2f
            glowPaint.color = orb.color
            glowPaint.alpha = ((Color.alpha(orb.color)) * (0.6f + pulse * 0.4f)).toInt()
            canvas.drawCircle(px, py, orb.r * d * (0.8f + pulse * 0.2f), glowPaint)
        }

        // ── Stars with parallax ───────────────────────────────────────────
        for (s in stars) {
            val speed = when (s.depth) { 0 -> 0.004f; 1 -> 0.009f; else -> 0.016f }
            val px = ((s.nx + speed * t) % 1f + 1f) % 1f * w
            val py = ((s.ny + speed * t * 0.6f) % 1f + 1f) % 1f * h
            val twinkle = (sin(t * s.twinkleHz * 2f * PI.toFloat() + s.twinklePhase) + 1f) / 2f
            val alpha = s.alpha * (0.5f + twinkle * 0.5f)
            if (s.depth == 2) {
                glowPaint.color = Color.argb((alpha * 60).toInt(), 255, 128, 200)
                canvas.drawCircle(px, py, s.size * d * 3f, glowPaint)
            }
            starPaint.alpha = (alpha * 255).toInt()
            canvas.drawCircle(px, py, s.size * d, starPaint)
        }

        // ── Portal breathing rings ────────────────────────────────────────
        val cx = w * 0.5f;  val cy = h * 0.45f
        val maxR = min(w, h) * 0.48f
        val breathT = (t % 8f) / 8f
        for (ring in 0..2) {
            val phase = ((breathT + ring / 3f) % 1f)
            val r = maxR * (0.15f + phase * 0.85f)
            val fade = 1f - phase
            ringPaint.strokeWidth = (1f + (1f - phase) * 2.5f) * d
            ringPaint.color = Color.argb(
                ((fade * 80)).toInt().coerceIn(0, 255),
                233, 30, 140
            )
            canvas.drawCircle(cx, cy, r, ringPaint)
        }

        // ── 3D wireframe sphere ───────────────────────────────────────────
        val rotX = t * 0.4f         // slow x-axis tumble
        val rotY = t * 0.7f         // faster y-axis spin
        val rotZ = t * 0.15f        // gentle z-axis roll
        val sphereRadius = min(w, h) * 0.28f
        val fov = 3.5f              // perspective strength

        // project a 3D vertex to screen 2D
        fun project(v: V3): PointF? {
            // Rotate around Y
            val x1 = v.x * cos(rotY) + v.z * sin(rotY)
            val z1 = -v.x * sin(rotY) + v.z * cos(rotY)
            val y1 = v.y
            // Rotate around X
            val y2 = y1 * cos(rotX) - z1 * sin(rotX)
            val z2 = y1 * sin(rotX) + z1 * cos(rotX)
            val x2 = x1
            // Rotate around Z
            val x3 = x2 * cos(rotZ) - y2 * sin(rotZ)
            val y3 = x2 * sin(rotZ) + y2 * cos(rotZ)
            val z3 = z2 + fov       // push sphere back on z

            if (z3 <= 0f) return null
            val scale = fov / z3
            return PointF(cx + x3 * scale * sphereRadius, cy + y3 * scale * sphereRadius)
        }

        val projected = sphereVerts.map { project(it) }

        // Draw latitude edges
        for ((a, b) in sphereEdgesLat) {
            val pa = projected[a] ?: continue
            val pb = projected[b] ?: continue
            wirePaint.color = Color.argb(140, 233, 30, 140)
            canvas.drawLine(pa.x, pa.y, pb.x, pb.y, wirePaint)
        }
        // Draw longitude edges (slightly brighter)
        for ((a, b) in sphereEdgesLon) {
            val pa = projected[a] ?: continue
            val pb = projected[b] ?: continue
            wirePaint.color = Color.argb(100, 255, 128, 200)
            canvas.drawLine(pa.x, pa.y, pb.x, pb.y, wirePaint)
        }

        // ── Hero logo — pops in big & glowing, framed by the rotating wireframe ──
        run {
            val pop  = (t / 0.8f).coerceIn(0f, 1f)
            val ease = 1f - (1f - pop) * (1f - pop)                 // ease-out
            val breathe = sin(t * 0.9f) * 0.018f                    // gentle life
            val logoScale = 0.72f + 0.28f * ease + breathe
            val logoR = sphereRadius * 1.02f * logoScale

            // Soft pink glow halo behind the logo (pulses with the breath)
            val glowA = (90 + (sin(t * 0.9f) + 1f) * 35f).toInt().coerceIn(0, 200)
            logoGlowPaint.color = Color.argb((glowA * ease).toInt(), 233, 30, 140)
            canvas.drawCircle(cx, cy, logoR * 1.06f, logoGlowPaint)

            // Clip to a circle and draw the app icon filling it
            val save = canvas.save()
            val clip = Path().apply { addCircle(cx, cy, logoR, Path.Direction.CW) }
            canvas.clipPath(clip)
            val dst = RectF(cx - logoR, cy - logoR, cx + logoR, cy + logoR)
            logoPaint.alpha = (255 * ease).toInt()
            canvas.drawBitmap(logo, null, dst, logoPaint)
            canvas.restoreToCount(save)

            // Crisp pink ring framing the logo
            ringPaint.strokeWidth = 2.5f * d
            ringPaint.color = Color.argb((200 * ease).toInt(), 255, 128, 200)
            canvas.drawCircle(cx, cy, logoR, ringPaint)
        }

        // ── Title: "Guided Meditation Portal" ─────────────────────────────
        if (titleAlpha > 0f) {
            val titleY = cy + sphereRadius + 90f * d
            val titleSize = (20f * d).coerceAtLeast(18f)
            textPaint.textSize = titleSize
            textPaint.color    = Color.argb((titleAlpha * 255).toInt(), 248, 250, 252)
            // Glow under
            glowPaint.color = Color.argb((titleAlpha * 100).toInt(), 233, 30, 140)
            glowPaint.textSize = titleSize
            glowPaint.typeface = textPaint.typeface
            glowPaint.textAlign = Paint.Align.CENTER
            glowPaint.letterSpacing = textPaint.letterSpacing
            canvas.drawText("MIND & BODY SHIELD", cx, titleY, glowPaint)
            canvas.drawText("MIND & BODY SHIELD", cx, titleY, textPaint)
        }

        // ── Tagline ───────────────────────────────────────────────────────
        if (taglineAlpha > 0f) {
            val tagY = cy + sphereRadius + 118f * d
            tagPaint.textSize = (12f * d).coerceAtLeast(11f)
            tagPaint.color    = Color.argb((taglineAlpha * 180).toInt(), 255, 128, 200)
            canvas.drawText("Breathe  ·  Reflect  ·  Restore", cx, tagY, tagPaint)
        }
    }
}
