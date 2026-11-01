package com.knownassurajit.tunr_tool.app.audio

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Detector-owned mutable result. Read it right after [PitchDetector.push] returns true. */
class PitchEstimate {
    var frequencyHz = 0f
    /** NSDF key-maximum height, 0..1. Doubles as the voicing/periodicity score. */
    var clarity = 0f
    var rms = 0f
    var voiced = false
}

/**
 * Streaming monophonic pitch detector: McLeod Pitch Method (NSDF + key-maximum picking) on a
 * 4x-decimated signal, with the autocorrelation computed by FFT. See docs/RESEARCH.md.
 *
 * - Input: raw PCM16 mono via [push]; any chunk size, one analysis per [hopSamples] of new audio.
 * - Zero allocation after construction (all buffers preallocated), so it is safe on the audio thread.
 * - Range 25 Hz (below bass B0 = 30.9 Hz) to 1.8 kHz; accuracy is sub-cent on clean tones.
 * - Voicing = RMS gate (with release hysteresis) AND NSDF clarity (rejects speech / room noise).
 *
 * Audio is processed in RAM only; nothing is written to disk.
 */
class PitchDetector(
    private val sampleRate: Int = 44100,
    val minFrequency: Float = 25f,
    val maxFrequency: Float = 1800f,
) {
    /** Tunables, safe to change from another thread (benign races; applied on the next frame). */
    @Volatile var noiseGate = 0.012f
    @Volatile var harmonicFilter = true
    @Volatile var subsonicFilter = true

    val estimate = PitchEstimate()

    /** Raw samples consumed between two analyses (≈46 ms at 44.1 kHz). */
    val hopSamples: Int = HOP_DECIMATED * DECIMATION

    private val dsRate = sampleRate.toDouble() / DECIMATION
    private val minLag = max(2, floor(dsRate / maxFrequency).toInt())
    private val maxLag = ceil(dsRate / minFrequency).toInt()

    // Pre-filter state
    private val hpR = 1.0 - 2.0 * PI * HIGH_PASS_HZ / sampleRate
    private var hpX1 = 0.0
    private var hpY1 = 0.0
    private val b0 = DoubleArray(2); private val b1 = DoubleArray(2); private val b2 = DoubleArray(2)
    private val a1 = DoubleArray(2); private val a2 = DoubleArray(2)
    private val z1 = DoubleArray(2); private val z2 = DoubleArray(2)
    private var phase = 0

    // Decimated history ring and analysis scratch (all preallocated)
    private val ring = DoubleArray(FRAME)
    private var write = 0
    private var filled = 0
    private var sinceAnalysis = 0
    private val frame = DoubleArray(FRAME)
    private val prefix = DoubleArray(FRAME + 1)
    private val re = DoubleArray(FFT_SIZE)
    private val im = DoubleArray(FFT_SIZE)
    private val nsdf = FloatArray(maxLag + 1)
    private val peakLag = IntArray(MAX_PEAKS)
    private val peakVal = FloatArray(MAX_PEAKS)
    private val fft = Fft(FFT_SIZE)
    private var voicedLatched = false

    init {
        require(FRAME + maxLag <= FFT_SIZE) { "sampleRate/minFrequency too low for the FFT size" }
        // 4th-order Butterworth anti-alias low-pass as two RBJ biquads (Q = 0.5412, 1.3066).
        val cutoff = min(3500.0, 0.4 * dsRate)
        val w0 = 2.0 * PI * cutoff / sampleRate
        val q = doubleArrayOf(0.5411961, 1.3065630)
        for (s in 0..1) {
            val alpha = sin(w0) / (2.0 * q[s])
            val a0 = 1.0 + alpha
            b0[s] = (1.0 - cos(w0)) / 2.0 / a0
            b1[s] = (1.0 - cos(w0)) / a0
            b2[s] = b0[s]
            a1[s] = -2.0 * cos(w0) / a0
            a2[s] = (1.0 - alpha) / a0
        }
    }

    fun reset() {
        hpX1 = 0.0; hpY1 = 0.0
        z1.fill(0.0); z2.fill(0.0)
        ring.fill(0.0)
        write = 0; filled = 0; sinceAnalysis = 0; phase = 0
        voicedLatched = false
        estimate.voiced = false; estimate.frequencyHz = 0f; estimate.clarity = 0f; estimate.rms = 0f
    }

    /**
     * Feeds [count] raw PCM16 samples. Returns true when a new [estimate] is available (at most
     * once per call; if the chunk spans several hops only the latest frame is analysed).
     */
    fun push(samples: ShortArray, count: Int): Boolean {
        val highPass = subsonicFilter
        for (i in 0 until count) {
            val x = samples[i] * (1.0 / 32768.0)
            val y = if (highPass) {
                val o = x - hpX1 + hpR * hpY1
                hpX1 = x; hpY1 = o
                o
            } else {
                hpX1 = x; hpY1 = 0.0
                x
            }
            // Biquad cascade, transposed direct form II
            val o0 = b0[0] * y + z1[0]
            z1[0] = b1[0] * y - a1[0] * o0 + z2[0]
            z2[0] = b2[0] * y - a2[0] * o0
            val o1 = b0[1] * o0 + z1[1]
            z1[1] = b1[1] * o0 - a1[1] * o1 + z2[1]
            z2[1] = b2[1] * o0 - a2[1] * o1

            if (++phase == DECIMATION) {
                phase = 0
                ring[write] = o1
                write = if (write + 1 == FRAME) 0 else write + 1
                if (filled < FRAME) filled++
                sinceAnalysis++
            }
        }
        if (filled >= FRAME && sinceAnalysis >= HOP_DECIMATED) {
            sinceAnalysis = 0
            analyze()
            return true
        }
        return false
    }

    private fun analyze() {
        val e = estimate
        // Chronological copy (oldest first), mean removed.
        var mean = 0.0
        var idx = write
        for (i in 0 until FRAME) {
            val v = ring[idx]
            frame[i] = v
            mean += v
            idx = if (idx + 1 == FRAME) 0 else idx + 1
        }
        mean /= FRAME
        var sumSq = 0.0
        prefix[0] = 0.0
        for (i in 0 until FRAME) {
            val v = frame[i] - mean
            frame[i] = v
            sumSq += v * v
            prefix[i + 1] = sumSq
        }
        val rms = sqrt(sumSq / FRAME).toFloat()
        e.rms = rms

        // Gate with release hysteresis: a ringing note may decay well below its attack level.
        val gate = if (voicedLatched && harmonicFilter) noiseGate * 0.38f else noiseGate
        if (rms < gate) return unvoiced(e, 0f)

        // Linear autocorrelation by FFT (zero padded: FRAME + maxLag <= FFT_SIZE, so no wrap-around).
        for (i in 0 until FRAME) { re[i] = frame[i]; im[i] = 0.0 }
        for (i in FRAME until FFT_SIZE) { re[i] = 0.0; im[i] = 0.0 }
        fft.transform(re, im)
        for (i in 0 until FFT_SIZE) { re[i] = re[i] * re[i] + im[i] * im[i]; im[i] = 0.0 }
        fft.transform(re, im, inverse = true)

        // NSDF(τ) = 2 r(τ) / m(τ), m(τ) = Σ x[j]² + x[j+τ]² over the overlapping region.
        val scale = 1.0 / FFT_SIZE
        for (tau in 0..maxLag) {
            val m = prefix[FRAME - tau] + (prefix[FRAME] - prefix[tau])
            nsdf[tau] = if (m > 1e-12) (2.0 * re[tau] * scale / m).toFloat() else 0f
        }

        // Key maxima: highest sample of every positive lobe after the first zero crossing.
        var n = 0
        var tau = 1
        while (tau < maxLag && nsdf[tau] > 0f) tau++
        while (tau < maxLag && n < MAX_PEAKS) {
            while (tau < maxLag && nsdf[tau] <= 0f) tau++
            if (tau >= maxLag) break
            var best = tau
            var bestVal = nsdf[tau]
            while (tau < maxLag && nsdf[tau] > 0f) {
                if (nsdf[tau] > bestVal) { best = tau; bestVal = nsdf[tau] }
                tau++
            }
            if (best >= minLag && best < maxLag && nsdf[best] >= nsdf[best + 1]) {
                peakLag[n] = best; peakVal[n] = bestVal; n++
            }
        }
        if (n == 0) return unvoiced(e, 0f)

        var maxVal = 0f
        for (i in 0 until n) if (peakVal[i] > maxVal) maxVal = peakVal[i]
        val clarityMin = if (harmonicFilter) CLARITY_STRICT else CLARITY_LOOSE
        if (maxVal < clarityMin) return unvoiced(e, maxVal)

        val threshold = KEY_MAX_RATIO * maxVal
        var chosen = 0
        for (i in 0 until n) if (peakVal[i] >= threshold) { chosen = i; break }

        // Sub-sample period from a parabola through the chosen peak…
        var period = interpolate(peakLag[chosen])

        // …then refine with the later multiples of the period: lag k·P pins the period to
        // (k·P ± ε)/k, so the interpolation error shrinks ~k-fold. All multiples are combined by
        // least squares through the origin (P = Σk·τk / Σk²), which is what makes low (long-period)
        // and very high (coarsely sampled) notes sub-cent accurate and halves the noise error.
        var sumKT = period
        var sumKK = 1.0
        var k = 2
        while (true) {
            val expected = k * period
            val w = 1 + (0.25 * k).toInt()
            val lo = (expected - w).toInt()
            val hi = ceil(expected + w).toInt()
            if (hi >= maxLag) break
            var best = -1
            for (lag in max(lo, 2)..hi) {
                if (nsdf[lag] > 0f && nsdf[lag] >= nsdf[lag - 1] && nsdf[lag] >= nsdf[lag + 1] && (best < 0 || nsdf[lag] > nsdf[best])) best = lag
            }
            if (best < 0 || nsdf[best] < MULTIPLE_MIN_RATIO * maxVal) break
            sumKT += k * interpolate(best)
            sumKK += k.toDouble() * k
            period = sumKT / sumKK
            k++
        }
        val freq = (dsRate / period).toFloat()
        if (freq < minFrequency * 0.98f || freq > maxFrequency * 1.02f) return unvoiced(e, maxVal)

        e.frequencyHz = freq
        e.clarity = maxVal.coerceIn(0f, 1f)
        e.voiced = true
        voicedLatched = true
    }

    /** Sub-sample vertex of the 3-point parabola through the NSDF around integer lag [t]. */
    private fun interpolate(t: Int): Double {
        val alpha = nsdf[t - 1].toDouble(); val beta = nsdf[t].toDouble(); val gamma = nsdf[t + 1].toDouble()
        val denom = 2.0 * (alpha - 2.0 * beta + gamma)
        val delta = if (denom != 0.0) ((alpha - gamma) / denom).coerceIn(-0.5, 0.5) else 0.0
        return t + delta
    }

    private fun unvoiced(e: PitchEstimate, clarity: Float) {
        e.frequencyHz = 0f
        e.clarity = clarity
        e.voiced = false
        voicedLatched = false
    }

    companion object {
        const val DECIMATION = 4
        /** Decimated samples per analysis frame (139 ms at 11.025 kHz). */
        const val FRAME = 1536
        const val HOP_DECIMATED = 512
        const val FFT_SIZE = 2048
        const val KEY_MAX_RATIO = 0.90f
        const val CLARITY_STRICT = 0.62f
        const val CLARITY_LOOSE = 0.48f
        private const val MULTIPLE_MIN_RATIO = 0.5f
        const val HIGH_PASS_HZ = 15.0
        private const val MAX_PEAKS = 48
    }
}
