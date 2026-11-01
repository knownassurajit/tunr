package com.knownassurajit.tunr_tool.app.audio

import com.knownassurajit.tunr_tool.app.model.TuneStatus
import com.knownassurajit.tunr_tool.app.model.TunerSensitivity
import com.knownassurajit.tunr_tool.app.model.TunerState
import com.knownassurajit.tunr_tool.app.model.Tuning
import com.knownassurajit.tunr_tool.app.model.TuningCatalog
import com.knownassurajit.tunr_tool.app.model.Tunings
import com.knownassurajit.tunr_tool.app.model.status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/**
 * End-to-end: synthetic instrument audio → PitchDetector → TunerPipeline, exactly the chain the
 * ViewModel runs on the audio thread (minus Android). Covers detection, A4 calibration, string
 * matching, custom detune and the debounced lock.
 */
class TunerChainTest {
    private val sr = 44100
    private val catalog = TuningCatalog.parse(File("src/main/assets/tuning_catalog.json").readText())

    private fun pluck(hz: Double, seconds: Double, fundamental: Double = 1.0, decay: Double = 0.4): ShortArray {
        val n = (seconds * sr).toInt()
        val out = DoubleArray(n)
        for (h in 1..10) for (i in 0 until n) {
            val t = i.toDouble() / sr
            out[i] += (if (h == 1) fundamental else 1.0) / h * exp(-decay * (1 + 0.3 * h) * t) * sin(2 * PI * hz * h * t + h)
        }
        val peak = out.maxOf { kotlin.math.abs(it) }
        return ShortArray(n) { (out[it] / peak * 0.5 * 32767).toInt().toShort() }
    }

    /** Runs [signal] through the chain; returns the last non-null frame and whether the lock was ever reached. */
    private class Outcome(val frame: TunerFrame?, val everLocked: Boolean, val states: List<TunerState>)

    private fun run(
        signal: ShortArray,
        tuning: Tuning,
        a4: Float = 440f,
        auto: Boolean = true,
        active: Int? = null,
        lockCents: Float = 3f,
    ): Outcome {
        val detector = PitchDetector(sr).also { it.noiseGate = 0.005f }
        val pipeline = TunerPipeline()
        var last: TunerFrame? = null
        var everLocked = false
        val states = ArrayList<TunerState>()
        var i = 0
        val buf = ShortArray(detector.hopSamples)
        while (i + buf.size <= signal.size) {
            System.arraycopy(signal, i, buf, 0, buf.size)
            if (detector.push(buf, buf.size)) {
                val e = detector.estimate
                val f = pipeline.update(e.voiced, e.frequencyHz, tuning, auto, active, a4, false, TunerSensitivity.Balanced, lockCents)
                if (f.reading != null) last = f
                everLocked = everLocked || f.locked
                states += TunerState(tuning = tuning, reading = f.reading, locked = f.locked)
            }
            i += buf.size
        }
        return Outcome(last, everLocked, states)
    }

    private fun hz(base: Double, cents: Double) = base * 2.0.pow(cents / 1200.0)

    @Test fun lowEOnStandardGuitar_flat() {
        val o = run(pluck(hz(82.407, -25.0), 1.5), Tunings.Standard)
        val r = o.frame!!.reading!!
        assertEquals("E", r.note); assertEquals(2, r.octave)
        assertEquals(6, o.frame.matchedString)
        assertEquals(-25f, r.cents, 1.0f)
        assertFalse(o.everLocked)
        assertEquals(TuneStatus.Far, o.states.last().status())
    }

    @Test fun locksWhenInTune() {
        val o = run(pluck(110.0, 1.5), Tunings.Standard)
        assertTrue(o.everLocked)
        assertEquals(TuneStatus.InTune, o.states.last().status())
        assertEquals(0f, o.frame!!.reading!!.cents, 0.5f)
    }

    @Test fun weakFundamentalLowEStillResolvesToE2NotAnOctaveUp() {
        // On phone mics the E2 fundamental is often 10-20 dB below the 2nd/3rd partials.
        val o = run(pluck(82.407, 1.5, fundamental = 0.15), Tunings.Standard)
        assertEquals(6, o.frame!!.matchedString)
        assertEquals(0f, o.frame.reading!!.cents, 2f)
    }

    @Test fun fiveStringBassLowB0() {
        val bass5 = catalog.find("bass.bass5.standard")!!
        val o = run(pluck(30.868, 2.0), bass5)
        val r = o.frame!!.reading!!
        assertEquals("B", r.note); assertEquals(0, r.octave)
        assertEquals(5, o.frame.matchedString)
        assertEquals(0f, r.cents, 1f)
    }

    @Test fun referencePitchCalibration() {
        // A4 played at 432 Hz: 0¢ when the reference is 432, ≈ −31.8¢ against 440.
        val a432 = pluck(432.0, 1.2)
        assertEquals(0f, run(a432, Tunings.Chromatic, a4 = 432f).frame!!.reading!!.cents, 0.5f)
        assertEquals(-31.77f, run(a432, Tunings.Chromatic, a4 = 440f).frame!!.reading!!.cents, 1f)
    }

    @Test fun customTuningWithCentsOffsetLocksAtTheDetunedTarget() {
        val custom = com.knownassurajit.tunr_tool.app.model.CustomTuning(
            "c", "Just A", listOf(com.knownassurajit.tunr_tool.app.model.CustomString("A", 2, -14f)),
        ).toTuning()
        val o = run(pluck(hz(110.0, -14.0), 1.5), custom)
        assertTrue("locks on the detuned target", o.everLocked)
        assertEquals(0f, o.frame!!.reading!!.cents, 0.7f)
    }

    @Test fun violinAndUkuleleStrings() {
        val violin = catalog.find("violin.violin.standard")!!
        assertEquals(1, run(pluck(659.255, 1.2), violin).frame!!.matchedString)   // E5 = string 1
        val uke = catalog.find("ukulele.concert.standard")!!
        assertEquals(1, run(pluck(440.0, 1.2), uke).frame!!.matchedString)        // A4
        assertEquals(4, run(pluck(392.0, 1.2), uke).frame!!.matchedString)        // re-entrant G4
    }

    @Test fun pinnedStringIgnoresNeighbours() {
        // User pins the A string (5) but plucks D3: cents are reported against A2, folded into ±600¢.
        val o = run(pluck(146.832, 1.2), Tunings.Standard, auto = false, active = 5)
        assertEquals(5, o.frame!!.matchedString)
        assertNotNull(o.frame.reading)
        assertEquals("A", o.frame.reading!!.note)
        assertTrue(kotlin.math.abs(o.frame.reading.cents) <= 600f)
    }

    @Test fun silenceProducesNoReading() {
        val o = run(ShortArray(sr), Tunings.Standard)
        assertEquals(null, o.frame)
    }
}
