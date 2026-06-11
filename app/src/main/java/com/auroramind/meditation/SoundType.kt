package com.auroramind.meditation

import android.graphics.Color

/**
 * Mood / goal a meditation primarily serves. Drives the home-screen filter
 * chips and goal-based recommendations (matches the "browse by goal" model
 * users expect from Calm / Headspace).
 */
enum class Mood(val label: String, val emoji: String) {
    SLEEP("Sleep", "🌙"),
    STRESS("Stress & Anxiety", "🌬️"),
    FOCUS("Focus", "🎯"),
    GROUNDING("Grounding", "🌿"),
    COMPASSION("Self-Compassion", "🤍"),
    ENERGY("Energy", "⚡"),
}

enum class SoundType(
    val displayName: String,
    val description: String,
    val emoji: String,
    val isPremium: Boolean,
    val category: String,
    /** Non-zero = file-backed (res/raw); zero = synthesized by AudioEngine. */
    val rawResId: Int = 0,
    /**
     * Chameleon accent color for this track.
     * The app background, nebula, portal rings and card highlights all
     * smoothly shift to this hue when the track starts playing.
     */
    val themeColor: Int = Color.parseColor("#E91E8C"),
    /** Primary mood/goal this track serves — drives filter chips & recommendations. */
    val mood: Mood = Mood.GROUNDING,
) {
    // ── Guided Meditations ──────────────────────────────────────────────────
    TONGLEN(
        "Tonglen", "A Tibetan practice of sending and taking to cultivate compassion and settle the mind",
        "🏔️", false, "Guided Meditations", R.raw.tonglen,
        Color.parseColor("#4FC3F7"), Mood.GROUNDING,
    ),
    HESYCHASM(
        "Hesychasm", "An ancient Christian monastic practice of silent prayer of the heart",
        "⛪", false, "Guided Meditations", R.raw.hesychasm,
        Color.parseColor("#9575CD"), Mood.GROUNDING,
    ),
    SUMARA(
        "Sumara", "A Javanese practice of total surrender, letting go of all effort and concentration",
        "🌊", false, "Guided Meditations", R.raw.sumara,
        Color.parseColor("#26D4A0"), Mood.SLEEP,
    ),
    MURAQABA(
        "Muraqaba", "A Sufi discipline of watchfulness, observing the heart and presence",
        "👁️", true, "Guided Meditations", R.raw.muraqaba,
        Color.parseColor("#FFA726"), Mood.FOCUS,
    ),
    DHIKR(
        "Dhikr", "A Sufi breath of remembrance through repetition to center the heart",
        "📿", true, "Guided Meditations", R.raw.dhikr,
        Color.parseColor("#66BB6A"), Mood.COMPASSION,
    ),
    HITBODEDUT(
        "Hitbodedut", "A Jewish practice of secluded spoken prayer in one's own everyday words",
        "🗣️", true, "Guided Meditations", R.raw.hitbodedut,
        Color.parseColor("#FF8A65"), Mood.STRESS,
    ),
    ZHAN_ZHUANG(
        "Zhan Zhuang", "A Qigong standing post practice to find root and alignment",
        "🌲", true, "Guided Meditations", R.raw.zhan_zhuang,
        Color.parseColor("#43A047"), Mood.GROUNDING,
    ),
    SOHAM(
        "Soham", "An ancient yogic practice of listening to the natural mantra of the breath",
        "🧘", true, "Guided Meditations", R.raw.soham,
        Color.parseColor("#E91E8C"), Mood.SLEEP,
    ),
    VIPASSANA(
        "Vipassana", "A systematic body-scanning practice to sharpen awareness and release tension",
        "⚡", true, "Guided Meditations", R.raw.vipassana,
        Color.parseColor("#BCAAA4"), Mood.FOCUS,
    ),
    BUDDHO(
        "Buddho", "A Thai forest tradition recitation of Buddho to align the breath with wakefulness",
        "☸️", true, "Guided Meditations", R.raw.buddho,
        Color.parseColor("#7E57C2"), Mood.FOCUS,
    ),
    THIEN(
        "Thien", "A Vietnamese Zen practice of breathing and smiling in the middle of daily life",
        "🎋", true, "Guided Meditations", R.raw.thien,
        Color.parseColor("#00ACC1"), Mood.STRESS,
    ),
    THIEN_2(
        "Thien 2", "A deeper Vietnamese Zen breathing practice for finding calm in any condition",
        "🎋", true, "Guided Meditations", R.raw.thien_2,
        Color.parseColor("#00838F"), Mood.GROUNDING,
    ),
    EVENING_REVIEW(
        "Evening Review", "A Stoic nightly reflection to review the day with honesty and patient self-compassion",
        "🏛️", true, "Guided Meditations", R.raw.evening_review,
        Color.parseColor("#5C6BC0"), Mood.SLEEP,
    ),
    AUTOGENIC_CALM(
        "Autogenic Calm", "A somatic relaxation practice teaching the body calm through quiet autosuggestions",
        "❄️", true, "Guided Meditations", R.raw.autogenic_calm,
        Color.parseColor("#3D5AFE"), Mood.STRESS,
    ),
    PROGRESSIVE_MUSCLE_RELEASE(
        "Progressive Muscle Release", "A physical relaxation method of tensing and releasing muscles to dissolve tension",
        "💪", true, "Guided Meditations", R.raw.progressive_muscle_release,
        Color.parseColor("#EC407A"), Mood.SLEEP,
    ),

    // ── Energy Music ────────────────────────────────────────────────────────
    CIRCUIT_THUNDERCLAP(
        "Circuit Thunderclap", "A high-energy electronic soundscape to spark creativity and focus",
        "⚡", false, "Energy Music", R.raw.circuit_thunderclap,
        Color.parseColor("#FF7043"), Mood.ENERGY,
    ),
    CIRCUIT_THUNDERCLAP_1(
        "Voltage Surge", "A deep pulsing rhythmic flow to drive dynamic action",
        "⚡", false, "Energy Music", R.raw.circuit_thunderclap_1,
        Color.parseColor("#FF5722"), Mood.ENERGY,
    ),
    CONCRETE_CHROME(
        "Concrete Chrome", "Sleek industrial vibes that steady the pulse while boosting drive",
        "💿", false, "Energy Music", R.raw.concrete_chrome,
        Color.parseColor("#26C6DA"), Mood.ENERGY,
    ),
    CONCRETE_CHROME_1(
        "Metallic Resonance", "Intense industrial frequencies for deep concentration",
        "💿", true, "Energy Music", R.raw.concrete_chrome_1,
        Color.parseColor("#00ACC1"), Mood.ENERGY,
    ),
    CONCRETE_SYNAPSE(
        "Concrete Synapse", "A digital neural flow connecting mind, breath, and energy",
        "🧠", true, "Energy Music", R.raw.concrete_synapse,
        Color.parseColor("#AB47BC"), Mood.ENERGY,
    ),
    CONCRETE_SYNAPSE_1(
        "Neural Overdrive", "Deep mechanical loops to sync thought and action",
        "🧠", true, "Energy Music", R.raw.concrete_synapse_1,
        Color.parseColor("#8E24AA"), Mood.ENERGY,
    ),
    NEON_CONTRADICTION(
        "Neon Contradiction", "Bright synth melodies clashing with heavy bass to raise awareness",
        "🌃", true, "Energy Music", R.raw.neon_contradiction,
        Color.parseColor("#FFCA28"), Mood.ENERGY,
    ),
    NEON_CONTRADICTION_1(
        "Uptempo Awakening", "A late-night uptempo progression that awakens the senses",
        "🌃", true, "Energy Music", R.raw.neon_contradiction_1,
        Color.parseColor("#FFB300"), Mood.ENERGY,
    ),
    SOLDERED_THROAT(
        "Soldered Throat", "Warm vocal and synthetic tones merging into a steady stream of power",
        "🔥", true, "Energy Music", R.raw.soldered_throat,
        Color.parseColor("#26A69A"), Mood.ENERGY,
    ),
    SOLDERED_THROAT_1(
        "Vocal Ignition", "An intense breathwork track to channel inner heat and strength",
        "🔥", true, "Energy Music", R.raw.soldered_throat_1,
        Color.parseColor("#00897B"), Mood.ENERGY,
    ),
    TARMAC_ANTHEM(
        "Tarmac Anthem", "A street-level urban rhythm to ground your movement and speed",
        "🛣️", true, "Energy Music", R.raw.tarmac_anthem,
        Color.parseColor("#EC407A"), Mood.ENERGY,
    ),
    TARMAC_ANTHEM_1(
        "Velocity Flow", "A driving, steady tempo flow to power physical work and presence",
        "🛣️", true, "Energy Music", R.raw.tarmac_anthem_1,
        Color.parseColor("#D81B60"), Mood.ENERGY,
    ),

    // ── Focus Music ─────────────────────────────────────────────────────────
    NINE_TO_FIVE(
        "9 to 5 Flow", "A steady lo-fi rhythm to keep you motivated through the workday",
        "💼", false, "Focus Music", R.raw.nine_to_five,
        Color.parseColor("#4DD0E1"), Mood.FOCUS,
    ),
    SONG_FOR_NINE_TO_FIVE(
        "Office Harmony", "Gentle synth chords to soften the drone of office life",
        "🏢", false, "Focus Music", R.raw.song_for_nine_to_five,
        Color.parseColor("#26C6DA"), Mood.FOCUS,
    ),
    BURNOUT_RECOVERY(
        "Burnout Recovery", "Sleek, comforting ambient wave to release mental overload",
        "🔋", false, "Focus Music", R.raw.burnout_recovery,
        Color.parseColor("#81C784"), Mood.FOCUS,
    ),
    BURNOUT_RELIEF(
        "Post-Burnout Calm", "A restorative soundscape to help you breathe and reset",
        "🍃", true, "Focus Music", R.raw.burnout_relief,
        Color.parseColor("#4CAF50"), Mood.FOCUS,
    ),
    EVERY_MINUTE_COUNTS(
        "Every Minute Counts", "An upbeat mechanical pulse to drive productivity",
        "⏱️", true, "Focus Music", R.raw.every_minute_counts,
        Color.parseColor("#FFB74D"), Mood.FOCUS,
    ),
    EVERY_MINUTE_COUNTS_ALT(
        "Precision Seconds", "A focused, ticking synthesizer loop for high efficiency",
        "🎯", true, "Focus Music", R.raw.every_minute_counts_alt,
        Color.parseColor("#FF9800"), Mood.FOCUS,
    ),
    WORK_FOCUS_CHILLOUT(
        "Workspace Flow", "Warm chords and a gentle beat to ease you into deep concentration",
        "🧠", true, "Focus Music", R.raw.work_focus_chillout,
        Color.parseColor("#BA68C8"), Mood.FOCUS,
    ),
    WORK_FOCUS_CHILLOUT_1(
        "Creative Catalyst", "An inspiring electronic ambient backdrop for coding or design",
        "💻", true, "Focus Music", R.raw.work_focus_chillout_1,
        Color.parseColor("#9C27B0"), Mood.FOCUS,
    ),
    WORK_FOCUS_CHILLOUT_2(
        "Synapse Synergy", "Steady pulsing digital tones to link mind and action",
        "🔌", true, "Focus Music", R.raw.work_focus_chillout_2,
        Color.parseColor("#64B5F6"), Mood.FOCUS,
    ),
    WORK_FOCUS_CHILLOUT_3(
        "Cognitive Clarity", "A clean, bright synthesizer soundscape to sharpen your thoughts",
        "💡", true, "Focus Music", R.raw.work_focus_chillout_3,
        Color.parseColor("#2196F3"), Mood.FOCUS,
    ),
    WORK_FOCUS_CHILLOUT_4(
        "Quiet Concentration", "A minimal, non-distracting background drone for intense study",
        "📚", true, "Focus Music", R.raw.work_focus_chillout_4,
        Color.parseColor("#0D47A1"), Mood.FOCUS,
    );

    companion object {
        /** Tracks matching a given mood, in declaration order. */
        fun byMood(mood: Mood): List<SoundType> = values().filter { it.mood == mood }
    }
}
