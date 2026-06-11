package com.auroramind.meditation

/**
 * The five gentle wake tones generated in real-time by [AudioEngine] for the
 * Meditation Alarm — no licensed audio files, synthesized on-device.
 */
enum class SynthTone(val displayName: String, val emoji: String) {
    SOFT_CHIMES("Soft Chimes", "🔔"),
    GENTLE_RAIN("Gentle Rain", "🌧️"),
    OCEAN_BREEZE("Ocean Breeze", "🌊"),
    WARM_HUSH("Warm Hush", "🌫️"),
    SLOW_HEARTBEAT("Slow Heartbeat", "❤️")
}
