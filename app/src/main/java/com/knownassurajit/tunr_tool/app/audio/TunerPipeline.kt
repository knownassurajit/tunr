package com.knownassurajit.tunr_tool.app.audio

import com.knownassurajit.tunr_tool.app.model.Notes
import com.knownassurajit.tunr_tool.app.model.PitchReading
import com.knownassurajit.tunr_tool.app.model.TunerSensitivity
import com.knownassurajit.tunr_tool.app.model.Tuning
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

/** One tuner update. [reading] is null when nothing is being played (after the hold time). */
class TunerFrame(
    val reading: PitchReading?,
    /** String the reading was matched to (null for chromatic / idle). */
    val matchedString: Int?,
    val locked: Boolean,
)

/**
 * Turns raw detector frequencies into a stable, musically meaningful reading:
 * median-of-N in the log-frequency domain → target resolution (string / nearest semitone) →
 * adaptive EMA on cents → debounced lock with hysteresis. Pure Kotlin, single-threaded use
 * (the audio thread); [reset] may be requested from any thread.
 */
class TunerPipeline {
    @Volatile private var resetRequested = false

    private val history = DoubleArray(MAX_MEDIAN)
    private val sorted = DoubleArray(MAX_MEDIAN)
    private var histCount = 0
    private var histPos = 0

    private var smoothedCents = Float.NaN
    private var smoothedHz = Float.NaN
    private var lastTarget: Int = Int.MIN_VALUE

    private var misses = 0
    private var last: TunerFrame = IDLE
    private var lockCount = 0
    private var locked = false

    fun reset() { resetRequested = true }

    private fun hardReset() {
        histCount = 0; histPos = 0
        smoothedCents = Float.NaN; smoothedHz = Float.NaN; lastTarget = Int.MIN_VALUE
        misses = 0; last = IDLE; lockCount = 0; locked = false
        resetRequested = false
    }

    /**
     * @param voiced whether the detector found a pitch this frame.
     * @param activeString user-pinned string number when [autoDetect] is false.
     */
    fun update(
        voiced: Boolean,
        frequencyHz: Float,
        tuning: Tuning,
        autoDetect: Boolean,
        activeString: Int?,
        a4Hz: Float,
        flats: Boolean,
        sensitivity: TunerSensitivity,
        lockCents: Float,
    ): TunerFrame {
        if (resetRequested) hardReset()

        if (!voiced || frequencyHz <= 0f) {
            // Brief gaps (pluck attack, note decay) keep the last reading instead of flickering to idle.
            if (++misses <= HOLD_FRAMES && last.reading != null) return last
            val idle = IDLE
            hardReset()
            return idle
        }
        misses = 0

        val window = sensitivity.medianWindow.coerceIn(1, MAX_MEDIAN)
        val logHz = ln(frequencyHz.toDouble()) / LN2
        history[histPos] = logHz
        histPos = (histPos + 1) % MAX_MEDIAN
        if (histCount < MAX_MEDIAN) histCount++
        val hz = 2.0.pow(median(window)).toFloat()

        val strings = tuning.strings
        val note: String
        val octave: Int
        val targetHz: Float
        val targetKey: Int
        var matched: Int? = null
        var rawCents: Float

        if (strings.isEmpty()) {
            val midiExact = 69.0 + 12.0 * ln((hz / a4Hz).toDouble()) / LN2
            val midi = midiExact.roundToInt().coerceIn(0, 127)
            note = Notes.name(midi, flats)
            octave = Notes.octave(midi)
            targetHz = Notes.hz(midi, 0f, a4Hz)
            targetKey = midi
            rawCents = centsBetween(hz, targetHz)
        } else {
            val pinned = if (!autoDetect && activeString != null) strings.firstOrNull { it.number == activeString } else null
            val target = pinned ?: strings.minBy { abs(centsBetween(hz, it.hz(a4Hz))) }
            note = target.note
            octave = target.octave
            targetHz = target.hz(a4Hz)
            targetKey = target.number
            matched = target.number
            rawCents = centsBetween(hz, targetHz)
            // A pinned string can't be a whole octave out: fold octave errors back into ±600¢.
            if (pinned != null) rawCents = rawCents - 1200f * floor((rawCents + 600f) / 1200f)
        }

        // Adaptive EMA: big jumps (new string / bend) follow quickly, small ones are damped by the dead-band.
        val cents: Float
        val shownHz: Float
        if (targetKey == lastTarget && !smoothedCents.isNaN()) {
            val delta = rawCents - smoothedCents
            var alpha = sensitivity.smoothingFactor
            if (abs(delta) > FAST_FOLLOW_CENTS) alpha = maxOf(alpha, 0.65f)
            val effective = if (abs(delta) < sensitivity.deadbandCents) delta * 0.15f else delta
            smoothedCents += effective * alpha
            smoothedHz += (hz - smoothedHz) * alpha
        } else {
            lastTarget = targetKey
            smoothedCents = rawCents
            smoothedHz = hz
            lockCount = 0
            locked = false
        }
        cents = smoothedCents
        shownHz = smoothedHz

        // Debounced lock: 3 consecutive frames inside the window to enter, 1.6x window to leave.
        val a = abs(cents)
        if (locked) {
            if (a > lockCents * EXIT_FACTOR) { locked = false; lockCount = 0 }
        } else if (a <= lockCents) {
            if (++lockCount >= ENTER_FRAMES) locked = true
        } else {
            lockCount = 0
        }

        last = TunerFrame(PitchReading(note, octave, shownHz, targetHz, cents), matched, locked)
        return last
    }

    private fun median(window: Int): Double {
        val n = minOf(window, histCount)
        for (i in 0 until n) sorted[i] = history[(histPos - 1 - i + MAX_MEDIAN) % MAX_MEDIAN]
        java.util.Arrays.sort(sorted, 0, n)
        return if (n % 2 == 1) sorted[n / 2] else (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
    }

    companion object {
        private const val MAX_MEDIAN = 5
        const val HOLD_FRAMES = 4
        const val ENTER_FRAMES = 3
        const val EXIT_FACTOR = 1.6f
        private const val FAST_FOLLOW_CENTS = 25f
        private val LN2 = ln(2.0)
        private val IDLE = TunerFrame(null, null, false)

        fun centsBetween(hz: Float, targetHz: Float): Float =
            (1200.0 * ln(hz.toDouble() / targetHz.toDouble()) / LN2).toFloat()
    }
}
