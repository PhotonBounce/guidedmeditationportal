package com.auroramind.meditation

/**
 * Tracks "watched-ad-to-unlock" sounds for the current app session.
 * Resets on process death — that's the deal we offered users.
 *
 * Held in-memory only; not persisted to SharedPreferences so the unlock
 * cleanly expires when the user closes the app.
 */
object SessionPremiumManager {
    private val unlocked = mutableSetOf<SoundType>()

    /** Grant a single premium sound for the rest of the session. */
    fun unlock(type: SoundType) { unlocked.add(type) }

    /**
     * Unlock ALL premium sounds for this session — free daily trial.
     * Free users get to experience every sound once per session.
     * Ads are still shown; upgrading removes ads permanently.
     */
    fun unlockAll() {
        unlocked.addAll(SoundType.values().filter { it.isPremium })
    }

    /** True if the user has watched an ad for this sound this session. */
    fun isUnlocked(type: SoundType): Boolean = type in unlocked

    /** True if any session unlock exists (used to decide whether to keep ads). */
    fun hasAnyUnlock(): Boolean = unlocked.isNotEmpty()

    fun clear() = unlocked.clear()
}
