package com.auroramind.meditation

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Full-screen milestone celebration: a gold particle burst behind the day
 * count and a themed message, shown when the user crosses a clean-time
 * milestone (launched from [DashboardActivity]). Pure Canvas — no assets.
 */
class MilestoneActivity : AppCompatActivity() {

    private lateinit var particles: ParticleView
    private var days = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        days = intent.getIntExtra(EXTRA_DAYS, 1)

        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#140d08"))
            layoutParams = ViewGroup.LayoutParams(MATCH, MATCH)
        }
        particles = ParticleView(this)
        root.addView(particles, FrameLayout.LayoutParams(MATCH, MATCH))

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(40))
            layoutParams = FrameLayout.LayoutParams(MATCH, MATCH)
        }

        col.addView(TextView(this).apply {
            text = "🏆"
            textSize = 52f
            gravity = Gravity.CENTER
        })
        col.addView(TextView(this).apply {
            text = days.toString()
            setTextColor(Color.parseColor("#FFC95E"))
            textSize = 88f
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        col.addView(TextView(this).apply {
            text = if (days == 1) "DAY FREE" else "DAYS FREE"
            setTextColor(Color.parseColor("#F5EEE6"))
            textSize = 16f
            letterSpacing = 0.18f
            gravity = Gravity.CENTER
        })
        col.addView(TextView(this).apply {
            text = headline(days)
            setTextColor(Color.parseColor("#cbb89f"))
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(28))
        })

        col.addView(MaterialButton(this).apply {
            text = "Share my win"
            setTextColor(Color.parseColor("#241400"))
            backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFC95E"))
            cornerRadius = dp(26)
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(52))
            setOnClickListener { shareWin() }
        })
        col.addView(MaterialButton(this).apply {
            text = "Keep going"
            setTextColor(Color.parseColor("#FFC95E"))
            backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(dp(240), dp(48)).apply { topMargin = dp(8) }
            setOnClickListener { finish() }
        })

        root.addView(col)
        setContentView(root)
    }

    private fun headline(d: Int): String = when {
        d >= 365 -> "One year free. You rewrote the story."
        d >= 180 -> "Half a year strong."
        d >= 90 -> "90 days — a different person."
        d >= 30 -> "A full month free!"
        d >= 14 -> "Two weeks strong!"
        d >= 7 -> "One week free — momentum is yours."
        d >= 3 -> "Three days. The fog is lifting."
        else -> "Day one. The hardest step, taken."
    }

    private fun shareWin() {
        val habit = PrefsManager(this).getHabitType().ifBlank { "the habit" }.replace('_', ' ')
        val text = "🏆 $days ${if (days == 1) "day" else "days"} free from $habit with Power of Mind. " +
            "Breaking free, one clean day at a time."
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Share your milestone"
            )
        )
    }

    override fun onDestroy() {
        particles.stop()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_DAYS = "days"
        private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    }

    /** Confetti/spark burst radiating from the upper-centre, with gravity. */
    private class ParticleView(context: android.content.Context) : View(context) {
        private data class P(
            var x: Float, var y: Float, var vx: Float, var vy: Float,
            var life: Float, val color: Int, val size: Float
        )

        private val colors = intArrayOf(
            Color.parseColor("#FFC95E"), Color.parseColor("#FFB347"),
            Color.parseColor("#E8742B"), Color.parseColor("#FFE6A8")
        )
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val parts = ArrayList<P>()
        private val handler = Handler(Looper.getMainLooper())
        private var lastBurst = 0L
        private val rnd = Random(System.nanoTime())
        private val frame = object : Runnable {
            override fun run() { invalidate(); handler.postDelayed(this, 16) }
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            handler.post(frame)
        }

        override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }

        fun stop() { handler.removeCallbacksAndMessages(null) }

        private fun burst() {
            val cx = width * 0.5f
            val cy = height * 0.42f
            repeat(70) {
                val ang = rnd.nextFloat() * 6.2832f
                val spd = 4f + rnd.nextFloat() * 11f
                parts.add(
                    P(
                        cx, cy,
                        cos(ang) * spd, sin(ang) * spd - 3f,
                        1f, colors[rnd.nextInt(colors.size)],
                        3f + rnd.nextFloat() * 4f
                    )
                )
            }
        }

        override fun onDraw(canvas: Canvas) {
            if (width == 0) return
            val now = System.currentTimeMillis()
            if (parts.isEmpty() || now - lastBurst > 1400) { burst(); lastBurst = now }
            val it = parts.iterator()
            while (it.hasNext()) {
                val p = it.next()
                p.x += p.vx; p.y += p.vy; p.vy += 0.18f; p.life -= 0.012f
                if (p.life <= 0f) { it.remove(); continue }
                paint.color = p.color
                paint.alpha = (p.life.coerceIn(0f, 1f) * 255).toInt()
                canvas.drawCircle(p.x, p.y, p.size, paint)
            }
        }
    }
}
