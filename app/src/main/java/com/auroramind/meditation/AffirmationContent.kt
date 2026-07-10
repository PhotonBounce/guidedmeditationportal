package com.auroramind.meditation

import android.content.Context

/**
 * Affirmation scripts for the player, blended per the user's habit.
 *
 * The lines and theme names live in res/values*/affirmations.xml so they
 * localize with the app language (see LocaleManager). Text-first by design:
 * these drive the on-screen affirmation sequence over a soundscape today, and
 * become the scripts for spoken voice-over once recordings exist. Phrased in
 * the present tense ("I am …"), the model used by the highest-grossing
 * affirmation apps.
 *
 * Freemium: the library is browsable as [themes]. Exactly one theme ("I Am
 * Free") is free; the rest carry [Theme.premium] = true and unlock with the
 * subscription. The personalized [forHabit] engine (Today's affirmation + panic
 * button) stays free for everyone — it's the core safety loop.
 */
object AffirmationContent {

    data class Theme(
        val id: String,
        val title: String,
        val emoji: String,
        val lines: List<String>,
        /** false only for the single free starter theme; true unlocks with the subscription. */
        val premium: Boolean = true,
    )

    /** Static per-theme metadata; the title + lines are resolved from resources. */
    private data class ThemeDef(
        val id: String,
        val emoji: String,
        val premium: Boolean,
        val titleRes: Int,
        val linesRes: Int,
    )

    private val DEFS = listOf(
        ThemeDef("free",     "🕊️", false, R.string.aff_theme_free,     R.array.aff_free),
        ThemeDef("craving",  "🛡️", true,  R.string.aff_theme_craving,  R.array.aff_craving),
        ThemeDef("morning",  "🌅", true,  R.string.aff_theme_morning,  R.array.aff_morning),
        ThemeDef("calm",     "🌿", true,  R.string.aff_theme_calm,     R.array.aff_calm),
        ThemeDef("sleep",    "🌙", true,  R.string.aff_theme_sleep,    R.array.aff_sleep),
        ThemeDef("strength", "💪", true,  R.string.aff_theme_strength, R.array.aff_strength),
        ThemeDef("worth",    "✨", true,  R.string.aff_theme_worth,    R.array.aff_worth),
    )

    private fun ThemeDef.toTheme(context: Context) = Theme(
        id = id,
        title = context.getString(titleRes),
        emoji = emoji,
        lines = context.resources.getStringArray(linesRes).toList(),
        premium = premium,
    )

    /** Browsable affirmation sets shown in the library, in the current language. */
    fun themes(context: Context): List<Theme> = DEFS.map { it.toTheme(context) }

    /** Themes anyone can play without subscribing. */
    fun freeThemes(context: Context): List<Theme> = themes(context).filter { !it.premium }

    fun getTheme(context: Context, id: String?): Theme? =
        DEFS.firstOrNull { it.id == id }?.toTheme(context)

    private val habitArrays = mapOf(
        "vaping"        to R.array.aff_vaping,
        "smoking"       to R.array.aff_smoking,
        "social_media"  to R.array.aff_social_media,
        "doomscrolling" to R.array.aff_doomscrolling,
        "alcohol"       to R.array.aff_alcohol,
    )

    /**
     * The affirmation sequence for a session: an optional personalized opener
     * from the quiz, then the habit-specific lines, then the general set.
     */
    fun forHabit(context: Context, habitType: String, freedomGoal: String = ""): List<String> {
        val opener = if (freedomGoal.isNotBlank()) {
            listOf(context.getString(R.string.aff_freedom_opener, freedomGoal.trim().trimEnd('.').take(90)))
        } else emptyList()
        val specific = habitArrays[habitType]?.let { context.resources.getStringArray(it).toList() }.orEmpty()
        val general = context.resources.getStringArray(R.array.aff_general).toList()
        return (opener + specific + general).distinct()
    }
}
