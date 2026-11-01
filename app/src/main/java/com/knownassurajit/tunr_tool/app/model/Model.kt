package com.knownassurajit.tunr_tool.app.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.abs
import kotlin.math.pow

enum class Precision(
    val label: String,
    val thresholdLabel: String,
    val fullName: String,
    val lockCents: Float,
    val decimals: Int,
    val summary: String,
) {
    Standard("Standard", "±3¢", "Standard precision", 3f, 0, "Everyday playing. Locks within ±3¢."),
    High("High", "±1¢", "High precision", 1f, 1, "Studio grade. Locks within ±1¢ with a 0.1¢ readout."),
    Ultra("Ultra", "±0.5¢", "Ultra precision", 0.5f, 2, "Luthier grade. Locks within ±0.5¢ with a 0.01¢ readout."),
}

/** Tuner response sensitivity and frequency jitter damping */
enum class TunerSensitivity(
    val label: String,
    val description: String,
    val smoothingFactor: Float,
    val deadbandCents: Float,
) {
    Smooth("Smooth", "Maximum jitter filtering & gentle dial movement. Calms fluctuating frequencies so you can easily catch up with the tuner dial.", 0.20f, 0.35f),
    Balanced("Standard", "Balanced responsiveness and anti-jitter stabilization for everyday playing.", 0.45f, 0.20f),
    Responsive("Fast", "Direct, instantaneous needle response with minimal damping.", 0.85f, 0.05f),
}

/** App-wide AMOLED color schemes selectable from settings */
enum class AppThemeScheme(
    val label: String,
    val description: String,
    val primaryColorHex: Long,
) {
    Emerald("Emerald", "Studio lime & acoustic mint", 0xFFB9F23CL),
    Azure("Azure", "Electric cyan & sky blue", 0xFF38BDF8L),
    Monochrome("Monochrome", "Minimalist matte slate", 0xFFE2E8F0L),
    Amber("Amber", "Warm gold & stage brass", 0xFFFBBF24L),
    Violet("Violet", "Radiant purple & lavender", 0xFFA855F7L),
    Custom("Custom", "Full spectrum & custom HEX code", 0xFFB9F23CL),
}

enum class ScreenTimeout(val label: String, val minutes: Int) {
    AlwaysOn("Always on", 0),
    TwoMinutes("2 min", 2),
    FiveMinutes("5 min", 5),
    TenMinutes("10 min", 10),
    SystemDefault("System", -1),
}

enum class ScreenDimTimeout(val label: String, val seconds: Int) {
    Never("Off", 0),
    ThirtySeconds("30 sec", 30),
    OneMinute("1 min", 60),
    TwoMinutes("2 min", 120),
    FiveMinutes("5 min", 300),
}

enum class NotationPreference(val label: String) {
    Sharps("Sharps ♯"),
    Flats("Flats ♭"),
}

enum class MetronomeSound(val label: String, val description: String) {
    Woodblock("Woodblock", "Warm acoustic wood strike"),
    Digital("Digital click", "Crisp electronic click"),
    Rimshot("Rimshot", "Punchy acoustic drum tap"),
    Clave("Clave", "Bright resonant Latin tone"),
}

/** Instrument representations with icons */
enum class Instrument(val label: String, val icon: ImageVector) {
    AcousticGuitar("Acoustic guitar", Icons.Rounded.MusicNote),
    ElectricGuitar("Electric guitar", Icons.Rounded.MusicNote),
    Bass("Bass guitar", Icons.Rounded.MusicNote),
    Ukulele("Ukulele", Icons.Rounded.MusicNote),
    Chromatic("Chromatic", Icons.Rounded.GraphicEq),
}

@Immutable
data class TuningString(val number: Int, val note: String, val octave: Int, val midi: Int) {
    /** Target frequency for the current A4 reference, so the chips follow calibration. */
    fun hz(a4: Float): Float = (a4 * 2.0.pow((midi - 69) / 12.0)).toFloat()
}

@Immutable
data class Tuning(
    val id: String,
    val name: String,
    val instrument: Instrument,
    val strings: List<TuningString>,
) {
    val notes: String get() = if (strings.isEmpty()) "All notes" else strings.joinToString(" ") { it.note }
}

private val SEMITONE = mapOf('C' to 0, 'D' to 2, 'E' to 4, 'F' to 5, 'G' to 7, 'A' to 9, 'B' to 11)

private fun s(number: Int, note: String, octave: Int): TuningString {
    val accidental = when {
        note.endsWith("♭") -> -1
        note.endsWith("♯") -> 1
        else -> 0
    }
    return TuningString(number, note, octave, 12 * (octave + 1) + SEMITONE.getValue(note[0]) + accidental)
}

