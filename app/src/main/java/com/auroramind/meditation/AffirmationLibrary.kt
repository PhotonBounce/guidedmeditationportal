package com.auroramind.meditation

import android.content.Context

/**
 * Discovers spoken-affirmation audio bundled under assets/affirmations/.
 *
 * The user drops audio files into that folder (committed to the repo) and the
 * filename becomes the display title — "use their titles for real names". When
 * the folder has tracks, [AffirmationPlayerActivity] plays the spoken audio over
 * the soundscape; when it's empty, the player falls back to text affirmations.
 */
object AffirmationLibrary {

    private val AUDIO_EXT = setOf("mp3", "m4a", "wav", "ogg", "aac")

    data class Track(val asset: String, val title: String)

    /** All affirmation audio tracks bundled in assets/affirmations/, by title. */
    fun list(context: Context): List<Track> = runCatching {
        (context.assets.list("affirmations") ?: emptyArray())
            .filter { it.substringAfterLast('.', "").lowercase() in AUDIO_EXT }
            .map { Track("affirmations/$it", titleFromFile(it)) }
            .sortedBy { it.title }
    }.getOrDefault(emptyList())

    /** Prefer a track whose name matches the user's habit; otherwise rotate by day. */
    fun pick(tracks: List<Track>, habitType: String, rotation: Int): Track? {
        if (tracks.isEmpty()) return null
        val key = habitType.trim()
        val match = if (key.isNotEmpty()) tracks.firstOrNull {
            it.title.contains(key, ignoreCase = true) || it.asset.contains(key, ignoreCase = true)
        } else null
        return match ?: tracks[((rotation % tracks.size) + tracks.size) % tracks.size]
    }

    /** "morning-power.mp3" -> "Morning Power" */
    private fun titleFromFile(name: String): String =
        name.substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
}
