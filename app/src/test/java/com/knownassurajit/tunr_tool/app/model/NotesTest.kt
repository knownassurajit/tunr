package com.knownassurajit.tunr_tool.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NotesTest {
    @Test fun parsesScientificPitchNotation() {
        assertEquals(40, Notes.parse("E2")!!.midi)
        assertEquals(51, Notes.parse("Eb3")!!.midi)
        assertEquals("E♭", Notes.parse("Eb3")!!.name)
        assertEquals(54, Notes.parse("F#3")!!.midi)
        assertEquals("F♯", Notes.parse("F#3")!!.name)
        assertEquals(32, Notes.parse("A♭1")!!.midi)
        assertEquals(23, Notes.parse("B0")!!.midi)
        assertEquals(69, Notes.parse("a4")!!.midi)
    }

    @Test fun enharmonicEdgeCasesKeepTheirOctave() {
        assertEquals(60, Notes.midi("B#", 3))   // B♯3 sounds as C4
        assertEquals(59, Notes.midi("Cb", 4))   // C♭4 sounds as B3
    }

    @Test fun rejectsMalformedNotes() {
        for (bad in listOf("H2", "E", "2", "E##2", "", "Eb", "E2.5")) assertNull("'$bad'", Notes.parse(bad))
    }

    @Test fun frequenciesFollowA4AndCents() {
        assertEquals(440.0, Notes.hz(69, 0f, 440f).toDouble(), 1e-3)
        assertEquals(261.63, Notes.hz(60, 0f, 440f).toDouble(), 0.01)
        assertEquals(442.0, Notes.hz(69, 0f, 442f).toDouble(), 1e-3)
        assertEquals(452.89, Notes.hz(69, 50f, 440f).toDouble(), 0.01) // +50¢
        assertEquals(30.87, Notes.hz(23, 0f, 440f).toDouble(), 0.01)   // B0, 5-string bass
    }

    @Test fun namesAndOctaves() {
        assertEquals("A♯", Notes.name(70, flats = false))
        assertEquals("B♭", Notes.name(70, flats = true))
        assertEquals(4, Notes.octave(69))
        assertEquals(-1, Notes.octave(0))
        assertNotNull(Notes.parse("C-1"))
    }
}
