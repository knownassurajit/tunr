package com.knownassurajit.tunr_tool.app.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Everything the user owns in the tuning library. Persisted as one JSON document. */
@Serializable
@Immutable
data class LibraryState(
    val custom: List<CustomTuning> = emptyList(),
    /** Tuning ids (catalog or custom), in the order the user starred them. */
    val favorites: List<String> = emptyList(),
    /** Most recently used first. */
    val recents: List<String> = emptyList(),
)

sealed interface TuningError {
    data object NameBlank : TuningError
    data object NameTooLong : TuningError
    data object NameTaken : TuningError
    data object NoStrings : TuningError
    data object TooManyStrings : TuningError
    data class BadNote(val index: Int) : TuningError
    data class OctaveRange(val index: Int) : TuningError
    data class CentsRange(val index: Int) : TuningError
}

object TuningValidation {
    const val MAX_NAME = 40
    const val MAX_STRINGS = 12
    const val MAX_CENTS = 50f

    /** [takenNames] are the other custom tunings' names (case-insensitive uniqueness). */
    fun validate(name: String, strings: List<CustomString>, takenNames: Collection<String> = emptyList()): List<TuningError> {
        val errors = ArrayList<TuningError>()
        val trimmed = name.trim()
        if (trimmed.isEmpty()) errors += TuningError.NameBlank
        if (trimmed.length > MAX_NAME) errors += TuningError.NameTooLong
        if (trimmed.isNotEmpty() && takenNames.any { it.trim().equals(trimmed, ignoreCase = true) }) errors += TuningError.NameTaken
        if (strings.isEmpty()) errors += TuningError.NoStrings
        if (strings.size > MAX_STRINGS) errors += TuningError.TooManyStrings
        strings.forEachIndexed { i, s ->
            val midi = Notes.midi(s.note, s.octave)
            when {
                Notes.pitchClass(s.note) == null -> errors += TuningError.BadNote(i)
                midi == null || midi !in Notes.MIN_MIDI..Notes.MAX_MIDI -> errors += TuningError.OctaveRange(i)
            }
            if (s.cents.isNaN() || s.cents < -MAX_CENTS || s.cents > MAX_CENTS) errors += TuningError.CentsRange(i)
        }
        return errors
    }
}

/** Pure, immutable operations on [LibraryState] (all persistence concerns live elsewhere). */
object LibraryOps {
    const val MAX_RECENTS = 8
    const val MAX_CUSTOM = 60

    fun upsert(state: LibraryState, tuning: CustomTuning): LibraryState {
        val idx = state.custom.indexOfFirst { it.id == tuning.id }
        return when {
            idx >= 0 -> state.copy(custom = state.custom.toMutableList().also { it[idx] = tuning })
            state.custom.size >= MAX_CUSTOM -> state
            else -> state.copy(custom = state.custom + tuning)
        }
    }

    fun duplicate(state: LibraryState, id: String, newId: String): LibraryState {
        val src = state.custom.firstOrNull { it.id == id } ?: return state
        val name = uniqueName(state.custom.map { it.name }, "${src.name} copy")
        val at = state.custom.indexOf(src) + 1
        if (state.custom.size >= MAX_CUSTOM) return state
        return state.copy(custom = state.custom.toMutableList().also { it.add(at, src.copy(id = newId, name = name)) })
    }

    fun delete(state: LibraryState, id: String): LibraryState = state.copy(
        custom = state.custom.filterNot { it.id == id },
        favorites = state.favorites - id,
        recents = state.recents - id,
    )

    fun move(state: LibraryState, id: String, delta: Int): LibraryState {
        val from = state.custom.indexOfFirst { it.id == id }
        if (from < 0) return state
        val to = (from + delta).coerceIn(0, state.custom.lastIndex)
        if (to == from) return state
        val list = state.custom.toMutableList()
        list.add(to, list.removeAt(from))
        return state.copy(custom = list)
    }

    fun toggleFavorite(state: LibraryState, id: String): LibraryState =
        state.copy(favorites = if (id in state.favorites) state.favorites - id else state.favorites + id)

    fun touchRecent(state: LibraryState, id: String): LibraryState {
        if (state.recents.firstOrNull() == id) return state
        return state.copy(recents = (listOf(id) + (state.recents - id)).take(MAX_RECENTS))
    }

    /** "Name", "Name (2)", "Name (3)" … never colliding (case-insensitive) with [existing]. */
    fun uniqueName(existing: Collection<String>, wanted: String): String {
        val taken = existing.map { it.trim().lowercase() }.toSet()
        var candidate = wanted.trim().take(TuningValidation.MAX_NAME)
        var n = 2
        while (candidate.lowercase() in taken) {
            val suffix = " ($n)"
            candidate = wanted.trim().take(TuningValidation.MAX_NAME - suffix.length) + suffix
            n++
        }
        return candidate
    }
}

/** JSON import / export of custom tunings (`.json` file the user can share or back up). */
object TuningExchange {
    private const val FORMAT = "tunr.custom-tunings"
    private const val VERSION = 1
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }

    @Serializable private data class Envelope(val format: String, val version: Int, val tunings: List<CustomTuning>)

    class ImportResult(val tunings: List<CustomTuning>, val skipped: Int)

    fun export(tunings: List<CustomTuning>): String =
        json.encodeToString(Envelope.serializer(), Envelope(FORMAT, VERSION, tunings))

    /**
     * Validates every entry, assigns fresh ids via [newId] and de-duplicates names against [existingNames].
     * Invalid entries are skipped, never partially imported. Throws on a file that is not a TUNR export.
     */
    fun import(text: String, existingNames: Collection<String>, newId: () -> String): ImportResult {
        val env = try {
            json.decodeFromString(Envelope.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a TUNR tuning file", e)
        }
        require(env.format == FORMAT) { "Not a TUNR tuning file" }
        require(env.version <= VERSION) { "File was made by a newer version of TUNR" }
        val names = existingNames.toMutableList()
        var skipped = 0
        val out = ArrayList<CustomTuning>()
        for (t in env.tunings) {
            val name = t.name.trim()
            val strings = t.strings.map { it.copy(note = Notes.pitchClass(it.note)?.let { _ -> normaliseNote(it.note) } ?: it.note) }
            if (TuningValidation.validate(name.ifBlank { "x" }, strings).isNotEmpty() || name.isBlank()) { skipped++; continue }
            val unique = LibraryOps.uniqueName(names, name)
            names += unique
            out += CustomTuning(newId(), unique, strings)
        }
        return ImportResult(out, skipped)
    }

    private fun normaliseNote(note: String): String {
        val t = note.trim()
        return t.substring(0, 1).uppercase() + when (t.substring(1)) { "#", "♯" -> "♯"; "b", "♭" -> "♭"; else -> "" }
    }
}
