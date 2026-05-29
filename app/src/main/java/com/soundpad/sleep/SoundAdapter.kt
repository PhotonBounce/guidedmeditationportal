package com.soundpad.sleep

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.soundpad.sleep.databinding.ItemSoundCardBinding
import com.soundpad.sleep.databinding.ItemSoundSectionHeaderBinding

/**
 * Sectioned grid adapter — section headers span both columns; sound cards
 * occupy one cell each. Use `spanSize(position)` from MainActivity to drive
 * the GridLayoutManager.spanSizeLookup.
 *
 * Adds three motion layers on sound cards:
 *   1. Press feedback — scale 0.93 → springs back with overshoot
 *   2. Active glow — pulsing alpha on the now-playing card's stroke
 *   3. Stagger entrance — first bind translates up + fades in, offset by index
 */
class SoundAdapter(
    private val items: List<SoundGridItem>,
    private var isPremium: Boolean,
    private var playingSound: SoundType?,
    private val onSoundClick:    (SoundType) -> Unit,
    private val onSoundLongPress:(SoundType) -> Unit,
    private val onPremiumClick:  (SoundType) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SOUND  = 1
    }

    inner class HeaderVH(val b: ItemSoundSectionHeaderBinding) : RecyclerView.ViewHolder(b.root)
    inner class SoundVH(val b: ItemSoundCardBinding) : RecyclerView.ViewHolder(b.root) {
        var glowAnimator: ValueAnimator? = null
    }

    private val seenPositions = mutableSetOf<Int>()

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SoundGridItem.Header -> TYPE_HEADER
        is SoundGridItem.Sound  -> TYPE_SOUND
    }

    /** Headers span both columns; cards span one. Wire to GridLayoutManager. */
    fun spanSize(position: Int, totalSpans: Int): Int =
        if (items[position] is SoundGridItem.Header) totalSpans else 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemSoundSectionHeaderBinding.inflate(inflater, parent, false))
            else        -> SoundVH(ItemSoundCardBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount() = items.size

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SoundGridItem.Header -> bindHeader(holder as HeaderVH, item, position)
            is SoundGridItem.Sound  -> bindSound(holder as SoundVH, item.type, position)
        }
    }

    private fun bindHeader(holder: HeaderVH, item: SoundGridItem.Header, position: Int) {
        holder.b.tvHeaderLabel.text = item.title

        // Colour-code header text and divider by category
        val catColor = categoryColor(item.title, holder.b.root.context)
        holder.b.tvHeaderLabel.setTextColor(catColor)
        holder.b.vHeaderDivider.setBackgroundColor(catColor)

        if (position !in seenPositions) {
            seenPositions.add(position)
            holder.itemView.alpha = 0f
            holder.itemView.translationY = dpToPx(12f, holder.itemView).toFloat()
            holder.itemView.animate()
                .alpha(1f).translationY(0f)
                .setStartDelay((position * 35L).coerceAtMost(400L))
                .setDuration(380)
                .start()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindSound(holder: SoundVH, sound: SoundType, position: Int) {
        val locked = sound.isPremium && !isPremium && !SessionPremiumManager.isUnlocked(sound)
        val active = sound == playingSound

        with(holder.b) {
            tvEmoji.text = sound.emoji
            tvName.text  = sound.displayName

            tvLock.visibility = if (locked) View.VISIBLE else View.GONE
            vPlayingBar.visibility = if (active) View.VISIBLE else View.GONE

            root.alpha = if (locked) 0.62f else 1f

            // Colour-code the category accent stripe and playing bar
            val catColor = categoryColor(sound.category, cardRoot.context)
            vCategoryAccent.setBackgroundColor(catColor)
            vPlayingBar.setBackgroundColor(catColor)

            cardRoot.setCardBackgroundColor(
                cardRoot.context.getColor(
                    if (active) R.color.card_active else R.color.card_bg
                )
            )
            cardRoot.strokeColor = cardRoot.context.getColor(
                if (active) R.color.card_border_active else R.color.card_border
            )
            cardRoot.strokeWidth =
                if (active) dpToPx(2f, holder.itemView) else dpToPx(1f, holder.itemView)

            // ── Active card glow pulse ───────────────────────────────────
            holder.glowAnimator?.cancel()
            if (active) {
                holder.glowAnimator = ValueAnimator.ofFloat(0.85f, 1.0f, 0.85f).apply {
                    duration = 2200
                    repeatCount = ValueAnimator.INFINITE
                    addUpdateListener { cardRoot.alpha = it.animatedValue as Float }
                    start()
                }
            } else {
                cardRoot.alpha = 1f
            }

            // ── Press feedback (scale) ───────────────────────────────────
            root.setOnTouchListener { v, ev ->
                when (ev.actionMasked) {
                    MotionEvent.ACTION_DOWN -> v.animate().scaleX(0.93f).scaleY(0.93f)
                        .setDuration(90).start()
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        v.animate().scaleX(1f).scaleY(1f)
                            .setInterpolator(OvershootInterpolator(2.5f))
                            .setDuration(220).start()
                }
                false
            }

            root.setOnClickListener {
                if (locked) onPremiumClick(sound) else onSoundClick(sound)
            }
            root.setOnLongClickListener {
                onSoundLongPress(sound)
                true
            }

            // ── Stagger entrance ─────────────────────────────────────────
            if (position !in seenPositions) {
                seenPositions.add(position)
                root.translationY = dpToPx(28f, holder.itemView).toFloat()
                root.alpha = 0f
                root.animate()
                    .translationY(0f)
                    .alpha(if (locked) 0.62f else 1f)
                    .setStartDelay((position * 45L).coerceAtMost(500L))
                    .setDuration(420)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start()
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is SoundVH) {
            holder.glowAnimator?.cancel()
            holder.glowAnimator = null
        }
        super.onViewRecycled(holder)
    }

    fun update(playing: SoundType?, premium: Boolean) {
        playingSound = playing
        isPremium    = premium
        notifyDataSetChanged()
    }

    private fun dpToPx(dp: Float, view: View): Int =
        (dp * view.context.resources.displayMetrics.density).toInt()

    /** Maps a sound category string to its vivid accent colour resource. */
    private fun categoryColor(category: String, context: android.content.Context): Int {
        val res = when (category) {
            "Noise"         -> R.color.cat_noise
            "Nature"        -> R.color.cat_nature
            "Mechanical"    -> R.color.cat_mechanical
            "Synthetic"     -> R.color.cat_synthetic
            "Ambient Music" -> R.color.cat_ambient
            "Energy Music"  -> R.color.cat_energy
            else            -> R.color.accent_iris
        }
        return context.getColor(res)
    }
}
