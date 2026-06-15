package com.auroramind.meditation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.*

/**
 * UI sound effects, synthesized in-memory with AudioTrack — no .wav files.
 *
 * Sounds are pre-rendered to short PCM ShortArrays at init time, then
 * played by handing them to a fresh AudioTrack on each fire (~1ms latency).
 * Volume is independent from the main playback engine and respects whether
 * the user has UI sounds enabled in their system settings.
 *
 * Sounds:
 *   - tap       : soft click, ~40ms, for every button press
 *   - chime     : warm bell ding, ~400ms, on start playback
 *   - swoosh    : white-noise sweep, ~300ms, on sound change
 *   - reward    : ascending arpeggio, ~800ms, on rewarded ad / purchase
 */
class SoundEffects(private val context: Context) {

    companion object {
        private const val SR = 44100
        private const val TWO_PI = 2.0 * PI
    }

    private val prefs = PrefsManager(context)
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val tap     = renderTap()
    private val chime   = renderChime()
    private val swoosh  = renderSwoosh()
    private val reward  = renderReward()
    private val breathingTop = renderBreathingTop()
    private val breathingBottom = renderBreathingBottom()

    fun tap()    = play(tap, 0.35f)
    fun chime()  = play(chime, 0.55f)
    fun swoosh() = play(swoosh, 0.45f)
    fun reward() = play(reward, 0.6f)

    fun breathingTop() = play(breathingTop, 0.45f, ignoreSettings = true)
    fun breathingBottom() = play(breathingBottom, 0.45f, ignoreSettings = true)

    /** Respect the user's system "touch sounds" preference. */
    private fun systemSoundsEnabled(): Boolean =
        audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM) > 0

    private fun play(buffer: ShortArray, gain: Float, ignoreSettings: Boolean = false) {
        if (!ignoreSettings) {
            if (!prefs.isUiSoundsEnabled()) return
            if (!systemSoundsEnabled()) return
        }
        thread(isDaemon = true, name = "SfxPlayer") {
            // A UI sound is never worth crashing the app: some OEM audio HALs throw
            // from AudioTrack build/write/play. Swallow it — silence beats a crash.
            runCatching {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SR)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.setVolume(gain)
                track.write(buffer, 0, buffer.size)
                track.play()
                // Let it drain then release
                Thread.sleep(((buffer.size * 1000L) / SR) + 50)
                runCatching { track.stop(); track.release() }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Sound synthesis — DSP, no files
    // ─────────────────────────────────────────────────────────────────────

    /** Soft click — short burst of bandpassed noise + tiny sine ping. */
    private fun renderTap(): ShortArray {
        val n = (SR * 0.05).toInt()
        val out = ShortArray(n)
        var last = 0.0
        for (i in 0 until n) {
            val t = i / SR.toDouble()
            // Exponential decay envelope, ~25ms half-life
            val env = exp(-t * 60.0)
            // Filtered noise (one-pole low-pass)
            val noise = (Math.random() * 2.0 - 1.0)
            last = last * 0.85 + noise * 0.15
            // Small high sine for "tick" character
            val ping = sin(TWO_PI * 1800.0 * t) * 0.3
            val s = (last * 0.7 + ping) * env
            out[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Warm bell — ringing partials with exponential decay. */
    private fun renderChime(): ShortArray {
        val n = (SR * 0.5).toInt()
        val out = ShortArray(n)
        val freqs   = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)   // C5 E5 G5 C6
        val amps    = doubleArrayOf(0.55, 0.40, 0.30, 0.20)
        val decays  = doubleArrayOf(4.0, 5.5, 7.0, 9.0)
        for (i in 0 until n) {
            val t = i / SR.toDouble()
            // Attack ramp (5ms) then exp decay
            val attack = (t * 200.0).coerceAtMost(1.0)
            var s = 0.0
            for (k in freqs.indices) {
                s += sin(TWO_PI * freqs[k] * t) * amps[k] * exp(-t * decays[k])
            }
            s *= attack
            out[i] = (s.coerceIn(-1.0, 1.0) * Short.MAX_VALUE * 0.55).toInt().toShort()
        }
        return out
    }

    /** Whoosh — filtered white noise with falling cutoff. */
    private fun renderSwoosh(): ShortArray {
        val n = (SR * 0.35).toInt()
        val out = ShortArray(n)
        var lp = 0.0
        for (i in 0 until n) {
            val t = i / SR.toDouble()
            // Triangular env: ramp up 80ms, fall 270ms
            val env = if (t < 0.08) t / 0.08 else (1.0 - (t - 0.08) / 0.27).coerceAtLeast(0.0)
            // Falling low-pass cutoff: starts wide, narrows
            val cutoff = 0.6 - (t / 0.35) * 0.5
            val noise = Math.random() * 2.0 - 1.0
            lp += (noise - lp) * cutoff.coerceIn(0.05, 1.0)
            out[i] = (lp * env * 0.7 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Reward — fast ascending arpeggio, satisfying "you got something" feel. */
    private fun renderReward(): ShortArray {
        val n = (SR * 0.9).toInt()
        val out = ShortArray(n)
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5, 1318.51)  // C E G C E
        val stepDur = 0.12   // seconds per note
        for (i in 0 until n) {
            val t = i / SR.toDouble()
            val noteIdx = (t / stepDur).toInt().coerceAtMost(notes.size - 1)
            val noteTime = t - noteIdx * stepDur
            // Each note: 5ms attack, then exp decay
            val env = (noteTime * 200.0).coerceAtMost(1.0) * exp(-noteTime * 6.0)
            val s = sin(TWO_PI * notes[noteIdx] * t) * 0.5 +
                    sin(TWO_PI * notes[noteIdx] * 2 * t) * 0.15
            out[i] = (s * env * 0.6 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Warm, calm top-of-breath chime/beep. */
    private fun renderBreathingTop(): ShortArray {
        val n = (SR * 0.25).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i / SR.toDouble()
            val env = exp(-t * 15.0)
            val s = sin(TWO_PI * 783.99 * t) * env
            out[i] = (s * Short.MAX_VALUE * 0.35).toInt().toShort()
        }
        return out
    }

    /** Warm, calm bottom-of-breath chime/beep. */
    private fun renderBreathingBottom(): ShortArray {
        val n = (SR * 0.25).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i / SR.toDouble()
            val env = exp(-t * 15.0)
            val s = sin(TWO_PI * 523.25 * t) * env
            out[i] = (s * Short.MAX_VALUE * 0.35).toInt().toShort()
        }
        return out
    }
}
