package com.soundpad.sleep

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soundpad.sleep.databinding.ItemSoundCardBinding

class SoundAdapter(
    private val sounds: List<SoundType>,
    private var isPremium: Boolean,
    private var playingSound: SoundType?,
    private val onSoundClick: (SoundType) -> Unit,
    private val onPremiumClick: () -> Unit
) : RecyclerView.Adapter<SoundAdapter.SoundVH>() {

    inner class SoundVH(val b: ItemSoundCardBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SoundVH(
        ItemSoundCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun getItemCount() = sounds.size

    override fun onBindViewHolder(holder: SoundVH, position: Int) {
        val sound   = sounds[position]
        val locked  = sound.isPremium && !isPremium
        val active  = sound == playingSound

        with(holder.b) {
            tvEmoji.text = sound.emoji
            tvName.text  = sound.displayName

            // Premium lock indicator
            tvLock.visibility = if (locked) View.VISIBLE else View.GONE

            // Active playing indicator bar at card bottom
            vPlayingBar.visibility = if (active) View.VISIBLE else View.GONE

            // Dim locked cards slightly
            root.alpha = if (locked) 0.65f else 1f

            // Card background highlight for active
            cardRoot.setCardBackgroundColor(
                cardRoot.context.getColor(
                    if (active) R.color.card_active else R.color.card_bg
                )
            )

            // Stroke highlight for active
            cardRoot.strokeColor = cardRoot.context.getColor(
                if (active) R.color.accent_purple else R.color.card_border
            )
            cardRoot.strokeWidth = if (active) dpToPx(2f, holder.itemView) else dpToPx(1f, holder.itemView)

            root.setOnClickListener {
                if (locked) onPremiumClick() else onSoundClick(sound)
            }
        }
    }

    fun update(playing: SoundType?, premium: Boolean) {
        playingSound = playing
        isPremium    = premium
        notifyDataSetChanged()
    }

    private fun dpToPx(dp: Float, view: View): Int =
        (dp * view.context.resources.displayMetrics.density).toInt()
}
