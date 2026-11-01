package com.knownassurajit.tunr_tool.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.management.ManagementFactory
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class PitchDetectorTest {

    private val sampleRate = 44100

    /** Plucked-string model: decaying harmonic series, optional stretch (inharmonicity) and noise. */
    private fun pluck(
        f0: Double,
        seconds: Double = 1.0,
        harmonics: Int = 12,
        rolloff: Double = 1.0,
        fundamentalGain: Double = 1.0,
        inharmonicity: Double = 0.0,
        decayPerSecond: Double = 1.5,
        noiseRms: Double = 0.0,
        seed: Long = 7,
        peak: Double = 0.5,
    ): ShortArray {
        val n = (seconds * sampleRate).toInt()
        val rnd = Random(seed)
        val out = DoubleArray(n)
        var maxAbs = 1e-9
        for (h in 1..harmonics) {
            val fh = h * f0 * sqrt(1.0 + inharmonicity * h * h)
            if (fh > sampleRate * 0.45) break
            val amp = (if (h == 1) fundamentalGain else 1.0) / h.toDouble().pow(rolloff)
            val decay = decayPerSecond * (1 + 0.3 * h)
            for (i in 0 until n) {
                val t = i.toDouble() / sampleRate
                out[i] += amp * exp(-decay * t) * sin(2 * PI * fh * t + h)
            }
        }
        for (v in out) maxAbs = maxOf(maxAbs, abs(v))
        return ShortArray(n) {
            val s = out[it] / maxAbs * peak + rnd.nextGaussian() * noiseRms
            (s.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
        }
    }

    private fun centsOff(measured: Float, expected: Double) = 1200.0 * ln(measured / expected) / ln(2.0)

    /** Feeds [signal] in [chunk]-sized blocks; returns the last estimate produced (null if none voiced). */
    private fun run(
        signal: ShortArray,
        chunk: Int = 2048,
        detector: PitchDetector = PitchDetector(sampleRate),
        gate: Float = 0.01f,
    ): PitchEstimate? {
        detector.noiseGate = gate
        var last: PitchEstimate? = null
        val buf = ShortArray(chunk)
        var i = 0
        while (i + chunk <= signal.size) {
            System.arraycopy(signal, i, buf, 0, chunk)
            if (detector.push(buf, chunk)) {
                last = PitchEstimate().also {
                    it.frequencyHz = detector.estimate.frequencyHz
                    it.clarity = detector.estimate.clarity
                    it.rms = detector.estimate.rms
                    it.voiced = detector.estimate.voiced
                }
            }
            i += chunk
        }
        return last
    }

    @Test
    fun accurateAcrossTheTuningRange_pureHarmonic() {
        val notes = listOf(
            "B0" to 30.868, "E1" to 41.203, "A1" to 55.0, "E2" to 82.407, "A2" to 110.0,
            "D3" to 146.832, "G3" to 195.998, "B3" to 246.942, "E4" to 329.628,
            "A4" to 440.0, "E5" to 659.255, "B5" to 987.767,
        )
        for ((name, f) in notes) {
            val e = run(pluck(f, decayPerSecond = 0.5))
            assertTrue("$name should be voiced", e != null && e.voiced)
            val err = centsOff(e!!.frequencyHz, f)
            println("PITCH %-3s %8.3f Hz -> %8.3f Hz  %+6.3f¢  clarity %.3f".format(name, f, e.frequencyHz, err, e.clarity))
            assertEquals("$name cents error", 0.0, err, if (f > 800) 2.0 else 0.6)
        }
    }

    @Test
    fun octaveSafe_weakFundamentalAndStretchedPartials() {
        // Real phone mics barely capture the fundamental of low strings; 2nd/3rd partials dominate.
        for (f in listOf(41.203, 82.407, 110.0, 146.832)) {
            val e = run(pluck(f, fundamentalGain = 0.12, rolloff = 0.6, inharmonicity = 0.0002))
            assertTrue("$f should be voiced", e != null && e.voiced)
            assertEquals("$f weak-fundamental cents error", 0.0, centsOff(e!!.frequencyHz, f), 3.0)
        }
    }

    @Test
    fun robustToNoise() {
        // noiseRms is relative to a 0.5 peak signal (≈ 0.17 rms): 0.005 ≈ 31 dB SNR, 0.03 ≈ 15 dB SNR.
        for ((noise, limit) in listOf(0.005 to 1.0, 0.03 to 8.0)) {
            for (f in listOf(82.407, 196.0, 440.0)) {
                val e = run(pluck(f, noiseRms = noise, rolloff = 1.0))
                assertTrue("$f voiced under noise $noise", e != null && e.voiced)
                val err = centsOff(e!!.frequencyHz, f)
                println("NOISE rms=%.3f %7.2f Hz -> %+6.3f¢".format(noise, f, err))
                assertEquals("$f noisy($noise) cents error", 0.0, err, limit)
            }
        }
    }

    @Test
    fun rejectsSilenceAndWhiteNoise() {
        val silence = ShortArray(sampleRate)
        assertFalse(run(silence)?.voiced ?: false)

        val rnd = Random(3)
        val noise = ShortArray(sampleRate) { (rnd.nextGaussian() * 0.1 * 32767).toInt().toShort() }
        val e = run(noise, gate = 0.005f)
        assertFalse("white noise must not be voiced (clarity ${e?.clarity})", e?.voiced ?: false)
    }

    @Test
    fun chunkSizeDoesNotChangeTheResult() {
        val signal = pluck(110.0)
        val a = run(signal, chunk = 2048)!!.frequencyHz
        val b = run(signal.copyOf(signal.size - signal.size % 2048), chunk = 512)!!.frequencyHz
        assertEquals(a.toDouble(), b.toDouble(), 0.01)
    }

    @Test
    fun releaseHysteresisKeepsDecayingNoteVoiced() {
        val detector = PitchDetector(sampleRate)
        val loud = pluck(110.0, decayPerSecond = 0.0, peak = 0.3, seconds = 1.0)
        detector.noiseGate = 0.001f
        run(loud, detector = detector, gate = 0.001f)
        val loudRms = detector.estimate.rms
        val gate = loudRms * 0.8f                       // attack needs 0.8x of the loud level…
        val quiet = ShortArray(loud.size) { (loud[it] * 0.5).toInt().toShort() } // …release only 0.38x of that
        detector.reset()
        assertTrue(run(loud, detector = detector, gate = gate)?.voiced == true)
        assertTrue("a decaying note must hold", run(quiet, detector = detector, gate = gate)?.voiced == true)
        detector.reset()
        assertFalse("but a cold start at that level must stay gated", run(quiet, detector = detector, gate = gate)?.voiced == true)
    }

    @Test
    fun noAllocationsOnTheAudioThread() {
        val bean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        if (!bean.isThreadAllocatedMemorySupported) return
        val detector = PitchDetector(sampleRate)
        val signal = pluck(82.407, seconds = 2.0)
        val buf = ShortArray(2048)
        // warm up (JIT, class init)
        for (k in 0 until 20) { System.arraycopy(signal, (k % 30) * 2048, buf, 0, 2048); detector.push(buf, 2048) }
        val id = Thread.currentThread().id
        val before = bean.getThreadAllocatedBytes(id)
        for (k in 0 until 40) { System.arraycopy(signal, (k % 30) * 2048, buf, 0, 2048); detector.push(buf, 2048) }
        val allocated = bean.getThreadAllocatedBytes(id) - before
        assertTrue("allocated $allocated bytes in 40 frames", allocated < 2048)
    }
}
