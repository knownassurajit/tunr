package com.knownassurajit.tunr_tool.app.model

import androidx.compose.runtime.Immutable
import kotlin.math.abs

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

/**
 * Tuner response sensitivity. [smoothingFactor] is the per-analysis EMA weight at the detector's
 * ~21.5 frames/s; [medianWindow] frames of pitch history are median-filtered first (kills octave
 * and outlier frames at the cost of (window-1)/2 frames of lag).
 */
enum class TunerSensitivity(
    val label: String,
    val description: String,
    val smoothingFactor: Float,
    val deadbandCents: Float,
    val medianWindow: Int,
) {
    Smooth("Smooth", "Maximum jitter filtering & gentle dial movement. Calms fluctuating frequencies so you can easily catch up with the tuner dial.", 0.12f, 0.35f, 5),
    Balanced("Standard", "Balanced responsiveness and anti-jitter stabilization for everyday playing.", 0.28f, 0.20f, 3),
    Responsive("Fast", "Direct, instantaneous needle response with minimal damping.", 0.60f, 0.05f, 1),
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
    /** Debounced in-tune state (3 stable frames in, hysteresis out). Drives colour, haptics and sound. */
    val locked: Boolean = false,
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
    /** Increments on every heard beat so a 1-beat bar still re-triggers haptics and the flash. */
    val beatSerial: Int = 0,
    val accents: List<BeatAccent> = listOf(BeatAccent.ACCENT, BeatAccent.NORMAL, BeatAccent.NORMAL, BeatAccent.NORMAL),
    /** Clicks per beat: 1 = none, 2 = eighths, 3 = triplets, 4 = sixteenths. */
    val subdivision: Int = 1,
)

enum class TuneStatus { Idle, Far, Near, InTune }

fun TunerState.status(): TuneStatus = when {
    reading == null -> TuneStatus.Idle
    locked -> TuneStatus.InTune
    abs(reading.cents) <= 10f -> TuneStatus.Near
    else -> TuneStatus.Far
}

fun PitchReading?.status(lockCents: Float): TuneStatus = when {
    this == null -> TuneStatus.Idle
    abs(cents) <= lockCents -> TuneStatus.InTune
    abs(cents) <= 10f -> TuneStatus.Near
    else -> TuneStatus.Far
}
