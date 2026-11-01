package com.knownassurajit.tunr_tool.app.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ---- Wire format of assets/tuning_catalog.json (Category > Instrument > Variant > Tuning) ----

@Serializable private data class CatalogDto(val version: Int = 1, val categories: List<CategoryDto>)
@Serializable private data class CategoryDto(val id: String, val name: String, val instruments: List<InstrumentDto>)
@Serializable private data class InstrumentDto(val id: String, val name: String, val variants: List<VariantDto>)
@Serializable private data class VariantDto(
    val id: String,
    val name: String,
    val note: String? = null,
    val tunings: List<TuningDto>,
)
@Serializable private data class TuningDto(val id: String, val name: String, val strings: List<String>)

/**
 * Immutable, searchable tuning catalog. The data lives in JSON so adding an instrument or a tuning
 * never touches UI code. Tuning ids are globally unique: `<instrument>.<variant>.<tuning>`.
 */
@Immutable
class TuningCatalog(val categories: List<TuningCategory>) {

    val all: List<Tuning> = categories.flatMap { c ->
        c.instruments.flatMap { i -> i.variants.flatMap { v -> v.tunings } }
    }
    private val byId: Map<String, Tuning> = all.associateBy { it.id }
    private val haystack: Map<String, String> = all.associate { it.id to normalise("${it.name} ${it.instrumentName} ${it.variantName} ${it.notes}") }

    fun find(id: String?): Tuning? = id?.let { byId[it] }

    fun instrument(id: String): InstrumentSpec? =
        categories.firstNotNullOfOrNull { c -> c.instruments.firstOrNull { it.id == id } }

    /** AND-match of whitespace separated tokens over name, instrument, variant and note names. */
    fun search(query: String, limit: Int = 60): List<Tuning> {
        val tokens = normalise(query).split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return emptyList()
        return all.asSequence()
            .filter { t -> val h = haystack.getValue(t.id); tokens.all { it in h } }
            .sortedBy { t -> if (normalise(t.name).startsWith(tokens.first())) 0 else 1 }
            .take(limit)
            .toList()
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        const val MAX_STRINGS = 12

        val Fallback = TuningCatalog(
            listOf(
                TuningCategory(
                    "fretted", "Fretted",
                    listOf(InstrumentSpec("guitar", "Guitar", "fretted", listOf(VariantSpec("acoustic", "Acoustic", null, listOf(Tunings.Standard))))),
                ),
                TuningCategory(
                    "chromatic", "Chromatic",
                    listOf(InstrumentSpec("chromatic", "Chromatic", "chromatic", listOf(VariantSpec("chromatic", "All 12 tones", null, listOf(Tunings.Chromatic))))),
                ),
            ),
        )

        /** @throws IllegalArgumentException on malformed notes, duplicate ids or out-of-range pitches. */
        fun parse(text: String): TuningCatalog {
            val dto = json.decodeFromString<CatalogDto>(text)
            val seen = HashSet<String>()
            val categories = dto.categories.map { c ->
                TuningCategory(
                    c.id, c.name,
                    c.instruments.map { i ->
                        InstrumentSpec(
                            i.id, i.name, c.id,
                            i.variants.map { v ->
                                VariantSpec(
                                    v.id, v.name, v.note,
                                    v.tunings.map { t ->
                                        require(seen.add(t.id)) { "duplicate tuning id ${t.id}" }
                                        require(t.name.isNotBlank()) { "blank name for ${t.id}" }
                                        require(t.strings.size <= MAX_STRINGS) { "${t.id}: too many strings" }
                                        val n = t.strings.size
                                        Tuning(
                                            id = t.id,
                                            name = t.name,
                                            categoryId = c.id,
                                            instrumentName = i.name,
                                            variantName = v.name,
                                            strings = t.strings.mapIndexed { idx, spec ->
                                                val p = requireNotNull(Notes.parse(spec)) { "${t.id}: bad note '$spec'" }
                                                require(p.midi in Notes.MIN_MIDI..Notes.MAX_MIDI) { "${t.id}: $spec outside tunable range" }
                                                TuningString(n - idx, p.name, p.octave, p.midi)
                                            },
                                        )
                                    },
                                )
                            },
                        )
                    },
                )
            }
            return TuningCatalog(categories)
        }

        /** Ids stored by earlier releases → current catalog ids, so a saved selection survives the upgrade. */
        val LegacyIds = mapOf(
            "standard" to Tunings.STANDARD_ID,
            "drop_d" to "guitar.electric6.drop_d",
            "dadgad" to "guitar.acoustic.dadgad",
            "half_step_down" to "guitar.electric6.half_step_down",
            "bass" to "bass.bass4.standard",
            "ukulele" to "ukulele.concert.standard",
        )

        private fun normalise(s: String) = s.lowercase().replace('♯', '#').replace('♭', 'b').trim()
    }
}
