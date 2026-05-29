package com.soundpad.sleep

/**
 * Items rendered in the main sound grid. Either a section header that
 * spans both columns, or a single sound card.
 */
sealed class SoundGridItem {
    data class Header(val title: String) : SoundGridItem()
    data class Sound(val type: SoundType) : SoundGridItem()
}

/**
 * Build the displayed grid order — group by SoundType.category, with a
 * deterministic category order.
 */
fun buildSoundGridItems(): List<SoundGridItem> {
    val categoryOrder = listOf("Noise", "Nature", "Mechanical", "Synthetic", "Ambient Music", "Energy Music")
    val grouped = SoundType.values().groupBy { it.category }
    return buildList {
        for (cat in categoryOrder) {
            val sounds = grouped[cat] ?: continue
            add(SoundGridItem.Header(cat))
            sounds.forEach { add(SoundGridItem.Sound(it)) }
        }
    }
}
