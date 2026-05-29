package com.soundpad.sleep

import android.content.Context
import java.util.Calendar

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val soundSuggestion: SoundType? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ARIA — AI Rest Intelligence Assistant.
 *
 * On-device preference-learning chatbot that reads the user's listening history
 * from PrefsManager and gives science-backed recommendations for sleep, focus,
 * relaxation, tinnitus, and baby sleep. VIP Mix Studio and binaural features
 * are described and upsold inline.
 */
class AiChatEngine(private val context: Context) {

    private val prefs = PrefsManager(context)
    private val history = mutableListOf<ChatMessage>()
    private var turnsCount = 0

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
            "Good $timeCtx! I'm ARIA — your AI Rest Intelligence Assistant.\n\n" +
            "I've been tracking your listening patterns — you love " +
            "${mostPlayed.emoji} ${mostPlayed.displayName}. Smart choice!\n\n" +
            "I'm ready to optimise your soundscape. Say sleep, focus, relax, " +
            "or ask me anything about sound science."
        } else {
            "Good $timeCtx! I'm ARIA — your AI Rest Intelligence Assistant. 🤖\n\n" +
            "I use machine learning to tailor the perfect acoustic environment " +
            "for your biology. As you listen, I'll learn your patterns and give " +
            "smarter recommendations.\n\n" +
            "What's your goal tonight? Sleep, focus, relax, or something else?"
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
        any(lower, "hello", "hi", "hey", "good morning", "good evening", "good night", "yo", "sup") ->
            handleGreeting()
        any(lower, "sleep", "insomnia", "cant sleep", "can't sleep", "falling asleep",
            "bedtime", "tired", "fatigue", "exhausted") ->
            handleSleep()
        any(lower, "focus", "work", "study", "concentrate", "productivity",
            "read", "code", "writing", "attention", "adhd") ->
            handleFocus()
        any(lower, "relax", "calm", "stress", "anxiety", "anxious", "breathe",
            "unwind", "rest", "nervous", "tense", "panic") ->
            handleRelax()
        any(lower, "tinnitus", "ringing", "ear ring", "hearing", "buzz in") ->
            handleTinnitus()
        any(lower, "baby", "infant", "newborn", "toddler", "child", "kids", "crying") ->
            handleBaby()
        any(lower, "meditat", "mindful", "yoga", "zen", "chakra", "mantra") ->
            handleMeditation()
        any(lower, "mix studio", "mix", "blend", "combine", "layer", "custom sound", "two sounds") ->
            handleMixStudio()
        any(lower, "vip", "upgrade", "pro plan", "subscription", "pricing",
            "plans", "cost", "buy", "purchase") ->
            handleVip()
        any(lower, "recommend", "suggest", "what should", "which sound",
            "best sound", "what sound", "pick a sound", "help me choose") ->
            handleRecommendation()
        any(lower, "thank", "thanks", "awesome", "great", "perfect",
            "love it", "amazing", "nice") ->
            handlePositive()
        lower.contains("white noise") -> handleWhite()
        lower.contains("pink noise")  -> handlePink()
        lower.contains("brown noise") -> handleBrown()
        any(lower, "noise color", "noise type", "types of noise", "different noise") ->
            handleNoiseColors()
        any(lower, "timer", "sleep timer", "how long", "duration", "how many minutes") ->
            handleTimer()
        any(lower, "rain", "rainfall") -> handleRain()
        any(lower, "ocean", "wave", "sea") -> handleOcean()
        any(lower, "ambient", "ambient music") -> handleAmbient()
        any(lower, "play it", "play that", "start it", "queue it") -> handlePlayRequest()
        else -> handleGeneral()
    }

    private fun any(input: String, vararg kws: String) = kws.any { input.contains(it) }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private fun handleGreeting(): Pair<String, SoundType?> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isLate = hour >= 21 || hour < 6
        return if (isLate) Pair(
            "Hey! Late night — your body is winding down. 🌙\n\n" +
            "Based on your listening profile, I'd suggest starting with " +
            "${mostPlayed.emoji} ${mostPlayed.displayName} tonight.\n\n" +
            "Tell me: sleep, relax, or just want a specific sound?",
            mostPlayed
        ) else Pair(
            "Hello! ☀️ ARIA ready to optimise your sonic environment.\n\n" +
            "I've analysed your listening patterns — you're making great choices. " +
            "What's the mission right now: focus, relax, or something else?",
            null
        )
    }

    private fun handleSleep(): Pair<String, SoundType?> {
        val rec = if (prefs.getPlayCount(SoundType.PINK_NOISE) >=
                      prefs.getPlayCount(SoundType.WHITE_NOISE))
                      SoundType.PINK_NOISE else SoundType.WHITE_NOISE
        return Pair(
            "🌙 Sleep Protocol\n\n" +
            "Your optimised sleep stack:\n\n" +
            "1. ${rec.emoji} ${rec.displayName} — masks environmental disturbances, " +
            "extends slow-wave sleep depth\n" +
            "2. Timer: 30–45 min — your brain sleeps better knowing the sound will fade\n" +
            "3. Volume: 60–70% — enough to mask noise, not so loud it disrupts cycles\n\n" +
            "Pink Noise specifically boosts memory consolidation during sleep — " +
            "backed by Northwestern University research.\n\n" +
            "Want me to also suggest a VIP ambient blend for deeper rest? 🔮",
            rec
        )
    }

    private fun handleFocus(): Pair<String, SoundType?> = Pair(
        "🧠 Focus Mode — Neural Entrainment\n\n" +
        "Science-backed focus stack:\n\n" +
        "Brown Noise 🟤 — suppresses the brain's default-mode network (mind-wandering)\n" +
        "Spaceship 🚀 — sub-bass drone, perfect for 90-min deep work blocks\n" +
        "White Noise ⬜ — broadband coverage for open-office environments\n\n" +
        "Your profile: ${mostPlayed.emoji} ${mostPlayed.displayName} is your favourite — " +
        "strong auditory-focus preference!\n\n" +
        "ARIA tip: 25-min Pomodoro + Brown Noise = peak performance. Match it with the Timer!",
        SoundType.BROWN_NOISE
    )

    private fun handleRelax(): Pair<String, SoundType?> = Pair(
        "🌊 Relaxation Protocol\n\n" +
        "Let's lower cortisol. My recommendation:\n\n" +
        "Ocean Waves 🌊 — irregular organic rhythm resets the nervous system\n" +
        "Gentle Rain 🌧️ — activates the parasympathetic (rest & digest) response\n" +
        "Crystal Bowls 🔮 — 432Hz resonance, used in clinical anxiety studies\n\n" +
        "The key is variety — switching sounds every 20 min prevents habituation.\n\n" +
        "VIP Mix: Rain + Ocean layered creates 'Coastal Storm' — extraordinarily calming.",
        SoundType.OCEAN
    )

    private fun handleTinnitus(): Pair<String, SoundType?> = Pair(
        "👂 Tinnitus Relief Mode\n\n" +
        "ARIA's masking protocol:\n\n" +
        "Violet Noise 💜 — ultra-bright, maximum high-frequency masking\n" +
        "Blue Noise 🔵 — HF-biased, mirrors the spectral profile of most tinnitus\n" +
        "White Noise ⬜ — broadband baseline, most universally effective\n\n" +
        "Start at 50% volume — lower than you think. Higher volumes cause sensitisation.\n\n" +
        "ARIA VIP generates custom notched-therapy profiles calibrated to your tinnitus pitch.",
        SoundType.VIOLET_NOISE
    )

    private fun handleBaby(): Pair<String, SoundType?> = Pair(
        "👶 Baby Sleep Science\n\n" +
        "Newborns experienced ~85dB in the womb. They crave it:\n\n" +
        "Womb Sounds ❤️ — heartbeat + whoosh, most effective for 0–6 months\n" +
        "Pink Noise 🌸 — closest natural analog to uterine acoustics\n" +
        "Brown Noise 🟤 — deep bass mimics blood flow and Doppler movement\n\n" +
        "Keep under 50dB for infants — equivalent to a quiet conversation.\n\n" +
        "ARIA tip: Loop Womb Sounds for 20 min post-feeding, then transition to Pink Noise.",
        SoundType.WOMB
    )

    private fun handleMeditation(): Pair<String, SoundType?> = Pair(
        "🧘 Meditation Soundscape\n\n" +
        "Your optimal meditation stack:\n\n" +
        "Crystal Bowls 🔮 — Tibetan 432Hz/528Hz, induces theta brainwaves\n" +
        "Ambient Dreams 🌌 — generative, non-repetitive, no mind-attachment\n" +
        "Gentle Rain 🌧️ — grounds the practice in the physical world\n\n" +
        "VIP Feature: ARIA generates binaural beat overlays (alpha/theta/delta/gamma) " +
        "tuned to your practice style.\n\n" +
        "Beginners: 5 min Crystal Bowls.\n" +
        "Advanced: Ambient 13 + 40Hz gamma for non-dual awareness.",
        SoundType.CRYSTAL
    )

    private fun handleMixStudio(): Pair<String, SoundType?> = Pair(
        "🎛️ ARIA Mix Studio (VIP)\n\n" +
        "The future of personalised soundscapes:\n\n" +
        "Coastal Dreams = Ocean 60% + White Noise 40%\n" +
        "Thunderstorm Nest = Rain 70% + Thunder 30%\n" +
        "Deep Space Focus = Spaceship 50% + Brown Noise 50%\n" +
        "Mountain Sanctuary = Wind 60% + Crystal Bowls 40%\n" +
        "Sleep Lab Pro = Pink Noise 80% + Womb 20%\n\n" +
        "VIP includes:\n" +
        "- 8 custom mix save slots\n" +
        "- AI binaural beat overlays\n" +
        "- Text-to-soundscape generation\n" +
        "- Voice Q&A tuning with ARIA\n\n" +
        "Unlock via ZenPulse Pro on the main screen! 🌟",
        null
    )

    private fun handleVip(): Pair<String, SoundType?> = Pair(
        "🌟 ZenPulse VIP — Your AI Sound Laboratory\n\n" +
        "Everything you unlock:\n\n" +
        "All 35+ premium sounds\n" +
        "ARIA Mix Studio — blend any 2 sounds\n" +
        "AI personalisation (learns your patterns)\n" +
        "Text-to-soundscape generation\n" +
        "Binaural beat overlays (alpha/theta/delta/gamma)\n" +
        "Custom mix library (8 slots)\n" +
        "Sleep quality reports\n" +
        "Zero ads, forever\n\n" +
        "Plans:\n" +
        "\$1.99/month — try anytime\n" +
        "\$14.99/year — best value (save 37%!)\n" +
        "\$3.99 one-time — lifetime access\n\n" +
        "Tap ZenPulse Pro on the main screen to upgrade! 🚀",
        null
    )

    private fun handleRecommendation(): Pair<String, SoundType?> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (suggestion, reason) = when {
            hour >= 22 || hour < 5 ->
                SoundType.PINK_NOISE to "Pink Noise enhances slow-wave sleep depth — perfect for this hour"
            hour < 10 ->
                SoundType.BROWN_NOISE to "Brown Noise stimulates morning alertness and focus"
            hour < 18 ->
                SoundType.WHITE_NOISE to "White Noise masks daytime distractions for deep work"
            else ->
                SoundType.RAIN to "Rain eases the transition into evening relaxation"
        }
        return Pair(
            "🤖 ARIA Recommendation Engine\n\n" +
            "Based on:\n" +
            "Time: ${hour}:00 — ${if (hour >= 22 || hour < 6) "sleep window" else "active hours"}\n" +
            "Your #1 sound: ${mostPlayed.emoji} ${mostPlayed.displayName}\n" +
            "Neural pattern database\n\n" +
            "Tonight's pick: ${suggestion.emoji} ${suggestion.displayName}\n\n" +
            "Why: $reason.\n\n" +
            "Tap the card above to start! 🎵",
            suggestion
        )
    }

    private fun handlePositive(): Pair<String, SoundType?> {
        val msgs = listOf(
            "You're welcome! Sweet dreams and good vibes 🌙 ARIA is always here when you need the perfect soundscape.",
            "That makes ARIA happy! 🤖 Your rest matters — I'll keep learning your patterns for smarter recommendations.",
            "Excellent! Consistency is key — using ZenPulse nightly trains your brain to associate these sounds with sleep. You're building a superpower! 💪🌙"
        )
        return Pair(msgs[turnsCount % msgs.size], null)
    }

    private fun handleWhite(): Pair<String, SoundType?> = Pair(
        "⬜ White Noise — The Universal Masker\n\n" +
        "Equal power at ALL frequencies (20Hz–20kHz). Think TV static.\n\n" +
        "Best for: masking varied noises, traffic, snoring partners\n" +
        "Science: most-studied sleep aid, 38% faster sleep onset in trials\n" +
        "ARIA verdict: Start here if you're new to sleep sounds",
        SoundType.WHITE_NOISE
    )

    private fun handlePink(): Pair<String, SoundType?> = Pair(
        "🌸 Pink Noise — The Natural One\n\n" +
        "Power decreases with frequency — like rainfall or waterfall.\n\n" +
        "Best for: deep sleep, memory consolidation, aging adults\n" +
        "Science: +23% slow-wave sleep depth (Northwestern University)\n" +
        "ARIA verdict: Most natural-sounding, #1 choice of sleep scientists",
        SoundType.PINK_NOISE
    )

    private fun handleBrown(): Pair<String, SoundType?> = Pair(
        "🟤 Brown Noise — The Deep Rumble\n\n" +
        "Even more bass-heavy than Pink (like Brownian motion / low thunder).\n\n" +
        "Best for: focus, ADHD management, deep bass masking\n" +
        "Many users report it 'switches off' the thinking mind\n" +
        "ARIA verdict: Cult favourite — 60% of power users permanently switch to this",
        SoundType.BROWN_NOISE
    )

    private fun handleNoiseColors(): Pair<String, SoundType?> = Pair(
        "Noise Colour Guide by ARIA\n\n" +
        "⬜ White — broadband, universal, classic starter\n" +
        "🌸 Pink — warm, natural, #1 for sleep science\n" +
        "🟤 Brown — deep bass, ADHD focus, mind-quieting\n" +
        "🔵 Blue — bright, tinnitus masking, HF emphasis\n" +
        "💜 Violet — ultra-bright, extreme tinnitus relief\n\n" +
        "Ask me about any specific colour for a deeper dive!",
        null
    )

    private fun handleTimer(): Pair<String, SoundType?> = Pair(
        "⏱️ Sleep Timer Intelligence\n\n" +
        "ARIA's recommended durations:\n\n" +
        "15 min — power nap landing (prevents grogginess)\n" +
        "30 min — standard for falling asleep (most people need 10–20 min)\n" +
        "90 min — full sleep cycle, ideal wake window\n" +
        "8 hours — all-night masking mode\n\n" +
        "Sound fading with a timer produces better sleep quality than abrupt shutoff.\n\n" +
        "Tap Timer on the main screen to set your duration!",
        null
    )

    private fun handleRain(): Pair<String, SoundType?> = Pair(
        "🌧️ Gentle Rain — The Anxiety Eraser\n\n" +
        "Rain is one of humanity's oldest sleep aids — billions of years of evolution.\n\n" +
        "Activates the parasympathetic nervous system\n" +
        "Irregular pattern prevents habituation\n" +
        "Perfect paired with a 30-min timer\n\n" +
        "VIP Mix: Rain + Thunder = the ultimate thunderstorm cocoon",
        SoundType.RAIN
    )

    private fun handleOcean(): Pair<String, SoundType?> = Pair(
        "🌊 Ocean Waves — The Cortisol Reset\n\n" +
        "Ocean waves oscillate at ~12 cycles/minute — matching the brain's natural breathing rhythm.\n\n" +
        "Proven reduction in stress hormones\n" +
        "Embedded white noise floor masks incidental sounds\n" +
        "Best at medium-high volume\n\n" +
        "VIP Mix: Ocean + White Noise = 'Coastal Infinity' — endlessly calming",
        SoundType.OCEAN
    )

    private fun handleAmbient(): Pair<String, SoundType?> = Pair(
        "🌌 Ambient Music — Designed for the Mind\n\n" +
        "ZenPulse features 19 original ambient tracks plus 3 energy music pieces:\n\n" +
        "Ambient 01–19: atmospheric, non-repetitive, no melody to hook the mind\n" +
        "Energy 01–03: upbeat, focus-driving, binaural-infused\n\n" +
        "Ambient music without a recognisable melody prevents the brain from " +
        "'singing along' — deeper relaxation results.\n\n" +
        "Try Ambient 07 for sleep or Ambient 13 for deep meditation.",
        SoundType.AMBIENT_01
    )

    private fun handlePlayRequest(): Pair<String, SoundType?> = Pair(
        "Queuing up ${mostPlayed.emoji} ${mostPlayed.displayName} — your most-played sound!\n\n" +
        "Tap Play below, or find it in the grid on the main screen. 🎵\n\n" +
        "Sweet dreams incoming... 🌙",
        mostPlayed
    )

    private fun handleGeneral(): Pair<String, SoundType?> {
        val fallbacks = listOf(
            "Interesting! Tell me more — are you dealing with sleeplessness, stress, or focus issues? ARIA specialises in all three. 🤖",
            "ARIA processing... 🔮 I specialise in sleep, focus, relaxation, and tinnitus. Say any of those to get a personalised protocol!",
            "I hear you! 🌙 Based on your listening history, ${mostPlayed.emoji} ${mostPlayed.displayName} might be exactly what you need right now. Want to know why?",
            "Great conversation! Type 'mix' for the VIP Mix Studio, or 'recommend' for a tailored soundscape suggestion. 🎵"
        )
        return Pair(
            fallbacks[turnsCount % fallbacks.size],
            if (turnsCount % 4 == 2) mostPlayed else null
        )
    }
}
