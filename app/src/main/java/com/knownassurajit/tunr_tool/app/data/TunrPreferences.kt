package com.knownassurajit.tunr_tool.app.data

import android.content.Context
import android.content.SharedPreferences
import com.knownassurajit.tunr_tool.app.model.AppThemeScheme
import com.knownassurajit.tunr_tool.app.model.BeatAccent
import com.knownassurajit.tunr_tool.app.model.MetronomeSound
import com.knownassurajit.tunr_tool.app.model.NotationPreference
import com.knownassurajit.tunr_tool.app.model.Precision
import com.knownassurajit.tunr_tool.app.model.ScreenDimTimeout
import com.knownassurajit.tunr_tool.app.model.ScreenTimeout
import com.knownassurajit.tunr_tool.app.model.SettingsState
import com.knownassurajit.tunr_tool.app.model.TimeSignature
import com.knownassurajit.tunr_tool.app.model.TunerSensitivity
import com.knownassurajit.tunr_tool.app.model.Tunings

/**
 * Robust persistent storage repository for TUNR user configurations.
 * Saves and restores all settings, instrument presets, and metronome configurations
 * using standard Android SharedPreferences with volatile RAM mirror for instant access.
 */
class TunrPreferences(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    companion object {
        const val PREFS_NAME = "tunr_user_preferences"

        // Calibration & Settings keys
        private const val KEY_PRECISION = "tuning_precision"
        private const val KEY_SENSITIVITY = "tuning_sensitivity"
        private const val KEY_THEME_SCHEME = "app_theme_scheme"
        private const val KEY_CUSTOM_THEME_HEX = "custom_theme_hex"
        private const val KEY_A4_HZ = "a4_calibration_hz"
        private const val KEY_NOISE_GATE = "noise_gate_threshold"
        private const val KEY_HARMONIC_FILTER = "harmonic_filter_enabled"
        private const val KEY_SUBSONIC_FILTER = "subsonic_filter_enabled"
        private const val KEY_NOTATION = "notation_preference"
        private const val KEY_METRONOME_SOUND = "metronome_sound_profile"
        private const val KEY_METRONOME_VOLUME = "metronome_volume"
        private const val KEY_METRONOME_HAPTICS = "metronome_haptics_enabled"
        private const val KEY_METRONOME_FLASH = "metronome_flash_enabled"
        private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        private const val KEY_SCREEN_TIMEOUT = "screen_timeout_mode"
        private const val KEY_SCREEN_DIM_TIMEOUT = "screen_dim_timeout_mode"
        private const val KEY_IN_TUNE_HAPTICS = "in_tune_haptics_enabled"
        private const val KEY_IN_TUNE_SOUND = "in_tune_sound_enabled"

        // Tuner preset keys
        private const val KEY_TUNING_ID = "selected_tuning_preset_id"
        private const val KEY_AUTO_DETECT = "auto_detect_enabled"

        // Metronome keys
        private const val KEY_BPM = "metronome_bpm"
        private const val KEY_SIG_BEATS = "metronome_sig_beats"
        private const val KEY_SIG_UNIT = "metronome_sig_unit"
        private const val KEY_ACCENTS_PREFIX = "metronome_accents_beats_"
        private const val KEY_SUBDIVISION = "metronome_subdivision"

        // Navigation
        private const val KEY_LAST_DESTINATION = "last_navigation_destination"
    }

    // ---- SettingsState ----------------------------------------------------

    fun loadSettings(): SettingsState {
        val precision = runCatching {
            val name = prefs.getString(KEY_PRECISION, Precision.High.name)
            Precision.valueOf(name ?: Precision.High.name)
        }.getOrDefault(Precision.High)

        val sensitivity = runCatching {
            val name = prefs.getString(KEY_SENSITIVITY, TunerSensitivity.Balanced.name)
            TunerSensitivity.valueOf(name ?: TunerSensitivity.Balanced.name)
        }.getOrDefault(TunerSensitivity.Balanced)

        val themeScheme = runCatching {
            val name = prefs.getString(KEY_THEME_SCHEME, AppThemeScheme.Emerald.name)
            AppThemeScheme.valueOf(name ?: AppThemeScheme.Emerald.name)
        }.getOrDefault(AppThemeScheme.Emerald)

        val customThemeHex = prefs.getString(KEY_CUSTOM_THEME_HEX, "#B9F23C") ?: "#B9F23C"

        val a4Hz = prefs.getFloat(KEY_A4_HZ, 440f).coerceIn(415f, 466f)
        val noiseGate = prefs.getFloat(KEY_NOISE_GATE, 0.5f).coerceIn(0f, 1f)
        val harmonicFilter = prefs.getBoolean(KEY_HARMONIC_FILTER, true)
        val subsonicFilter = prefs.getBoolean(KEY_SUBSONIC_FILTER, true)

        val notation = runCatching {
            val name = prefs.getString(KEY_NOTATION, NotationPreference.Sharps.name)
            NotationPreference.valueOf(name ?: NotationPreference.Sharps.name)
        }.getOrDefault(NotationPreference.Sharps)

        val sound = runCatching {
            val name = prefs.getString(KEY_METRONOME_SOUND, MetronomeSound.Woodblock.name)
            MetronomeSound.valueOf(name ?: MetronomeSound.Woodblock.name)
        }.getOrDefault(MetronomeSound.Woodblock)

        val metronomeVolume = prefs.getFloat(KEY_METRONOME_VOLUME, 0.9f).coerceIn(0.1f, 1f)
        val metronomeHaptics = prefs.getBoolean(KEY_METRONOME_HAPTICS, true)
        val metronomeFlash = prefs.getBoolean(KEY_METRONOME_FLASH, false)
        val keepScreenAwake = prefs.getBoolean(KEY_KEEP_SCREEN_AWAKE, true)

        val screenTimeout = runCatching {
            val name = prefs.getString(KEY_SCREEN_TIMEOUT, ScreenTimeout.AlwaysOn.name)
            ScreenTimeout.valueOf(name ?: ScreenTimeout.AlwaysOn.name)
        }.getOrDefault(ScreenTimeout.AlwaysOn)

        val screenDimTimeout = runCatching {
            val name = prefs.getString(KEY_SCREEN_DIM_TIMEOUT, ScreenDimTimeout.TwoMinutes.name)
            ScreenDimTimeout.valueOf(name ?: ScreenDimTimeout.TwoMinutes.name)
        }.getOrDefault(ScreenDimTimeout.TwoMinutes)

        val haptics = prefs.getBoolean(KEY_IN_TUNE_HAPTICS, true)
        val soundFeedback = prefs.getBoolean(KEY_IN_TUNE_SOUND, true)

        return SettingsState(
            precision = precision,
            sensitivity = sensitivity,
            themeScheme = themeScheme,
            customThemeHex = customThemeHex,
            a4Hz = a4Hz,
            noiseGate = noiseGate,
            harmonicFilter = harmonicFilter,
            subsonicFilter = subsonicFilter,
            notation = notation,
            metronomeSound = sound,
            metronomeVolume = metronomeVolume,
            metronomeHaptics = metronomeHaptics,
            metronomeFlash = metronomeFlash,
            keepScreenAwake = keepScreenAwake,
            screenTimeout = screenTimeout,
            screenDimTimeout = screenDimTimeout,
            haptics = haptics,
            soundFeedback = soundFeedback,
        )
    }

    fun saveSettings(settings: SettingsState) {
        prefs.edit()
            .putString(KEY_PRECISION, settings.precision.name)
            .putString(KEY_SENSITIVITY, settings.sensitivity.name)
            .putString(KEY_THEME_SCHEME, settings.themeScheme.name)
            .putString(KEY_CUSTOM_THEME_HEX, settings.customThemeHex)
            .putFloat(KEY_A4_HZ, settings.a4Hz)
            .putFloat(KEY_NOISE_GATE, settings.noiseGate)
            .putBoolean(KEY_HARMONIC_FILTER, settings.harmonicFilter)
            .putBoolean(KEY_SUBSONIC_FILTER, settings.subsonicFilter)
            .putString(KEY_NOTATION, settings.notation.name)
            .putString(KEY_METRONOME_SOUND, settings.metronomeSound.name)
            .putFloat(KEY_METRONOME_VOLUME, settings.metronomeVolume)
            .putBoolean(KEY_METRONOME_HAPTICS, settings.metronomeHaptics)
            .putBoolean(KEY_METRONOME_FLASH, settings.metronomeFlash)
            .putBoolean(KEY_KEEP_SCREEN_AWAKE, settings.keepScreenAwake)
            .putString(KEY_SCREEN_TIMEOUT, settings.screenTimeout.name)
            .putString(KEY_SCREEN_DIM_TIMEOUT, settings.screenDimTimeout.name)
            .putBoolean(KEY_IN_TUNE_HAPTICS, settings.haptics)
            .putBoolean(KEY_IN_TUNE_SOUND, settings.soundFeedback)
            .apply()
    }

    // ---- TunerState -------------------------------------------------------

    /** Id of the selected tuning (catalog, legacy, or custom). Resolution happens in the ViewModel. */
    fun loadTuningId(): String = prefs.getString(KEY_TUNING_ID, Tunings.STANDARD_ID) ?: Tunings.STANDARD_ID

    fun saveTuningId(id: String) {
        prefs.edit().putString(KEY_TUNING_ID, id).apply()
    }

    fun loadAutoDetect(): Boolean = prefs.getBoolean(KEY_AUTO_DETECT, true)

    fun saveAutoDetect(autoDetect: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_DETECT, autoDetect).apply()
    }

    // ---- MetronomeState ---------------------------------------------------

    fun loadBpm(): Int = prefs.getInt(KEY_BPM, 120).coerceIn(30, 240)

    fun saveBpm(bpm: Int) {
        prefs.edit().putInt(KEY_BPM, bpm.coerceIn(30, 240)).apply()
    }

    fun loadSubdivision(): Int = prefs.getInt(KEY_SUBDIVISION, 1).coerceIn(1, 4)

    fun saveSubdivision(subdivision: Int) {
        prefs.edit().putInt(KEY_SUBDIVISION, subdivision.coerceIn(1, 4)).apply()
    }

    fun loadSignature(): TimeSignature {
        val beats = prefs.getInt(KEY_SIG_BEATS, 4)
        val unit = prefs.getInt(KEY_SIG_UNIT, 4)
        return TimeSignature(beats, unit)
    }

    fun saveSignature(signature: TimeSignature) {
        prefs.edit()
            .putInt(KEY_SIG_BEATS, signature.beats)
            .putInt(KEY_SIG_UNIT, signature.unit)
            .apply()
    }

    fun loadAccents(beats: Int): List<BeatAccent> {
        val raw = prefs.getString(KEY_ACCENTS_PREFIX + beats, null)
        if (raw != null) {
            val parsed = raw.split(",").mapNotNull { token ->
                runCatching { BeatAccent.valueOf(token.trim()) }.getOrNull()
            }
            if (parsed.size == beats) return parsed
        }
        return List(beats) { if (it == 0) BeatAccent.ACCENT else BeatAccent.NORMAL }
    }

    fun saveAccents(beats: Int, accents: List<BeatAccent>) {
        val raw = accents.joinToString(",") { it.name }
        prefs.edit().putString(KEY_ACCENTS_PREFIX + beats, raw).apply()
    }

    // ---- Navigation Destination -------------------------------------------

    fun loadLastDestination(): String = prefs.getString(KEY_LAST_DESTINATION, "tuner") ?: "tuner"

    fun saveLastDestination(destination: String) {
        prefs.edit().putString(KEY_LAST_DESTINATION, destination).apply()
    }
}
