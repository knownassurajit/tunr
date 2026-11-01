package com.knownassurajit.tunr_tool.app.audio

import com.knownassurajit.tunr_tool.app.model.TunerSensitivity
import com.knownassurajit.tunr_tool.app.model.Tunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class TunerPipelineTest {
    private val std = Tunings.Standard
    private fun detuned(hz: Double, cents: Double) = (hz * 2.0.pow(cents / 1200.0)).toFloat()

    private fun TunerPipeline.feed(
        hz: Float, auto: Boolean = true, active: Int? = null, sens: TunerSensitivity = TunerSensitivity.Balanced,
        lock: Float = 1f, a4: Float = 440f, tuning: com.knownassurajit.tunr_tool.app.model.Tuning = std,
    ) = update(true, hz, tuning, auto, active, a4, false, sens, lock)

    @Test fun matchesNearestStringInLogSpace() {
        val p = TunerPipeline()
        val f = p.feed(110f * 1.0f)
        assertEquals(5, f.matchedString)
        assertEquals("A", f.reading!!.note)
        assertEquals(0f, f.reading!!.cents, 0.05f)
        // E2 82.41 / A2 110: the linear midpoint is 96.2 Hz but the musical (geometric) midpoint is 95.2 Hz
        assertEquals(6, TunerPipeline().feed(94.8f).matchedString)
        assertEquals(5, TunerPipeline().feed(95.7f).matchedString)
    }

    @Test fun followsCalibrationAndCustomCentsOffset() {
        val p = TunerPipeline()
        assertEquals(0f, p.feed(442f, a4 = 442f, tuning = Tunings.Chromatic).reading!!.cents, 0.05f)
        val custom = com.knownassurajit.tunr_tool.app.model.CustomTuning(
            "c", "Detuned", listOf(com.knownassurajit.tunr_tool.app.model.CustomString("A", 2, -20f)),
        ).toTuning()
        val reading = TunerPipeline().feed(detuned(110.0, -20.0), tuning = custom).reading!!
        assertEquals(0f, reading.cents, 0.1f)
        assertEquals(110f * 2.0.pow(-20.0 / 1200).toFloat(), reading.targetHz, 0.01f)
    }

    @Test fun chromaticResolvesNearestSemitone() {
        val r = TunerPipeline().feed(detuned(440.0, 20.0), tuning = Tunings.Chromatic).reading!!
        assertEquals("A", r.note); assertEquals(4, r.octave); assertEquals(20f, r.cents, 0.2f)
        val flat = TunerPipeline().update(true, 466.16f, Tunings.Chromatic, true, null, 440f, true, TunerSensitivity.Balanced, 1f).reading!!
        assertEquals("B♭", flat.note)
    }

    @Test fun medianRejectsAnOctaveGlitch() {
        val p = TunerPipeline()
        var last: TunerFrame? = null
        for (hz in listOf(110f, 110f, 220f, 110f, 110f)) last = p.feed(hz, sens = TunerSensitivity.Smooth)
        assertEquals("A", last!!.reading!!.note)
        assertEquals(0f, last.reading!!.cents, 1f)
    }

    @Test fun pinnedStringFoldsOctaveErrors() {
        val p = TunerPipeline()
        val up = p.feed(164.8f, auto = false, active = 6).reading!!        // E3 heard while E2 is selected
        assertEquals(0f, up.cents, 1f)
        val down = TunerPipeline().feed(41.2f, auto = false, active = 6).reading!!   // E1
        assertEquals(0f, down.cents, 1f)
        assertEquals("E", up.note); assertEquals(2, up.octave)
    }

    @Test fun lockNeedsThreeStableFramesAndHasHysteresis() {
        val p = TunerPipeline()
        val locked = ArrayList<Boolean>()
        repeat(4) { locked += p.feed(110f, lock = 1f, sens = TunerSensitivity.Responsive).locked }
        assertEquals(listOf(false, false, true, true), locked)

        // 1.3¢ is outside the 1¢ window but inside the 1.6x release band → stays locked
        assertTrue(p.feed(detuned(110.0, 1.3), lock = 1f, sens = TunerSensitivity.Responsive).locked)
        // a real drift releases it
        var f: TunerFrame? = null
        repeat(6) { f = p.feed(detuned(110.0, 6.0), lock = 1f, sens = TunerSensitivity.Responsive) }
        assertFalse(f!!.locked)
    }

    @Test fun lockDoesNotChatterOnABoundaryFlutter() {
        val p = TunerPipeline()
        repeat(5) { p.feed(110f, lock = 1f, sens = TunerSensitivity.Responsive) }
        var flips = 0
        var prev = true
        for (i in 0 until 40) {
            val cents = if (i % 2 == 0) 0.9 else 1.4          // flutters across the 1¢ edge
            val l = p.feed(detuned(110.0, cents), lock = 1f, sens = TunerSensitivity.Responsive).locked
            if (l != prev) flips++
            prev = l
        }
        assertEquals("lock must not toggle (haptic chatter)", 0, flips)
    }

    @Test fun holdsReadingAcrossShortDropoutsThenClears() {
        val p = TunerPipeline()
        repeat(3) { p.feed(110f) }
        repeat(TunerPipeline.HOLD_FRAMES) {
            assertNotNull("dropout frame ${it + 1} should be bridged", p.update(false, 0f, std, true, null, 440f, false, TunerSensitivity.Balanced, 1f).reading)
        }
        assertNull(p.update(false, 0f, std, true, null, 440f, false, TunerSensitivity.Balanced, 1f).reading)
    }

    @Test fun switchingStringResetsSmoothingImmediately() {
        val p = TunerPipeline()
        repeat(5) { p.feed(110f, sens = TunerSensitivity.Smooth) }
        val d = (1..4).map { p.feed(147f, sens = TunerSensitivity.Smooth) }.last()  // D3, +~0.4¢
        assertEquals(4, d.matchedString)
        assertTrue(kotlin.math.abs(d.reading!!.cents) < 5f)
    }

    @Test fun resetClearsState() {
        val p = TunerPipeline()
        repeat(5) { p.feed(110f, lock = 1f) }
        p.reset()
        assertFalse(p.feed(110f, lock = 1f).locked)
    }
}
