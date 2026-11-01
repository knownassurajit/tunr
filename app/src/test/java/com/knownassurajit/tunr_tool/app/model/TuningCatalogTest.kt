package com.knownassurajit.tunr_tool.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** Validates the shipped assets/tuning_catalog.json end to end (the file is data, so it is tested like code). */
class TuningCatalogTest {
    private val catalog: TuningCatalog = TuningCatalog.parse(File("src/main/assets/tuning_catalog.json").readText())

    @Test fun taxonomyIsCategoryInstrumentVariantTuning() {
        assertEquals(listOf("fretted", "bowed", "chromatic"), catalog.categories.map { it.id })
        val fretted = catalog.categories.first { it.id == "fretted" }.instruments.map { it.id }
        for (needed in listOf("guitar", "bass", "ukulele", "mandolin", "banjo")) assertTrue("missing $needed", needed in fretted)
        val bowed = catalog.categories.first { it.id == "bowed" }.instruments.map { it.id }
        assertEquals(listOf("violin", "viola", "cello", "double_bass"), bowed)
        assertTrue("catalog should be broad, was ${catalog.all.size}", catalog.all.size >= 100)
    }

    @Test fun guitarVariantsAndTunings() {
        val guitar = catalog.instrument("guitar")!!
        assertEquals(
            listOf("acoustic", "classical", "electric6", "electric7", "electric8", "twelve"),
            guitar.variants.map { it.id },
        )
        val wanted = listOf(
            "standard", "half_step_down", "full_step_down", "drop_d", "drop_c", "dadgad",
            "open_g", "open_d", "open_e", "open_a", "open_c", "nashville",
        )
        for (key in wanted) assertNotNull("guitar.acoustic.$key", catalog.find("guitar.acoustic.$key"))
        assertEquals(6, catalog.find("guitar.acoustic.nashville")!!.strings.size)
        assertEquals(7, catalog.find("guitar.electric7.standard")!!.strings.size)
        assertEquals(8, catalog.find("guitar.electric8.standard")!!.strings.size)
        // 12-string lists each distinct pitch once (unison B and E courses are merged)
        assertEquals(10, catalog.find("guitar.twelve.standard")!!.strings.size)
    }

    @Test fun standardGuitarMatchesTheTextbook() {
        val std = catalog.find("guitar.acoustic.standard")!!
        assertEquals(listOf("E2", "A2", "D3", "G3", "B3", "E4"), std.strings.map { it.label })
        assertEquals(listOf(6, 5, 4, 3, 2, 1), std.strings.map { it.number })
        assertEquals(82.41f, std.strings[0].hz(440f), 0.01f)
        assertEquals(329.63f, std.strings[5].hz(440f), 0.01f)
        assertEquals(std.strings.map { it.hz(440f) }, Tunings.Standard.strings.map { it.hz(440f) })
    }

    @Test fun lowStringsReachBassB0() {
        val fiveString = catalog.find("bass.bass5.standard")!!
        assertEquals("B0", fiveString.strings.first().label)
        assertEquals(30.87f, fiveString.strings.first().hz(440f), 0.01f)
        assertEquals("B0", catalog.find("double_bass.bass5.low_b")!!.strings.first().label)
    }

    @Test fun bowedInstrumentsAreTunedInFifths() {
        for ((id, expected) in listOf(
            "violin.violin.standard" to listOf("G3", "D4", "A4", "E5"),
            "viola.viola.standard" to listOf("C3", "G3", "D4", "A4"),
            "cello.cello.standard" to listOf("C2", "G2", "D3", "A3"),
        )) assertEquals(id, expected, catalog.find(id)!!.strings.map { it.label })
    }

    @Test fun ukuleleSizesAndReentrantTuning() {
        for (size in listOf("soprano", "concert", "tenor", "baritone")) assertNotNull(catalog.find("ukulele.$size.standard"))
        // re-entrant: the 4th string (G4) is higher than the 3rd (C4)
        val gcea = catalog.find("ukulele.soprano.standard")!!.strings
        assertEquals(listOf("G4", "C4", "E4", "A4"), gcea.map { it.label })
        assertEquals(listOf("D3", "G3", "B3", "E4"), catalog.find("ukulele.baritone.standard")!!.strings.map { it.label })
    }

    @Test fun everyStringIsInTunableRange() {
        for (t in catalog.all) for (s in t.strings) {
            assertTrue("${t.id} ${s.label}", s.midi in Notes.MIN_MIDI..Notes.MAX_MIDI)
            val hz = s.hz(440f)
            assertTrue("${t.id} ${s.label} $hz Hz", hz in 27f..1600f)
        }
        assertTrue(catalog.all.all { it.strings.size <= TuningCatalog.MAX_STRINGS })
    }

    @Test fun chromaticHasNoStrings() {
        val c = catalog.find(Tunings.CHROMATIC_ID)!!
        assertTrue(c.isChromatic)
    }

    @Test fun legacyIdsStillResolve() {
        for ((old, new) in TuningCatalog.LegacyIds) assertNotNull("$old -> $new", catalog.find(new))
    }

    @Test fun searchMatchesNameInstrumentAndNotes() {
        assertTrue(catalog.search("dadgad").any { it.id == "guitar.acoustic.dadgad" })
        assertTrue(catalog.search("drop d").all { "drop" in it.name.lowercase() || "drop" in it.id })
        assertTrue(catalog.search("violin").any { it.id == "violin.violin.standard" })
        assertTrue("flat spelling", catalog.search("eb ab db").any { it.id == "guitar.acoustic.half_step_down" })
        assertTrue("sharp spelling", catalog.search("f# b e").isNotEmpty())
        assertTrue(catalog.search("   ").isEmpty())
        assertTrue(catalog.search("zzzzzz").isEmpty())
    }

    @Test fun malformedCatalogsAreRejected() {
        fun bad(json: String) { try { TuningCatalog.parse(json); fail("expected failure: $json") } catch (_: Exception) {} }
        val wrap = { t: String -> """{"categories":[{"id":"c","name":"C","instruments":[{"id":"i","name":"I","variants":[{"id":"v","name":"V","tunings":[$t]}]}]}]}""" }
        bad(wrap("""{"id":"a","name":"A","strings":["H2"]}"""))
        bad(wrap("""{"id":"a","name":"A","strings":["E9"]}"""))
        bad(wrap("""{"id":"a","name":"A","strings":["E2"]},{"id":"a","name":"B","strings":["A2"]}"""))
        bad("not json")
    }
}
