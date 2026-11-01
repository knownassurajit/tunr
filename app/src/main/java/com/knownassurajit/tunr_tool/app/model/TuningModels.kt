package com.knownassurajit.tunr_tool.app.model

import androidx.compose.runtime.Immutable

/** One string of a tuning. [number] counts from the thinnest string (1) upward. */
@Immutable
data class TuningString(
    val number: Int,
    val note: String,
    val octave: Int,
    val midi: Int,
    /** Per-string detune in cents (custom tunings, temperaments). */
    val centsOffset: Float = 0f,
) {
    val label: String get() = "$note$octave"

    /** Target frequency for the current A4 reference, so the chips follow calibration. */
    fun hz(a4: Float): Float = Notes.hz(midi, centsOffset, a4)
}

/** A playable tuning: catalog entry or user-defined. An empty [strings] list means chromatic mode. */
@Immutable
data class Tuning(
    val id: String,
    val name: String,
    val categoryId: String,
    val instrumentName: String,
    val variantName: String,
    val strings: List<TuningString>,
    val isCustom: Boolean = false,
) {
    val isChromatic: Boolean get() = strings.isEmpty()
    val notes: String get() = if (strings.isEmpty()) "All notes" else strings.joinToString(" ") { it.note }
    val path: String get() = if (variantName.isBlank() || variantName == instrumentName) instrumentName else "$instrumentName · $variantName"
}

object Tunings {
    const val CHROMATIC_ID = "chromatic"
    const val STANDARD_ID = "guitar.acoustic.standard"
    const val CUSTOM_CATEGORY = "custom"

    private fun s(number: Int, spec: String): TuningString {
        val p = checkNotNull(Notes.parse(spec)) { "bad note $spec" }
        return TuningString(number, p.name, p.octave, p.midi)
    }

    /** Built-in fallbacks so the app still works if the catalog asset were ever unreadable. */
    val Standard = Tuning(
        STANDARD_ID, "Standard", "fretted", "Guitar", "Acoustic",
        listOf(s(6, "E2"), s(5, "A2"), s(4, "D3"), s(3, "G3"), s(2, "B3"), s(1, "E4")),
    )
    val Chromatic = Tuning(CHROMATIC_ID, "Chromatic", "chromatic", "Chromatic", "All 12 tones", emptyList())
}

// ---- Catalog (read-only, data-driven) -----------------------------------------------------

@Immutable
class VariantSpec(val id: String, val name: String, val note: String?, val tunings: List<Tuning>)

@Immutable
class InstrumentSpec(val id: String, val name: String, val categoryId: String, val variants: List<VariantSpec>) {
    val tuningCount: Int get() = variants.sumOf { it.tunings.size }
}

@Immutable
class TuningCategory(val id: String, val name: String, val instruments: List<InstrumentSpec>)

// ---- Custom tunings (user data) -----------------------------------------------------------

@kotlinx.serialization.Serializable
@Immutable
data class CustomString(
    val note: String,
    val octave: Int,
    /** −50…+50 cents. */
    val cents: Float = 0f,
)

@kotlinx.serialization.Serializable
@Immutable
data class CustomTuning(
    val id: String,
    val name: String,
    /** Thickest (highest number) string first, same order as the catalog. */
    val strings: List<CustomString>,
) {
    fun toTuning(): Tuning {
        val n = strings.size
        return Tuning(
            id = id,
            name = name,
            categoryId = Tunings.CUSTOM_CATEGORY,
            instrumentName = "Custom",
            variantName = "My tunings",
            strings = strings.mapIndexed { i, cs ->
                val midi = checkNotNull(Notes.midi(cs.note, cs.octave)) { "bad note ${cs.note}" }
                TuningString(n - i, cs.note, cs.octave, midi, cs.cents)
            },
            isCustom = true,
        )
    }
}
