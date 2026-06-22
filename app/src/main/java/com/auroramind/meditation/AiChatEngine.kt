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
    private val stats = StatsManager(context)
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
        val greeting = when {
            hour in 22..23 || hour < 5 -> "Hey — late night 🌙"
            hour < 12                  -> "Good morning 🌅"
            hour < 18                  -> "Good afternoon ☀️"
            else                       -> "Good evening 🌙"
        }
        val hasHistory = prefs.getPlayHistory().isNotEmpty()
        val streak = stats.currentStreak()
        val minutes = stats.totalMinutes()
        val sessions = stats.totalSessions()

        val streakLine = when {
            streak >= 30 -> "${streak}-day streak — a genuine, sustained practice. 🔥\n\n"
            streak >= 7  -> "${streak} days in a row. That kind of consistency builds something real. ✨\n\n"
            streak >= 3  -> "${streak} days running — momentum is forming. Keep showing up. 🌱\n\n"
            else         -> ""
        }
        val depthLine = when {
            minutes >= 60 && sessions >= 10 ->
                "${minutes} minutes across ${sessions} sessions — a meaningful investment in yourself.\n\n"
            else -> ""
        }
        val activeProgram = Programs.all.firstOrNull { prog ->
            val done = prefs.getProgramProgress(prog.id)
            done > 0 && done < prog.days.size
        }
        val programNudge = if (activeProgram != null) {
            val done = prefs.getProgramProgress(activeProgram.id)
            "${activeProgram.emoji} You're on Day $done of ${activeProgram.title} — ready to continue?\n\n"
        } else ""

        return if (hasHistory) {
            "$greeting! I'm Spirit — your meditation companion. 🤍\n\n" +
            streakLine +
            programNudge +
            "Welcome back. Your go-to is ${mostPlayed.emoji} ${mostPlayed.displayName} — great choice for whenever you're ready.\n\n" +
            depthLine +
            "What are you here for right now — rest, focus, relaxation, or something else?"
        } else {
            "$greeting! I'm Spirit — your meditation companion. 🤍\n\n" +
            "I can suggest a track for how you're feeling, walk you through a technique like breathwork " +
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
        // Crisis detection — always first, no topic context should override this
        any(lower, "suicidal", "suicide", "want to die", "don't want to live", "dont want to live",
            "end it all", "end my life", "kill myself", "self harm", "self-harm",
            "hurt myself", "no reason to live", "better off dead",
            "take my own life", "take my life",
            "life isn't worth living", "life isnt worth living", "not worth living",
            "don't want to be here anymore", "dont want to be here anymore",
            "don't want to exist", "dont want to exist",
            "unalive", "cutting myself", "overdose",
            "cant go on like this", "can't go on like this",
            "feel like a burden", "like such a burden", "being a burden", "am a burden", "i'm a burden", "i am a burden",
            "better off without me", "world would be better without me",
            "nothing to live for", "nothing left to live for",
            "want to disappear", "wish i could disappear", "just want to disappear",
            "wish i was dead", "wish i were dead",
            "no point going on", "no point carrying on", "no point in carrying on",
            "can't carry on", "cant carry on",
            "ending it all", "ending my life",
            "harming myself", "harm myself",
            "thoughts of suicide", "thoughts of ending my life",
            "suicidal thoughts", "suicidal ideation") ->
            handleCrisis()

        // Follow-up detection — must come first so "yes please" gets context-aware reply
        lastTopic.isNotEmpty() && (any(lower,
            "please", "okay", "go on", "continue", "next",
            "tell me more", "walk me through", "guide me", "show me",
            "how do i", "teach me", "what do i do", "let's do it", "lets do it",
            "let's go", "lets go", "go ahead", "sounds good", "i'd like that",
            "i would like that", "absolutely", "of course", "why not") ||
            anyWord(lower, "yes", "sure", "ok")) ->
            handleFollowUp()

        any(lower, "hello", "good morning", "good afternoon", "good evening", "good night",
            "howdy", "greetings", "what's up", "whats up") ||
        anyWord(lower, "hi", "hey", "yo", "sup") ->
            handleGreeting().also { lastTopic = "" }
        // [Topic] timer phrases — fire before all topic routes so "sleep timer" reaches timer, not sleep
        any(lower, "sleep timer", "nap timer", "meditation timer", "meditate timer",
            "breathing timer", "breathwork timer", "relaxation timer", "yoga timer",
            "anxiety timer", "focus timer", "energy timer") ->
            handleTimer().also { lastTopic = "" }
        // Baby+sleep compounds must come before the bare "sleep" route to win
        any(lower, "baby sleep", "baby won't sleep", "baby can't sleep", "baby keeps waking",
            "infant sleep", "toddler sleep", "toddler won't sleep", "child won't sleep") ->
            handleBaby().also { lastTopic = "baby" }
        // Sleep paralysis — frightening, needs grounding/safety response, not wind-down advice
        any(lower, "sleep paralysis", "paralyzed in sleep", "paralyzed while sleeping",
            "paralyzed when waking", "awake but can't move", "awake but cant move",
            "woke up couldn't move", "woke up cant move", "frozen while sleeping",
            "frozen when waking", "frozen on waking", "frozen waking up",
            "cant move when waking", "can't move when waking") ->
            handleSleepParalysis().also { lastTopic = "relax" }
        any(lower, "sleep", "insomnia", "cant sleep", "can't sleep", "falling asleep",
            "bedtime", "tired", "fatigue", "exhausted", "exhaustion", "wide awake", "cant switch off",
            "can't switch off", "racing mind", "racing thoughts", "napping", "night shift",
            "shift work", "mind won't stop", "mind wont stop", "nightmare", "nightmares", "bad dreams",
            "jet lag", "jet lagged", "jet-lagged", "restless", "restlessness",
            "woke up at", "keep waking", "3am", "4am", "middle of the night",
            "drift off", "drifting off", "can't drift",
            "counting sheep", "wired at night", "wired tonight", "can't wind down",
            "cant wind down", "light sleeper", "heavy sleeper", "sleep hygiene",
            "body clock", "circadian", "night terrors",
            "night sweats", "hot flashes", "hot flush", "menopause", "perimenopause",
            "can't turn off", "cant turn off", "turn my brain off",
            "brain won't stop", "brain wont stop",
            "never feel rested", "never fully rested", "wake up exhausted",
            "wake up tired", "wake up unrested",
            "lucid dreaming", "lucid dream", "have lucid dreams",
            "long covid", "post covid", "post-covid", "post-viral fatigue",
            "chronic fatigue syndrome", "me/cfs",
            "sleep apnea", "sleep apnoea", "apnea", "apnoea",
            "restless legs", "restless leg syndrome",
            "snoring", "snore",
            "narcolepsy", "narcoleptic",
            "sleepwalking", "somnambulism",
            "hypersomnia", "excessive daytime sleepiness",
            "melatonin", "taking melatonin", "melatonin supplement",
            "magnesium for sleep", "sleep supplement",
            "vivid dreams", "vivid dream", "recurring dreams", "recurring dream",
            "strange dream", "weird dream", "unsettling dream", "dreams every night",
            "had a dream", "keep having dreams", "dream last night",
            "dreamed about", "dreamt about", "keep dreaming", "remember my dreams",
            "disturbing dreams", "disturbing dream") ||
        anyWord(lower, "nap", "rls") ->
            handleSleep().also { lastTopic = "sleep" }
        any(lower, "focus", "study", "concentrat", "productivity",
            "writing", "attention", "adhd", "distract",
            "brain fog", "foggy", "mental clarity", "sharp", "clear mind",
            "procrastinat", "multitasking", "information overload",
            "doom scrolling", "doomscrolling", "doom-watching", "doom loop", "doom spiral",
            "mindless scrolling", "phone addiction",
            "screen addiction", "endless scrolling", "too much screen", "screen time",
            "mental block", "writer's block", "writers block", "creative block",
            "brain freeze", "can't think straight", "cant think straight",
            "mind blank", "mind went blank", "mind has gone blank",
            "analysis paralysis", "decision paralysis", "overthinking decisions",
            "hyperfocus", "hyperfocusing", "can't switch tasks",
            "cant switch tasks", "task switching",
            "poor memory", "memory problems", "memory issues", "bad memory",
            "my memory", "memory loss", "memory lapses", "terrible memory",
            "keep forgetting", "can't remember", "cant remember",
            "forget things", "forget everything", "forgetting everything",
            "mind wanders", "wandering mind",
            "executive function", "executive dysfunction",
            "working memory", "cognitive load", "mental load",
            "need clarity", "lack of clarity", "need to think clearly",
            "time blindness", "neurodivergent",
            "autism", "autistic", "asperger", "aspergers", "asd",
            "grounding",
            "flow state", "get into flow", "enter flow", "in the zone",
            "deep work", "deep focus", "focus mode", "distraction-free",
            "context switching", "working from home", "wfh distractions",
            "home office distractions",
            "brain training", "cognitive training", "mental fitness", "mental agility",
            "brain fitness", "mental sharpness", "sharpen my mind",
            "memory improvement", "improve my memory", "improve memory",
            "memory exercises", "memory training",
            "revision", "revising", "revise", "exam revision",
            "pomodoro", "pomodoro technique",
            "scattered thoughts", "scattered brain", "scattered thinking",
            "can't organise my thoughts", "can't organize my thoughts",
            "disorganised thinking", "disorganized thinking") ||
        anyWord(lower, "read", "code") ->
            handleFocus().also { lastTopic = "focus" }
        any(lower, "energy", "energise", "energize", "wake up", "waking up",
            "uplift", "motivat", "active", "exercise", "workout",
            "morning boost", "morning energy", "sluggish", "lethargic",
            "cold shower", "cold water", "ice bath", "wim hof",
            "afternoon slump", "afternoon crash", "2pm slump", "post-lunch dip",
            "pick me up", "need a boost", "feeling flat", "flat today",
            "drained", "wiped out", "run down", "worn out", "no drive",
            "groggy", "grogginess", "brain dead", "zombie mode",
            "recharge", "recharging", "need to recharge", "fully recharged",
            "listless", "listlessness", "vitality", "low vitality", "no vitality",
            "feel dull", "feeling dull", "flat energy", "energy levels low",
            "thyroid", "thyroid issues", "underactive thyroid", "hypothyroid", "hypothyroidism",
            "cortisol", "cortisol levels", "high cortisol", "cortisol spike",
            "adrenal", "adrenals are",
            "cold plunge", "cold-plunge", "cold exposure", "cold therapy", "cold water therapy",
            "afternoon energy dip", "mid-afternoon dip", "mid-afternoon crash",
            "morning slump", "mid-morning slump",
            "no enthusiasm", "lacking enthusiasm", "no passion",
            "caffeine crash", "coffee crash", "sugar crash",
            "need coffee", "running low", "low battery") ->
            handleEnergy().also { lastTopic = "energy" }
        any(lower, "relax", "calm", "stress", "anxiety", "anxious", "breathe",
            "unwind", "nervous", "panic", "overthink", "overthinking",
            "can't stop thinking", "cant stop thinking", "intrusive thoughts", "ruminating",
            "social anxiety", "public speaking", "presentation nerves", "exam nerves",
            "stage fright", "interview nerves", "interview anxiety", "nerves before",
            "performance anxiety", "performance pressure",
            "pounding heart", "heart racing", "mind keeps wandering", "can't stop my mind",
            "cant stop my mind", "racing heart", "trauma", "traumatic", "ptsd",
            "post-traumatic", "triggered", "hyperventilat", "chest tight",
            "tight chest", "can't breathe", "cant breathe",
            "fear", "scared", "frightened", "phobia", "afraid",
            "worry", "worried", "worrying", "i worry", "constant worry",
            "dissociation", "dissociating", "dissociated", "derealization", "depersonalization",
            "feel unreal", "feeling unreal", "not feeling real", "feel detached",
            "ocd", "obsessive", "compulsive thoughts",
            "freaking out", "freak out", "losing my mind", "losing it",
            "can't cope with", "cant cope with", "spiralling", "spiraling",
            "dwell on", "dwelling on", "can't stop dwelling", "keep dwelling",
            "negative thoughts", "negative thinking", "negative self-talk",
            "thought spiral", "thought spirals", "racing thoughts",
            "dread", "dreading", "sense of dread",
            "hypervigilant", "hypervigilance",
            "sunday scaries", "anticipatory anxiety",
            "catastrophiz", "catastrophising", "what if thoughts",
            "fight or flight", "fight-or-flight", "adrenaline spike",
            "on edge", "jittery", "jitters", "butterflies in",
            "trembling", "can't stop shaking", "cant stop shaking",
            "shortness of breath", "breathlessness", "out of breath",
            "palpitations", "heart palpitations", "heart flutters",
            "vagus nerve", "vagal", "somatic therapy", "somatic healing", "somatic exercises",
            "nervous system regulation", "regulate my nervous system",
            "emotional regulation", "regulate my emotions", "emotional dysregulation",
            "can't regulate", "cant regulate",
            "muscle tension", "body tension", "physical tension",
            "neck tension", "shoulder tension",
            "pms", "pmdd", "premenstrual", "period symptoms",
            "hormonal anxiety", "hormonal mood",
            "sensory overload", "sensory overwhelm", "sensory sensitivity",
            "rejection sensitive", "rejection sensitivity",
            "rejection sensitive dysphoria",
            "mind is racing", "head is spinning", "head is full",
            "can't quiet my mind", "cant quiet my mind", "quiet my mind",
            "feel jumpy", "jumpy", "overstimulated",
            "stomach in knots", "knot in my stomach", "stomach tied in knots",
            "stomach churning", "knot in stomach",
            "new job nerves", "starting a new job", "first day nerves",
            "sensory processing", "sensory processing disorder",
            "tight shoulders", "tense shoulders", "clenching my fists", "clenching fists",
            "gritting my teeth", "gritting teeth",
            "freeze response", "fawn response", "fawn mode",
            "hyperarousal", "hypoarousal",
            "polyvagal", "job interview", "going for a job interview",
            "performance review", "annual review", "appraisal",
            "hormonal changes", "hormone changes", "hormone imbalance",
            "hormones all over the place",
            "nervous system dysregulation",
            "unsettled", "uncertainty", "uncertain", "uneasy",
            "mind chatter", "mental chatter", "busy mind",
            "chattering mind", "monkey mind",
            "stuck in my head", "living in my head", "all in my head",
            "decompress", "decompressing", "need to decompress",
            "need a breather", "catch my breath", "need some space",
            "drinking to cope", "drink to cope", "alcohol to cope", "drink to forget",
            "drinking to forget", "using alcohol", "using drink",
            "blood pressure", "high blood pressure", "hypertension",
            "cardiac stress", "heart health stress",
            "social pressure", "peer pressure", "exam pressure",
            "performance pressure", "pressure to perform",
            "heart is racing", "heart races", "heart started racing",
            "heart is pounding", "heart has been pounding",
            "noise sensitivity",
            "flashback", "having flashbacks", "intrusive memories",
            "body memories", "trauma response", "trauma trigger",
            "hypochondria", "hypochondriac", "health anxiety disorder",
            "illness anxiety", "medical anxiety",
            "wind down", "wind-down", "winding down", "need to wind down") ||
        anyWord(lower, "rest", "tense", "rsd") ->
            handleRelax().also { lastTopic = "relax" }
        any(lower, "tinnitus", "ringing", "ear ring", "hearing", "buzz in",
            "hyperacusis", "misophonia", "sound sensitivity", "ear noise",
            "noise in my ears", "noise in my head",
            "humming in my ear", "whistling in my ear", "clicking in my ear",
            "whooshing in my ear", "throbbing in my ear", "blocked ear",
            "ears feel blocked", "ear fullness", "ear pressure",
            "auditory processing") ->
            handleTinnitus().also { lastTopic = "tinnitus" }
        any(lower, "baby", "infant", "newborn", "toddler", "child", "kids",
            "new parent", "new mum", "new mom", "new dad", "new father",
            "first time parent", "new baby",
            "postpartum", "postnatal", "post natal", "post-natal",
            "breastfeeding", "breast feeding", "nursing baby",
            "colic", "baby blues", "maternity leave", "paternity leave",
            "pregnant", "expecting a baby", "expecting baby",
            "trying to conceive", "ivf", "fertility treatment",
            "first trimester", "second trimester", "third trimester",
            "teething", "cluster feeding", "tongue tie", "tongue-tie",
            "mastitis", "weaning", "feeding schedule", "growth spurt",
            "overdue", "c-section", "caesarean", "surrogacy", "surrogate",
            "adoption", "adopted", "foster parent", "fostering",
            "premature baby", "prem baby", "neonatal", "nicu") ->
            handleBaby().also { lastTopic = "baby" }
        // "How long" queries containing "meditat" must fire before the bare meditation route
        any(lower, "how long to meditate", "how long should i meditate",
            "how many minutes to meditate", "how long for meditation",
            "how long should i practice") ->
            handleTimer().also { lastTopic = "" }
        any(lower, "how long have i been meditating", "how long have i been practicing",
            "how many times have i meditated", "how many sessions have i done") ->
            handleStats().also { lastTopic = "" }
        any(lower, "meditat", "mindful", "yoga", "chakra", "mantra",
            "vipassana", "tonglen", "soham", "thien", "sumara", "muraqaba",
            "hesychasm", "dhikr", "hitbodedut", "zhan zhuang", "buddho",
            "sufi", "tibetan", "qigong", "stoic", "stoicism", "spiritual",
            "kundalini", "kundalini yoga",
            "namaste", "contemplation", "contemplative", "centering prayer",
            "lectio divina", "contemplative prayer",
            "tai chi", "tai-chi", "taichi", "nidra", "nsdr", "non-sleep deep rest",
            "open monitoring", "open awareness", "choiceless awareness",
            "witnessing meditation", "pure awareness", "awareness practice",
            "non-dual", "nondual",
            "inner peace", "peace of mind", "peaceful mind", "inner calm",
            "present moment", "be present", "stay present", "living in the moment",
            "higher self", "soul work", "true self", "authentic self",
            "stillness", "still the mind", "still my mind", "being still",
            "sit with myself", "sitting with", "inner silence") ||
        anyWord(lower, "zen") ->
            handleMeditation().also { lastTopic = "meditation" }
        any(lower, "technique", "breathwork", "breathing", "body scan", "loving-kindness",
            "loving kindness", "visualization", "visualisation", "how do i meditate",
            "how to meditate", "types of meditation", "autogenic", "box breath",
            "4-7-8", "4 7 8", "physiological sigh", "progressive muscle", "metta",
            "self-compassion", "self compassion", "compassion practice", "kind to myself",
            "self esteem", "self-esteem", "low confidence", "build confidence", "self-worth",
            "self worth", "confidence", "stretching", "morning routine", "bored", "boredom",
            "morning pages", "habit stacking", "habit tracker", "daily habit",
            "habit formation", "habit building", "build a habit", "building habits",
            "morning practice", "evening practice", "night routine",
            "cbt", "cognitive behavioral", "dbt", "dialectical behavior",
            "act therapy", "acceptance and commitment", "emdr",
            "body doubling", "pomodoro", "time blocking",
            "self-care", "self care", "self-care routine", "self care routine",
            "self-care practice", "taking care of myself", "look after myself",
            "evening routine", "wind down routine", "wind-down routine",
            "setting boundaries", "healthy boundaries", "set boundaries",
            "establish boundaries", "learn to say no",
            "window of tolerance", "nervous system reset",
            "polyvagal theory", "co-regulation", "coregulation",
            "pranayama", "alternate nostril", "pranic breathing",
            "personal growth", "personal development", "self-development",
            "introspection", "self-discovery", "know myself better",
            "work on myself", "self-awareness",
            "mirror work", "mirror meditation",
            "parts work", "internal family systems", "ifs therapy",
            "inner parts", "self parts", "exile", "exiles",
            "reframing", "cognitive reframing", "reframe my thoughts",
            "thought patterns", "unhelpful thoughts", "unhelpful thinking",
            "mindset shift", "shift my mindset", "mindset work", "growth mindset",
            "tapping", "eft tapping", "emotional freedom technique",
            "acupressure", "acupuncture for",
            "hypnosis", "self-hypnosis", "hypnotherapy",
            "nlp", "neuro-linguistic", "neuro linguistic programming",
            "mental wellness", "emotional wellness", "wellness journey",
            "personal development", "personal growth journey", "self-improvement",
            "self-development",
            "self-care", "self care", "self care routine", "self-care routine",
            "breath of fire", "kapalbhati", "ujjayi", "pranayama") ->
            handleTechniques().also { lastTopic = "techniques" }

        // Emotional intent handlers — sadness, overwhelm, anger
        any(lower, "sad", "grief", "grieving", "heartbreak", "heartbroken",
            "lonely", "alone", "loneliness", "depressed", "depression",
            "cry", "crying", "sobbing", "weeping", "in tears", "tearing up",
            "burst into tears", "bawling",
            "upset", "miserable", "unhappy", "low mood",
            "empty inside", "feel empty", "feeling empty", "hopeless", "helpless",
            "despair", "despairing", "in despair", "feel desperate", "feeling desperate",
            "devastated", "feel devastated",
            "hollow", "disconnected", "meaningless", "no motivation", "nothing matters",
            "gaslighting", "gaslit", "being gaslit", "emotional abuse",
            "bipolar", "manic", "mania", "manic episode", "hypomania",
            "breakup", "broke up", "split up", "feeling blue", "feeling lost",
            "lost and", "i feel lost", "feel so lost", "blue today", "can't find", "lost myself",
            "bereaved", "bereavement", "loss of", "lost someone", "lost my",
            "losing my", "losing someone", "losing a",
            "passed away", "death of", "missing them", "miss them so",
            "isolated", "feeling isolated", "so isolated",
            "got fired", "just fired", "lost my job", "lost their job", "laid off",
            "made redundant", "partner left me", "been left",
            "relationship", "divorce", "divorc", "separation", "separated from",
            "feeling down", "feel down", "i feel down", "feeling low", "feel low",
            "i feel low", "feel so low", "feeling so low", "feeling very low",
            "life feels pointless", "feels pointless", "feel pointless",
            "mood swings", "bad mood", "my mood", "mental health",
            "i'm suffering", "need to vent", "need to talk", "venting",
            "mourning", "in mourning", "heartache", "missing my ex", "miss my ex",
            "betrayed", "betrayal", "feel betrayed", "been betrayed",
            "cheated on", "been cheated", "trust issues", "can't trust",
            "feel like a burden", "i'm a burden", "am a burden",
            "winter blues", "lack of sunlight", "low in winter",
            "seasonal affective", "seasonal depression", "sad disorder",
            "no purpose", "lack of purpose", "feel purposeless", "existential",
            "longing", "longing for", "miss him", "miss her",
            "yearning", "yearning for", "yearning to", "deep yearning",
            "homesick", "homesickness", "missing home", "miss home",
            "empty nest", "empty nester", "kids moved out", "children left home",
            "miscarriage", "stillbirth", "pregnancy loss", "child loss", "infertility",
            "lost my baby", "lost our baby",
            "complicated grief", "anticipatory grief", "disenfranchised grief",
            "ambiguous loss",
            "cancer diagnosis", "cancer treatment", "living with cancer",
            "terminal illness", "terminal diagnosis", "life-limiting illness",
            "feeling old", "getting older", "fear of aging", "fear of getting old",
            "growing old", "not young anymore",
            "feel rejected", "feel abandoned", "abandoned", "abandonment", "abandonment issues",
            "rejection", "been rejected",
            "apathy", "apathetic", "feel apathetic", "feeling apathetic",
            "numbness", "feeling numb", "feel numb", "went numb", "gone numb",
            "couples therapy", "marriage counselling", "marriage counseling",
            "relationship counselling", "relationship counseling",
            "feel invisible", "feel unseen", "feel unloved", "feel unlovable",
            "unlovable", "not loved", "no one cares",
            "not great", "not so great", "not feeling great", "not doing great",
            "feeling off", "bit off", "not myself", "off today", "not okay",
            "not ok today", "not doing ok", "not doing well",
            "unwell", "not well", "not feeling well", "feeling unwell",
            "not fine", "i'm not fine", "im not fine",
            "feels off", "feel off", "something feels off", "something's off",
            "not like myself", "not quite myself", "not feeling like myself",
            "don't feel like myself", "dont feel like myself",
            "don't seem like myself", "dont seem like myself",
            "in a funk", "bit of a funk", "in a bit of a funk",
            "in my feelings", "in the dumps", "down in the dumps",
            "need hope", "need some hope", "lost all hope",
            "feeling low", "feel low", "so low", "really low", "been feeling low",
            "feeling blue", "feel blue", "so blue",
            "feeling down", "feel down", "been feeling down", "really down", "so down",
            "widowed", "widow", "ghosted",
            "feel like giving up", "want to give up", "ready to give up",
            "thinking of giving up", "about to give up",
            "what's the point", "whats the point", "what is the point",
            "no sense of belonging", "don't belong", "dont belong",
            "no one understands", "nobody understands",
            "feel misunderstood", "always misunderstood",
            "people always leave", "everyone leaves", "people keep leaving",
            "doom and gloom",
            "find my purpose", "finding my purpose", "life purpose",
            "searching for meaning", "search for meaning",
            "find meaning", "sense of meaning",
            "rough patch", "going through a rough time", "going through a hard time",
            "in a dark place", "dark place", "bad place right now",
            "hard time right now", "difficult place",
            "really struggling", "been struggling", "struggle is real",
            "emotional healing", "healing journey", "still healing",
            "trying to heal", "on a healing journey",
            "been through a lot", "gone through a lot",
            "a lot to process", "so much to process",
            "carrying a lot", "carrying so much",
            "coping with loss", "cope with loss", "dealing with loss",
            "living with grief", "coping with grief", "cope with grief",
            "identity crisis", "midlife crisis", "mid-life crisis",
            "quarterlife crisis", "quarter-life crisis", "quarter life crisis",
            "heavy heart", "heart feels heavy", "heart is heavy",
            "fomo", "fear of missing out",
            "melancholy", "melancholic", "ennui", "bereft", "forlorn",
            "bpd", "borderline personality", "borderline personality disorder",
            "conflicted", "ambivalent", "mixed feelings",
            "feel nothing", "can't feel anything", "cant feel anything",
            "antidepressant", "antidepressants", "ssri", "ssris",
            "unexpected loss", "sudden loss", "unexpected death",
            "anhedonia", "going through the motions", "on autopilot",
            "feel like a robot", "feeling like a robot", "like a zombie",
            "nothing brings me joy", "joy has gone", "lost my spark",
            "lost my zest", "no zest for life",
            "finding my calling", "what am i here for", "why am i here",
            "cyclothymia", "dysthymia", "persistent depressive",
            "love bombing", "situationship", "situationships",
            "dark night of the soul", "spiritual dryness",
            "narcissistic abuse", "narcissist partner", "narcissistic partner",
            "coercive control", "emotional manipulation",
            "codependency", "codependent", "codependent relationship",
            "avoidant attachment", "attachment wound", "attachment issues",
            "fearful avoidant", "disorganized attachment",
            "estranged", "estrangement", "family estrangement", "estranged from",
            "cut off from family", "family cut me off", "cut off by family",
            "rock bottom", "hit rock bottom", "at rock bottom",
            "fall to pieces", "falling to pieces", "going to pieces",
            "feel unwanted", "feeling unwanted", "feel unloved by",
            "identity loss", "loss of identity",
            "divorce", "divorcing", "getting divorced", "filed for divorce",
            "separation", "separated", "we're separated", "going through a separation",
            "broke up", "broken up", "we broke up", "just broke up",
            "she left me", "he left me", "they left me", "my partner left",
            "ended the relationship", "end of relationship", "relationship ended",
            "feel empty", "feeling empty", "emotional emptiness", "inner emptiness",
            "bipolar", "bipolar disorder",
            "manic", "manic episode", "manic phase", "depressive episode",
            "hypomania", "hypomanic",
            "hate my life", "hate this life", "hate life", "i hate my life",
            "hate everything",
            "layoff", "layoffs", "being laid off", "facing layoff",
            "retrenchment", "downsizing", "made redundant",
            "coming out", "coming out to my", "came out as", "came out to",
            "gender dysphoria", "queer identity", "questioning my sexuality",
            "questioning my gender",
            "losing my faith", "lost my faith", "faith crisis", "crisis of faith",
            "deconstructing my faith", "deconversion", "leaving my religion",
            "leaving the church") ||
        anyWord(lower, "numb", "died", "vent") ->
            handleSadness().also { lastTopic = "sadness" }
        any(lower, "shame", "ashamed", "guilt", "guilty", "i feel guilty",
            "i feel ashamed", "embarrassed", "humiliated", "self-blame", "self blame",
            "blame myself", "blaming myself", "i keep beating myself up", "beating myself up",
            "hate my body", "body image", "feel ugly", "look ugly", "feel so ugly", "i look ugly",
            "feel worthless", "worthless", "i'm worthless", "im worthless", "not good enough",
            "feel like a failure", "i'm a failure", "im a failure", "i am a failure",
            "feel inadequate", "i feel inadequate", "feel unworthy", "i feel unworthy",
            "forgive myself", "self-forgiveness", "self forgiveness",
            "perfectionism", "perfectionist", "never good enough", "hard on myself",
            "i'm not perfect", "im not perfect", "i am not perfect",
            "nobody's perfect", "nobody is perfect", "nothing is perfect",
            "too hard on myself", "self-critical", "self critical",
            "body image issues", "body image problem", "negative body image",
            "body dysmorphia", "dysmorphia", "body dysmorphic",
            "struggle with my body", "hate how i look", "hate my appearance",
            "eating disorder", "disordered eating",
            "anorexia", "anorexic", "bulimia", "bulimic", "binge eating", "binge and purge",
            "orthorexia", "orthorexic",
            "body shaming", "body shame", "body shamed", "body-shaming",
            "comparing myself", "comparison trap", "always comparing",
            "compare myself", "social comparison",
            "self-sabotage", "self sabotage", "self-sabotaging",
            "inner critic", "critical of myself", "critical voice",
            "i'm my own worst enemy", "im my own worst enemy",
            "my own worst enemy",
            "not measuring up", "not living up to", "can't live up to",
            "cant live up to", "don't feel good enough", "dont feel good enough",
            "inner child", "inner child work", "reparenting", "re-parenting",
            "imposter syndrome", "impostor syndrome",
            "feel like a fraud", "feel like such a fraud",
            "feeling like a fraud", "feeling like such a fraud",
            "feel like an imposter", "feel like such an imposter",
            "feeling like an imposter",
            "addiction", "in recovery", "sobriety", "substance abuse",
            "drug addiction", "drug problem", "drinking problem", "alcohol problem",
            "quitting drinking", "staying sober", "getting sober",
            "self-destruct", "self-destructive", "self-destructive behavior", "self-destructive patterns",
            "self-punishment", "punishing myself", "self-punishing",
            "emotional eating", "comfort eating", "eating my feelings",
            "feel judged", "feeling judged", "being judged",
            "fear of judgment", "fear of being judged", "feel watched",
            "hate myself", "i hate myself", "hate who i am", "i hate who i am",
            "hate myself so much", "hate myself for",
            "feel like i'm failing", "feel like im failing", "i'm failing at",
            "im failing at", "failing as a parent", "failing as a partner",
            "failing at everything", "failing at life") ||
        anyWord(lower, "sober") ->
            handleShameGuilt().also { lastTopic = "sadness" }
        any(lower, "overwhelm", "overwhelmed", "burnout", "burnt out", "burned out", "burning out",
            "too much", "cant cope", "can't cope", "too busy", "overloaded",
            "swamped", "falling apart", "breaking point", "can't take",
            "work stress", "work anxiety", "work is killing me", "job stress",
            "feeling stuck", "feel stuck", "stuck in a rut", "stuck in life",
            "deadline", "under pressure", "work pressure", "pressure at work",
            "work-life balance", "work life balance", "no time for myself", "no time for me",
            "single parent", "single mum", "single mom", "single dad",
            "sole parent", "solo parenting", "solo parent",
            "people pleaser", "people-pleaser", "cant say no", "can't say no",
            "always putting others first", "never put myself first", "spread too thin",
            "running on empty", "meltdown", "having a meltdown", "on the edge",
            "at my limit", "hit my limit", "can't handle it", "cant handle it",
            "can't handle this", "cant handle this",
            "can't keep up", "cant keep up", "hostile work",
            "drowning in", "drowning at work", "drowning in work",
            "hate my job", "hate this job", "hate going to work", "hate my work",
            "can't catch a break", "cant catch a break",
            "just need a break", "need a break",
            "moving house", "moving to a new city", "moving to a new place",
            "major life change", "big life change", "life upheaval",
            "relocating", "relocation stress",
            "sandwich generation", "caring for aging parents", "caring for elderly parents",
            "caring for elderly", "caring for my", "elder care", "eldercare",
            "caregiver", "carer", "looking after my", "looking after elderly",
            "quiet quitting", "quiet quit",
            "financial crisis", "financial pressure", "financial strain",
            "money pressure", "debt problems", "serious debt", "drowning in debt",
            "parenting a teenager", "parenting teens", "raising a teenager",
            "my teenager is", "teenagers are",
            "too many decisions", "decision overload", "choice overload",
            "paralysed by choice", "paralyzed by choice",
            "overwhelmed by options", "too many options",
            "stretched too thin", "stretched so thin",
            "can't manage anymore", "can't manage it all", "can't manage everything",
            "cant manage anymore", "cant manage it all") ||
        anyWord(lower, "toxic") ->
            handleOverwhelm().also { lastTopic = "overwhelm" }
        any(lower, "angry", "furious", "frustrated", "frustration",
            "rage", "irritated", "irritable", "annoyed", "wound up", "agitated",
            "pissed off", "livid", "seething", "seeing red", "fuming",
            "holding a grudge", "grudge", "can't let it go", "cant let it go",
            "lost my temper", "losing my temper", "lose my temper",
            "about to explode", "about to snap", "lost it", "blow up",
            "want to scream", "could scream", "need to scream",
            "snapped at", "keep snapping", "lashing out",
            "resentment", "resentful", "resentment toward", "full of resentment",
            "want to punch", "feel like punching", "slamming",
            "passive aggressive", "passive-aggressive",
            "microaggression", "microaggressions",
            "talked over", "constantly interrupted", "being interrupted",
            "not taken seriously", "nobody takes me seriously",
            "my voice isn't heard", "not being heard at work",
            "feel invalidated", "feeling invalidated", "invalidated", "feel manipulated",
            "feeling manipulated", "feel used", "being used",
            "unfair treatment", "double standards",
            "short fuse", "quick temper", "bad temper", "short tempered", "short-tempered",
            "road rage",
            "cyberbullying", "trolled", "being trolled", "online troll",
            "online harassment", "social media harassment", "hate comments",
            "bullying", "workplace bullying", "being bullied", "bullied at work",
            "discrimination", "discriminated against", "discriminated",
            "racism", "racist", "racial abuse", "racial discrimination",
            "sexism", "sexist", "gender discrimination",
            "prejudice", "prejudiced", "bias against",
            "revenge", "seeking revenge", "want revenge", "planning revenge",
            "bitter", "bitterness", "bitter toward", "bitter about",
            "contempt", "contemptuous",
            "jealous", "jealousy", "envy", "envious",
            "arguing", "we argue", "keep arguing", "constant arguments",
            "bickering", "we bicker",
            "silent treatment",
            "feel disrespected", "disrespected", "feel dismissed",
            "feel unheard", "not being heard", "nobody listens",
            "taken advantage of", "being taken advantage",
            "narcissist", "narcissistic",
            "micromanaged", "being micromanaged", "micromanagement", "micromanaging",
            "stonewalling", "being stonewalled",
            "condescending", "condescension",
            "undermined", "being undermined", "feel undermined",
            "lied to me", "she lied", "he lied", "they lied", "been lied to",
            "went behind my back", "behind my back", "going behind my back",
            "took credit for my work", "taking credit for my work", "stole my idea", "takes credit",
            "blame me for everything", "blames me for everything", "always my fault",
            "manipulative", "manipulator", "being manipulated",
            "controlling partner", "controlling behavior", "controlling behaviour",
            "hate my boss", "bad boss", "horrible boss", "boss is awful",
            "boss is terrible", "awful manager", "terrible manager", "manager is awful",
            "manager is terrible", "my boss keeps",
            "family conflict", "family drama", "family argument",
            "family feud", "family row", "had a row with", "falling out with my") ||
        anyWord(lower, "mad", "anger", "angered") ->
            handleAnger().also { lastTopic = "anger" }

        any(lower, "vip", "upgrade", "pro plan", "subscription", "pricing",
            "plans", "cost", "buy", "purchase") ->
            handleVip().also { lastTopic = "" }
        any(lower, "recommend", "suggest", "what should", "which sound",
            "best sound", "what sound", "pick a sound", "help me choose") ->
            handleRecommendation().also { lastTopic = "recommendation" }
        any(lower, "thank", "thanks", "awesome", "perfect",
            "love it", "amazing", "nice", "wonderful", "brilliant",
            "fantastic", "great job", "well done", "cheers",
            "celebrate", "celebrating", "proud of myself", "so proud", "proud of",
            "big achievement", "accomplished something", "nailed it",
            "things are looking up", "something good happened", "good news today",
            "life is good", "all is well", "feeling really good",
            "that helped", "that was helpful", "you helped", "exactly what i needed",
            "loved it", "that really helped", "that really worked", "really enjoyed that",
            "loved that session", "enjoyed that session",
            "feel good", "feeling good", "feel great", "feeling great",
            "feel better", "feeling better", "feel much better", "feeling much better",
            "feel happy", "feeling happy", "in a good mood", "good mood today",
            "had a good day", "great day today", "mood is better", "mood has lifted",
            "lifted my mood", "feeling positive",
            "doing well", "having a good day", "all good today", "life is great",
            "happy today", "having a great day", "actually doing okay",
            "had a breakthrough", "just had a breakthrough", "big breakthrough",
            "proud moment", "really proud", "so proud of myself",
            "achieved my goal", "hit my goal", "reached my goal",
            "finally did it", "i finally did it", "just did it") ->
            handlePositive().also { lastTopic = "" }
        any(lower, "timer", "sleep timer", "how long should", "how long to meditate",
            "how long for", "duration", "how many minutes") ->
            handleTimer().also { lastTopic = "" }
        any(lower, "set alarm", "set an alarm", "morning alarm", "alarm for", "alarm at",
            "alarm clock", "wake alarm", "daily alarm", "wake me up", "schedule alarm") ->
            handleAlarm().also { lastTopic = "" }
        // playRequest before ambient — "play my favourite track" must hit playRequest, not "track" in ambient
        any(lower, "play it", "play that", "play my", "play my fav", "play favourite", "play favorite",
            "start it", "queue it", "play something", "play now", "can you play",
            "put on a", "put on some", "start playing") ->
            handlePlayRequest().also { lastTopic = "" }
        // Favorites before ambient — "saved tracks" contains "tracks" which ambient matches first
        any(lower, "my favorites", "my favourites", "saved tracks", "what i saved",
            "what i've saved", "favorite tracks", "favourite tracks", "my saved") ->
            handleFavorites().also { lastTopic = "" }
        any(lower, "track", "tracks", "library", "guided",
            "which track", "what track", "play list", "playlist",
            "white noise", "pink noise", "brown noise",
            "binaural", "binaural beats", "solfeggio", "solfeggio frequencies",
            "nature sounds", "rain sounds", "ocean sounds", "soundscape",
            "sound therapy", "sound bath", "sound healing") ->
            handleAmbient().also { lastTopic = "" }
        any(lower, "headache", "migraine", "ache", "sore", "tension headache",
            "physical", "body tension", "muscle tension", "stiff", "tension",
            "chronic pain", "chronic illness", "fibromyalgia", "arthritis", "back pain",
            "lower back", "neck pain", "neck tension", "shoulder pain", "joint pain",
            "sciatica", "period pain", "menstrual cramps", "cramps",
            "jaw pain", "jaw tension", "jaw clenching", "teeth grinding", "grind my teeth", "grinding my teeth", "bruxism",
            "inflammation", "inflammatory", "repetitive strain", "carpal tunnel", "frozen shoulder",
            "tendonitis", "tendinitis", "endometriosis", "plantar fasciitis", "plantar",
            "neuropathy", "nerve damage", "nerve pain", "neuralgia",
            "pinched nerve", "herniated disc", "bulging disc", "slipped disc",
            "muscle spasm", "muscle spasms", "back spasm", "back spasms",
            "tight muscles", "muscle tightness", "muscle knots", "knotted muscles",
            "hip flexor", "it band", "iliotibial",
            "tennis elbow", "golfer's elbow", "rsi injury",
            "painful", "pains", "in pain") ||
        anyWord(lower, "pain") ->
            handlePain().also { lastTopic = "pain" }
        any(lower, "grateful", "gratitude", "journal", "journaling", "reflect",
            "reflection", "intention", "intentions", "thankful", "thankfulness",
            "appreciate", "appreciation", "count my blessings", "count your blessings",
            "what am i grateful", "things i'm grateful", "blessings",
            "three good things", "3 good things", "silver lining", "look for the good",
            "count the positives", "find the positive",
            "abundance", "abundance mindset", "manifestation", "manifesting",
            "law of attraction", "positive affirmations", "affirmation practice",
            "glass half full", "positive outlook", "optimistic", "optimism") ->
            handleGratitude().also { lastTopic = "gratitude" }
        any(lower, "what can you do", "how do you work", "your features", "about spirit",
            "what are you", "what is spirit", "how can you help",
            "what do you do", "how do i use this", "how do i use you",
            "tell me about yourself", "how does this work", "i need help",
            "what can i ask you", "what can i ask", "i don't know where to start",
            "i dont know where to start", "not sure where to start",
            "how do i get started", "how do i start", "where do i start",
            "getting started", "just downloaded", "new to this", "first time using",
            "just started using") ||
        anyWord(lower, "help") ->
            handleHelp().also { lastTopic = "" }
        any(lower, "inspire me", "inspiration", "quote", "affirmation", "motivate me",
            "encourage me", "daily tip", "today's practice", "what should i practice",
            "technique of the day", "something to try",
            "pep talk", "cheer me up", "lift my spirits", "give me a boost",
            "need encouragement",
            "daily inspiration", "inspiring message", "positive message",
            "boost my mood", "inspiring", "something inspiring",
            "words of wisdom", "wise words", "words of hope",
            "something meaningful", "meaningful message") ->
            handleInspiration().also { lastTopic = "inspiration" }
        any(lower, "journey", "journeys", "program", "programs", "course",
            "guided course", "structured", "7 day", "7-day", "5 day", "5-day", "challenge",
            "daily plan", "wellness plan", "structured plan", "learning path") ->
            handlePrograms().also { lastTopic = "" }
        any(lower, "my stats", "my progress", "how am i doing", "my history",
            "how long have i", "sessions", "minutes meditated", "progress report",
            "my streak", "streak", "day streak",
            "longest streak", "total time", "total minutes", "how many sessions") ->
            handleStats().also { lastTopic = "" }
        any(lower, "my favorites", "my favourites", "saved tracks", "what i saved",
            "what i've saved", "favorite tracks", "favourite tracks", "my saved") ->
            handleFavorites().also { lastTopic = "" }
        else -> handleGeneral(lower).also { lastTopic = "" }
    }

    private fun any(input: String, vararg kws: String) = kws.any { input.contains(it) }
    // Word-boundary variant — use for short single-syllable keywords that are substrings of common words
    // e.g. "hi" inside "this", "hey" inside "they", "yo" inside "you", "sup" inside "support"
    private fun anyWord(input: String, vararg kws: String) =
        kws.any { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(input) }

    private fun progLine(programId: String): String {
        val prog = Programs.all.find { it.id == programId } ?: return ""
        val done = prefs.getProgramProgress(programId)
        val status = when {
            done >= prog.days.size -> "✓ Complete"
            done == 0              -> "not started — find it under Journeys on the main screen"
            else                   -> "Day $done of ${prog.days.size} — tap Journeys to continue"
        }
        return "\n\n${prog.emoji} Structured journey: ${prog.title} — ${prog.blurb}\n($status)"
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private fun handleGreeting(): Pair<String, SoundType?> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isLate = hour >= 21 || hour < 6
        val streak = stats.currentStreak()
        return if (isLate) {
            val streakLine = if (streak >= 2) "${streak}-day streak — keep going. " else ""
            Pair(
                "Hey there. Late hour — a good time to let the day go. 🌙\n\n" +
                "${streakLine}Based on what's settled you before, " +
                "${mostPlayed.emoji} ${mostPlayed.displayName} is a lovely place to start tonight.\n\n" +
                "Tell me: rest, relax, or a specific sound?",
                mostPlayed
            )
        } else {
            val intro = when {
                streak >= 7 -> "Great to see you. ${streak} days in a row — a real practice is forming. ✨\n\n"
                streak >= 2 -> "Good to see you again. ${streak} days running — the habit is taking shape. 🌱\n\n"
                else        -> "Hello! ☀️ Spirit here.\n\n"
            }
            Pair(
                "${intro}What's the focus right now — sleep, relax, focus, or something else?",
                null
            )
        }
    }

    private fun handleSleepParalysis(): Pair<String, SoundType?> = Pair(
        "🌙 Sleep Paralysis — You're Safe\n\n" +
        "Sleep paralysis happens when your brain wakes up before your body does — the muscle-suspension " +
        "of REM sleep lingers a few seconds or minutes. It's deeply frightening, but never harmful.\n\n" +
        "During an episode:\n" +
        "• Focus on one tiny movement — a finger, your tongue, or just your breath. Small twitches break it faster than trying to sit up\n" +
        "• One slow, deliberate exhale. Panic prolongs it; breath signals 'I am awake'\n" +
        "• If you sense something frightening: 'This is hypnagogic imagery — my brain, not reality'\n\n" +
        "After — ground yourself back into your body:\n" +
        "${SoundType.VIPASSANA.emoji} ${SoundType.VIPASSANA.displayName} — body-scan attention; the most direct way to feel 'in' your body again\n" +
        "${SoundType.HESYCHASM.emoji} ${SoundType.HESYCHASM.displayName} — wordless, still quiet; no performance needed to feel safe\n\n" +
        "To reduce frequency:\n" +
        "• Keep consistent sleep/wake times — even weekends\n" +
        "• Avoid sleeping on your back (episodes peak face-up)\n" +
        "• Stress and sleep deprivation are the biggest triggers — managing them cuts episodes significantly\n\n" +
        "Want me to walk you through a calming breathwork practice to settle your nervous system now?",
        SoundType.VIPASSANA
    )

    private fun handleSleep(): Pair<String, SoundType?> {
        val rec = SoundType.EVENING_REVIEW
        return Pair(
            "🌙 Wind-Down Practice\n\n" +
            "A gentle stack to ease you toward rest:\n\n" +
            "${rec.emoji} ${rec.displayName} — a Stoic nightly reflection to review the day with honesty and self-compassion\n" +
            "${SoundType.SOHAM.emoji} ${SoundType.SOHAM.displayName} — ancient yogic breath-mantra; drifts you toward sleep naturally\n" +
            "${SoundType.SUMARA.emoji} ${SoundType.SUMARA.displayName} — unhurried Javanese surrender; lovely for those who can't switch off\n\n" +
            "Timer: 30–45 min. Volume: 60–70% — present enough to anchor, soft enough to release.\n\n" +
            "Want me to walk you through a wind-down breathing pattern?" +
            progLine("sleep7"),
            rec
        )
    }

    private fun handleFocus(): Pair<String, SoundType?> = Pair(
        "🧠 Focus & Concentration\n\n" +
        "A grounding stack for clear, settled attention:\n\n" +
        "${SoundType.WORK_FOCUS_CHILLOUT.emoji} ${SoundType.WORK_FOCUS_CHILLOUT.displayName} — warm chords and a gentle beat to ease you into deep concentration\n" +
        "${SoundType.WORK_FOCUS_CHILLOUT_1.emoji} ${SoundType.WORK_FOCUS_CHILLOUT_1.displayName} — inspiring electronic ambient for coding or design\n" +
        "${SoundType.WORK_FOCUS_CHILLOUT_3.emoji} ${SoundType.WORK_FOCUS_CHILLOUT_3.displayName} — a clean synthesizer soundscape to sharpen your thoughts\n\n" +
        "Or try these guided practices:\n" +
        "${SoundType.VIPASSANA.emoji} ${SoundType.VIPASSANA.displayName} — body-scanning to sharpen awareness\n" +
        "${SoundType.BUDDHO.emoji} ${SoundType.BUDDHO.displayName} — Thai forest recitation to align the breath with wakefulness\n\n" +
        "Want a quick Pre-Focus Reset before you start? (2 minutes — sets the tone for the whole session)" +
        progLine("focus7"),
        SoundType.WORK_FOCUS_CHILLOUT
    )

    private fun handleRelax(): Pair<String, SoundType?> = Pair(
        "🌊 Relaxation Practice\n\n" +
        "If you're in acute panic or struggling to breathe: two sharp inhales through the nose, then one long exhale — " +
        "the Physiological Sigh. Fastest nervous-system reset known. Do 3 rounds before anything else.\n\n" +
        "Then, when you're ready:\n\n" +
        "${SoundType.SOHAM.emoji} ${SoundType.SOHAM.displayName} — listen to the natural mantra of the breath\n" +
        "${SoundType.AUTOGENIC_CALM.emoji} ${SoundType.AUTOGENIC_CALM.displayName} — somatic relaxation through quiet autosuggestions\n" +
        "${SoundType.THIEN.emoji} ${SoundType.THIEN.displayName} — breathe and smile in the middle of daily life\n\n" +
        "Try a slow 4-count inhale, 6-count exhale alongside any of these (or tap the Breathe chip above for a visual pacer).\n\n" +
        "Spirit can walk you through breathwork or a body scan — just ask." +
        progLine("anxiety5"),
        SoundType.SOHAM
    )

    private fun handleEnergy(): Pair<String, SoundType?> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (primary, secondary) = if (hour < 10)
            SoundType.ZHAN_ZHUANG to SoundType.CIRCUIT_THUNDERCLAP
        else
            SoundType.CIRCUIT_THUNDERCLAP to SoundType.NINE_TO_FIVE
        return Pair(
            "⚡ Energy & Activation\n\n" +
            "A stack to spark focus and presence:\n\n" +
            "${primary.emoji} ${primary.displayName} — ${primary.description}\n" +
            "${secondary.emoji} ${secondary.displayName} — ${secondary.description}\n" +
            "${SoundType.CIRCUIT_THUNDERCLAP_1.emoji} ${SoundType.CIRCUIT_THUNDERCLAP_1.displayName} — ${SoundType.CIRCUIT_THUNDERCLAP_1.description}\n\n" +
            "Before you start: two deep breaths, then set a single clear intention for the next 25 minutes. " +
            "Energy without direction is just noise — give it somewhere to go.\n\n" +
            "Want a 30-second activation? Or say 'technique' for an energy micro-practice. 🎵",
            primary
        )
    }

    private fun handleTinnitus(): Pair<String, SoundType?> = Pair(
        "👂 Gentle Tinnitus Support\n\n" +
        "Spirit's suggestions for easing the focus on ringing:\n\n" +
        "${SoundType.SUMARA.emoji} ${SoundType.SUMARA.displayName} — unhurried Javanese surrender; a natural backdrop that gently draws attention outward\n" +
        "${SoundType.VIPASSANA.emoji} ${SoundType.VIPASSANA.displayName} — systematic body-scanning to redirect attention away from the ringing\n" +
        "${SoundType.SOHAM.emoji} ${SoundType.SOHAM.displayName} — breath-mantra rhythm absorbs awareness and lifts it away from the sound\n\n" +
        "Start around 50% volume — lower than you'd think. Comfort over coverage.\n\n" +
        "A body-scan practice can also help shift attention from the ringing — want to try one?",
        SoundType.SUMARA
    )

    private fun handleBaby(): Pair<String, SoundType?> = Pair(
        "👶 Soothing Little Ones\n\n" +
        "A few gentle, narrated tracks that work well in a quiet nursery:\n\n" +
        "${SoundType.SOHAM.emoji} ${SoundType.SOHAM.displayName} — slow, steady, easy to drift off to\n" +
        "${SoundType.SUMARA.emoji} ${SoundType.SUMARA.displayName} — open and unhurried; a gentle constant presence\n" +
        "${SoundType.PROGRESSIVE_MUSCLE_RELEASE.emoji} ${SoundType.PROGRESSIVE_MUSCLE_RELEASE.displayName} — a soft, grounding rhythm that releases physical tension\n\n" +
        "Keep volume low and steady — soft enough that the room feels calm rather than full.\n\n" +
        "Spirit's tip: loop a track after feeding to build a calming sleep cue over time.",
        SoundType.SOHAM
    )

    private fun handleMeditation(): Pair<String, SoundType?> = Pair(
        "🧘 Meditation Soundscape\n\n" +
        "A gentle stack to support your sitting practice:\n\n" +
        "${SoundType.THIEN.emoji} ${SoundType.THIEN.displayName} — Zen breathing and smiling in the middle of daily life\n" +
        "${SoundType.TONGLEN.emoji} ${SoundType.TONGLEN.displayName} — Tibetan practice of sending and receiving to settle the mind\n" +
        "${SoundType.MURAQABA.emoji} ${SoundType.MURAQABA.displayName} — Sufi discipline of watchfulness and quiet observation\n\n" +
        "New to sitting: 5 minutes with ${SoundType.MURAQABA.displayName} is a soft, accessible start.\n" +
        "Deepening your practice: ${SoundType.THIEN.displayName} or ${SoundType.TONGLEN.displayName} reward longer sits.\n\n" +
        "Curious about a specific technique — mindfulness, breathwork, body scan, loving-kindness? Just ask.",
        SoundType.THIEN
    )

    private fun handleTechniques(): Pair<String, SoundType?> {
        val today = MicroTechniques.today()
        return Pair(
            "🌿 Today's Technique — ${today.emoji} ${today.title}\n\n" +
            "${today.body}\n\n" +
            "── Other paths into stillness ──\n\n" +
            "Box Breathing 🫧 — in 4 · hold 4 · out 4 · hold 4 (or tap the Breathe chip above for a visual pacer)\n" +
            "Body Scan 🦶 — sweep attention from toes to head, softening as you go\n" +
            "Loving-Kindness 🤍 — offer warmth to yourself, then gently outward\n" +
            "Physiological Sigh 🌬️ — two short inhales, one long exhale; fastest real-time calm\n\n" +
            "Pair any of these with a track from the Sounds tab.\n\n" +
            "Want me to walk you through one step by step?",
            SoundType.THIEN
        )
    }

    private fun handleVip(): Pair<String, SoundType?> = Pair(
        "🌟 Unlock Meditation Portal\n\n" +
        "One purchase, everything — forever:\n\n" +
        "The full guided meditation library\n" +
        "Spirit, your meditation companion\n" +
        "Meditation alarm with gentle wake tones\n" +
        "Zero ads, forever\n\n" +
        "Try free for 14 days — then just \$1.99, one time. No subscriptions, no surprises.\n\n" +
        "Tap Unlock on the main screen anytime you're ready! 🚀",
        null
    )

    private fun handleRecommendation(): Pair<String, SoundType?> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Explicit onboarding goal takes priority over inferred mood from playback history.
        val favMood = prefs.getGoal() ?: mostPlayed.mood
        val (suggestion, reason) = when {
            hour >= 22 || hour < 5 ->
                SoundType.EVENING_REVIEW to "perfect for this hour — a soft Stoic close to the day"
            favMood == Mood.FOCUS && hour in 7..17 ->
                SoundType.WORK_FOCUS_CHILLOUT to "you tend toward focus practices — a warm anchor for the workday"
            favMood == Mood.SLEEP ->
                SoundType.SOHAM to "you often practice for rest — this eases the transition into calm"
            favMood == Mood.STRESS ->
                SoundType.AUTOGENIC_CALM to "you tend to practice for calm — this quiets the nervous system systematically"
            favMood == Mood.COMPASSION ->
                SoundType.TONGLEN to "you gravitate toward compassion practices — this opens the heart with warmth"
            favMood == Mood.ENERGY ->
                SoundType.CIRCUIT_THUNDERCLAP to "you tend toward energising practices — this sparks focus and drive"
            favMood == Mood.GROUNDING ->
                SoundType.VIPASSANA to "you gravitate toward grounding practices — Vipassana's body-scan attention anchors you in the present moment"
            hour < 10 ->
                SoundType.ZHAN_ZHUANG to "settles a busy mind before the day picks up speed"
            hour < 18 ->
                SoundType.BUDDHO to "a hushed, focused space to anchor an afternoon practice"
            else ->
                SoundType.SOHAM to "eases the transition into a calm evening"
        }
        val matchingFav = prefs.getFavorites()
            .firstOrNull { it.mood == suggestion.mood && it != suggestion }
        val favNote = if (matchingFav != null)
            "\n\nYou've also saved ${matchingFav.emoji} ${matchingFav.displayName} — try that if you prefer something familiar."
        else ""
        return Pair(
            "🤍 Spirit's Suggestion — Personalised to You\n\n" +
            "Based on:\n" +
            "Time: ${hour}:00 — ${if (hour >= 22 || hour < 5) "wind-down window" else "active hours"}\n" +
            "Your most-played: ${mostPlayed.emoji} ${mostPlayed.displayName} (${favMood.label})\n" +
            "What's tended to settle you before\n\n" +
            "Right now, try: ${suggestion.emoji} ${suggestion.displayName}\n\n" +
            "Why: $reason.$favNote\n\n" +
            "Tap the card above to begin. 🎵",
            suggestion
        )
    }

    private fun handlePositive(): Pair<String, SoundType?> {
        val streak = stats.currentStreak()
        val minutes = stats.totalMinutes()
        val milestoneNote = when {
            streak >= 7  -> " Your ${streak}-day streak is building something genuinely lasting."
            minutes >= 60 -> " Over ${minutes} minutes in practice — that consistency shows."
            else          -> ""
        }
        val activeProgram = Programs.all.firstOrNull {
            val done = prefs.getProgramProgress(it.id)
            done > 0 && done < it.days.size
        }
        val prog4 = if (activeProgram != null) {
            val done = prefs.getProgramProgress(activeProgram.id)
            "Wonderful! ${activeProgram.emoji} ${activeProgram.title} — Day ${done} done. Keep going, you're building something real. 🌱"
        } else {
            "So glad Spirit could help. Come back anytime — I'll be here. 🤍"
        }
        val msgs = listOf(
            "You're so welcome.$milestoneNote Rest easy and be gentle with yourself. 🤍",
            "That makes me glad! 🌙 Showing up for your practice matters — even a few quiet minutes a day adds up.",
            "Lovely!$milestoneNote Consistency is what makes a practice — you're doing beautifully. 💪",
            prog4
        )
        return Pair(msgs[turnsCount % msgs.size], null)
    }

    private fun handleTimer(): Pair<String, SoundType?> {
        val goalMood = prefs.getGoal()
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val rec = when {
            goalMood == Mood.SLEEP || hour >= 21 || hour < 6 ->
                "30–45 min — enough to guide you into sleep without waking you when it ends"
            goalMood == Mood.FOCUS ->
                "25 min (Pomodoro cycle) or 50 min — aligned with a natural focus sprint"
            goalMood == Mood.STRESS ->
                "10–20 min — short enough to commit to, long enough to actually land"
            else ->
                "20–30 min — a standard sitting or wind-down practice"
        }
        return Pair(
            "⏱️ Session Timer\n\n" +
            "Spirit's suggestion for your practice right now:\n\n" +
            "$rec\n\n" +
            "General reference:\n" +
            "5–10 min — a micro-reset between tasks\n" +
            "20–30 min — a full sit or breathwork session\n" +
            "45–90 min — deep restorative or body-scan practice\n" +
            "8 hours — all-night gentle presence for sleep\n\n" +
            "Tap Timer on the main screen to set your duration. A fade-out feels more natural than an abrupt stop. 🌙",
            null
        )
    }

    private fun handleAmbient(): Pair<String, SoundType?> = Pair(
        "🌌 Guided Meditation Library\n\n" +
        "Portal features a full library of narrated practices:\n\n" +
        "Breathing & release: ${SoundType.AUTOGENIC_CALM.emoji} ${SoundType.AUTOGENIC_CALM.displayName}, " +
        "${SoundType.PROGRESSIVE_MUSCLE_RELEASE.emoji} ${SoundType.PROGRESSIVE_MUSCLE_RELEASE.displayName}\n" +
        "Grounding & body-scan: ${SoundType.ZHAN_ZHUANG.emoji} ${SoundType.ZHAN_ZHUANG.displayName}, " +
        "${SoundType.VIPASSANA.emoji} ${SoundType.VIPASSANA.displayName}\n" +
        "Spacious awareness: ${SoundType.THIEN.emoji} ${SoundType.THIEN.displayName}, " +
        "${SoundType.TONGLEN.emoji} ${SoundType.TONGLEN.displayName}, " +
        "${SoundType.SUMARA.emoji} ${SoundType.SUMARA.displayName}\n" +
        "Devotional quiet: ${SoundType.MURAQABA.emoji} ${SoundType.MURAQABA.displayName}, " +
        "${SoundType.HESYCHASM.emoji} ${SoundType.HESYCHASM.displayName}\n\n" +
        "Try ${SoundType.SOHAM.emoji} ${SoundType.SOHAM.displayName} for winding down, or " +
        "${SoundType.TONGLEN.emoji} ${SoundType.TONGLEN.displayName} any time you need permission to simply rest.",
        SoundType.SOHAM
    )

    private fun handleHelp(): Pair<String, SoundType?> = Pair(
        "🤍 What Spirit Can Do\n\n" +
        "I'm your meditation companion — here's how we can work together:\n\n" +
        "🎵 Sound recommendations — 'recommend' or 'what should I play'\n" +
        "😴 Sleep / 🧠 Focus / ⚡ Energy / 🌿 Relax — mood-specific track stacks\n" +
        "💜 Emotional support — 'sad', 'overwhelmed', 'angry', 'struggling'\n" +
        "🌿 Technique walkthroughs — breathwork, body scan, loving-kindness, box breathing\n" +
        "🌬️ Breathe — tap the Breathe chip above for a visual guided box-breathing pacer\n" +
        "✨ Daily inspiration — 'inspire me' for today's rotating practice\n" +
        "📊 Progress & favorites — 'my stats' or 'my favorites'\n" +
        "🗓️ Structured journeys — 'journeys' to see your guided programs\n" +
        "⏰ Alarm — 'set alarm' or tap the Alarm chip above to set a meditation wake-up\n" +
        "⏱️ Timer help — 'how long should I meditate'\n" +
        "👶 Tinnitus & baby tracks — just say what you need\n\n" +
        "What would you like to explore first?",
        null
    )

    private fun handleInspiration(): Pair<String, SoundType?> {
        val today = MicroTechniques.today()
        val goalMood = prefs.getGoal()
        val goalTech = if (goalMood != null) MicroTechniques.forGoal(goalMood).firstOrNull { it.title != today.title } else null
        val extra = if (goalTech != null)
            "\n\nMatched to your practice goal — ${goalTech.emoji} ${goalTech.title}: ${goalTech.teaser}"
        else ""
        val quote = Quotes.today()
        return Pair(
            "✨ Today's Technique — ${today.emoji} ${today.title}\n\n" +
            "\"${today.teaser}\"\n\n" +
            "${today.body}" +
            extra +
            "\n\n💬 \"${quote.first}\"\n— ${quote.second}" +
            "\n\nPair with ${mostPlayed.emoji} ${mostPlayed.displayName} for a grounded session. Want to go deeper?",
            mostPlayed
        )
    }

    private fun handlePrograms(): Pair<String, SoundType?> {
        val lines = Programs.all.joinToString("\n\n") { prog ->
            val done = prefs.getProgramProgress(prog.id)
            val status = when {
                done >= prog.days.size -> "✓ Complete"
                done == 0              -> "Not started"
                else                   -> "Day $done of ${prog.days.size}"
            }
            "${prog.emoji} ${prog.title} — ${prog.days.size} days\n${prog.blurb}\n$status"
        }
        return Pair(
            "🗓️ Guided Journeys\n\n" +
            "$lines\n\n" +
            "Find all Journeys via the Journeys button on the main screen. Each day is a short guided session — " +
            "tap 'Begin today's session' to continue where you left off.",
            null
        )
    }

    private fun handleStats(): Pair<String, SoundType?> {
        val streak = stats.currentStreak()
        val longest = stats.longestStreak()
        val sessions = stats.totalSessions()
        val minutes = stats.totalMinutes()
        val favorites = prefs.getFavorites()
        val streakLine = when {
            streak >= 7 -> "🔥 ${streak}-day streak — exceptional."
            streak >= 2 -> "🌱 ${streak} days in a row."
            streak == 1 -> "✨ 1 day — a solid start."
            else        -> "Streak: 0 days. Today is a great time to begin."
        }
        val longestNote = if (longest > streak && longest > 1) " (Best: ${longest} days)" else ""
        val minuteLine = when {
            minutes >= 60 -> "${minutes} min meditated across ${sessions} sessions."
            minutes > 0   -> "${minutes} min across ${sessions} sessions."
            else          -> "No sessions logged yet."
        }
        val favLine = if (favorites.isEmpty()) {
            "No favorites saved yet — heart a track on the main screen."
        } else {
            "Saved: ${favorites.take(3).joinToString("  ") { "${it.emoji} ${it.displayName}" }}"
        }
        return Pair(
            "🤍 Your Practice\n\n" +
            "$streakLine$longestNote\n" +
            "$minuteLine\n" +
            "Most-played: ${mostPlayed.emoji} ${mostPlayed.displayName}\n\n" +
            "$favLine\n\n" +
            "Every session counts — you're building something real.",
            null
        )
    }

    private fun handleFavorites(): Pair<String, SoundType?> {
        val favorites = prefs.getFavorites()
        if (favorites.isEmpty()) {
            return Pair(
                "You haven't saved any favorites yet. 🤍\n\n" +
                "Tap the heart icon on any track card to save it here.\n\n" +
                "Spirit's suggestion for your first: ${mostPlayed.emoji} ${mostPlayed.displayName} — your most-played.",
                mostPlayed
            )
        }
        val list = favorites.joinToString("\n") { "• ${it.emoji} ${it.displayName}" }
        val topFav = favorites.first()
        return Pair(
            "⭐ Your Saved Tracks\n\n" +
            "$list\n\n" +
            "Tap the card above to play ${topFav.emoji} ${topFav.displayName} now.",
            topFav
        )
    }

    private fun handlePlayRequest(): Pair<String, SoundType?> {
        val favorites = prefs.getFavorites()
        val toPlay = favorites.firstOrNull() ?: mostPlayed
        val source = if (favorites.isNotEmpty()) "your top saved favorite" else "your most-played"
        return Pair(
            "Queuing up ${toPlay.emoji} ${toPlay.displayName} — ${source}!\n\n" +
            "Tap Play below, or find it in the grid on the main screen. 🎵\n\n" +
            "Settle in and breathe... 🌙",
            toPlay
        )
    }

    private fun handleSadness(): Pair<String, SoundType?> = Pair(
        "💜 I hear you. Sadness deserves space — not fixing.\n\n" +
        "When we're low, the mind wants to understand *why*, but sometimes the most healing thing is simply to sit with the feeling rather than push it away.\n\n" +
        "A few practices that can help:\n\n" +
        "${SoundType.TONGLEN.emoji} ${SoundType.TONGLEN.displayName} — breathing *with* pain rather than away from it; a quiet companion for grief\n" +
        "Loving-Kindness (Metta) — offering yourself the same warmth you'd give a close friend\n" +
        "${SoundType.HESYCHASM.emoji} ${SoundType.HESYCHASM.displayName} — a gentle, wordless prayer of the heart; no effort required\n\n" +
        "You don't have to feel better right away. Spirit is here.\n\n" +
        "Would you like me to walk you through a loving-kindness practice? 🤍" +
        progLine("compassion5"),
        SoundType.TONGLEN
    )

    private fun handleOverwhelm(): Pair<String, SoundType?> = Pair(
        "🌊 Overwhelm is a signal — your system is carrying more than it was designed to hold alone.\n\n" +
        "First: one slow exhale right now. Longer out than in. That alone begins to ease the physical grip.\n\n" +
        "Then, when you're ready:\n\n" +
        "${SoundType.BURNOUT_RECOVERY.emoji} ${SoundType.BURNOUT_RECOVERY.displayName} — a comforting ambient wave that releases mental overload\n" +
        "${SoundType.AUTOGENIC_CALM.emoji} ${SoundType.AUTOGENIC_CALM.displayName} — quietly guides your body out of fight-or-flight, system by system\n" +
        "Box breathing — 4 counts in · 4 hold · 4 out · 4 hold — used by paramedics to restore calm fast\n\n" +
        "You don't need to solve everything right now. Just this breath, then the next.\n\n" +
        "Want me to walk you through box breathing?" +
        progLine("anxiety5"),
        SoundType.BURNOUT_RECOVERY
    )

    private fun handleAnger(): Pair<String, SoundType?> = Pair(
        "🔥 Anger is real — and it's often pointing at something that matters.\n\n" +
        "Don't suppress it. That drives it underground. Let it move through the body first:\n\n" +
        "A long, forceful exhale — longer out than in — activates the parasympathetic brake\n" +
        "${SoundType.PROGRESSIVE_MUSCLE_RELEASE.emoji} ${SoundType.PROGRESSIVE_MUSCLE_RELEASE.displayName} — deliberately channels the energy out through the body\n" +
        "${SoundType.VIPASSANA.emoji} ${SoundType.VIPASSANA.displayName} — teaches you to observe the sensation of anger without being swept away by it\n\n" +
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
        "pain" -> Pair(
            "💪 Progressive Muscle Release — Step by Step\n\n" +
            "Find a comfortable position — lying down is best.\n\n" +
            "1. Three slow breaths to settle.\n" +
            "2. Tense your feet and toes tightly for 5 seconds... then release completely.\n" +
            "3. Move up slowly: calves → thighs → abdomen → chest → hands → arms → shoulders → face.\n" +
            "4. After tensing each group, exhale slowly and notice the contrast — the release is where the relief is.\n\n" +
            "A full pass takes 10–15 minutes. Most people feel noticeably lighter by the end.\n\n" +
            "Progressive Muscle Release 💪 is queued — let the narration guide you. 🤍",
            SoundType.PROGRESSIVE_MUSCLE_RELEASE
        )
        "inspiration" -> {
            val goalMood = prefs.getGoal()
            val pool = if (goalMood != null) MicroTechniques.forGoal(goalMood) else MicroTechniques.all
            val next = pool.filterNot { it.title == MicroTechniques.today().title }.randomOrNull()
                ?: MicroTechniques.today()
            Pair(
                "✨ Another Practice — ${next.emoji} ${next.title}\n\n" +
                "${next.body}\n\n" +
                "Try it right now — even 60 seconds changes the tone of the moment. " +
                "Pair with ${mostPlayed.emoji} ${mostPlayed.displayName} if you'd like a backdrop. 🌙",
                mostPlayed
            )
        }
        "energy" -> Pair(
            "⚡ 30-Second Activation — Anywhere\n\n" +
            "1. Stand up. Feet shoulder-width, spine tall.\n" +
            "2. Take 3 big breaths: inhale fully, hold 2 seconds, release fast.\n" +
            "3. Roll your shoulders back twice.\n" +
            "4. Set one micro-goal for the next 25 minutes — just one.\n\n" +
            "Direction + breath = energy. That's the whole equation.\n\n" +
            "${SoundType.CIRCUIT_THUNDERCLAP.emoji} ${SoundType.CIRCUIT_THUNDERCLAP.displayName} is queued — let it carry you forward. 🎵",
            SoundType.CIRCUIT_THUNDERCLAP
        )
        "focus" -> Pair(
            "🧠 Pre-Focus Reset — 2 Minutes\n\n" +
            "Before you open the work, do this:\n\n" +
            "1. Write one sentence: what does success look like in the next 90 minutes?\n" +
            "2. Close everything unrelated. Phone face-down.\n" +
            "3. Take 4 slow breaths: in 4 · hold 4 · out 4 · hold 4.\n\n" +
            "The first 5 minutes of a session set the tone for the rest. Resist the urge to check one more thing.\n\n" +
            "${SoundType.WORK_FOCUS_CHILLOUT.emoji} ${SoundType.WORK_FOCUS_CHILLOUT.displayName} is queued — let it carry you in. 🎵",
            SoundType.WORK_FOCUS_CHILLOUT
        )
        "gratitude" -> Pair(
            "🙏 Loving-Kindness Opening — For Reflection\n\n" +
            "Before you write or reflect, try this 3-minute centering:\n\n" +
            "1. Sit comfortably. Close your eyes. Three slow breaths.\n" +
            "2. Think of one small thing from today that wasn't terrible — a warm drink, a kind word, a moment of quiet.\n" +
            "3. Let that image rest in your chest. Breathe into it.\n" +
            "4. Silently: 'I am grateful for this. I am glad it existed.'\n" +
            "5. Open your eyes and write from that place.\n\n" +
            "Evening Review 🏛️ structures this beautifully — a Stoic close that makes gratitude natural rather than forced. 🤍",
            SoundType.EVENING_REVIEW
        )
        "recommendation" -> Pair(
            "Tap the track card above to begin — the sound will start right away. 🎵\n\n" +
            "Give it 2–3 minutes before judging whether it's right. The nervous system needs a moment to settle.\n\n" +
            "Want a different suggestion? Just say 'recommend again' or describe your mood.",
            null
        )
        "meditation" -> Pair(
            "🧘 Let's Begin — A Simple Sitting\n\n" +
            "Find a comfortable seat. No special posture needed — just upright enough to stay awake.\n\n" +
            "1. Close your eyes or soften your gaze.\n" +
            "2. Take three slow breaths. Let the last exhale be a little longer.\n" +
            "3. Choose one anchor: the feeling of air at your nostrils, or the rise and fall of your chest.\n" +
            "4. Rest there. When thoughts pull you away — and they will — simply notice and return.\n\n" +
            "That's all. The return is the practice.\n\n" +
            "${SoundType.THIEN.emoji} ${SoundType.THIEN.displayName} is a perfect companion for this — gentle enough to support silence.",
            SoundType.THIEN
        )
        "tinnitus" -> Pair(
            "👂 Attention Redirect — Body Scan for Tinnitus\n\n" +
            "The ringing competes for focus. We're going to give your attention somewhere else to land:\n\n" +
            "1. Sit or lie comfortably. Close your eyes.\n" +
            "2. Bring your attention to the soles of your feet — feel the weight, warmth, or texture there.\n" +
            "3. Very slowly, let awareness travel upward: calves, knees, thighs, hips...\n" +
            "4. At each area, breathe in and deliberately soften. Don't fight the ringing — just don't follow it.\n" +
            "5. Continue up through belly, chest, shoulders, jaw, forehead.\n\n" +
            "The scan doesn't silence tinnitus — it trains attention to move away from it. " +
            "With practice, the ringing becomes background noise rather than foreground.\n\n" +
            "${SoundType.VIPASSANA.emoji} ${SoundType.VIPASSANA.displayName} is a natural pairing — its systematic structure mirrors the scan perfectly. 🎵",
            SoundType.VIPASSANA
        )
        "baby" -> Pair(
            "👶 Building a Sleep Cue — The Gentle Ritual\n\n" +
            "Babies learn faster from repetition than instruction. Here's a 3-step cue sequence:\n\n" +
            "1. Dim the lights 30 minutes before you want them to settle — melatonin cues begin with darkness.\n" +
            "2. Play the same track every night at low volume (around 40%). Consistency builds association.\n" +
            "3. During feeding or rocking, breathe slowly yourself — babies co-regulate with caregivers. " +
            "Your calm is contagious.\n\n" +
            "If they're fighting sleep: try ${SoundType.SOHAM.emoji} ${SoundType.SOHAM.displayName} — " +
            "the slow, steady rhythm is close to an adult resting heart rate and naturally signals safety.\n\n" +
            "You're doing brilliantly. This season passes. 🤍",
            SoundType.SOHAM
        )
        else -> handleGeneral()
    }

    private fun handlePain(): Pair<String, SoundType?> = Pair(
        "💜 Physical tension and pain hold so much. The body often carries what the mind hasn't yet processed.\n\n" +
        "A few practices that can ease physical discomfort:\n\n" +
        "${SoundType.PROGRESSIVE_MUSCLE_RELEASE.emoji} ${SoundType.PROGRESSIVE_MUSCLE_RELEASE.displayName} — deliberately tense and release each muscle group; " +
        "very effective for tension headaches and held stress\n" +
        "Body Scan — move awareness slowly through the body, breathing into areas of tightness " +
        "rather than bracing against them\n" +
        "${SoundType.AUTOGENIC_CALM.emoji} ${SoundType.AUTOGENIC_CALM.displayName} — quiet autosuggestions that guide the body toward warmth and heaviness; " +
        "can noticeably reduce tension in 10 minutes\n\n" +
        "Lower the lights if you can, and give yourself permission to stop trying to fix it — " +
        "just observe the sensation with curiosity rather than resistance.\n\n" +
        "Would you like me to walk you through PMR or a body scan?",
        SoundType.PROGRESSIVE_MUSCLE_RELEASE
    )

    private fun handleGratitude(): Pair<String, SoundType?> = Pair(
        "🙏 Reflection and gratitude are some of the most consistently supported wellbeing practices.\n\n" +
        "A few ways in:\n\n" +
        "${SoundType.EVENING_REVIEW.emoji} ${SoundType.EVENING_REVIEW.displayName} — a Stoic nightly reflection that naturally integrates gratitude " +
        "into an honest close-of-day review\n" +
        "Loving-Kindness (Metta) — extend warmth first to yourself, then to people in your life; " +
        "a gratitude practice that moves outward\n" +
        "Journaling anchor: two minutes with any grounding track before you write — " +
        "it settles the mind so the words come more easily\n\n" +
        "Want Spirit to walk you through a brief loving-kindness practice to open the reflection? 🤍",
        SoundType.EVENING_REVIEW
    )

    private fun handleShameGuilt(): Pair<String, SoundType?> = Pair(
        "🤍 You're carrying something heavy.\n\n" +
        "Guilt says: I did something wrong. Shame says: I am wrong. Both are painful — " +
        "and both tighten their grip when we try to turn away from them.\n\n" +
        "Meditation doesn't erase guilt or shame. But it creates enough stillness to see them " +
        "without being entirely defined by them.\n\n" +
        "${SoundType.HESYCHASM.emoji} ${SoundType.HESYCHASM.displayName} — a wordless, forgiving quiet; no performance needed\n" +
        "${SoundType.TONGLEN.emoji} ${SoundType.TONGLEN.displayName} — breathe the feeling in fully, breathe out spaciousness; " +
        "transforms by not avoiding\n\n" +
        "Loving-Kindness (Metta) is the most direct path: deliberately offering yourself the same " +
        "warmth you would give a friend who came to you with this exact feeling.\n\n" +
        "Try this: place a hand on your heart. Say quietly — " +
        "'I am struggling. I am still worthy of care.'\n\n" +
        "Want me to walk you through a self-compassion practice? 🤍" +
        progLine("compassion5"),
        SoundType.HESYCHASM
    )

    private fun handleCrisis(): Pair<String, SoundType?> = Pair(
        "You matter. 🤍\n\n" +
        "If you're having thoughts of ending your life or harming yourself, " +
        "please reach out to someone trained to help right now:\n\n" +
        "🇬🇧 Samaritans: 116 123 (free, 24/7)\n" +
        "🇺🇸 988 Suicide & Crisis Lifeline: call or text 988\n" +
        "🌍 Crisis Text Line: text HOME to 741741\n\n" +
        "I'm here with you — but a real person on those lines can offer something I can't.\n\n" +
        "You don't have to carry this alone. If you'd like, I can also play something " +
        "gentle while you gather the courage to reach out. 🌙",
        SoundType.HESYCHASM
    )

    private fun handleAlarm(): Pair<String, SoundType?> = Pair(
        "⏰ Meditation Alarm\n\n" +
        "Set a daily wake-up that eases you into the morning:\n\n" +
        "Tap the ⏰ Alarm chip above, or visit the Alarm tab at the bottom of the screen.\n\n" +
        "You can choose to wake with a gentle synthesised tone or with any meditation track from the library — " +
        "a soft nudge into consciousness rather than a jolt.\n\n" +
        "Spirit's pick for a morning wake: ${SoundType.SOHAM.emoji} ${SoundType.SOHAM.displayName} — " +
        "a slow, grounding breath mantra that eases the nervous system into the day.",
        null
    )

    private fun handleGeneral(input: String = ""): Pair<String, SoundType?> {
        val isStrugg = any(input, "bad", "hard", "difficult", "struggling", "not great",
            "terrible", "awful", "horrible", "rough", "tough", "lost", "broken")
        val isWell = any(input, "good", "great", "well", "happy", "fine", "wonderful")
        return when {
            isStrugg -> Pair(
                "I hear that things feel hard right now. 🤍\n\n" +
                "Spirit can sit with you in that — whether you need a breathing practice to ease " +
                "the weight, a grounding track to steady the moment, or just something quiet to listen to.\n\n" +
                "Tell me a little more — is it more stress, exhaustion, sadness, or something else? " +
                "Even one word helps me find the right practice for you.",
                mostPlayed
            )
            isWell -> Pair(
                "That's lovely to hear. ☀️\n\n" +
                "A good moment is a great time to deepen your practice — what are you here for today? " +
                "Focus, relaxation, or exploring a new technique?\n\n" +
                "I can also suggest a track matched to this energy if you'd like.",
                null
            )
            else -> {
                val fallbacks = listOf(
                    "I'm listening. 🤍 Tell me more — are you working with restlessness, stress, or scattered focus? Spirit can help with all three.",
                    "Spirit here 🌙 — I can guide you through rest, focus, relaxation, or talk you through a meditation technique. What's most needed right now?",
                    "I hear you. 🌙 Your most-played — ${mostPlayed.emoji} ${mostPlayed.displayName} — might suit this moment. Want to know why?",
                    "Lovely chatting. Say 'technique' to explore a new way to meditate, or 'recommend' for a soundscape suggestion tailored to right now. 🎵"
                )
                Pair(fallbacks[turnsCount % fallbacks.size], if (turnsCount % 4 == 2) mostPlayed else null)
            }
        }
    }
}
