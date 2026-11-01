package com.knownassurajit.tunr_tool.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knownassurajit.tunr_tool.app.audio.AudioEngine
import com.knownassurajit.tunr_tool.app.audio.MetronomeEngine
import com.knownassurajit.tunr_tool.app.audio.PitchDetector
import com.knownassurajit.tunr_tool.app.data.TunrPreferences
import com.knownassurajit.tunr_tool.app.model.BeatAccent
import com.knownassurajit.tunr_tool.app.model.MetronomeState
import com.knownassurajit.tunr_tool.app.model.MusicalNote
import com.knownassurajit.tunr_tool.app.model.NotationPreference
import com.knownassurajit.tunr_tool.app.model.PitchReading
import com.knownassurajit.tunr_tool.app.model.SettingsState
import com.knownassurajit.tunr_tool.app.model.TimeSignature
import com.knownassurajit.tunr_tool.app.model.TunerState
import com.knownassurajit.tunr_tool.app.model.Tuning
import com.knownassurajit.tunr_tool.app.util.AppIconManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt

private val NOTE_NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B")

/**
 * UI-state holder with full preference persistence across process lifecycles.
 * Manages real-time audio analysis, metronome synthesis, and ensures user settings
 * and configurations are preserved exactly as configured.
 */
class TunrViewModel @JvmOverloads constructor(
    application: Application,
    private val preferences: TunrPreferences = TunrPreferences(application)
) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine()
    private val pitchDetector = PitchDetector()
    private val metronomeEngine = MetronomeEngine()

    // Load persisted settings on startup
    private val initialSettings = preferences.loadSettings()
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    // Load persisted tuner state on startup
    private val _tuner = MutableStateFlow(
        TunerState(
            tuning = preferences.loadTuning(),
            autoDetect = preferences.loadAutoDetect()
        )
    )
    val tuner: StateFlow<TunerState> = _tuner.asStateFlow()

    // Load persisted metronome state on startup
    private val initialBpm = preferences.loadBpm()
    private val initialSignature = preferences.loadSignature()
    private val initialAccents = preferences.loadAccents(initialSignature.beats)
    private val _metronome = MutableStateFlow(
        MetronomeState(
            bpm = initialBpm,
            signature = initialSignature,
            accents = initialAccents
        )
    )
    val metronome: StateFlow<MetronomeState> = _metronome.asStateFlow()

    init {
        // Sync hardware / engine parameters with persisted settings
        metronomeEngine.sound = initialSettings.metronomeSound
        metronomeEngine.volume = initialSettings.metronomeVolume
        metronomeEngine.bpm = initialBpm
        metronomeEngine.beatsPerMeasure = initialSignature.beats
        metronomeEngine.accents = initialAccents

        // Collect real-time audio buffers from AudioEngine
        viewModelScope.launch(Dispatchers.Default) {
            audioEngine.audioBufferFlow.collect { buffer ->
                processAudioBuffer(buffer)
            }
        }

        // Collect metronome beat updates
        viewModelScope.launch {
            metronomeEngine.currentBeat.collect { beat ->
                _metronome.update { it.copy(beat = beat) }
            }
        }

        // Collect metronome play/pause state
        viewModelScope.launch {
            metronomeEngine.isPlaying.collect { playing ->
                _metronome.update { it.copy(playing = playing) }
            }
        }
        // Sync dynamic launcher icon with user preference
        AppIconManager.updateLauncherIcon(
            application,
            initialSettings.themeScheme,
            initialSettings.customThemeHex
        )
    }

    // ---- Audio Lifecycle -------------------------------------------------

    // Anti-jitter frequency and cent deviation smoothing states
    private var smoothedCents: Float? = null
    private var smoothedHz: Float? = null
    private var lastMatchedTarget: String? = null

    fun startListening() {
        audioEngine.start {
            // Permission callback if needed
        }
    }

    fun stopListening() {
        audioEngine.stop()
        resetSmoothing()
        _tuner.update { it.copy(reading = null) }
    }

    private fun resetSmoothing() {
        smoothedCents = null
        smoothedHz = null
        lastMatchedTarget = null
    }

    private fun processAudioBuffer(buffer: ShortArray) {
        val currentSettings = _settings.value
        val noiseThreshold = 0.003f + (currentSettings.noiseGate * 0.037f)
        val wasVoiced = _tuner.value.reading != null

        val result = pitchDetector.detectPitch(
            audioBuffer = buffer,
            bufferLength = buffer.size,
            noiseGateThreshold = noiseThreshold,
            harmonicFilter = currentSettings.harmonicFilter,
            subsonicFilter = currentSettings.subsonicFilter,
            wasVoiced = wasVoiced
        )

        if (!result.isVoiced || result.frequencyHz <= 0f) {
            resetSmoothing()
            onPitch(null)
            return
        }

        val freq = result.frequencyHz
        val a4 = currentSettings.a4Hz
        val currentTuner = _tuner.value
        val strings = currentTuner.tuning.strings

        if (strings.isEmpty()) {
            // Chromatic free-pitch estimation
            val continuousMidi = 69.0 + 12.0 * log2((freq / a4).toDouble())
            val nearestMidi = continuousMidi.roundToInt()
            val noteIndex = ((nearestMidi % 12) + 12) % 12
            val octave = (nearestMidi / 12) - 1
            val targetHz = (a4 * 2.0.pow((nearestMidi - 69.0) / 12.0)).toFloat()
            val rawCents = (1200.0 * log2((freq / targetHz).toDouble())).toFloat()

            val noteName = if (currentSettings.notation == NotationPreference.Flats) {
                MusicalNote.NOTE_NAMES_FLAT[noteIndex]
            } else {
                MusicalNote.NOTE_NAMES_SHARP[noteIndex]
            }

            val targetId = "$noteName$octave"
            val (cents, displayHz) = applySmoothing(rawCents, freq, targetId, currentSettings.sensitivity)

            val reading = PitchReading(
                note = noteName,
                octave = octave,
                detectedHz = displayHz,
                targetHz = targetHz,
                cents = cents,
            )
            onPitch(reading, null)
        } else {
            // Target specific string or auto-detect closest string in instrument preset
            val targetString = if (!currentTuner.autoDetect && currentTuner.activeString != null) {
                strings.firstOrNull { it.number == currentTuner.activeString } ?: strings.first()
            } else {
                strings.minByOrNull { abs(it.hz(a4) - freq) } ?: strings.first()
            }

            val targetHz = targetString.hz(a4)
            val rawCents = (1200.0 * log2((freq / targetHz).toDouble())).toFloat()
            val targetId = "string_${targetString.number}"
            val (cents, displayHz) = applySmoothing(rawCents, freq, targetId, currentSettings.sensitivity)

            val reading = PitchReading(
                note = targetString.note,
                octave = targetString.octave,
                detectedHz = displayHz,
                targetHz = targetHz,
                cents = cents,
            )
            onPitch(reading, targetString.number)
        }
    }

    private fun applySmoothing(
        rawCents: Float,
        rawHz: Float,
        targetId: String,
        sensitivity: com.knownassurajit.tunr_tool.app.model.TunerSensitivity
    ): Pair<Float, Float> {
        val prevCents = smoothedCents
        val prevHz = smoothedHz

        return if (lastMatchedTarget == targetId && prevCents != null && prevHz != null) {
            val alpha = sensitivity.smoothingFactor
            val deltaCents = rawCents - prevCents
            val effectiveDelta = if (abs(deltaCents) < sensitivity.deadbandCents) {
                deltaCents * 0.15f
            } else {
                deltaCents
            }
            val smoothedC = prevCents + effectiveDelta * alpha
            val smoothedH = prevHz + (rawHz - prevHz) * alpha
            smoothedCents = smoothedC
            smoothedHz = smoothedH
            Pair(smoothedC, smoothedH)
        } else {
            // Instantaneous response on string switch / first frame
            lastMatchedTarget = targetId
            smoothedCents = rawCents
            smoothedHz = rawHz
            Pair(rawCents, rawHz)
        }
    }

    // ---- Tuner -----------------------------------------------------------

    fun selectTuning(tuning: Tuning) {
        _tuner.update { TunerState(tuning = tuning, autoDetect = it.autoDetect) }
        preferences.saveTuning(tuning)
    }

    fun setAutoDetect(on: Boolean) {
        _tuner.update { it.copy(autoDetect = on, activeString = if (on) null else it.activeString) }
        preferences.saveAutoDetect(on)
    }

    fun selectString(number: Int) =
        _tuner.update { it.copy(autoDetect = false, activeString = number) }

    /** Call from pitch detector. [matchedString] is the closest string (null for chromatic / no match). */
    fun onPitch(reading: PitchReading?, matchedString: Int? = null) = _tuner.update { s ->
        val active = if (s.autoDetect) matchedString ?: s.activeString else s.activeString
        val locked = reading != null && abs(reading.cents) <= _settings.value.precision.lockCents
        s.copy(
            reading = reading,
            activeString = active,
            tunedStrings = if (locked && active != null) s.tunedStrings + active else s.tunedStrings,
        )
    }

    // ---- Settings --------------------------------------------------------

    fun updateSettings(settings: SettingsState) {
        _settings.value = settings
        metronomeEngine.sound = settings.metronomeSound
        metronomeEngine.volume = settings.metronomeVolume
        preferences.saveSettings(settings)

        // Update launcher icon to match the chosen theme or closest palette
        AppIconManager.updateLauncherIcon(
            getApplication(),
            settings.themeScheme,
            settings.customThemeHex
        )
    }

    // ---- Metronome -------------------------------------------------------

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(30, 240)
        metronomeEngine.bpm = clamped
        _metronome.update { it.copy(bpm = clamped) }
        preferences.saveBpm(clamped)
    }

    fun nudgeBpm(delta: Int) = setBpm(_metronome.value.bpm + delta)

    fun setSignature(signature: TimeSignature) {
        if (_metronome.value.signature == signature) return
        val savedAccents = preferences.loadAccents(signature.beats)
        metronomeEngine.beatsPerMeasure = signature.beats
        metronomeEngine.accents = savedAccents
        _metronome.update { it.copy(signature = signature, beat = 0, accents = savedAccents) }
        preferences.saveSignature(signature)
    }

    fun toggleBeatAccent(index: Int) {
        _metronome.update { current ->
            val list = current.accents.toMutableList()
            if (index in list.indices) {
                val next = when (list[index]) {
                    BeatAccent.ACCENT -> BeatAccent.NORMAL
                    BeatAccent.NORMAL -> BeatAccent.MUTE
                    BeatAccent.MUTE -> BeatAccent.ACCENT
                }
                list[index] = next
                metronomeEngine.accents = list
                preferences.saveAccents(current.signature.beats, list)
                current.copy(accents = list)
            } else current
        }
    }

    fun setPlaying(on: Boolean) {
        if (on) {
            metronomeEngine.bpm = _metronome.value.bpm
            metronomeEngine.beatsPerMeasure = _metronome.value.signature.beats
            metronomeEngine.accents = _metronome.value.accents
            metronomeEngine.start()
        } else {
            metronomeEngine.stop()
        }
        _metronome.update { it.copy(playing = on, beat = 0) }
    }

    private val taps = ArrayDeque<Long>()

    fun tapTempo(nowMs: Long = SystemClock.elapsedRealtime()) {
        if (taps.isNotEmpty() && nowMs - taps.last() > 2_000) taps.clear()
        taps.addLast(nowMs)
        while (taps.size > 5) taps.removeFirst()
        if (taps.size >= 2) {
            val avgMs = (taps.last() - taps.first()).toFloat() / (taps.size - 1)
            setBpm((60_000f / avgMs).roundToInt())
        }
    }

    // ---- Navigation Destination -------------------------------------------

    fun onDestinationSelected(destName: String, micGranted: Boolean) {
        preferences.saveLastDestination(destName)
        if (destName == "tuner") {
            // When moving from metronome to tuner: metronome playback must stop
            setPlaying(false)
            if (micGranted) {
                startListening()
            }
        } else {
            // When moving from tuner to metronome: microphone pitch detection must stop
            stopListening()
        }
    }

    fun setDestination(destName: String) {
        preferences.saveLastDestination(destName)
    }

    fun getLastDestination(): String = preferences.loadLastDestination()

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        metronomeEngine.release()
    }
}
