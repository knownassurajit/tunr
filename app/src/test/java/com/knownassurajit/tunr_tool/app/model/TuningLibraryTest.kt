package com.knownassurajit.tunr_tool.app.model

import com.knownassurajit.tunr_tool.app.data.LibraryStorage
import com.knownassurajit.tunr_tool.app.data.TuningLibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TuningLibraryTest {
    private fun custom(id: String, name: String = id, vararg notes: Pair<String, Int>) =
        CustomTuning(id, name, (if (notes.isEmpty()) arrayOf("E" to 2, "A" to 2) else notes).map { CustomString(it.first, it.second) })

    // ---- validation -------------------------------------------------------------------------
    @Test fun validationRules() {
        val ok = listOf(CustomString("E", 2), CustomString("A♭", 2, -12.5f))
        assertTrue(TuningValidation.validate("My tuning", ok).isEmpty())
        assertTrue(TuningError.NameBlank in TuningValidation.validate("  ", ok))
        assertTrue(TuningError.NameTooLong in TuningValidation.validate("x".repeat(41), ok))
        assertTrue(TuningError.NameTaken in TuningValidation.validate("my TUNING", ok, listOf("My tuning")))
        assertTrue(TuningError.NoStrings in TuningValidation.validate("n", emptyList()))
        assertTrue(TuningError.TooManyStrings in TuningValidation.validate("n", List(13) { CustomString("E", 2) }))
        assertTrue(TuningValidation.validate("n", List(12) { CustomString("E", 2) }).isEmpty())
        assertTrue(TuningError.BadNote(0) in TuningValidation.validate("n", listOf(CustomString("H", 2))))
        assertTrue(TuningError.OctaveRange(0) in TuningValidation.validate("n", listOf(CustomString("C", 0))))   // C0 = 16 Hz
        assertTrue(TuningError.OctaveRange(0) in TuningValidation.validate("n", listOf(CustomString("C", 8))))
        assertTrue(TuningError.CentsRange(0) in TuningValidation.validate("n", listOf(CustomString("E", 2, 51f))))
        assertTrue(TuningError.CentsRange(0) in TuningValidation.validate("n", listOf(CustomString("E", 2, Float.NaN))))
    }

    @Test fun customTuningConvertsWithCentsAndNumbering() {
        val t = CustomTuning("c1", "Test", listOf(CustomString("D", 2, -10f), CustomString("A", 4))).toTuning()
        assertTrue(t.isCustom)
        assertEquals(listOf(2, 1), t.strings.map { it.number })
        assertEquals(440f, t.strings[1].hz(440f), 0.01f)
        assertEquals(73.42f * Math.pow(2.0, -10.0 / 1200).toFloat(), t.strings[0].hz(440f), 0.02f)
    }

    // ---- ops: create / edit / duplicate / delete / reorder / favorites / recents ---------------
    @Test fun upsertDuplicateDeleteMove() {
        var s = LibraryState()
        s = LibraryOps.upsert(s, custom("a", "Alpha"))
        s = LibraryOps.upsert(s, custom("b", "Beta"))
        s = LibraryOps.upsert(s, custom("a", "Alpha 2"))              // edit in place keeps position
        assertEquals(listOf("Alpha 2", "Beta"), s.custom.map { it.name })

        s = LibraryOps.duplicate(s, "a", "a-copy")                    // lands right after the original
        assertEquals(listOf("a", "a-copy", "b"), s.custom.map { it.id })
        assertEquals("Alpha 2 copy", s.custom[1].name)
        s = LibraryOps.duplicate(s, "a", "a-copy2")
        assertEquals("Alpha 2 copy (2)", s.custom[1].name)

        s = LibraryOps.move(s, "b", -2)
        assertEquals("b", s.custom[1].id)
        s = LibraryOps.move(s, "b", -99)                              // clamped
        assertEquals("b", s.custom.first().id)
        assertEquals(s, LibraryOps.move(s, "b", -1))                  // already first → no change
        assertEquals(s, LibraryOps.move(s, "missing", 1))

        s = LibraryOps.toggleFavorite(LibraryOps.touchRecent(s, "b"), "b")
        s = LibraryOps.delete(s, "b")                                 // cleans favorites + recents too
        assertFalse(s.custom.any { it.id == "b" })
        assertTrue(s.favorites.isEmpty() && s.recents.isEmpty())
    }

    @Test fun favoritesAndRecents() {
        var s = LibraryState()
        s = LibraryOps.toggleFavorite(s, "x"); s = LibraryOps.toggleFavorite(s, "y")
        assertEquals(listOf("x", "y"), s.favorites)
        s = LibraryOps.toggleFavorite(s, "x")
        assertEquals(listOf("y"), s.favorites)

        for (i in 1..12) s = LibraryOps.touchRecent(s, "t$i")
        assertEquals(LibraryOps.MAX_RECENTS, s.recents.size)
        assertEquals("t12", s.recents.first())
        s = LibraryOps.touchRecent(s, "t8")                            // MRU moves to front, no duplicates
        assertEquals("t8", s.recents.first())
        assertEquals(s.recents.size, s.recents.toSet().size)
    }

    @Test fun libraryIsCappedAtMaxCustom() {
        var s = LibraryState()
        for (i in 0 until LibraryOps.MAX_CUSTOM + 5) s = LibraryOps.upsert(s, custom("c$i", "T$i"))
        assertEquals(LibraryOps.MAX_CUSTOM, s.custom.size)
    }

    @Test fun uniqueNames() {
        assertEquals("New", LibraryOps.uniqueName(listOf("x"), "New"))
        assertEquals("New (2)", LibraryOps.uniqueName(listOf("new"), "New"))
        assertEquals("New (3)", LibraryOps.uniqueName(listOf("New", "new (2)"), "New"))
        assertTrue(LibraryOps.uniqueName(listOf("x".repeat(40)), "x".repeat(40)).length <= 40)
    }

    // ---- import / export -------------------------------------------------------------------------
    @Test fun exportImportRoundTrip() {
        val original = listOf(
            CustomTuning("1", "Open Dm", listOf(CustomString("D", 2), CustomString("A", 2, 3.5f), CustomString("F", 3))),
            CustomTuning("2", "Bad-ish", listOf(CustomString("E♭", 3))),
        )
        val json = TuningExchange.export(original)
        var n = 0
        val result = TuningExchange.import(json, existingNames = listOf("Open Dm"), newId = { "new${n++}" })
        assertEquals(0, result.skipped)
        assertEquals(listOf("Open Dm (2)", "Bad-ish"), result.tunings.map { it.name })       // collision → suffix
        assertEquals(listOf("new0", "new1"), result.tunings.map { it.id })                   // fresh ids, never the file's
        assertEquals(original[0].strings, result.tunings[0].strings)
    }

    @Test fun importSkipsInvalidEntriesAndRejectsForeignFiles() {
        val text = """{"format":"tunr.custom-tunings","version":1,"tunings":[
            {"id":"a","name":"Fine","strings":[{"note":"e","octave":2}]},
            {"id":"b","name":"","strings":[{"note":"E","octave":2}]},
            {"id":"c","name":"Too high","strings":[{"note":"C","octave":9}]},
            {"id":"d","name":"Cents","strings":[{"note":"E","octave":2,"cents":75}]}]}"""
        val r = TuningExchange.import(text, emptyList()) { "id" }
        assertEquals(listOf("Fine"), r.tunings.map { it.name })
        assertEquals("E", r.tunings[0].strings[0].note)       // note spelling normalised
        assertEquals(3, r.skipped)

        for (bad in listOf("hello", """{"format":"other","version":1,"tunings":[]}""", """{"format":"tunr.custom-tunings","version":99,"tunings":[]}""")) {
            try { TuningExchange.import(bad, emptyList()) { "x" }; error("accepted $bad") } catch (_: IllegalArgumentException) {}
        }
    }

    // ---- persistence ---------------------------------------------------------------------------------
    private class FakeStorage(var text: String? = null) : LibraryStorage {
        override suspend fun read() = text
        override suspend fun write(text: String) { this.text = text }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun repositoryPersistsAndRestores() = runTest {
        val storage = FakeStorage()
        val io = UnconfinedTestDispatcher(testScheduler)
        var seq = 0
        val repo = TuningLibraryRepository(storage, backgroundScope, io) { "custom.${seq++}" }
        repo.load()
        repo.upsert(custom("custom.x", "Persisted"))
        repo.toggleFavorite("guitar.acoustic.standard")
        repo.touchRecent("custom.x")
        repo.flush()

        val restored = TuningLibraryRepository(storage, backgroundScope, io)
        assertFalse(restored.loaded.value)
        restored.load()
        assertTrue(restored.loaded.value)
        assertEquals(repo.state.value, restored.state.value)
        assertEquals(listOf("Persisted"), restored.state.value.custom.map { it.name })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun editsBeforeLoadAreNotLost() = runTest {
        val storage = FakeStorage(
            kotlinx.serialization.json.Json.encodeToString(LibraryState.serializer(), LibraryState(custom = listOf(custom("old", "Old")))),
        )
        val repo = TuningLibraryRepository(storage, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        repo.upsert(custom("fresh", "Fresh"))      // before load() completes
        repo.load()
        assertEquals(listOf("Old", "Fresh"), repo.state.value.custom.map { it.name })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun corruptStorageFallsBackToEmpty() = runTest {
        val repo = TuningLibraryRepository(FakeStorage("{{{ not json"), backgroundScope, UnconfinedTestDispatcher(testScheduler))
        repo.load()
        assertTrue(repo.loaded.value)
        assertEquals(LibraryState(), repo.state.value)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test fun repositoryImportAndExport() = runTest {
        val repo = TuningLibraryRepository(FakeStorage(), backgroundScope, UnconfinedTestDispatcher(testScheduler))
        repo.load()
        repo.upsert(custom("a", "Alpha"))
        val json = repo.exportJson()
        val (imported, skipped) = repo.importJson(json)
        assertEquals(1 to 0, imported to skipped)
        assertEquals(listOf("Alpha", "Alpha (2)"), repo.state.value.custom.map { it.name })
    }
}
