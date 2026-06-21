package com.auroramind.meditation

import java.util.Calendar

/**
 * Bite-sized, text-only meditation techniques.
 *
 * These power two things competitors charge for via recorded courses — without
 * needing any audio:
 *   1. The daily reminder notification carries a rotating "quick technique" so
 *      every ping teaches something, not just nags.
 *   2. A "Technique of the day" card the user can tap to read in full.
 *
 * Each technique is short enough to act on in 30–90 seconds.
 */
data class Technique(
    val title: String,
    val emoji: String,
    /** One-line teaser used in notifications. */
    val teaser: String,
    /** Full step-by-step body shown when expanded. */
    val body: String,
    val mood: Mood,
)

object MicroTechniques {

    val all = listOf(
        Technique(
            "Box Breathing", "🫧",
            "Breathe a slow square: in 4 · hold 4 · out 4 · hold 4.",
            "Sit comfortably. Breathe in through your nose for 4 counts. Hold for 4. " +
            "Exhale through your mouth for 4. Hold empty for 4. Repeat 4 rounds. " +
            "This steadies your nervous system in under a minute.",
            Mood.STRESS,
        ),
        Technique(
            "5-4-3-2-1 Grounding", "🌿",
            "Name 5 things you see, 4 you feel, 3 you hear, 2 you smell, 1 you taste.",
            "When your mind races, anchor to your senses. Slowly notice: 5 things you can see, " +
            "4 things you can physically feel, 3 things you can hear, 2 things you can smell, " +
            "and 1 thing you can taste. By the end, you're back in the present moment.",
            Mood.GROUNDING,
        ),
        Technique(
            "4-7-8 Breath for Sleep", "🌙",
            "Inhale 4 · hold 7 · exhale 8. Repeat to drift off.",
            "Lying down, exhale fully. Inhale quietly through your nose for 4 counts. " +
            "Hold your breath for 7. Exhale slowly through your mouth for 8 counts. " +
            "Repeat 4 cycles — the long exhale signals your body it's safe to rest.",
            Mood.SLEEP,
        ),
        Technique(
            "Body Scan", "🦶",
            "Sweep attention slowly from your toes to the crown of your head.",
            "Close your eyes. Bring gentle attention to your toes, then slowly move upward — " +
            "feet, legs, belly, chest, arms, shoulders, neck, face. At each area, soften and " +
            "release any holding. No fixing, just noticing.",
            Mood.GROUNDING,
        ),
        Technique(
            "Loving-Kindness", "🤍",
            "Silently wish: may I be well, may you be well, may all be well.",
            "Bring someone kind to mind. Silently repeat: 'May you be happy. May you be healthy. " +
            "May you be at ease.' Then offer the same to yourself, then to someone neutral, " +
            "then to everyone. Warmth grows the more you practice.",
            Mood.COMPASSION,
        ),
        Technique(
            "Single-Point Focus", "🎯",
            "Rest all attention on one breath. When it wanders, gently return.",
            "Pick one anchor — the feeling of air at your nostrils, or the rise of your chest. " +
            "Rest your full attention there. When thoughts pull you away (they will), simply " +
            "notice and return, without judgment. That return IS the practice.",
            Mood.FOCUS,
        ),
        Technique(
            "Physiological Sigh", "🌬️",
            "Two quick inhales through the nose, one long exhale. Resets stress fast.",
            "Take a normal breath in through your nose, then a second short sip of air on top. " +
            "Now exhale slowly and completely through your mouth. Two or three of these is the " +
            "fastest known way to calm your body in real time.",
            Mood.STRESS,
        ),
        Technique(
            "Note & Let Go", "🍃",
            "Label each thought 'thinking' and let it float by like a cloud.",
            "Sit quietly. When a thought arises, gently label it — 'planning', 'worrying', " +
            "'remembering' — then let it pass like a cloud across the sky. You are the sky, " +
            "not the weather.",
            Mood.FOCUS,
        ),
        Technique(
            "Gratitude Pause", "✨",
            "Bring to mind three small things you're grateful for right now.",
            "Pause and name three things, however small — warm light, a good cup of tea, " +
            "a message from a friend. Let yourself actually feel each one for a breath. " +
            "Gratitude rewires attention toward what's already good.",
            Mood.COMPASSION,
        ),
        Technique(
            "Warm Hands, Soft Belly", "🤲",
            "Rest a warm hand on your belly and let it rise and fall.",
            "Place one hand on your chest and one on your belly. Breathe so that only the lower " +
            "hand moves. Slow, low belly-breathing tells your body the danger has passed and " +
            "it's safe to settle.",
            Mood.SLEEP,
        ),
        Technique(
            "Counting Down to Calm", "🔢",
            "Breathe out and count 10… 9… 8… all the way down to one.",
            "With each slow exhale, silently count down from ten to one. If you lose track, " +
            "simply start again at ten. By the time you reach one, your breath — and your " +
            "mind — will have slowed.",
            Mood.STRESS,
        ),
        Technique(
            "Soften the Jaw & Shoulders", "💆",
            "Unclench your jaw, drop your shoulders, soften your forehead.",
            "We hold stress in three places. Gently part your teeth so the jaw is loose. Let " +
            "your shoulders melt away from your ears. Smooth the space between your eyebrows. " +
            "Notice the relief.",
            Mood.STRESS,
        ),
        Technique(
            "Mountain Pose Mind", "⛰️",
            "Sit tall and steady, like a mountain unmoved by passing weather.",
            "Sit with a straight, dignified spine. Imagine you're a mountain — thoughts and " +
            "feelings are weather passing across your slopes, while you remain grounded, vast, " +
            "and still beneath it all.",
            Mood.GROUNDING,
        ),
        Technique(
            "Three Good Breaths", "🌸",
            "Three slow breaths, each one a little fuller than the last.",
            "Right now, take three deliberate breaths. Make the first one slow, the second " +
            "slower, the third the slowest. A 20-second reset you can do absolutely anywhere.",
            Mood.FOCUS,
        ),
        Technique(
            "Hand on Heart", "🫶",
            "Place a hand on your heart and offer yourself one kind sentence.",
            "Rest a hand over your heart and feel its warmth. Silently say: 'This is a moment " +
            "of difficulty. Difficulty is part of being human. May I be kind to myself.' " +
            "Self-kindness is a skill that grows with practice.",
            Mood.COMPASSION,
        ),
        Technique(
            "Peaceful Place", "🏡",
            "Picture a place where you feel completely safe — and stay there.",
            "Close your eyes. Imagine a place — real or created — where you feel completely safe and still. " +
            "It might be a beach at dusk, a quiet forest, a childhood room. Slowly fill in the details: " +
            "what do you see around you? What sounds are present? What does the air feel like on your skin? " +
            "Let yourself fully inhabit the scene. When thoughts intrude, gently return there.",
            Mood.SLEEP,
        ),
        Technique(
            "Power Breath", "⚡",
            "20 quick belly pumps followed by a 5-count hold — a natural energy shot.",
            "Sit upright. Take a deep inhale. Now do 20 quick, sharp exhales through your nose, " +
            "letting your belly pump in and out. The inhales happen passively. This is Kapalabhati. " +
            "After 20 pumps, take one deep inhale and hold for 5 counts. Exhale fully. " +
            "Repeat 3 rounds. You'll feel warm, alert, and clear-headed.",
            Mood.ENERGY,
        ),
        Technique(
            "Wake-Up Body Tap", "👐",
            "Pat your legs, torso, and arms with open palms to wake the body's surface energy.",
            "Stand or sit upright. Use open palms to firmly but gently pat your thighs down to " +
            "your feet. Move up to your hips, belly, and lower back. Pat your chest and shoulders. " +
            "Finish by massaging your scalp and face with your fingertips. " +
            "This physical stimulation activates circulation and tells the nervous system: awake now.",
            Mood.ENERGY,
        ),
        Technique(
            "Sun Breath", "🌅",
            "Reach overhead on the inhale, pull down on the exhale — shakes off sluggishness in 5 reps.",
            "Stand with feet hip-width apart. Inhale deeply through your nose while raising both arms " +
            "overhead — feel your chest expand. At the top, hold for a count. Exhale forcefully through " +
            "your mouth while sweeping your arms back down and bending your knees slightly — let the " +
            "exhale be a sound if you like. Repeat 5 times. End with a slow inhale and a smile. " +
            "The big movement breaks the stillness of sitting and sends fresh oxygen to the brain.",
            Mood.ENERGY,
        ),
    )

    /** Today's technique — stable per calendar day, rotates at midnight. */
    fun today(): Technique {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return all[day % all.size]
    }

    /** Techniques suited to the user's chosen goal, else all techniques. */
    fun forGoal(goal: Mood?): List<Technique> {
        if (goal == null) return all
        val matches = all.filter { it.mood == goal }
        return if (matches.isEmpty()) all else matches
    }
}
