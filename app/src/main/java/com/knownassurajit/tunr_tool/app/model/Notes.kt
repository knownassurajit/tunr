package com.knownassurajit.tunr_tool.app.model

import kotlin.math.pow

/** 12-TET note maths. MIDI 69 = A4. Accidentals are stored as ♯ / ♭ and parsed from # / b too. */
object Notes {
    /** Detector range is 25 Hz – 1.8 kHz, so the usable tuning range is A0 (27.5 Hz) … G6 (1568 Hz). */
    const val MIN_MIDI = 21
    const val MAX_MIDI = 91

    val SHARP = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
    val FLAT = arrayOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")

    private val SEMITONE = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

    data class Parsed(val name: String, val octave: Int, val midi: Int)

    /** Semitone offset (0..11) of a spelled note such as "E", "F#", "A♭". Null if malformed. */
    fun pitchClass(name: String): Int? {
        val t = name.trim()
        if (t.isEmpty()) return null
        val base = SEMITONE[t[0].uppercaseChar()] ?: return null
        val shift = when (t.substring(1)) {
            "" -> 0
            "#", "♯" -> 1
            "b", "♭" -> -1
            else -> return null
        }
        return ((base + shift) % 12 + 12) % 12
    }

    fun midi(name: String, octave: Int): Int? = pitchClass(name)?.let { pc ->
        val t = name.trim()
        val base = SEMITONE.getValue(t[0].uppercaseChar())
        val shift = pc - base
        // Keep B♯ / C♭ in the octave that scientific pitch notation gives them.
        val adj = when {
            shift > 6 -> shift - 12
            shift < -6 -> shift + 12
            else -> shift
        }
        12 * (octave + 1) + base + adj
    }

    /** Parses "E2", "Eb3", "F#3", "A♭1". Normalises accidentals to ♯/♭. */
    fun parse(text: String): Parsed? {
        val t = text.trim()
        if (t.length < 2) return null
        val split = t.indexOfFirst { it.isDigit() || it == '-' }
        if (split < 1) return null
        val spelled = t.substring(0, split)
        val octave = t.substring(split).toIntOrNull() ?: return null
        val midi = midi(spelled, octave) ?: return null
        val display = spelled.substring(0, 1).uppercase() + when (spelled.substring(1)) {
            "#", "♯" -> "♯"
            "b", "♭" -> "♭"
            else -> ""
        }
        return Parsed(display, octave, midi)
    }

    fun name(midi: Int, flats: Boolean): String =
        (if (flats) FLAT else SHARP)[((midi % 12) + 12) % 12]

    fun octave(midi: Int): Int = Math.floorDiv(midi, 12) - 1

    fun label(midi: Int, flats: Boolean): String = name(midi, flats) + octave(midi)

    fun hz(midi: Int, centsOffset: Float, a4: Float): Float =
        (a4 * 2.0.pow((midi - 69 + centsOffset / 100.0) / 12.0)).toFloat()
}
