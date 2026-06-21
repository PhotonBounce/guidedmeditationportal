package com.auroramind.meditation

import android.content.Context
import java.util.Calendar

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val soundSuggestion: SoundType? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Spirit — an inspirational meditation companion.
 *
 * On-device guide that gives mindful recommendations for rest, focus,
 * relaxation, tinnitus relief, and calming little ones, and can walk
 * the user through meditation techniques of all kinds — mindfulness,
 * breathwork, body scan, loving-kindness, visualization, mantra, and more.
 */
class AiChatEngine(private val context: Context) {

    private val prefs = PrefsManager(context)
    private val history = mutableListOf<ChatMessage>()
    private var turnsCount = 0
    // Tracks the last topic so follow-up phrases ("yes", "walk me through it") get contextual responses.
    private var lastTopic = ""

    private val mostPlayed: SoundType get() = prefs.getMostPlayedSound()

    fun getHistory(): List<ChatMessage> = history.toList()

    /** Called once on chat open to produce the personalised welcome message. */
    fun addWelcome(): ChatMessage {
        val msg = ChatMessage(buildWelcome(), isUser = false)
        history.add(msg)
        return msg
    }

    private fun buildWelcome(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeCtx = when {
            hour in 22..23 || hour < 5 -> "late night 🌙"
            hour < 12                  -> "morning 🌅"
            hour < 18                  -> "afternoon ☀️"
            else                       -> "evening 🌙"
        }
        val hasHistory = prefs.getPlayHistory().isNotEmpty()
        return if (hasHistory) {
            "Good $timeCtx! I'm Spirit — your meditation companion. 🤍\n\n" +
            "Welcome back. Last time, ${mostPlayed.emoji} ${mostPlayed.displayName} " +
            "seemed to settle you nicely.\n\n" +
            "I'm here to help you find the right practice for this moment, or simply " +
            "to talk — about rest, focus, relaxation, or any meditation technique you'd like to explore."
        } else {
            "Good $timeCtx! I'm Spirit — your meditation companion. 🤍\n\n" +
            "Think of me as an encouraging voice on your practice: I can suggest a " +
            "track for how you're feeling, walk you through a technique like breathwork " +
            "or body scanning, or just keep you company for a few quiet minutes.\n\n" +
            "What are you here for right now? Rest, focus, relaxation, or something else?"
        }
    }

    /** Process a user message and return the AI response. */
    fun respond(userInput: String): ChatMessage {
        history.add(ChatMessage(userInput.trim(), isUser = true))
        turnsCount++
        val lower = userInput.lowercase().trim()
        val (text, sound) = route(lower)
        val aiMsg = ChatMessage(text, isUser = false, soundSuggestion = sound)
        history.add(aiMsg)
        return aiMsg
    }

    // ── Router ────────────────────────────────────────────────────────────────

