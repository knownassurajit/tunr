package com.knownassurajit.tunr_tool.app.model

data class InstrumentString(
    val stringNumber: Int,
    val noteName: String,
    val octave: Int,
    val baseFrequencyHz: Float
) {
    val displayLabel: String
        get() = "$noteName$octave"

    fun getCalibratedFrequency(a4CalibrationHz: Float): Float {
        // Recalibrate relative to standard A4 (440Hz)
        return baseFrequencyHz * (a4CalibrationHz / 440f)
    }
}

data class TuningPreset(
    val id: String,
    val name: String,
    val instrumentType: InstrumentType,
    val strings: List<InstrumentString>
) {
    val isChromatic: Boolean
        get() = strings.isEmpty()

    companion object {
        val GUITAR_STANDARD = TuningPreset(
            id = "guitar_standard",
            name = "Standard (E A D G B E)",
            instrumentType = InstrumentType.GUITAR_ACOUSTIC,
            strings = listOf(
                InstrumentString(6, "E", 2, 82.41f),
                InstrumentString(5, "A", 2, 110.00f),
                InstrumentString(4, "D", 3, 146.83f),
                InstrumentString(3, "G", 3, 196.00f),
                InstrumentString(2, "B", 3, 246.94f),
                InstrumentString(1, "E", 4, 329.63f)
            )
        )

        val GUITAR_DROP_D = TuningPreset(
            id = "guitar_drop_d",
            name = "Drop D (D A D G B E)",
            instrumentType = InstrumentType.GUITAR_ELECTRIC,
            strings = listOf(
                InstrumentString(6, "D", 2, 73.42f),
                InstrumentString(5, "A", 2, 110.00f),
                InstrumentString(4, "D", 3, 146.83f),
                InstrumentString(3, "G", 3, 196.00f),
                InstrumentString(2, "B", 3, 246.94f),
                InstrumentString(1, "E", 4, 329.63f)
            )
        )

        val GUITAR_DADGAD = TuningPreset(
            id = "guitar_dadgad",
            name = "DADGAD (Celtic / Folk)",
            instrumentType = InstrumentType.GUITAR_ACOUSTIC,
            strings = listOf(
                InstrumentString(6, "D", 2, 73.42f),
                InstrumentString(5, "A", 2, 110.00f),
                InstrumentString(4, "D", 3, 146.83f),
                InstrumentString(3, "G", 3, 196.00f),
                InstrumentString(2, "A", 3, 220.00f),
                InstrumentString(1, "D", 4, 293.66f)
            )
        )

        val GUITAR_HALF_STEP_DOWN = TuningPreset(
            id = "guitar_half_step_down",
            name = "Half-Step Down (E♭ A♭ D♭ G♭ B♭ E♭)",
            instrumentType = InstrumentType.GUITAR_ELECTRIC,
            strings = listOf(
                InstrumentString(6, "E♭", 2, 77.78f),
                InstrumentString(5, "A♭", 2, 103.83f),
                InstrumentString(4, "D♭", 3, 138.59f),
                InstrumentString(3, "G♭", 3, 185.00f),
                InstrumentString(2, "B♭", 3, 233.08f),
                InstrumentString(1, "E♭", 4, 311.13f)
            )
        )

        val BASS_STANDARD = TuningPreset(
            id = "bass_standard",
            name = "Bass Standard (E A D G)",
            instrumentType = InstrumentType.BASS_GUITAR,
            strings = listOf(
                InstrumentString(4, "E", 1, 41.20f),
                InstrumentString(3, "A", 1, 55.00f),
                InstrumentString(2, "D", 2, 73.42f),
                InstrumentString(1, "G", 2, 98.00f)
            )
        )

        val UKULELE_STANDARD = TuningPreset(
            id = "ukulele_standard",
            name = "Ukulele Standard (G C E A)",
            instrumentType = InstrumentType.UKULELE,
            strings = listOf(
                InstrumentString(4, "G", 4, 392.00f),
                InstrumentString(3, "C", 4, 261.63f),
                InstrumentString(2, "E", 4, 329.63f),
                InstrumentString(1, "A", 4, 440.00f)
            )
        )

        val CHROMATIC = TuningPreset(
            id = "chromatic",
            name = "Chromatic (All Notes)",
            instrumentType = InstrumentType.CHROMATIC,
            strings = emptyList()
        )

        val ALL_PRESETS = listOf(
            GUITAR_STANDARD,
            GUITAR_DROP_D,
            GUITAR_DADGAD,
            GUITAR_HALF_STEP_DOWN,
            BASS_STANDARD,
            UKULELE_STANDARD,
            CHROMATIC
        )
    }
}
