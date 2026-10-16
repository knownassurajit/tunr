package com.knownassurajit.tunr_tool.app.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knownassurajit.tunr_tool.app.audio.AudioEngine
import com.knownassurajit.tunr_tool.app.audio.MetronomeEngine
import com.knownassurajit.tunr_tool.app.audio.PitchDetector
import com.knownassurajit.tunr_tool.app.model.InstrumentString
import com.knownassurajit.tunr_tool.app.model.MusicalNote
import com.knownassurajit.tunr_tool.app.model.TuningMode
import com.knownassurajit.tunr_tool.app.model.TuningPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class NavigationTab {
    TUNER,
    METRONOME
}

data class TunerUiState(
    val currentNote: MusicalNote = MusicalNote("--", 0, 0f, 0),
    val detectedFrequencyHz: Float = 0f,
    val targetFrequencyHz: Float = 0f,
    val centsDeviation: Float = 0f,
    val isVoiced: Boolean = false,
    val rms: Float = 0f,
    val currentPreset: TuningPreset = TuningPreset.GUITAR_STANDARD,
    val activeStringNumber: Int? = null,
    val lockedStringNumbers: Set<Int> = emptySet(),
    val isManualLockMode: Boolean = false,
    val tuningMode: TuningMode = TuningMode.DEFAULT,
    val a4CalibrationHz: Float = 440f,
    val noiseGateThreshold: Float = 0.012f,
    val hapticFeedbackEnabled: Boolean = true,
    val activeTab: NavigationTab = NavigationTab.TUNER,
    val metronomeBpm: Int = 120,
    val metronomeBeats: Int = 4,
    val isMetronomePlaying: Boolean = false,
    val metronomeCurrentBeat: Int = 0
)

class TunerViewModel(application: Application) : AndroidViewModel(application) {
    private val audioEngine = AudioEngine()
    private val pitchDetector = PitchDetector()
    private val metronomeEngine = MetronomeEngine()

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private var wasInTune = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    init {
        // Collect real-time audio buffers from AudioEngine
        viewModelScope.launch(Dispatchers.Default) {
            audioEngine.audioBufferFlow.collect { buffer ->
                processAudioBuffer(buffer)
            }
        }

        // Collect metronome state
        viewModelScope.launch {
            metronomeEngine.isPlaying.collect { playing ->
                _uiState.update { it.copy(isMetronomePlaying = playing) }
            }
        }
        viewModelScope.launch {
            metronomeEngine.currentBeat.collect { beat ->
                _uiState.update { it.copy(metronomeCurrentBeat = beat) }
            }
        }
    }

    fun startListening(onPermissionRequired: () -> Unit) {
        audioEngine.start(onPermissionRequired)
    }

    fun stopListening() {
        audioEngine.stop()
        _uiState.update {
            it.copy(
                isVoiced = false,
                detectedFrequencyHz = 0f,
                centsDeviation = 0f
            )
        }
    }

