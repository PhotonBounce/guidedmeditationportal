package com.auroramind.meditation

/**
 * Affirmation scripts for the player, blended per the user's habit.
 *
 * Text-first by design: these lines drive the on-screen affirmation sequence
 * over a soundscape today, and become the scripts for spoken voice-over once
 * recordings exist. Lines are phrased in the present tense ("I am …"), the
 * model used by the highest-grossing affirmation apps.
 *
 * Freemium: the library is browsable as [THEMES]. Exactly one theme ("I Am
 * Free") is free; the rest carry [Theme.premium] = true and are unlocked by the
 * subscription. The personalized [forHabit] engine (Today's affirmation + panic
 * button) stays free for everyone — it's the core safety loop.
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
        "Each clean day makes the next one easier.",
        "I am the one in control now.",
        "My future self is thanking me right now.",
        "I rise every time, and I am rising now.",
        "I am exactly the kind of person who can do this.",
    )

    private val byHabit = mapOf(
        "vaping" to listOf(
            "My lungs are healing with every clean breath.",
            "I breathe deeply, because I can.",
            "I am not a smoker. I am free.",
            "Each breath I take is cleaner than the last.",
            "I don't need a vape to get through this moment.",
            "My body is recovering, and I can feel it.",
        ),
        "smoking" to listOf(
            "Each smoke-free hour is a gift to my future self.",
            "I am clean, clear, and in control.",
            "My body thanks me for every craving I ride out.",
            "I breathe easier and live longer with every clean day.",
            "I am done letting cigarettes run my life.",
            "The craving fades; my freedom stays.",
        ),
        "social_media" to listOf(
            "My attention belongs to me.",
            "I am fully present in my real life.",
            "The feed can wait. My life cannot.",
            "I choose real moments over endless scrolling.",
            "My worth is not measured in likes.",
            "I put the phone down and I feel myself return.",
        ),
        "doomscrolling" to listOf(
            "I put the phone down and pick my life up.",
            "My mind is calm when I stop feeding it noise.",
            "I decide what deserves my attention.",
            "I don't need to know everything to be at peace.",
            "My attention is precious, and I spend it on purpose.",
            "I close the screen and open my life.",
        ),
        "alcohol" to listOf(
            "I wake up clear, and I am proud of that.",
            "I don't need a drink to feel at ease.",
            "Clear-headed is my natural state.",
            "I am calmer and steadier without it.",
            "I handle my feelings; I don't drown them.",
            "Every clear morning is proof of who I am now.",
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

    data class Theme(
        val id: String,
        val title: String,
        val emoji: String,
        val lines: List<String>,
        /** false only for the single free starter theme; true unlocks with the subscription. */
        val premium: Boolean = true,
    )

    /** Browsable affirmation sets shown in the library. */
    val THEMES = listOf(
        Theme("free", "I Am Free", "🕊️", premium = false, lines = listOf(
            "I am free, one breath at a time.",
            "I am not my habit — I am the one who chose to stop.",
            "Every clean hour is rebuilding me.",
            "I choose the person I am becoming.",
            "Freedom feels better than the habit ever did.",
            "I am already the kind of person who doesn't need it.",
            "My freedom is mine, and no craving can take it.",
            "I am writing a new story, one clean day at a time.",
            "I let the old me go without regret.",
            "I am lighter, clearer, and more myself every day.",
            "This is who I am now: free.",
            "I trust myself to stay on this path.")),
        Theme("craving", "Craving Crusher", "🛡️", lines = listOf(
            "This craving will pass — whether I feed it or not.",
            "I can feel the urge and still not move.",
            "The wave rises, the wave falls. I am the shore.",
            "I've ridden this out before. I do it again now.",
            "Five minutes — I only need to outlast five minutes.",
            "A craving is just a feeling, and feelings move through me.",
            "I am bigger than this moment.",
            "I don't have to act on every thought I have.",
            "The urge is loud, but I am louder.",
            "I breathe through it, and it loosens its grip.",
            "Each craving I outlast makes the next one weaker.",
            "I am winning, right now, by simply waiting.")),
        Theme("morning", "Morning Power", "🌅", lines = listOf(
            "Today I begin clean and clear.",
            "I decide who I am today, and I choose free.",
            "My energy is mine to spend on what matters.",
            "I meet this day with a steady mind.",
            "This morning, I choose myself.",
            "I am awake, alive, and in control.",
            "Today is another clean day I get to be proud of.",
            "I set my intention: free, focused, calm.",
            "Nothing I face today is bigger than my resolve.",
            "I rise, and I rise free.",
            "My future self is built by what I do today.",
            "I am ready for this day.")),
        Theme("calm", "Calm & Steady", "🌿", lines = listOf(
            "My calm belongs to me.",
            "I breathe in steadiness, I breathe out the urge.",
            "I am grounded, I am safe, I am enough.",
            "Stillness is my strength.",
            "I return to my breath, and my breath returns me to me.",
            "I am the calm in my own storm.",
            "Peace is a place inside me I can always reach.",
            "I slow down, and the craving slows with me.",
            "I am steady, even when things are not.",
            "Nothing needs to be rushed. I am here.",
            "My body is learning that it is safe to rest.",
            "I am calm, and calm is enough.")),
        Theme("sleep", "Sleep & Release", "🌙", lines = listOf(
            "I let go of today and rest in my progress.",
            "My body heals as I sleep, clean and calm.",
            "I release what I cannot control.",
            "Tomorrow I wake up free.",
            "I have done enough today. I can rest now.",
            "I forgive myself for what was hard.",
            "Each night clean is a night my body repairs.",
            "I set down the day like a bag I no longer carry.",
            "Sleep restores me, and I let it.",
            "I am proud of today, and I rest in that.",
            "My mind grows quiet, my body grows still.",
            "I drift off free, and I wake up free.")),
        Theme("strength", "Strength & Resolve", "💪", lines = listOf(
            "I am stronger than this craving.",
            "I have done hard things before. I am doing one now.",
            "My resolve is deeper than any urge.",
            "I keep my promises to myself.",
            "Every \"no\" I say makes me stronger.",
            "I am built for this.",
            "Discomfort is just strength being made.",
            "I don't quit on myself.",
            "I am disciplined, and discipline sets me free.",
            "My willpower grows every time I use it.",
            "I choose the hard right over the easy wrong.",
            "I am unshakeable in what I want.")),
        Theme("worth", "Worthy & Whole", "✨", lines = listOf(
            "I am worthy of the clean life I'm building.",
            "I am enough, exactly as I am right now.",
            "I deserve to feel good without it.",
            "I treat myself with patience and respect.",
            "I am whole — nothing outside me can complete or break me.",
            "I am proud of who I'm becoming.",
            "I matter, and my health matters.",
            "I forgive my past and choose my future.",
            "I am someone worth fighting for.",
            "My value was never in the habit.",
            "I speak to myself like someone I love.",
            "I am becoming the person I always was underneath.")),
    )

    /** Themes anyone can play without subscribing. */
    fun freeThemes(): List<Theme> = THEMES.filter { !it.premium }

    fun getTheme(id: String?): Theme? = THEMES.firstOrNull { it.id == id }
}
