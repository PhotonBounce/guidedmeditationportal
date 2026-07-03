package com.auroramind.meditation

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*
import kotlin.random.Random

/**
 * Pure audio synthesis engine. Runs its own thread. Zero Android UI dependencies.
 * Generates real-time PCM for the gentle alarm wake tones ([SynthTone]) using DSP algorithms.
 */
class AudioEngine {

    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 4096
        val TWO_PI = 2.0 * PI
    }

    private var audioTrack: AudioTrack? = null
    private var playThread: Thread? = null

    @Volatile private var playing = false
    @Volatile private var currentType: SynthTone = SynthTone.WARM_HUSH
    @Volatile private var volume: Float = 0.7f

    // ── Pink noise state (Voss-McCartney algorithm) ───────────────────────────
    private var pB0 = 0.0; private var pB1 = 0.0; private var pB2 = 0.0
    private var pB3 = 0.0; private var pB4 = 0.0; private var pB5 = 0.0; private var pB6 = 0.0

    // ── Ocean ─────────────────────────────────────────────────────────────────
    private var oceanBg = 0.0

    // ── Wind / rain ───────────────────────────────────────────────────────────
    private var windS1 = 0.0; private var windS2 = 0.0

    // ── Womb / heartbeat ──────────────────────────────────────────────────────
    private var wombBg = 0.0

    // ── Crystal bowl ─────────────────────────────────────────────────────────
    private var crystalEnv = 0.0
    private var crystalTimer = 0
    private val crystalFreqs = doubleArrayOf(261.63, 329.63, 392.0, 523.25, 659.25, 783.99)
    private val crystalPhase = doubleArrayOf(0.0, 1.2, 2.4, 0.8, 2.0, 3.3)

    // ── Sample counter ────────────────────────────────────────────────────────
    private var n = 0L   // total sample count — drives time-based generators

    // ── Equalizer coefficients and state ──────────────────────────────────────
    @Volatile var eqBass: Float = 1.0f
    @Volatile var eqMid: Float = 1.0f
    @Volatile var eqTreble: Float = 1.0f

    private var lpBass = 0.0
    private var hpTreble = 0.0
    private var lastRaw = 0.0

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun start(type: SynthTone, usage: Int = AudioAttributes.USAGE_MEDIA) {
        stop()
        currentType = type
        resetState()

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(BUFFER_SIZE * 4, minBuf))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()
        playing = true

        playThread = Thread({
            val buf = ShortArray(BUFFER_SIZE)
            while (playing) {
                fillBuffer(buf, currentType)
                audioTrack?.write(buf, 0, buf.size)
            }
        }, "AudioEngine").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        playing = false
        playThread?.join(600)
        audioTrack?.run { stop(); release() }
        audioTrack = null
        playThread = null
    }

    fun changeSound(type: SynthTone) {
        currentType = type
        resetState()
    }

    fun setVolume(vol: Float) {
        volume = vol.coerceIn(0f, 1f)
        audioTrack?.setVolume(volume)
    }

    fun isPlaying() = playing
    fun getCurrentType() = currentType

    // ─────────────────────────────────────────────────────────────────────────
    // Internal: buffer fill
    // ─────────────────────────────────────────────────────────────────────────

    private fun fillBuffer(buf: ShortArray, type: SynthTone) {
        // Timers that tick per-buffer (not per-sample) for efficiency
        crystalTimer -= buf.size

        if (crystalTimer <= 0) {
            crystalTimer = SAMPLE_RATE * (Random.nextInt(3) + 3)   // 3–6 s
            if (type == SynthTone.SOFT_CHIMES) crystalEnv = 1.0
        }

        for (i in buf.indices) {
            val raw = when (type) {
                SynthTone.SOFT_CHIMES   -> crystal()
                SynthTone.GENTLE_RAIN   -> wind()
                SynthTone.OCEAN_BREEZE  -> ocean()
                SynthTone.WARM_HUSH     -> pinkNoise()
                SynthTone.SLOW_HEARTBEAT-> womb()
            }
            // 3-band EQ DSP filtering
            lpBass = lpBass * 0.965 + raw * 0.035
            hpTreble = hpTreble * 0.64 + (raw - lastRaw) * 0.36
            lastRaw = raw.toDouble()
            
            val bassVal = lpBass.toFloat()
            val trebleVal = hpTreble.toFloat()
            val midVal = raw - bassVal - trebleVal
            val rawEq = bassVal * eqBass + midVal * eqMid + trebleVal * eqTreble

            buf[i] = (rawEq.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            n++
        }


    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generators
    // ─────────────────────────────────────────────────────────────────────────

    private fun pinkNoise(): Float {
        val w = rnd()
        pB0 = 0.99886 * pB0 + w * 0.0555179
        pB1 = 0.99332 * pB1 + w * 0.0750759
        pB2 = 0.96900 * pB2 + w * 0.1538520
        pB3 = 0.86650 * pB3 + w * 0.3104856
        pB4 = 0.55000 * pB4 + w * 0.5329522
        pB5 = -0.7616 * pB5 - w * 0.0168980
        val pink = pB0 + pB1 + pB2 + pB3 + pB4 + pB5 + pB6 + w * 0.5362
        pB6 = w * 0.115926
        return (pink * 0.11).toFloat()
    }

    private fun ocean(): Float {
        val t = n.toDouble() / SAMPLE_RATE
        // Three overlapping slow sine LFOs modulate a noise floor
        val env = ((sin(TWO_PI * 0.07 * t) +
                    sin(TWO_PI * 0.11 * t + 1.5) * 0.5 +
                    sin(TWO_PI * 0.05 * t + 0.8) * 0.3) / 1.8 + 1.0) / 2.0
        oceanBg = oceanBg * 0.979 + rnd() * 0.021
        return (oceanBg * env * 2.5).toFloat()
    }

    private fun wind(): Float {
        val t = n.toDouble() / SAMPLE_RATE
        val mod = (sin(TWO_PI * 0.09 * t) + 1.0) / 2.0 * 0.75 + 0.25
        windS1 = windS1 * 0.972 + rnd() * 0.028
        windS2 = windS2 * 0.994 + windS1 * 0.006
        return (windS2 * mod * 3.2).toFloat()
    }

    /** Womb: realistic heartbeat (lub-dub) + low whooshing noise floor */
    private fun womb(): Float {
        val beatSamples = (SAMPLE_RATE * 60.0 / 68.0).toLong()   // 68 BPM
        val pos = n % beatSamples

        // Lub — first heart sound (~80 Hz thud)
        val a1 = SAMPLE_RATE * 0.022
        val d1 = SAMPLE_RATE * 0.095
        val lub = when {
            pos < a1              -> pos / a1
            pos < a1 + d1        -> 1.0 - (pos - a1) / d1
            else                  -> 0.0
        }.coerceIn(0.0, 1.0) * sin(TWO_PI * 82 * pos / SAMPLE_RATE) * 0.65

        // Dub — second heart sound at 36% of beat (~100 Hz, quieter)
        val dubStart = (beatSamples * 0.36).toLong()
        val pos2 = pos - dubStart
        val a2 = SAMPLE_RATE * 0.015
        val d2 = SAMPLE_RATE * 0.065
        val dub = if (pos2 > 0) when {
            pos2 < a2             -> pos2 / a2
            pos2 < a2 + d2       -> 1.0 - (pos2 - a2) / d2
            else                  -> 0.0
        }.coerceIn(0.0, 1.0) * sin(TWO_PI * 98 * pos2 / SAMPLE_RATE) * 0.35
        else 0.0

        wombBg = wombBg * 0.9965 + rnd() * 0.0035   // muffled whoosh
        return (lub + dub + wombBg * 0.22).toFloat()
    }

    /** Tibetan crystal singing bowls: C-major overtone series, strike envelope */
    private fun crystal(): Float {
        val t = n.toDouble() / SAMPLE_RATE
        // Slow exponential decay per strike
        crystalEnv *= 0.99993
        if (crystalEnv < 0.001) crystalEnv = 0.0

        var s = 0.0
        for (i in crystalFreqs.indices) {
            val amp = 0.28 / (i + 1)
            s += sin(TWO_PI * crystalFreqs[i] * t + crystalPhase[i]) * amp
            // Faint second harmonic for "glass" quality
            s += sin(TWO_PI * crystalFreqs[i] * 2 * t + crystalPhase[i] * 1.3) * amp * 0.08
        }
        return (s * crystalEnv * 0.55).toFloat()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun rnd() = Random.nextDouble() * 2.0 - 1.0

    private fun resetState() {
        pB0=0.0; pB1=0.0; pB2=0.0; pB3=0.0; pB4=0.0; pB5=0.0; pB6=0.0
        oceanBg=0.0
        windS1=0.0; windS2=0.0
        wombBg=0.0
        crystalEnv=0.0; crystalTimer=0
        lpBass=0.0; hpTreble=0.0; lastRaw=0.0
        n=0L
    }

}