    private fun route(lower: String): Pair<String, SoundType?> = when {
        // Follow-up detection — must come first so "yes please" gets context-aware reply
        lastTopic.isNotEmpty() && any(lower,
            "yes", "please", "sure", "ok", "okay", "go on", "continue", "next",
            "tell me more", "walk me through", "guide me", "show me",
            "how do i", "teach me", "what do i do", "let's do it", "lets do it") ->
            handleFollowUp()

        any(lower, "hello", "hi", "hey", "good morning", "good evening", "good night", "yo", "sup") ->
            handleGreeting().also { lastTopic = "greeting" }
        any(lower, "sleep", "insomnia", "cant sleep", "can't sleep", "falling asleep",
            "bedtime", "tired", "fatigue", "exhausted") ->
            handleSleep().also { lastTopic = "sleep" }
        any(lower, "focus", "work", "study", "concentrate", "productivity",
            "read", "code", "writing", "attention", "adhd") ->
            handleFocus().also { lastTopic = "focus" }
        any(lower, "relax", "calm", "stress", "anxiety", "anxious", "breathe",
            "unwind", "rest", "nervous", "tense", "panic") ->
            handleRelax().also { lastTopic = "relax" }
        any(lower, "tinnitus", "ringing", "ear ring", "hearing", "buzz in") ->
            handleTinnitus().also { lastTopic = "tinnitus" }
        any(lower, "baby", "infant", "newborn", "toddler", "child", "kids", "crying") ->
            handleBaby().also { lastTopic = "baby" }
        any(lower, "meditat", "mindful", "yoga", "zen", "chakra", "mantra") ->
            handleMeditation().also { lastTopic = "meditation" }
        any(lower, "technique", "breathwork", "breathing", "body scan", "loving-kindness",
            "loving kindness", "visualization", "visualisation", "how do i meditate",
            "how to meditate", "types of meditation") ->
            handleTechniques().also { lastTopic = "techniques" }

        // Emotional intent handlers — sadness, overwhelm, anger
        any(lower, "sad", "grief", "grieving", "heartbreak", "heartbroken",
            "lonely", "alone", "loneliness", "depressed", "depression",
            "cry", "crying", "upset", "miserable", "unhappy", "low mood") ->
            handleSadness().also { lastTopic = "sadness" }
        any(lower, "overwhelm", "overwhelmed", "burnout", "burnt out", "burned out",
            "too much", "cant cope", "can't cope", "too busy", "overloaded",
            "swamped", "falling apart", "breaking point", "can't take") ->
            handleOverwhelm().also { lastTopic = "overwhelm" }
        any(lower, "angry", "anger", "furious", "mad", "frustrated", "frustration",
            "rage", "irritated", "irritable", "annoyed", "wound up", "agitated") ->
            handleAnger().also { lastTopic = "anger" }

        any(lower, "vip", "upgrade", "pro plan", "subscription", "pricing",
            "plans", "cost", "buy", "purchase") ->
            handleVip().also { lastTopic = "" }
        any(lower, "recommend", "suggest", "what should", "which sound",
            "best sound", "what sound", "pick a sound", "help me choose") ->
            handleRecommendation().also { lastTopic = "recommendation" }
        any(lower, "thank", "thanks", "awesome", "great", "perfect",
            "love it", "amazing", "nice") ->
            handlePositive().also { lastTopic = "" }
        any(lower, "timer", "sleep timer", "how long", "duration", "how many minutes") ->
            handleTimer().also { lastTopic = "" }
        any(lower, "track", "tracks", "session", "sessions", "library", "guided",
            "which track", "what track", "play list", "playlist") ->
            handleAmbient().also { lastTopic = "" }
        any(lower, "play it", "play that", "start it", "queue it") ->
            handlePlayRequest().also { lastTopic = "" }
        else -> handleGeneral().also { lastTopic = "" }
    }

