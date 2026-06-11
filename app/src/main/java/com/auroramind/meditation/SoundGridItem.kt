package com.auroramind.meditation

/**
 * Items rendered in the main sound grid. Either a section header that
 * spans both columns, or a single sound card.
 */
sealed class SoundGridItem {
    data class Header(val title: String) : SoundGridItem()
    data class Sound(val type: SoundType) : SoundGridItem()
}

/**
 * Build the displayed grid order.
 *
 * @param favorites  tracks the user has starred — pinned in a "Favorites"
 *                   section at the very top when non-empty (and no mood filter).
 * @param moodFilter when non-null, only tracks matching this mood are shown
 *                   (drives the home-screen filter chips).
 */
fun buildSoundGridItems(
    favorites: List<SoundType> = emptyList(),
    moodFilter: Mood? = null,
): List<SoundGridItem> {
    // Mood filter view — single flat section of matching tracks.
    if (moodFilter != null) {
        val matches = SoundType.byMood(moodFilter)
        return buildList {
            add(SoundGridItem.Header("${moodFilter.emoji}  ${moodFilter.label}"))
            matches.forEach { add(SoundGridItem.Sound(it)) }
        }
    }

    return buildList {
        // Favorites pinned on top
        if (favorites.isNotEmpty()) {
            add(SoundGridItem.Header("⭐  Favorites"))
            favorites.forEach { add(SoundGridItem.Sound(it)) }
        }

        val categoryOrder = listOf("Guided Meditations")
        val grouped = SoundType.values().groupBy { it.category }
        for (cat in categoryOrder) {
            val sounds = grouped[cat] ?: continue
            add(SoundGridItem.Header(cat))
            // List all first-voice tracks first, then the "2" (second-voice) variants,
            // so near-identical names never sit next to each other. sortedBy is stable.
            sounds.sortedBy { it.name.endsWith("_2") }
                .forEach { add(SoundGridItem.Sound(it)) }
        }
    }
}
