package com.auroramind.meditation

/**
 * Affirmation scripts for the player, blended per the user's habit.
 *
 * Text-first by design: these lines drive the on-screen affirmation sequence
 * over a soundscape today, and become the scripts for spoken voice-over once
 * recordings exist. Lines are phrased in the present tense ("I am …"), the
 * model used by the highest-grossing affirmation apps.
 */
object AffirmationContent {

    private val general = listOf(
        "I am stronger than this craving.",
        "This urge will pass — whether I feed it or not.",
        "Every clean hour is rebuilding me.",
        "I choose the person I am becoming.",
        "I don't need it. I never really did.",
        "My calm belongs to me.",
        "I am free, one breath at a time.",
        "The discomfort is temporary. My freedom is the point.",
        "I have done hard things before. I am doing one now.",
        "I am proud of how far I've already come.",
    )

    private val byHabit = mapOf(
        "vaping" to listOf(
            "My lungs are healing with every clean breath.",
            "I breathe deeply, because I can.",
            "I am not a smoker. I am free.",
        ),
        "smoking" to listOf(
            "Each smoke-free hour is a gift to my future self.",
            "I am clean, clear, and in control.",
            "My body thanks me for every craving I ride out.",
        ),
        "social_media" to listOf(
            "My attention belongs to me.",
            "I am fully present in my real life.",
            "The feed can wait. My life cannot.",
        ),
        "doomscrolling" to listOf(
            "I put the phone down and pick my life up.",
            "My mind is calm when I stop feeding it noise.",
            "I decide what deserves my attention.",
        ),
        "alcohol" to listOf(
            "I wake up clear, and I am proud of that.",
            "I don't need a drink to feel at ease.",
            "Clear-headed is my natural state.",
        ),
    )

    /**
     * The affirmation sequence for a session: an optional personalized opener
     * from the quiz, then the habit-specific lines, then the general set.
     */
    fun forHabit(habitType: String, freedomGoal: String = ""): List<String> {
        val opener = if (freedomGoal.isNotBlank()) {
            listOf("Freedom, to me, is ${freedomGoal.trim().trimEnd('.').take(90)}.")
        } else emptyList()
        val specific = byHabit[habitType].orEmpty()
        return (opener + specific + general).distinct()
    }

    data class Theme(val id: String, val title: String, val emoji: String, val lines: List<String>)

    /** Browsable affirmation sets shown in the library. */
    val THEMES = listOf(
        Theme("free", "I Am Free", "🕊️", listOf(
            "I am free, one breath at a time.",
            "I am not my habit — I am the one who chose to stop.",
            "Every clean hour is rebuilding me.",
            "I choose the person I am becoming.",
            "Freedom feels better than the habit ever did.")),
        Theme("craving", "Craving Crusher", "🛡️", listOf(
            "This craving will pass — whether I feed it or not.",
            "I can feel the urge and still not move.",
            "The wave rises, the wave falls. I am the shore.",
            "I've ridden this out before. I do it again now.",
            "Five minutes — I only need to outlast five minutes.")),
        Theme("morning", "Morning Power", "🌅", listOf(
            "Today I begin clean and clear.",
            "I decide who I am today, and I choose free.",
            "My energy is mine to spend on what matters.",
            "I meet this day with a steady mind.")),
        Theme("calm", "Calm & Steady", "🌿", listOf(
            "My calm belongs to me.",
            "I breathe in steadiness, I breathe out the urge.",
            "I am grounded, I am safe, I am enough.",
            "Stillness is my strength.")),
        Theme("sleep", "Sleep & Release", "🌙", listOf(
            "I let go of today and rest in my progress.",
            "My body heals as I sleep, clean and calm.",
            "I release what I cannot control.",
            "Tomorrow I wake up free.")),
    )

    fun getTheme(id: String?): Theme? = THEMES.firstOrNull { it.id == id }
}
