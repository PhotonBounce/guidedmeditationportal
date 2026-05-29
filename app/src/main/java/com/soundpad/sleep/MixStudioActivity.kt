package com.soundpad.sleep

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.soundpad.sleep.databinding.ActivityMixStudioBinding
import com.soundpad.sleep.databinding.ItemMixLayerBinding

/**
 * Mix Studio — layer up to 3 sounds simultaneously.
 *
 * Architecture:
 *  - Maintains a local list of LayerData (type + volume) for UI state
 *  - Binds to SoundService for actual audio playback
 *  - Preset chips offer curated sound combinations
 *  - Sound picker uses a flat AlertDialog list (all sounds, excluding already-added)
 */
class MixStudioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMixStudioBinding
    private lateinit var prefs: PrefsManager

    private var service: SoundService? = null
    private var bound = false
    private var pendingApply = false

    private data class LayerData(val type: SoundType, var vol: Float)

    private val layers = mutableListOf<LayerData>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            service = (binder as SoundService.SoundBinder).service()
            bound = true
            if (pendingApply) {
                pendingApply = false
                doStartMix()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            bound = false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMixStudioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.header.updatePadding(top = bars.top + 8)
            insets
        }

        // Default starter layer
        if (layers.isEmpty()) layers.add(LayerData(SoundType.BROWN_NOISE, 0.7f))
        prefs = PrefsManager(this)

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnAddLayer.setOnClickListener { showSoundPicker() }
        binding.btnPlay.setOnClickListener { applyMix() }
        binding.btnStop.setOnClickListener { stopMix() }
        binding.btnSaveMix.setOnClickListener { saveMixDialog() }
        binding.btnLoadMix.setOnClickListener { showSavedMixes() }

        setupPresetChips()
        refreshLayerViews()
    }

    override fun onStart() {
        super.onStart()
        if (SoundService.isRunning) {
            bindService(
                Intent(this, SoundService::class.java),
                connection,
                Context.BIND_AUTO_CREATE
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (bound) {
            unbindService(connection)
            bound = false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Preset chips
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupPresetChips() {
        binding.chipStorm.setOnClickListener {
            layers.setPreset(
                SoundType.RAIN    to 0.75f,
                SoundType.THUNDER to 0.50f,
                SoundType.BROWN_NOISE to 0.35f
            )
            refreshLayerViews()
        }
        binding.chipFocus.setOnClickListener {
            layers.setPreset(
                SoundType.BROWN_NOISE to 0.70f,
                SoundType.FAN         to 0.45f
            )
            refreshLayerViews()
        }
        binding.chipForest.setOnClickListener {
            layers.setPreset(
                SoundType.WIND       to 0.70f,
                SoundType.RAIN       to 0.50f,
                SoundType.PINK_NOISE to 0.25f
            )
            refreshLayerViews()
        }
        binding.chipBaby.setOnClickListener {
            layers.setPreset(
                SoundType.WOMB        to 0.80f,
                SoundType.WHITE_NOISE to 0.40f
            )
            refreshLayerViews()
        }
        binding.chipCampfire.setOnClickListener {
            layers.setPreset(
                SoundType.FIRE        to 0.75f,
                SoundType.WIND        to 0.50f,
                SoundType.BROWN_NOISE to 0.30f
            )
            refreshLayerViews()
        }
        binding.chipOffice.setOnClickListener {
            layers.setPreset(
                SoundType.FAN         to 0.60f,
                SoundType.WHITE_NOISE to 0.40f
            )
            refreshLayerViews()
        }
        binding.chipSpa.setOnClickListener {
            layers.setPreset(
                SoundType.CRYSTAL to 0.65f,
                SoundType.OCEAN   to 0.45f
            )
            refreshLayerViews()
        }
        binding.chipShuffle.setOnClickListener {
            shufflePreset()
        }
    }

    private fun MutableList<LayerData>.setPreset(vararg pairs: Pair<SoundType, Float>) {
        clear()
        pairs.forEach { (type, vol) -> add(LayerData(type, vol)) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sound picker
    // ─────────────────────────────────────────────────────────────────────────

    private fun showSoundPicker() {
        if (layers.size >= 3) {
            android.widget.Toast.makeText(
                this, "Maximum 3 layers — remove one first", android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        val used = layers.map { it.type }.toSet()
        val available = SoundType.values().filter { it !in used }
        val labels = available.map { "${it.emoji}  ${it.displayName}  •  ${it.category}" }
            .toTypedArray()

        MaterialAlertDialogBuilder(this, R.style.AlertDialogDark)
            .setTitle("Add Sound Layer")
            .setItems(labels) { _, idx ->
                layers.add(LayerData(available[idx], 0.7f))
                refreshLayerViews()
            }
            .show()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Layer views
    // ─────────────────────────────────────────────────────────────────────────

    private fun refreshLayerViews() {
        binding.containerLayers.removeAllViews()

        layers.forEachIndexed { i, layer ->
            val b = ItemMixLayerBinding.inflate(layoutInflater, binding.containerLayers, true)

            b.tvLayerEmoji.text = layer.type.emoji
            b.tvLayerName.text = layer.type.displayName
            b.tvLayerCategory.text = layer.type.category

            // Category accent stripe colour
            val catColorRes = when (layer.type.category) {
                "Noise"         -> R.color.cat_noise
                "Nature"        -> R.color.cat_nature
                "Mechanical"    -> R.color.cat_mechanical
                "Synthetic"     -> R.color.cat_synthetic
                "Ambient Music" -> R.color.cat_ambient
                "Energy Music"  -> R.color.cat_energy
                else            -> R.color.accent_iris
            }
            b.vCatStripe.setBackgroundColor(getColor(catColorRes))

            b.sliderLayerVol.value = layer.vol
            b.tvVolPct.text = "${(layer.vol * 100).toInt()}%"

            b.sliderLayerVol.addOnChangeListener { _, value, _ ->
                layers[i].vol = value
                b.tvVolPct.text = "${(value * 100).toInt()}%"
                service?.setMixLayerVolume(layer.type, value)
            }

            b.btnRemoveLayer.setOnClickListener {
                layers.removeAt(i)
                refreshLayerViews()
            }
        }

        val canAdd = layers.size < 3
        binding.btnAddLayer.isEnabled = canAdd
        binding.btnAddLayer.alpha = if (canAdd) 1f else 0.4f
        binding.tvLayerCount.text = "${layers.size} / 3 layers"
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Playback control
    // ─────────────────────────────────────────────────────────────────────────

    private fun applyMix() {
        if (layers.isEmpty()) return

        // Start / wake service if needed
        val intent = Intent(this, SoundService::class.java).apply {
            putExtra("SOUND_TYPE", layers[0].type.name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent)
        else startService(intent)

        if (bound) {
            doStartMix()
        } else {
            pendingApply = true
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun doStartMix() {
        service?.startMix(layers.map { it.type to it.vol })
        val names = layers.joinToString(" + ") { "${it.type.emoji} ${it.type.displayName}" }
        binding.tvStatus.text = "▶ $names"
        binding.tvStatus.setTextColor(getColor(R.color.cat_nature))
    }

    private fun stopMix() {
        service?.stopMix()
        binding.tvStatus.text = "⏹ Stopped"
        binding.tvStatus.setTextColor(getColor(R.color.text_secondary))
    }

    // ───────────────────────────────────────────────────────────────────────────
    // Shuffle / Save / Load
    // ───────────────────────────────────────────────────────────────────────────

    private fun shufflePreset() {
        // Pick from synthesised sounds only (no file-backed ambient) for reliability
        val pool = SoundType.values().filter { it.rawResId == 0 }
        val count = listOf(2, 2, 3).random()   // weighted: 2/3 chance of 2 layers
        val defaultVols = listOf(0.70f, 0.60f, 0.50f)
        val picked = pool.shuffled().take(count)
        layers.setPreset(*picked.mapIndexed { i, t -> t to defaultVols[i] }.toTypedArray())
        refreshLayerViews()
        Toast.makeText(this, "🎲 Shuffled!", Toast.LENGTH_SHORT).show()
    }

    private fun saveMixDialog() {
        if (layers.isEmpty()) {
            Toast.makeText(this, "Add sounds to your mix first", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this).apply {
            hint = "e.g. \"Deep Work\""
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
            setPadding(64, 24, 64, 8)
        }
        MaterialAlertDialogBuilder(this, R.style.AlertDialogDark)
            .setTitle("💾 Save Mix")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isBlank()) {
                    Toast.makeText(this, "Name can't be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                prefs.saveMix(name, layers.map { it.type to it.vol })
                Toast.makeText(this, "'$name' saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showSavedMixes() {
        val saved = prefs.getSavedMixes()
        if (saved.isEmpty()) {
            Toast.makeText(this, "No saved mixes yet — build one and tap Save", Toast.LENGTH_SHORT).show()
            return
        }
        val names = saved.keys.sorted().toTypedArray()
        MaterialAlertDialogBuilder(this, R.style.AlertDialogDark)
            .setTitle("📂 My Mixes")
            .setItems(names) { _, idx ->
                val mixLayers = saved[names[idx]] ?: return@setItems
                layers.setPreset(*mixLayers.toTypedArray())
                refreshLayerViews()
                Toast.makeText(this, "Loaded \"${names[idx]}\"", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Delete...") { _, _ -> deleteSavedMixDialog(names, saved) }
            .show()
    }

    private fun deleteSavedMixDialog(
        names: Array<String>,
        saved: Map<String, List<Pair<SoundType, Float>>>
    ) {
        MaterialAlertDialogBuilder(this, R.style.AlertDialogDark)
            .setTitle("Delete Mix")
            .setItems(names) { _, idx ->
                prefs.deleteMix(names[idx])
                Toast.makeText(this, "\"${names[idx]}\" deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
