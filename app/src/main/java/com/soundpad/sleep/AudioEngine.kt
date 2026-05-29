package com.soundpad.sleep

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.*
import kotlin.random.Random

/**
 * Pure audio synthesis engine. Runs its own thread. Zero Android UI dependencies.
 * Generates real-time PCM for 14 distinct sound types using DSP algorithms.
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
    @Volatile private var currentType: SoundType = SoundType.WHITE_NOISE
    @Volatile private var volume: Float = 0.7f

    // ── Pink noise state (Voss-McCartney algorithm) ───────────────────────────
    private var pB0 = 0.0; private var pB1 = 0.0; private var pB2 = 0.0
    private var pB3 = 0.0; private var pB4 = 0.0; private var pB5 = 0.0; private var pB6 = 0.0

    // ── Brown noise state ─────────────────────────────────────────────────────
    private var brownLast = 0.0

    // ── Blue / Violet noise state ─────────────────────────────────────────────
    private var blueLastW = 0.0
    private var violetLastW = 0.0; private var violetLastB = 0.0

    // ── Rain ──────────────────────────────────────────────────────────────────
    private data class Drop(var amp: Float, val decay: Float, var life: Int)
    private val rainDrops = mutableListOf<Drop>()
    private var rainBg = 0.0
    private var rainDropTimer = 0

    // ── Ocean ─────────────────────────────────────────────────────────────────
    private var oceanBg = 0.0

    // ── Fire ──────────────────────────────────────────────────────────────────
    private var fireBg = 0.0
    private var crackleAmp = 0f
    private var crackleTimer = 2000

    // ── Fan ───────────────────────────────────────────────────────────────────
    // (stateless, time-based)

    // ── Wind ──────────────────────────────────────────────────────────────────
    private var windS1 = 0.0; private var windS2 = 0.0

    // ── Thunder ───────────────────────────────────────────────────────────────
    private var thunderBg = 0.0
    private var thunderDecay = 0.0
    private var thunderTimer = SAMPLE_RATE * 12

    // ── Spaceship ────────────────────────────────────────────────────────────
    // (time-based)

    // ── Womb ─────────────────────────────────────────────────────────────────
    private var wombBg = 0.0

    // ── Crystal bowl ─────────────────────────────────────────────────────────
    private var crystalEnv = 0.0
    private var crystalTimer = 0
    private val crystalFreqs = doubleArrayOf(261.63, 329.63, 392.0, 523.25, 659.25, 783.99)
    private val crystalPhase = doubleArrayOf(0.0, 1.2, 2.4, 0.8, 2.0, 3.3)

    // ── Sample counter ────────────────────────────────────────────────────────
    private var n = 0L   // total sample count — drives time-based generators

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun start(type: SoundType) {
        stop()
        currentType = type
        resetState()

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
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

    fun changeSound(type: SoundType) {
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

    private fun fillBuffer(buf: ShortArray, type: SoundType) {
        // Timers that tick per-buffer (not per-sample) for efficiency
        rainDropTimer -= buf.size
        thunderTimer -= buf.size
        crystalTimer -= buf.size

        if (rainDropTimer <= 0) {
            rainDropTimer = SAMPLE_RATE / 8   // ~125 ms
            if (type == SoundType.RAIN && Random.nextFloat() < 0.55f) {
                rainDrops.add(
                    Drop(
                        amp   = Random.nextFloat() * 0.35f + 0.06f,
                        decay = Random.nextFloat() * 0.00028f + 0.00008f,
                        life  = Random.nextInt(3000) + 600
                    )
                )
            }
        }

        if (thunderTimer <= 0) {
            thunderTimer = SAMPLE_RATE * (Random.nextInt(18) + 8)  // 8–26 s
            if (type == SoundType.THUNDER && Random.nextFloat() < 0.55f) {
                thunderDecay = Random.nextFloat() * 0.4f + 0.4
            }
        }

        if (crystalTimer <= 0) {
            crystalTimer = SAMPLE_RATE * (Random.nextInt(3) + 3)   // 3–6 s
            if (type == SoundType.CRYSTAL) crystalEnv = 1.0
        }

        for (i in buf.indices) {
            val raw = when (type) {
                SoundType.WHITE_NOISE -> whiteNoise()
                SoundType.PINK_NOISE  -> pinkNoise()
                SoundType.BROWN_NOISE -> brownNoise()
                SoundType.BLUE_NOISE  -> blueNoise()
                SoundType.VIOLET_NOISE-> violetNoise()
                SoundType.RAIN        -> rain()
                SoundType.OCEAN       -> ocean()
                SoundType.FIRE        -> fire()
                SoundType.FAN         -> fan()
                SoundType.WIND        -> wind()
                SoundType.THUNDER     -> thunder()
                SoundType.SPACESHIP   -> spaceship()
                SoundType.WOMB        -> womb()
                SoundType.CRYSTAL     -> crystal()
                else                  -> 0f  // file-backed sounds handled by MediaPlayer
            }
            buf[i] = (raw.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
            n++
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Noise generators
    // ─────────────────────────────────────────────────────────────────────────

    private fun whiteNoise() = (Random.nextFloat() * 2f - 1f)

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

    private fun brownNoise(): Float {
        val w = rnd()
        brownLast = (brownLast + 0.02 * w) / 1.02
        return (brownLast * 3.5).toFloat()
    }

    private fun blueNoise(): Float {
        val w = rnd()
        val blue = w - blueLastW
        blueLastW = w
        return (blue * 0.5).toFloat()
    }

    private fun violetNoise(): Float {
        val w = rnd()
        val b = w - violetLastW
        val v = b - violetLastB
        violetLastW = w
        violetLastB = b
        return (v * 0.25).toFloat()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Nature generators
    // ─────────────────────────────────────────────────────────────────────────

    private fun rain(): Float {
        // Low-pass filtered noise for ambient rain hiss
        rainBg = rainBg * 0.97 + rnd() * 0.03
        var s = rainBg * 0.35

        // Render individual drops (Poisson-triggered, envelope decayed)
        val dead = mutableListOf<Drop>()
        for (d in rainDrops) {
            s += rnd() * d.amp
            d.amp -= d.decay
            d.life--
            if (d.amp <= 0f || d.life <= 0) dead.add(d)
        }
        rainDrops.removeAll(dead)
        return s.toFloat()
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

    private fun fire(): Float {
        fireBg = fireBg * 0.95 + rnd() * 0.05   // warm low-pass rumble
        var s = fireBg * 0.45

        crackleTimer--
        if (crackleTimer <= 0) {
            crackleTimer = Random.nextInt(9000) + 1500
            crackleAmp  = Random.nextFloat() * 0.65f + 0.2f
        }
        if (crackleAmp > 0.01f) {
            s += rnd() * crackleAmp
            crackleAmp *= 0.83f
        }
        return s.toFloat()
    }

    private fun fan(): Float {
        val t = n.toDouble() / SAMPLE_RATE
        val f = 60.0   // motor fundamental 60 Hz
        return ((sin(TWO_PI * f * t) * 0.30 +
                 sin(TWO_PI * f * 2 * t) * 0.14 +
                 sin(TWO_PI * f * 3 * t) * 0.07 +
                 sin(TWO_PI * f * 4 * t) * 0.03 +
                 rnd() * 0.04) * 0.65).toFloat()
    }

    private fun wind(): Float {
        val t = n.toDouble() / SAMPLE_RATE
        val mod = (sin(TWO_PI * 0.09 * t) + 1.0) / 2.0 * 0.75 + 0.25
        windS1 = windS1 * 0.972 + rnd() * 0.028
        windS2 = windS2 * 0.994 + windS1 * 0.006
        return (windS2 * mod * 3.2).toFloat()
    }

    private fun thunder(): Float {
        thunderBg = thunderBg * 0.992 + rnd() * 0.008   // very deep rumble
        var s = thunderBg * thunderDecay * 2.0
        thunderDecay *= 0.99992                          // very slow fade
        if (thunderDecay < 0.001) thunderDecay = 0.0
        s += rnd() * 0.006  // persistent light rain under thunder
        return s.toFloat()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Synthetic generators (original designs)
    // ─────────────────────────────────────────────────────────────────────────

    /** Deep space ambient drone: detuned oscillators + sub-bass + slow LFO */
    private fun spaceship(): Float {
        val t = n.toDouble() / SAMPLE_RATE
        val base = 38.0
        // Slightly detuned oscillators → beating, "alive" drone
        val drone = sin(TWO_PI * base * t) * 0.22 +
                    sin(TWO_PI * (base + 0.17) * t) * 0.14 +
                    sin(TWO_PI * base * 1.497 * t) * 0.10 +    // perfect 5th
                    sin(TWO_PI * base * 2.0 * t) * 0.06 +
                    sin(TWO_PI * (base + sin(TWO_PI * 0.04 * t) * 1.5) * t) * 0.10
        val mod = (sin(TWO_PI * 0.065 * t) + 1.0) / 2.0 * 0.28 + 0.72
        return (drone * mod * 1.9 + rnd() * 0.025).toFloat()
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
        brownLast=0.0; blueLastW=0.0; violetLastW=0.0; violetLastB=0.0
        rainBg=0.0; rainDrops.clear(); rainDropTimer=0
        oceanBg=0.0
        fireBg=0.0; crackleAmp=0f; crackleTimer=2000
        windS1=0.0; windS2=0.0
        thunderBg=0.0; thunderDecay=0.0; thunderTimer=SAMPLE_RATE*12
        wombBg=0.0
        crystalEnv=0.0; crystalTimer=0
        n=0L
    }

}
