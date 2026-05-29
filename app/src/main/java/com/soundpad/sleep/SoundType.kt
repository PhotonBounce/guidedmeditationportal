package com.soundpad.sleep

enum class SoundType(
    val displayName: String,
    val description: String,
    val emoji: String,
    val isPremium: Boolean,
    val category: String,
    /** Non-zero = file-backed (res/raw); zero = synthesized by AudioEngine. */
    val rawResId: Int = 0
) {
    // ── FREE ──────────────────────────────────────────────────────────────────
    WHITE_NOISE(
        "White Noise", "All frequencies equal — classic sleep masking",
        "🌫️", false, "Noise"
    ),
    PINK_NOISE(
        "Pink Noise", "Warm, balanced — most natural sounding noise",
        "🌸", false, "Noise"
    ),
    BROWN_NOISE(
        "Brown Noise", "Deep rumbling bass — great for focus & sleep",
        "🟤", false, "Noise"
    ),

    // ── PREMIUM — Noise Colors ─────────────────────────────────────────────
    BLUE_NOISE(
        "Blue Noise", "Crisp high-frequency hiss — masks tinnitus",
        "🔵", true, "Noise"
    ),
    VIOLET_NOISE(
        "Violet Noise", "Ultra-bright hiss — extreme tinnitus masking",
        "💜", true, "Noise"
    ),

    // ── PREMIUM — Nature ───────────────────────────────────────────────────
    RAIN(
        "Gentle Rain", "Soft rainfall on leaves and rooftops",
        "🌧️", true, "Nature"
    ),
    OCEAN(
        "Ocean Waves", "Rolling waves washing on a beach",
        "🌊", true, "Nature"
    ),
    FIRE(
        "Campfire", "Crackling fire with warm low rumble",
        "🔥", true, "Nature"
    ),
    WIND(
        "Forest Wind", "Breeze sweeping through the treetops",
        "🌬️", true, "Nature"
    ),
    THUNDER(
        "Thunder Roll", "Distant rolling thunder & light rain",
        "⛈️", true, "Nature"
    ),

    // ── PREMIUM — Mechanical ──────────────────────────────────────────────
    FAN(
        "Box Fan", "Classic bedroom fan hum — nostalgic comfort",
        "💨", true, "Mechanical"
    ),

    // ── PREMIUM — Synthetic (Original) ────────────────────────────────────
    SPACESHIP(
        "Spaceship", "Deep space ambient drone — focus & study",
        "🚀", true, "Synthetic"
    ),
    WOMB(
        "Womb Sounds", "Heartbeat + warm whoosh — perfect for babies",
        "❤️", true, "Synthetic"
    ),
    CRYSTAL(
        "Crystal Bowls", "Resonant Tibetan singing bowls — meditation",
        "🔮", true, "Synthetic"
    ),

    // ── PREMIUM — Ambient Music ───────────────────────────────────────────
    AMBIENT_01("Ambient Dream 1",  "Soft ambient soundscape for deep sleep", "🌙", true, "Ambient Music", R.raw.ambient_01),
    AMBIENT_02("Ambient Dream 2",  "Gentle floating tones for relaxation",   "✨", true, "Ambient Music", R.raw.ambient_02),
    AMBIENT_03("Ambient Dream 3",  "Ethereal layers for peaceful rest",       "🌌", true, "Ambient Music", R.raw.ambient_03),
    AMBIENT_04("Ambient Dream 4",  "Dreamy pads for drifting off",            "💫", true, "Ambient Music", R.raw.ambient_04),
    AMBIENT_05("Ambient Dream 5",  "Warm atmospheric textures",               "🌠", true, "Ambient Music", R.raw.ambient_05),
    AMBIENT_06("Ambient Dream 6",  "Slow drifting tones for calm nights",     "🌙", true, "Ambient Music", R.raw.ambient_06),
    AMBIENT_07("Ambient Dream 7",  "Hushed reverb clouds for sleep",          "✨", true, "Ambient Music", R.raw.ambient_07),
    AMBIENT_08("Ambient Dream 8",  "Soft harmonic flow for relaxation",       "🌌", true, "Ambient Music", R.raw.ambient_08),
    AMBIENT_09("Ambient Dream 9",  "Gentle shimmer tones",                    "💫", true, "Ambient Music", R.raw.ambient_09),
    AMBIENT_10("Ambient Dream 10", "Breathing ambient waves",                 "🌠", true, "Ambient Music", R.raw.ambient_10),
    AMBIENT_11("Ambient Dream 11", "Lush pads fading into silence",           "🌙", true, "Ambient Music", R.raw.ambient_11),
    AMBIENT_12("Ambient Dream 12", "Airy texture for quiet minds",            "✨", true, "Ambient Music", R.raw.ambient_12),
    AMBIENT_13("Ambient Dream 13", "Deep resonance for slow breathing",       "🌌", true, "Ambient Music", R.raw.ambient_13),
    AMBIENT_14("Ambient Dream 14", "Floaty tones for letting go",             "💫", true, "Ambient Music", R.raw.ambient_14),
    AMBIENT_15("Ambient Dream 15", "Serene atmosphere for night rest",        "🌠", true, "Ambient Music", R.raw.ambient_15),
    AMBIENT_16("Ambient Dream 16", "Hypnotic loops for deep relaxation",      "🌙", true, "Ambient Music", R.raw.ambient_16),
    AMBIENT_17("Ambient Dream 17", "Velvet ambient cushion for sleep",        "✨", true, "Ambient Music", R.raw.ambient_17),
    AMBIENT_18("Ambient Dream 18", "Distant harmonics for quiet nights",      "🌌", true, "Ambient Music", R.raw.ambient_18),
    AMBIENT_19("Ambient Dream 19", "Twilight pads for restful sleep",         "💫", true, "Ambient Music", R.raw.ambient_19),

    // ── PREMIUM — Energy Music ────────────────────────────────────────────
    ENERGY_01("Morning Boost",    "Uplifting energy to kick-start your day",         "⚡", true, "Energy Music", R.raw.energy_01),
    ENERGY_02("Workout Drive",    "High-energy rhythm to power through your workout","🏃", true, "Energy Music", R.raw.energy_02),
    ENERGY_03("Peak Performance", "Intense drive for focus and high performance",    "🔥", true, "Energy Music", R.raw.energy_03);
}
