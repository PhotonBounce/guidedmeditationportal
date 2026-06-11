package com.auroramind.meditation

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.auroramind.meditation.databinding.ActivityAiChatBinding
import com.auroramind.meditation.databinding.ItemChatMessageBinding

/**
 * AiChatActivity — Spirit, your meditation guide and companion.
 *
 * Futuristic chat interface with:
 *  - On-device meditation guide (AiChatEngine) with technique guidance
 *  - Quick-action chips for Sleep / Focus / Relax / Alarm / VIP
 *  - Typing indicator with simulated "thinking" delay for realistic UX
 *  - Sound suggestion chips that launch the recommended sound directly
 */
class AiChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiChatBinding
    private lateinit var engine: AiChatEngine
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var sfx: SoundEffects
    private lateinit var haptic: HapticHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sfx = SoundEffects(this)
        haptic = HapticHelper(this)

        // Edge-to-edge insets — push header below status bar, bottom nav above nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.header.updatePadding(top = bars.top + 8)
            binding.bottomNavigation.updatePadding(bottom = bars.bottom)
            insets
        }

        engine = AiChatEngine(this)
        chatAdapter = ChatAdapter(messages) { sound -> launchSound(sound) }

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@AiChatActivity).also { it.stackFromEnd = true }
            adapter = chatAdapter
        }

        binding.btnBack.setOnClickListener {
            haptic.tick()
            sfx.tap()
            onBackPressedDispatcher.onBackPressed()
        }
        setupInput()
        setupChips()
        setupBottomNavigation()

        // Slight delay makes the welcome message feel like ARIA is "waking up"
        handler.postDelayed({
            val welcome = engine.addWelcome()
            addMessage(welcome)
        }, 700)
    }

    private fun setupInput() {
        binding.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                haptic.click()
                sfx.tap()
                sendMessage()
                true
            } else false
        }
        binding.btnSend.setOnClickListener {
            haptic.click()
            sfx.tap()
            sendMessage()
        }
        binding.etInput.doOnTextChanged { text, _, _, _ ->
            binding.btnSend.isEnabled = !text.isNullOrBlank()
        }
    }

    private fun setupChips() {
        binding.chipGroupQuick.removeAllViews()

        // Alarm Clock gets its own chip that launches directly (not through chat)
        Chip(this).apply {
            text = "⏰ Alarm"
            isCheckable = false
            setTextColor(getColor(R.color.cat_energy))
            chipBackgroundColor = ColorStateList.valueOf(getColor(R.color.card_bg))
            chipStrokeColor = ColorStateList.valueOf(getColor(R.color.cat_energy))
            chipStrokeWidth = 1.5f
            textSize = 12f
            setOnClickListener {
                haptic.tick()
                sfx.tap()
                val intent = Intent(this@AiChatActivity, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            }
        }.also { binding.chipGroupQuick.addView(it) }

        // Regular chat chips
        listOf(
            "😴 Sleep"  to "I need help sleeping",
            "🧠 Focus"  to "Help me focus and concentrate",
            "🌿 Relax"  to "I need to relax and de-stress",
            "🌟 VIP"    to "Tell me about VIP features"
        ).forEach { (label, prompt) ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = false
                setTextColor(getColor(R.color.accent_iris))
                chipBackgroundColor = ColorStateList.valueOf(getColor(R.color.card_bg))
                chipStrokeColor = ColorStateList.valueOf(getColor(R.color.card_border))
                chipStrokeWidth = 1.5f
                textSize = 12f
                setOnClickListener {
                    haptic.tick()
                    sfx.tap()
                    binding.etInput.setText(prompt)
                    sendMessage()
                }
            }
            binding.chipGroupQuick.addView(chip)
        }
    }

    private fun sendMessage() {
        val text = binding.etInput.text?.toString()?.trim() ?: return
        if (text.isBlank()) return
        binding.etInput.text?.clear()
        binding.nightSky.react(NightSkyView.ReactionKind.CHAT, originY = 0.3f)
        addMessage(ChatMessage(text, isUser = true))
        showTyping(true)
        val delay = (800L..1800L).random()
        handler.postDelayed({
            showTyping(false)
            addMessage(engine.respond(text))
        }, delay)
    }

    private fun addMessage(msg: ChatMessage) {
        messages.add(msg)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.rvMessages.post {
            binding.rvMessages.smoothScrollToPosition(messages.size - 1)
        }
    }

    private fun showTyping(show: Boolean) {
        binding.tvTyping.visibility = if (show) View.VISIBLE else View.GONE
        if (show) binding.rvMessages.post {
            binding.rvMessages.smoothScrollToPosition(
                (chatAdapter.itemCount - 1).coerceAtLeast(0)
            )
        }
    }

    /**
     * Launch a sound suggested by Spirit.
     *
     * Starts SoundService IMMEDIATELY (so audio begins before the transition
     * animation even finishes), then brings MainActivity to front with the
     * LAUNCH_SOUND extra so the UI can sync to the already-playing track.
     *
     * Using FLAG_ACTIVITY_CLEAR_TOP | SINGLE_TOP delivers onNewIntent() to
     * the existing MainActivity instance rather than recreating it.
     */
    private fun launchSound(sound: SoundType) {
        haptic.click()
        sfx.tap()

        // ── Start service directly so the sound plays right now ──────────
        val serviceIntent = Intent(this, SoundService::class.java).apply {
            putExtra("SOUND_TYPE", sound.name)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(serviceIntent)
        else
            startService(serviceIntent)

        // ── Bring MainActivity to front and tell it which track is now active ──
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("LAUNCH_SOUND", sound.name)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.tab_aria
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.tab_aria
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.tab_aria) return@setOnItemSelectedListener true
            haptic.tick()
            sfx.tap()
            val targetClass = when (item.itemId) {
                R.id.tab_sounds   -> MainActivity::class.java
                R.id.tab_alarm    -> AlarmActivity::class.java
                R.id.tab_settings -> SettingsActivity::class.java
                R.id.tab_unlock   -> MainActivity::class.java
                else              -> null
            }
            if (targetClass != null) {
                val intent = Intent(this, targetClass).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    if (item.itemId == R.id.tab_unlock) {
                        putExtra(MainActivity.EXTRA_SHOW_UNLOCK, true)
                    }
                }
                startActivity(intent)
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                true
            } else false
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ChatAdapter
// ─────────────────────────────────────────────────────────────────────────────

class ChatAdapter(
    private val items: List<ChatMessage>,
    private val onSoundTap: (SoundType) -> Unit
) : RecyclerView.Adapter<ChatAdapter.MsgVH>() {

    inner class MsgVH(val b: ItemChatMessageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgVH =
        MsgVH(ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: MsgVH, position: Int) {
        val msg = items[position]
        with(holder.b) {
            if (msg.isUser) {
                bubbleUser.visibility = View.VISIBLE
                bubbleAria.visibility = View.GONE
                tvUser.text = msg.text
            } else {
                bubbleUser.visibility = View.GONE
                bubbleAria.visibility = View.VISIBLE
                tvAria.text = msg.text
                if (msg.soundSuggestion != null) {
                    btnSoundChip.visibility = View.VISIBLE
                    btnSoundChip.text =
                        "▶ Play ${msg.soundSuggestion.emoji} ${msg.soundSuggestion.displayName}"
                    btnSoundChip.setOnClickListener { onSoundTap(msg.soundSuggestion) }
                } else {
                    btnSoundChip.visibility = View.GONE
                }
            }
        }
    }
}