object Tunings {
    val Standard = Tuning(
        "standard", "Standard", Instrument.AcousticGuitar,
        listOf(s(6, "E", 2), s(5, "A", 2), s(4, "D", 3), s(3, "G", 3), s(2, "B", 3), s(1, "E", 4)),
    )
    val DropD = Tuning(
        "drop_d", "Drop D", Instrument.ElectricGuitar,
        listOf(s(6, "D", 2), s(5, "A", 2), s(4, "D", 3), s(3, "G", 3), s(2, "B", 3), s(1, "E", 4)),
    )
    val Dadgad = Tuning(
        "dadgad", "DADGAD (Celtic / Folk)", Instrument.AcousticGuitar,
        listOf(s(6, "D", 2), s(5, "A", 2), s(4, "D", 3), s(3, "G", 3), s(2, "A", 3), s(1, "D", 4)),
    )
    val HalfStepDown = Tuning(
        "half_step_down", "Half-step down", Instrument.ElectricGuitar,
        listOf(s(6, "E♭", 2), s(5, "A♭", 2), s(4, "D♭", 3), s(3, "G♭", 3), s(2, "B♭", 3), s(1, "E♭", 4)),
    )
    val Bass = Tuning(
        "bass", "Bass standard", Instrument.Bass,
        listOf(s(4, "E", 1), s(3, "A", 1), s(2, "D", 2), s(1, "G", 2)),
    )
    val Ukulele = Tuning(
        "ukulele", "Ukulele standard", Instrument.Ukulele,
        listOf(s(4, "G", 4), s(3, "C", 4), s(2, "E", 4), s(1, "A", 4)),
    )
    val Chromatic = Tuning("chromatic", "Chromatic", Instrument.Chromatic, emptyList())

    val All = listOf(Standard, DropD, Dadgad, HalfStepDown, Bass, Ukulele, Chromatic)
}

/** One frame from the pitch detector. [cents] is negative when flat, positive when sharp. */
@Immutable
data class PitchReading(
    val note: String,
    val octave: Int,
    val detectedHz: Float,
    val targetHz: Float,
    val cents: Float,
)

@Immutable
data class TunerState(
    val tuning: Tuning = Tunings.Standard,
    val reading: PitchReading? = null,
    val autoDetect: Boolean = true,
    val activeString: Int? = null,
    val tunedStrings: Set<Int> = emptySet(),
)

@Immutable
data class SettingsState(
    val precision: Precision = Precision.High,
    val sensitivity: TunerSensitivity = TunerSensitivity.Balanced,
    val themeScheme: AppThemeScheme = AppThemeScheme.Emerald,
    val customThemeHex: String = "#B9F23C",
    val a4Hz: Float = 440f,
    val noiseGate: Float = 0.5f,
    val harmonicFilter: Boolean = true,
    val subsonicFilter: Boolean = true,
    val notation: NotationPreference = NotationPreference.Sharps,
    val metronomeSound: MetronomeSound = MetronomeSound.Woodblock,
    val metronomeVolume: Float = 0.9f,
    val metronomeHaptics: Boolean = true,
    val metronomeFlash: Boolean = false,
    val keepScreenAwake: Boolean = true,
    val screenTimeout: ScreenTimeout = ScreenTimeout.AlwaysOn,
    val screenDimTimeout: ScreenDimTimeout = ScreenDimTimeout.TwoMinutes,
    val haptics: Boolean = true,
    val soundFeedback: Boolean = true,
)

@Immutable
data class TimeSignature(val beats: Int, val unit: Int) {
    val label: String get() = "$beats/$unit"
}

enum class BeatAccent {
    ACCENT,
    NORMAL,
    MUTE,
}

@Immutable
data class MetronomeState(
    val bpm: Int = 120,
    val signature: TimeSignature = TimeSignature(4, 4),
    val playing: Boolean = false,
    val beat: Int = 0,
    val accents: List<BeatAccent> = listOf(BeatAccent.ACCENT, BeatAccent.NORMAL, BeatAccent.NORMAL, BeatAccent.NORMAL),
)

enum class TuneStatus { Idle, Far, Near, InTune }

fun PitchReading?.status(lockCents: Float): TuneStatus = when {
    this == null -> TuneStatus.Idle
    abs(cents) <= lockCents -> TuneStatus.InTune
    abs(cents) <= 10f -> TuneStatus.Near
    else -> TuneStatus.Far
}