    private fun processAudioBuffer(buffer: ShortArray) {
        val state = _uiState.value
        val result = pitchDetector.detectPitch(
            audioBuffer = buffer,
            bufferLength = buffer.size,
            noiseGateThreshold = state.noiseGateThreshold
        )

        if (!result.isVoiced || result.frequencyHz <= 0f) {
            _uiState.update {
                it.copy(
                    isVoiced = false,
                    rms = result.rms
                )
            }
            wasInTune = false
            return
        }

        val freq = result.frequencyHz
        val calibration = state.a4CalibrationHz
        val preset = state.currentPreset

        // Determine target note and target frequency
        val (targetNote, targetFreq, detectedStringNum) = if (state.isManualLockMode && state.activeStringNumber != null) {
            // Manual lock: target the specifically chosen string
            val chosenString = preset.strings.firstOrNull { it.stringNumber == state.activeStringNumber }
            if (chosenString != null) {
                val target = chosenString.getCalibratedFrequency(calibration)
                val note = MusicalNote.fromFrequency(target, calibration)
                Triple(note, target, chosenString.stringNumber)
            } else {
                val note = MusicalNote.fromFrequency(freq, calibration)
                Triple(note, note.targetFrequencyHz, null)
            }
        } else if (!preset.isChromatic && preset.strings.isNotEmpty()) {
            // Find closest matching string in current instrument tuning
            val closestString = preset.strings.minByOrNull { s ->
                abs(s.getCalibratedFrequency(calibration) - freq)
            } ?: preset.strings.first()

            val target = closestString.getCalibratedFrequency(calibration)
            val note = MusicalNote.fromFrequency(target, calibration)
            Triple(note, target, closestString.stringNumber)
        } else {
            // Chromatic free-tuning mode
            val note = MusicalNote.fromFrequency(freq, calibration)
            Triple(note, note.targetFrequencyHz, null)
        }

        val cents = MusicalNote.calculateCents(freq, targetFreq)
        val isInTune = abs(cents) <= state.tuningMode.inTuneThresholdCents

        // Trigger tactile haptics on lock entry
        if (isInTune && !wasInTune && state.hapticFeedbackEnabled) {
            triggerHapticLock()
        }
        wasInTune = isInTune

        // Update locked string tracking
        val updatedLocked = state.lockedStringNumbers.toMutableSet()
        if (isInTune && detectedStringNum != null) {
            updatedLocked.add(detectedStringNum)
        }

        _uiState.update {
            it.copy(
                currentNote = targetNote,
                detectedFrequencyHz = freq,
                targetFrequencyHz = targetFreq,
                centsDeviation = cents,
                isVoiced = true,
                rms = result.rms,
                activeStringNumber = detectedStringNum ?: it.activeStringNumber,
                lockedStringNumbers = updatedLocked
            )
        }
    }

    private fun triggerHapticLock() {
        try {
            vibrator?.let { vib ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE)
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(25)
                }
            }
        } catch (_: Exception) {}
    }

    fun selectPreset(preset: TuningPreset) {
        _uiState.update {
            it.copy(
                currentPreset = preset,
                activeStringNumber = null,
                isManualLockMode = false,
                lockedStringNumbers = emptySet()
            )
        }
    }

    fun selectString(stringItem: InstrumentString?) {
        _uiState.update {
            if (stringItem == null) {
                it.copy(activeStringNumber = null, isManualLockMode = false)
            } else {
                it.copy(activeStringNumber = stringItem.stringNumber, isManualLockMode = true)
            }
        }
    }

    fun setTuningMode(mode: TuningMode) {
        _uiState.update { it.copy(tuningMode = mode) }
    }

    fun setA4Calibration(calibrationHz: Float) {
        _uiState.update { it.copy(a4CalibrationHz = calibrationHz.coerceIn(432f, 446f)) }
    }

    fun setNoiseGate(threshold: Float) {
        _uiState.update { it.copy(noiseGateThreshold = threshold.coerceIn(0.005f, 0.05f)) }
    }

    fun toggleHapticFeedback(enabled: Boolean) {
        _uiState.update { it.copy(hapticFeedbackEnabled = enabled) }
    }

    fun setActiveTab(tab: NavigationTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    // Metronome Controls
    fun setMetronomeBpm(bpm: Int) {
        val clamped = bpm.coerceIn(30, 300)
        metronomeEngine.bpm = clamped
        _uiState.update { it.copy(metronomeBpm = clamped) }
    }

    fun setMetronomeBeats(beats: Int) {
        val clamped = beats.coerceIn(1, 12)
        metronomeEngine.beatsPerMeasure = clamped
        _uiState.update { it.copy(metronomeBeats = clamped) }
    }

    fun toggleMetronome() {
        if (_uiState.value.isMetronomePlaying) {
            metronomeEngine.stop()
        } else {
            metronomeEngine.bpm = _uiState.value.metronomeBpm
            metronomeEngine.beatsPerMeasure = _uiState.value.metronomeBeats
            metronomeEngine.start()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        metronomeEngine.release()
    }
}
