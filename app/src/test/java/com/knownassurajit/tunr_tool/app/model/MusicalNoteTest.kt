package com.knownassurajit.tunr_tool.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MusicalNoteTest {

    @Test
    fun testFromFrequencyStandardA440() {
        val noteA4 = MusicalNote.fromFrequency(440f)
        assertEquals("A", noteA4.name)
        assertEquals(4, noteA4.octave)
        assertEquals(69, noteA4.midiNoteNumber)

        val noteE2 = MusicalNote.fromFrequency(82.41f)
        assertEquals("E", noteE2.name)
        assertEquals(2, noteE2.octave)

        val noteMiddleC = MusicalNote.fromFrequency(261.63f)
        assertEquals("C", noteMiddleC.name)
        assertEquals(4, noteMiddleC.octave)
    }

    @Test
    fun testCentCalculation() {
        // Exact pitch should be 0 cents
        val exactCents = MusicalNote.calculateCents(440f, 440f)
        assertEquals(0f, exactCents, 0.01f)

        // Half a semitone sharp = +50 cents
        val sharpCents = MusicalNote.calculateCents(452.89f, 440f)
        assertTrue("Should be positive cents", sharpCents > 0f)
        assertEquals(50f, sharpCents, 0.5f)

        // Flat = negative cents
        val flatCents = MusicalNote.calculateCents(427.47f, 440f)
        assertTrue("Should be negative cents", flatCents < 0f)
        assertEquals(-50f, flatCents, 0.5f)
    }

    @Test
    fun testA432Calibration() {
        val noteA4 = MusicalNote.fromFrequency(432f, a4CalibrationHz = 432f)
        assertEquals("A", noteA4.name)
        assertEquals(4, noteA4.octave)
        assertEquals(432f, noteA4.targetFrequencyHz, 0.01f)
    }
}
