package com.knownassurajit.tunr_tool.app.model

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Represents a musical note in 12-tone equal temperament (12-TET).
 */
data class MusicalNote(
    val name: String,
    val octave: Int,
    val targetFrequencyHz: Float,
    val midiNoteNumber: Int
) {
    val displayName: String
        get() = "$name${toSubscript(octave)}"

    companion object {
        val NOTE_NAMES_SHARP = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")
        val NOTE_NAMES_FLAT = arrayOf("C", "D♭", "D", "E♭", "E", "F", "G♭", "G", "A♭", "A", "B♭", "B")

        /**
         * Resolves the nearest musical note given a detected frequency and calibration (standard A4 = 440 Hz).
         */
        fun fromFrequency(frequencyHz: Float, a4CalibrationHz: Float = 440f, useSharps: Boolean = true): MusicalNote {
            if (frequencyHz <= 0f) {
                return MusicalNote(name = "--", octave = 0, targetFrequencyHz = 0f, midiNoteNumber = 0)
            }
            // MIDI note formula: n = 69 + 12 * log2(f / a4)
            val midiFloat = 69f + 12f * log2(frequencyHz / a4CalibrationHz)
            val midiInt = midiFloat.roundToInt().coerceIn(12, 127) // C0 to G9

            val noteIndex = (midiInt % 12 + 12) % 12
            val octave = (midiInt / 12) - 1
            val noteName = if (useSharps) NOTE_NAMES_SHARP[noteIndex] else NOTE_NAMES_FLAT[noteIndex]
            val exactTargetHz = a4CalibrationHz * 2.0.pow((midiInt - 69) / 12.0).toFloat()

            return MusicalNote(
                name = noteName,
                octave = octave,
                targetFrequencyHz = exactTargetHz,
                midiNoteNumber = midiInt
            )
        }

        /**
         * Computes cent deviation between measured frequency and target frequency.
         * cents = 1200 * log2(f / f_target)
         */
        fun calculateCents(frequencyHz: Float, targetFrequencyHz: Float): Float {
            if (frequencyHz <= 0f || targetFrequencyHz <= 0f) return 0f
            return (1200.0 * log2(frequencyHz.toDouble() / targetFrequencyHz.toDouble())).toFloat()
        }

        private fun toSubscript(number: Int): String {
            return when (number) {
                0 -> "₀"
                1 -> "₁"
                2 -> "₂"
                3 -> "₃"
                4 -> "₄"
                5 -> "₅"
                6 -> "₆"
                7 -> "₇"
                8 -> "₈"
                9 -> "₉"
                else -> number.toString()
            }
        }
    }
}
