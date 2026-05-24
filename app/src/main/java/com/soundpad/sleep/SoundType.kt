package com.soundpad.sleep

enum class SoundType(
    val displayName: String,
    val description: String,
    val emoji: String,
    val isPremium: Boolean,
    val category: String
) {
    // ── FREE ──────────────────────────────────────────────────────────────────
    WHITE_NOISE(
        "White Noise", "All frequencies equal — classic sleep masking",
        "⬜", false, "Noise"
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
    );
}
