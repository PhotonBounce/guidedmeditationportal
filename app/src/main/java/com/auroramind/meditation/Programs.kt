package com.auroramind.meditation

/**
 * Multi-day guided "journeys" — structured, Headspace-style courses delivered
 * entirely as text technique + a suggested track each day. No recorded audio
 * needed: each day pairs a short written practice with one of the app's
 * meditations. Progress is tracked per-program in [PrefsManager].
 */
data class ProgramDay(
    val title: String,
    val technique: String,
    val track: SoundType,
)

data class Program(
    val id: String,
    val title: String,
    val emoji: String,
    val blurb: String,
    val days: List<ProgramDay>,
)

object Programs {

    val all: List<Program> = listOf(
        Program(
            id = "sleep7",
            title = "7 Days to Calmer Sleep",
            emoji = "🌙",
            blurb = "A week of wind-down practices to fall asleep faster and rest deeper.",
            days = listOf(
                ProgramDay("Settle the body", "Lie down and take five slow breaths, each exhale twice as long as the inhale. Let the mattress hold all your weight.", SoundType.EVENING_REVIEW),
                ProgramDay("The 4-7-8 breath", "Inhale 4, hold 7, exhale 8. Four rounds. The long exhale tells your body it's safe to switch off.", SoundType.SOHAM),
                ProgramDay("Body scan downward", "Starting at the crown of your head, sweep attention slowly to your toes, softening each part as you pass.", SoundType.SUMARA),
                ProgramDay("Empty the mind's inbox", "Name three things still on your mind, then imagine setting each one on a shelf to deal with tomorrow.", SoundType.EVENING_REVIEW),
                ProgramDay("Warm, heavy limbs", "Picture warmth spreading from your chest into your arms and legs until they feel pleasantly heavy.", SoundType.SOHAM),
                ProgramDay("Let thoughts drift", "Imagine each thought as a leaf on a slow river, floating past. You are the bank, not the water.", SoundType.SUMARA),
                ProgramDay("Effortless rest", "Nothing to do, nowhere to be. Rest in the simple feeling of breathing until sleep finds you.", SoundType.EVENING_REVIEW),
            ),
        ),
        Program(
            id = "anxiety5",
            title = "5 Days Less Anxious",
            emoji = "🌬️",
            blurb = "Five short practices to calm a racing mind and steady your nervous system.",
            days = listOf(
                ProgramDay("Box breathing", "In 4 · hold 4 · out 4 · hold 4. Four rounds. A square you can trace any time the day gets loud.", SoundType.AUTOGENIC_CALM),
                ProgramDay("Name it to tame it", "Silently label what you feel — 'worry', 'tightness', 'racing'. Naming a feeling loosens its grip.", SoundType.AUTOGENIC_CALM),
                ProgramDay("5-4-3-2-1 grounding", "Notice 5 things you see, 4 you feel, 3 you hear, 2 you smell, 1 you taste. Back to the present.", SoundType.PROGRESSIVE_MUSCLE_RELEASE),
                ProgramDay("Set down the backpack", "Imagine the worries you carry as stones in a backpack. One by one, set each one down.", SoundType.PROGRESSIVE_MUSCLE_RELEASE),
                ProgramDay("The physiological sigh", "Two inhales through the nose, one long exhale through the mouth. The fastest way to calm in real time.", SoundType.AUTOGENIC_CALM),
            ),
        ),
        Program(
            id = "focus7",
            title = "7 Days of Focus",
            emoji = "🎯",
            blurb = "Train steady attention with a week of short concentration practices.",
            days = listOf(
                ProgramDay("One breath at a time", "Rest all attention on a single breath. When the mind wanders, gently return. The return is the practice.", SoundType.VIPASSANA),
                ProgramDay("Counting breaths", "Count each exhale up to ten, then start again. Lose count? No problem — begin from one.", SoundType.BUDDHO),
                ProgramDay("The anchor point", "Pick one spot — the air at your nostrils. Keep returning there, like a shape to its focus.", SoundType.VIPASSANA),
                ProgramDay("Note and return", "When a thought pulls you away, note 'thinking' and come back. Strengthen the muscle of attention.", SoundType.ZHAN_ZHUANG),
                ProgramDay("Open monitoring", "Instead of one anchor, simply watch whatever arises — sounds, sensations — without chasing any of it.", SoundType.BUDDHO),
                ProgramDay("Single-tasking", "Choose one small task after this. Do only that, fully, for two minutes. Focus is trained off the cushion too.", SoundType.ZHAN_ZHUANG),
                ProgramDay("Spacious awareness", "Let attention rest wide and open, like a clear sky holding everything without effort.", SoundType.THIEN),
            ),
        ),
        Program(
            id = "compassion5",
            title = "5 Days of Self-Compassion",
            emoji = "🤍",
            blurb = "Five gentle practices to meet yourself with the kindness you'd offer a friend.",
            days = listOf(
                ProgramDay("Hand on heart", "Rest a hand over your heart and feel its warmth. Say silently: 'May I be kind to myself in this moment.'", SoundType.TONGLEN),
                ProgramDay("The friend test", "Think of how you'd speak to a dear friend who was struggling. Now offer those same words to yourself.", SoundType.TONGLEN),
                ProgramDay("Permission to rest", "You don't have to earn rest. Let go of the need to achieve and simply be, just for these few minutes.", SoundType.MURAQABA),
                ProgramDay("Loving-kindness", "Silently wish: 'May I be happy. May I be healthy. May I be at ease.' Then extend it to someone you love.", SoundType.TONGLEN),
                ProgramDay("Enough, as you are", "Notice the urge to fix or improve yourself, and set it down. Right now, in this breath, you are enough.", SoundType.THIEN),
            ),
        ),
    )

    fun byId(id: String): Program? = all.firstOrNull { it.id == id }
}
