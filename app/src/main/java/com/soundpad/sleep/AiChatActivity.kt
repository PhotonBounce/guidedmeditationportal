package com.soundpad.sleep

import android.content.Intent
import android.content.res.ColorStateList
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
import com.soundpad.sleep.databinding.ActivityAiChatBinding
import com.soundpad.sleep.databinding.ItemChatMessageBinding

/**
 * AiChatActivity — ARIA AI Rest Intelligence Assistant.
 *
 * Futuristic chat interface with:
 *  - On-device personalised AI (AiChatEngine) that knows the user's play history
 *  - Quick-action chips for Sleep / Focus / Relax / Mix Studio / VIP
 *  - Typing indicator with simulated "thinking" delay for realistic UX
 *  - Sound suggestion chips that launch the recommended sound directly
 */
class AiChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAiChatBinding
    private lateinit var engine: AiChatEngine
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityAiChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Edge-to-edge insets — push header below status bar, input above nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.header.updatePadding(top = bars.top + 8)
            binding.bottomPanel.updatePadding(bottom = bars.bottom + 4)
            insets
        }

        engine = AiChatEngine(this)
        chatAdapter = ChatAdapter(messages) { sound -> launchSound(sound) }

        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@AiChatActivity).also { it.stackFromEnd = true }
            adapter = chatAdapter
        }

        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        setupInput()
        setupChips()

        // Slight delay makes the welcome message feel like ARIA is "waking up"
        handler.postDelayed({
            val welcome = engine.addWelcome()
            addMessage(welcome)
        }, 700)
    }

    private fun setupInput() {
        binding.etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etInput.doOnTextChanged { text, _, _, _ ->
            binding.btnSend.isEnabled = !text.isNullOrBlank()
        }
    }

    private fun setupChips() {
        binding.chipGroupQuick.removeAllViews()

        // Mix Studio gets its own chip that launches directly (not through chat)
        Chip(this).apply {
            text = "🎛️ Mix Studio"
            isCheckable = false
            setTextColor(getColor(R.color.cat_energy))
            chipBackgroundColor = ColorStateList.valueOf(getColor(R.color.card_bg))
            chipStrokeColor = ColorStateList.valueOf(getColor(R.color.cat_energy))
            chipStrokeWidth = 1.5f
            textSize = 12f
            setOnClickListener {
                startActivity(Intent(this@AiChatActivity, MixStudioActivity::class.java))
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
     * Launch MainActivity and request it to play the recommended sound.
     * Uses FLAG_ACTIVITY_SINGLE_TOP so MainActivity isn't recreated if already running.
     */
    private fun launchSound(sound: SoundType) {
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
