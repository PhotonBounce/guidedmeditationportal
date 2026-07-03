package com.auroramind.meditation

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Share screen — renders a branded "N days free · $X saved" card to a bitmap,
 * shows it, and shares it as a PNG via FileProvider (falling back to text if
 * image generation/sharing fails). A growth surface for the quit-habit loop.
 */
class ShareActivity : AppCompatActivity() {

    private val mp by lazy { resources.displayMetrics.density }
    private fun dp(v: Int) = (v * mp).toInt()

    private var cardUri: Uri? = null
    private lateinit var shareText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        goEdgeToEdge()
        val habit = HabitStatsManager(this)
        val prefs = PrefsManager(this)
        val days = habit.daysClean()
        val saved = habit.moneySaved()
        val habitName = habitDisplay(prefs.getHabitType())
        shareText = "🔥 $days ${if (days == 1) "day" else "days"} free from $habitName with Power of Mind — " +
            "$" + String.format("%.0f", saved) + " saved and counting. Break free, one clean day at a time."

        val cream = ContextCompat.getColor(this, R.color.text_primary)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_cosmic_gradient)
            setPadding(dp(24), dp(44), dp(24), dp(28))
            gravity = Gravity.CENTER_HORIZONTAL
        }
        root.addView(TextView(this).apply {
            text = "Share your progress"; setTextColor(cream); textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        })

        val card = runCatching { buildCard(days, saved, habitName) }.getOrNull()
        root.addView(ImageView(this).apply {
            if (card != null) setImageBitmap(card)
            adjustViewBounds = true
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(20) }
        })
        if (card != null) cardUri = runCatching { saveCard(card) }.getOrNull()

        root.addView(TextView(this).apply {
            text = "Share ▸"; gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#1A120B")); textSize = 16f; setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE; cornerRadius = dp(26).toFloat()
                setColor(Color.parseColor("#FFC95E"))
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply { topMargin = dp(22) }
            isClickable = true; isFocusable = true
            setOnClickListener { share() }
        })
        setContentView(root)
        // The gradient background still fills the padded area, so it stays full-bleed.
        root.padSystemBars()
    }

    private fun share() {
        val intent = Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_TEXT, shareText) }
        val uri = cardUri
        if (uri != null) {
            intent.type = "image/png"
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            intent.type = "text/plain"
        }
        runCatching { startActivity(Intent.createChooser(intent, "Share your progress")) }
    }

    private fun buildCard(days: Int, saved: Float, habitName: String): Bitmap {
        val w = 1080; val h = 1080
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
                intArrayOf(0xFF2A1C0E.toInt(), 0xFF19110A.toInt()), null, Shader.TileMode.CLAMP)
        })
        c.drawRoundRect(24f, 24f, w - 24f, h - 24f, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 6f; color = Color.parseColor("#FFC95E")
        })
        runCatching {
            BitmapFactory.decodeResource(resources, R.drawable.appicon)?.let { logo ->
                val size = 200
                c.drawBitmap(logo, null, RectF((w - size) / 2f, 150f, (w + size) / 2f, 150f + size), Paint(Paint.FILTER_BITMAP_FLAG))
            }
        }
        val center = w / 2f
        c.drawText(days.toString(), center, 630f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFC95E"); textAlign = Paint.Align.CENTER
            textSize = 300f; typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        })
        c.drawText("DAYS FREE", center, 720f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5EEE6"); textAlign = Paint.Align.CENTER
            textSize = 64f; letterSpacing = 0.2f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        })
        c.drawText("$" + String.format("%.0f", saved) + " saved · free from $habitName", center, 810f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBB89F"); textAlign = Paint.Align.CENTER; textSize = 44f
        })
        c.drawText("Power of Mind", center, 930f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFC95E"); textAlign = Paint.Align.CENTER; textSize = 50f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        })
        return bmp
    }

    private fun saveCard(bmp: Bitmap): Uri {
        val dir = File(cacheDir, "shared").apply { mkdirs() }
        val file = File(dir, "progress.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    }

    private fun habitDisplay(key: String): String = when (key) {
        "vaping" -> "vaping"
        "smoking" -> "smoking"
        "social_media" -> "social media"
        "doomscrolling" -> "doomscrolling"
        "alcohol" -> "alcohol"
        else -> "the habit"
    }
}