    private fun any(input: String, vararg kws: String) = kws.any { input.contains(it) }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private fun handleGreeting(): Pair<String, SoundType?> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isLate = hour >= 21 || hour < 6
        return if (isLate) Pair(
            "Hey there. Late hour — a good time to let the day go. 🌙\n\n" +
            "Based on what's soothed you before, I'd suggest starting with " +
            "${mostPlayed.emoji} ${mostPlayed.displayName} for tonight's wind-down.\n\n" +
            "Tell me: rest, relax, or want a specific sound?",
            mostPlayed
        ) else Pair(
            "Hello! ☀️ Spirit here, ready to set the mood for your practice.\n\n" +
            "I've noticed what tends to settle you — you're building a lovely habit. " +
            "What's the focus right now: focus, relax, or something else?",
            null
        )
    }

    private fun handleSleep(): Pair<String, SoundType?> {
        val rec = SoundType.EVENING_REVIEW
        return Pair(
            "🌙 Wind-Down Practice\n\n" +
            "A gentle stack to ease you toward rest:\n\n" +
            "1. ${rec.emoji} ${rec.displayName} — a Stoic nightly reflection to review the day with honesty and patient self-compassion\n" +
            "2. Timer: 30–45 min — drift off as the narration gently fades with you\n" +
            "3. Volume: 60–70% — present enough to anchor you, gentle enough not to intrude\n\n" +
            "A slow yogic breath like 'Soham' is also lovely right before sleep.\n\n" +
            "Want me to walk you through a wind-down breathing pattern? 🤍",
            rec
        )
    }

    private fun handleFocus(): Pair<String, SoundType?> = Pair(
        "🧠 Focus & Concentration\n\n" +
        "A grounding stack for clear, settled attention:\n\n" +
        "Workspace Flow 🧠 — warm chords and a gentle beat to ease you into deep concentration\n" +
        "Creative Catalyst 💻 — inspiring electronic ambient backdrop for coding or design\n" +
        "Cognitive Clarity 💡 — a clean, bright synthesizer soundscape to sharpen your thoughts\n\n" +
        "Or try these guided practices:\n" +
        "Vipassana ⚡ — systematic body-scanning to sharpen awareness and release tension\n" +
        "Buddho ☸️ — Thai forest recitation to align the breath with wakefulness\n\n" +
        "Your favourite so far: ${mostPlayed.emoji} ${mostPlayed.displayName} — a great anchor for steady attention!",
        SoundType.WORK_FOCUS_CHILLOUT
    )

    private fun handleRelax(): Pair<String, SoundType?> = Pair(
        "🌊 Relaxation Practice\n\n" +
        "Let's ease the tension. My recommendation:\n\n" +
        "Soham 🧘 — listen to the natural mantra of the breath\n" +
        "Autogenic Calm ❄️ — somatic relaxation through quiet autosuggestions\n" +
        "Thien 🎋 — breathe and smile in the middle of daily life\n\n" +
        "Try a slow 4-count inhale, 6-count exhale alongside any of these.\n\n" +
        "Spirit can also walk you through a full breathwork or body-scan practice — just ask.",
        SoundType.SOHAM
    )

    private fun handleTinnitus(): Pair<String, SoundType?> = Pair(
        "👂 Gentle Tinnitus Support\n\n" +
        "Spirit's suggestions for easing the focus on ringing:\n\n" +
        "Sumara 🌊 — Javanese surrender, a natural backdrop that gently draws attention outward\n" +
        "Vipassana ⚡ — systematic body-scanning to redirect attention away from the ringing\n" +
        "Sufi-toned breathing practices — redirect awareness to the breath instead\n\n" +
        "Start around 50% volume — lower than you'd think. Comfort matters more than coverage.\n\n" +
        "A body-scan practice can also help shift attention away from the ringing — want to try one?",
        SoundType.SUMARA
    )

    private fun handleBaby(): Pair<String, SoundType?> = Pair(
        "👶 Soothing Little Ones\n\n" +
        "A few gentle, narrated tracks that work well in a quiet nursery:\n\n" +
        "Soham 🧘 — slow, steady, easy to drift off to\n" +
        "Progressive Muscle Release 💪 — a soft, grounding physical relaxation rhythm\n" +
        "Sumara 🌊 — open and unhurried, a gentle constant presence\n\n" +
        "Keep the volume low and steady — soft enough for a quiet room.\n\n" +
        "Spirit's tip: loop a track at low volume after feeding to build a calming bedtime cue.",
        SoundType.SOHAM
    )

    private fun handleMeditation(): Pair<String, SoundType?> = Pair(
        "🧘 Meditation Soundscape\n\n" +
        "A gentle stack to support your sitting practice:\n\n" +
        "Thien 🎋 — Zen breathing and smiling in the middle of daily life\n" +
        "Tonglen 🏔️ — Tibetan practice of sending and taking to settle the mind\n" +
        "Muraqaba 👁️ — Sufi discipline of watchfulness and quiet observation\n\n" +
        "New to sitting: try 5 minutes with 'Muraqaba.'\n" +
        "Deepening your practice: try 'Thien' or 'Tonglen' for an extended, spacious sit.\n\n" +
        "Curious about a specific technique — mindfulness, breathwork, body scan, loving-kindness? Just ask.",
        SoundType.THIEN
    )

    private fun handleTechniques(): Pair<String, SoundType?> = Pair(
        "🌿 A few paths into stillness:\n\n" +
        "Mindfulness — rest attention on the breath, gently returning each time it wanders\n" +
        "Body Scan — move awareness slowly from head to toe, releasing tension as you go\n" +
        "Breathwork — slow, deliberate breathing patterns (try 4-7-8 or box breathing) to settle the nervous system\n" +
        "Loving-Kindness (Metta) — silently offer warmth and goodwill to yourself, then others\n" +
        "Visualization — picture a calm place or warm light filling the body\n" +
        "Mantra — repeat a word or phrase to anchor a wandering mind\n\n" +
        "Pair any of these with a track from the Sounds tab to set the mood. Want me to walk you through one?",
        SoundType.THIEN
    )

    private fun handleVip(): Pair<String, SoundType?> = Pair(
        "🌟 Unlock Meditation Portal\n\n" +
        "One purchase, everything — forever:\n\n" +
        "The full guided meditation library\n" +
        "Spirit, your meditation companion\n" +
        "Meditation alarm with gentle wake tones\n" +
        "Zero ads, forever\n\n" +
        "Just \$2.00, one time. No subscriptions, no surprises.\n\n" +
        "Tap Unlock on the main screen anytime you're ready! 🚀",
        null
    )

    private fun handleRecommendation(): Pair<String, SoundType?> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (suggestion, reason) = when {
            hour >= 22 || hour < 5 ->
                SoundType.EVENING_REVIEW to "a soft, unhurried close to the day — perfect for this hour"
            hour < 10 ->
                SoundType.ZHAN_ZHUANG to "settles a busy mind before the day picks up speed"
            hour < 18 ->
                SoundType.BUDDHO to "a hushed, focused space to anchor an afternoon practice"
            else ->
                SoundType.SOHAM to "eases the transition into a calm evening"
        }
        return Pair(
            "🤍 Spirit's Gentle Suggestion\n\n" +
            "Based on:\n" +
            "Time: ${hour}:00 — ${if (hour >= 22 || hour < 6) "wind-down window" else "active hours"}\n" +
            "Your favourite: ${mostPlayed.emoji} ${mostPlayed.displayName}\n" +
            "What's tended to settle you before\n\n" +
            "Right now, try: ${suggestion.emoji} ${suggestion.displayName}\n\n" +
            "Why: $reason.\n\n" +
            "Tap the card above to begin. 🎵",
            suggestion
        )
    }

    private fun handlePositive(): Pair<String, SoundType?> {
        val msgs = listOf(
            "You're so welcome. Rest easy and be gentle with yourself 🤍 Spirit's always here when you need a moment of stillness.",
            "That makes me glad! 🌙 Showing up for your practice matters — even a few quiet minutes a day adds up.",
            "Lovely! Consistency is what makes a practice — you're doing beautifully. 💪🌙"
        )
        return Pair(msgs[turnsCount % msgs.size], null)
    }

    private fun handleTimer(): Pair<String, SoundType?> = Pair(
        "⏱️ Session Timer Guidance\n\n" +
        "Spirit's gentle suggestions for session length:\n\n" +
        "10–15 min — a short pause between moments of your day\n" +
        "30 min — a standard wind-down or sitting practice\n" +
        "90 min — a full restorative session\n" +
        "8 hours — all-night gentle presence\n\n" +
        "A slow fade-out tends to feel more natural than an abrupt stop.\n\n" +
        "Tap Timer on the main screen to set your duration!",
        null
    )

    private fun handleAmbient(): Pair<String, SoundType?> = Pair(
        "🌌 Guided Meditations — Narrated Practices\n\n" +
        "Portal features a full library of guided meditation sessions — gentle, " +
        "narrated practices for releasing tension, grounding the body, and resting " +
        "the mind:\n\n" +
        "Breathing & release: Autogenic Calm, Progressive Muscle Release\n" +
        "Grounding & body-scan: Zhan Zhuang, Vipassana\n" +
        "Spacious awareness: Thien, Tonglen, Sumara, Muraqaba\n\n" +
        "Each session is a calm, narrated guide — easy to follow, easy to sink into.\n\n" +
        "Try 'Soham' for winding down, or 'Tonglen' any time " +
        "you need permission to simply rest.",
        SoundType.SOHAM
    )

    private fun handlePlayRequest(): Pair<String, SoundType?> = Pair(
        "Queuing up ${mostPlayed.emoji} ${mostPlayed.displayName} — your most-played soundscape!\n\n" +
        "Tap Play below, or find it in the grid on the main screen. 🎵\n\n" +
        "Settle in and breathe... 🌙",
        mostPlayed
    )

    private fun handleSadness(): Pair<String, SoundType?> = Pair(
        "💜 I hear you. Sadness deserves space — not fixing.\n\n" +
        "When we're low, the mind wants to understand *why*, but sometimes the most healing thing is simply to sit with the feeling rather than push it away.\n\n" +
        "A few practices that can help:\n\n" +
        "Tonglen 🏔️ — a Tibetan practice of breathing *with* pain rather than away from it; a quiet companion for grief\n" +
        "Loving-Kindness — offering yourself the same warmth you'd give a close friend\n" +
        "Hesychasm ⛪ — a gentle, wordless prayer of the heart; no effort required\n\n" +
        "You don't have to feel better right away. Spirit is here.\n\n" +
        "Would you like me to walk you through a loving-kindness practice? 🤍",
        SoundType.TONGLEN
    )

    private fun handleOverwhelm(): Pair<String, SoundType?> = Pair(
        "🌊 Overwhelm is a signal — your system is carrying more than it was designed to hold alone.\n\n" +
        "First: one slow exhale right now. Longer out than in. That alone begins to ease the physical grip.\n\n" +
        "Then, when you're ready:\n\n" +
        "Burnout Recovery 🔋 — a comforting ambient wave that releases mental overload; free to play now\n" +
        "Autogenic Calm ❄️ — quietly guides your body out of fight-or-flight, system by system\n" +
        "Box breathing — 4 counts in · 4 hold · 4 out · 4 hold — used by paramedics to restore calm fast\n\n" +
        "You don't need to solve everything right now. Just this breath, then the next.\n\n" +
        "Want me to walk you through box breathing?",
        SoundType.BURNOUT_RECOVERY
    )

    private fun handleAnger(): Pair<String, SoundType?> = Pair(
        "🔥 Anger is real — and it's often pointing at something that matters.\n\n" +
        "Don't suppress it. That drives it underground. Let it move through the body first:\n\n" +
        "A long, forceful exhale — longer out than in — activates the parasympathetic brake\n" +
        "Progressive Muscle Release 💪 — deliberately channels the energy out through the body\n" +
        "Vipassana ⚡ — teaches you to observe the sensation of anger without being swept away by it\n\n" +
        "After the heat passes, there's usually something underneath worth listening to — hurt, fear, or an unmet need.\n\n" +
        "Want to try a body-based practice for anger right now?",
        SoundType.VIPASSANA
    )

    private fun handleFollowUp(): Pair<String, SoundType?> = when (lastTopic) {
        "sleep" -> Pair(
            "🌙 Sleep Breathwork — Let's Begin\n\n" +
            "Lie down and close your eyes. Let your body feel heavy.\n\n" +
            "1. Breathe in slowly for 4 counts...\n" +
            "2. Hold gently for 2 counts...\n" +
            "3. Breathe out for 6 counts — let the day dissolve.\n\n" +
            "Repeat for 5–10 cycles. If thoughts arrive, simply label them 'thinking' and return to the breath. Nothing needs solving tonight.\n\n" +
            "Evening Review 🏛️ is queued — a Stoic wind-down to close the day with self-compassion. Drift off whenever you're ready. 🤍",
            SoundType.EVENING_REVIEW
        )
        "relax" -> Pair(
            "🌿 4-7-8 Breathwork — Begin Now\n\n" +
            "This pattern activates your parasympathetic nervous system within minutes:\n\n" +
            "1. Exhale fully through your mouth.\n" +
            "2. Inhale through your nose for 4 counts...\n" +
            "3. Hold for 7 counts...\n" +
            "4. Exhale slowly through your mouth for 8 counts.\n\n" +
            "Repeat 4 cycles. You may feel slightly light-headed — that's normal and will pass.\n\n" +
            "Soham 🧘 plays beautifully alongside this — let the mantra rhythm anchor your breath. 🌊",
            SoundType.SOHAM
        )
        "techniques" -> Pair(
            "🌿 Body Scan — Step by Step\n\n" +
            "Lie flat. Close your eyes. Three slow breaths to begin.\n\n" +
            "1. Bring attention to the crown of your head. Notice: warmth, tightness, nothing at all.\n" +
            "2. Move slowly down — forehead → jaw → neck → shoulders → arms → hands.\n" +
            "3. Continue — chest → belly → lower back → hips → thighs → knees → feet.\n" +
            "4. At each region, exhale and consciously let go.\n\n" +
            "No need to change anything — just observe. Thien 🎋 makes a beautiful backdrop for this. 🤍",
            SoundType.THIEN
        )
        "sadness" -> Pair(
            "💜 Loving-Kindness (Metta) — For Difficult Feelings\n\n" +
            "Sit quietly. Place one hand on your heart.\n\n" +
            "1. Silently repeat: 'May I be safe. May I be healthy. May I be at peace.'\n" +
            "2. Picture someone who loves you — feel that warmth directed toward you.\n" +
            "3. Repeat: 'May I be held. May I be comforted. May I know I'm not alone.'\n" +
            "4. When you're ready, extend the same wish outward to others who are hurting right now.\n\n" +
            "There's no pressure to feel better immediately — just to be present with yourself.\n\n" +
            "Tonglen 🏔️ works with pain rather than away from it — a companion for whatever you're carrying. 🤍",
            SoundType.TONGLEN
        )
        "overwhelm" -> Pair(
            "🌬️ Box Breathing — A Reset for an Overwhelmed System\n\n" +
            "Used by paramedics and pilots. Simple and very effective:\n\n" +
            "1. Breathe in for 4 counts.\n" +
            "2. Hold for 4 counts.\n" +
            "3. Breathe out for 4 counts.\n" +
            "4. Hold for 4 counts.\n\n" +
            "Repeat for 2–3 minutes. Your nervous system will begin to regulate itself.\n\n" +
            "After the breathing: write down just one thing you can actually control right now. Everything else can wait.\n\n" +
            "Autogenic Calm ❄️ will carry you the rest of the way — it guides your body into deep rest, one system at a time. 🌿",
            SoundType.AUTOGENIC_CALM
        )
        "anger" -> Pair(
            "🔥 Releasing Anger — A Body Practice\n\n" +
            "Anger is energy that needs somewhere to go:\n\n" +
            "1. Take a long, slow exhale — audible if that helps.\n" +
            "2. Tense every muscle in your body for 5 seconds... then release completely.\n" +
            "3. Notice where you hold anger in your body — jaw? shoulders? chest?\n" +
            "4. Breathe into that spot. On the exhale, imagine the tension leaving as heat.\n" +
            "5. Repeat silently: 'I am feeling angry. That's allowed. I don't have to act on it.'\n\n" +
            "Vipassana ⚡ is ideal here — it trains you to observe sensation without being consumed by it. 🌿",
            SoundType.VIPASSANA
        )
        else -> handleGeneral()
    }

    private fun handleGeneral(): Pair<String, SoundType?> {
        val fallbacks = listOf(
            "I'm listening. Tell me more — are you working with restlessness, stress, or scattered focus? Spirit can help with all three. 🤍",
            "Spirit here 🌙 — I can help with rest, focus, relaxation, and tinnitus support, or talk through a meditation technique. Mention any of those for a tailored suggestion!",
            "I hear you. 🌙 Based on what you've enjoyed before, ${mostPlayed.emoji} ${mostPlayed.displayName} might suit this moment well. Want to know why?",
            "Lovely chatting. Ask about a 'technique' to learn a new way to meditate, or say 'recommend' for a soundscape suggestion tailored to right now. 🎵"
        )
        return Pair(
            fallbacks[turnsCount % fallbacks.size],
            if (turnsCount % 4 == 2) mostPlayed else null
        )
    }
}
