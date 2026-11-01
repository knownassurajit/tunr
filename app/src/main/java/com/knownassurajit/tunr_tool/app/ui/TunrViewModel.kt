package com.knownassurajit.tunr_tool.app.ui

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.knownassurajit.tunr_tool.app.audio.AudioEngine
import com.knownassurajit.tunr_tool.app.audio.AudioSink
import com.knownassurajit.tunr_tool.app.audio.MetronomeEngine
import com.knownassurajit.tunr_tool.app.audio.PitchDetector
import com.knownassurajit.tunr_tool.app.audio.TapTempo
import com.knownassurajit.tunr_tool.app.audio.TunerFrame
import com.knownassurajit.tunr_tool.app.audio.TunerPipeline
import com.knownassurajit.tunr_tool.app.data.DataStoreLibraryStorage
import com.knownassurajit.tunr_tool.app.data.TunrPreferences
import com.knownassurajit.tunr_tool.app.data.TuningLibraryRepository
import com.knownassurajit.tunr_tool.app.model.BeatAccent
import com.knownassurajit.tunr_tool.app.model.CustomTuning
import com.knownassurajit.tunr_tool.app.model.LibraryState
import com.knownassurajit.tunr_tool.app.model.MetronomeState
import com.knownassurajit.tunr_tool.app.model.NotationPreference
import com.knownassurajit.tunr_tool.app.model.SettingsState
import com.knownassurajit.tunr_tool.app.model.TimeSignature
import com.knownassurajit.tunr_tool.app.model.TunerState
import com.knownassurajit.tunr_tool.app.model.Tuning
import com.knownassurajit.tunr_tool.app.model.TuningCatalog
import com.knownassurajit.tunr_tool.app.model.Tunings
import com.knownassurajit.tunr_tool.app.util.AppIconManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the tuning-library sheet renders, resolved to [Tuning]s once per change. */
@Immutable
data class LibraryUi(
    val catalog: TuningCatalog,
    val custom: List<Tuning>,
    val favorites: List<Tuning>,
    val recents: List<Tuning>,
    val favoriteIds: Set<String>,
)

sealed interface ImportOutcome {
    data class Done(val imported: Int, val skipped: Int) : ImportOutcome
    data class Failed(val reason: String) : ImportOutcome
}

/**
 * UI-state holder. DSP runs on the dedicated audio thread (see [AudioEngine]); this class only
 * wires engines to immutable StateFlows and persists preferences.
 */
class TunrViewModel @JvmOverloads constructor(
    application: Application,
    private val preferences: TunrPreferences = TunrPreferences(application),
) : AndroidViewModel(application) {

    private val audioEngine = AudioEngine(application)
    private val detector = PitchDetector()
    private val pipeline = TunerPipeline()
    private val metronomeEngine = MetronomeEngine()
    private val tapTempo = TapTempo()

    // ---- Catalog + library ---------------------------------------------------------------
    val catalog: TuningCatalog = runCatching {
        application.assets.open("tuning_catalog.json").bufferedReader().use { TuningCatalog.parse(it.readText()) }
    }.getOrElse {
        Log.e(TAG, "tuning catalog unreadable, using built-in fallback", it)
        TuningCatalog.Fallback
    }

    private val repository = TuningLibraryRepository(DataStoreLibraryStorage(application), viewModelScope)

    val library: StateFlow<LibraryUi> = repository.state
        .map { buildLibraryUi(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, buildLibraryUi(LibraryState()))

    // ---- Settings ----------------------------------------------------------------------------
    private val initialSettings = preferences.loadSettings()
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    // ---- Tuner -------------------------------------------------------------------------------
    private val savedTuningId = preferences.loadTuningId()
    /** A saved custom-tuning id can only be resolved once DataStore has loaded. */
    private var pendingCustomId: String? =
        savedTuningId.takeIf { resolveCatalogTuning(it) == null && it != Tunings.STANDARD_ID }
    private val _tuner = MutableStateFlow(
        TunerState(
            tuning = resolveCatalogTuning(savedTuningId) ?: Tunings.Standard,
            autoDetect = preferences.loadAutoDetect(),
        ),
    )
    val tuner: StateFlow<TunerState> = _tuner.asStateFlow()

    // Narrow projections: the app shell must not recompose at the tuner's ~21 Hz frame rate.
    val activeTuning: StateFlow<Tuning> = _tuner.map { it.tuning }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, _tuner.value.tuning)
    val hearingPitch: StateFlow<Boolean> = _tuner.map { it.reading != null }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // ---- Metronome ---------------------------------------------------------------------------
    private val initialBpm = preferences.loadBpm()
    private val initialSignature = preferences.loadSignature()
    private val initialAccents = preferences.loadAccents(initialSignature.beats)
    private val initialSubdivision = preferences.loadSubdivision()
    private val _metronome = MutableStateFlow(
        MetronomeState(
            bpm = initialBpm,
            signature = initialSignature,
            accents = initialAccents,
            subdivision = initialSubdivision,
        ),
    )
    val metronome: StateFlow<MetronomeState> = _metronome.asStateFlow()
    val metronomePlaying: StateFlow<Boolean> = _metronome.map { it.playing }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val audioSink = AudioSink { buffer, count -> onAudio(buffer, count) }

    init {
        metronomeEngine.sound = initialSettings.metronomeSound
        metronomeEngine.volume = initialSettings.metronomeVolume
        metronomeEngine.bpm = initialBpm
        metronomeEngine.beatsPerMeasure = initialSignature.beats
        metronomeEngine.accents = initialAccents
        metronomeEngine.subdivision = initialSubdivision

        viewModelScope.launch { metronomeEngine.currentBeat.collect { b -> _metronome.update { it.copy(beat = b.beat, beatSerial = b.serial) } } }
        viewModelScope.launch { metronomeEngine.isPlaying.collect { p -> _metronome.update { it.copy(playing = p) } } }

        // Load persisted custom tunings, then keep the active tuning in sync with edits/deletes.
        viewModelScope.launch(Dispatchers.Default) { repository.load() }
        viewModelScope.launch {
            repository.state.collect { state ->
                pendingCustomId?.let { id ->
                    state.custom.firstOrNull { it.id == id }
                        ?.let { c -> runCatching { c.toTuning() }.getOrNull() }
                        ?.let(::replaceTuning)
                    if (repository.loaded.value) pendingCustomId = null
                }
                val current = _tuner.value.tuning
                if (current.isCustom) {
                    val updated = state.custom.firstOrNull { it.id == current.id }
                        ?.let { c -> runCatching { c.toTuning() }.getOrNull() }
                    when {
                        updated != null && updated != current -> replaceTuning(updated)
                        updated == null && repository.loaded.value -> replaceTuning(Tunings.Standard)
                    }
                }
            }
        }

        AppIconManager.updateLauncherIcon(application, initialSettings.themeScheme, initialSettings.customThemeHex)
    }

    // ---- Audio lifecycle -----------------------------------------------------------------

    fun startListening() {
        if (audioEngine.isRecording) return // detector/pipeline state belongs to the audio thread while it runs
        detector.reset()
        pipeline.reset()
        audioEngine.start(audioSink) { Log.w(TAG, "microphone unavailable") }
    }

    fun stopListening() {
        audioEngine.stop()
        pipeline.reset()
        _tuner.update { it.copy(reading = null, locked = false) }
    }

    /** Audio thread. Allocation-free until a frame completes; then one small immutable state update (~21/s). */
    private fun onAudio(buffer: ShortArray, count: Int) {
        val s = _settings.value
        detector.noiseGate = 0.003f + s.noiseGate * 0.037f
        detector.harmonicFilter = s.harmonicFilter
        detector.subsonicFilter = s.subsonicFilter
        if (!detector.push(buffer, count)) return

        val est = detector.estimate
        val t = _tuner.value
        if (!est.voiced && t.reading == null) return
        val frame = pipeline.update(
            voiced = est.voiced,
            frequencyHz = est.frequencyHz,
            tuning = t.tuning,
            autoDetect = t.autoDetect,
            activeString = t.activeString,
            a4Hz = s.a4Hz,
            flats = s.notation == NotationPreference.Flats,
            sensitivity = s.sensitivity,
            lockCents = s.precision.lockCents,
        )
        publish(frame)
    }

    private fun publish(frame: TunerFrame) = _tuner.update { s ->
        val active = if (s.autoDetect) frame.matchedString ?: s.activeString else s.activeString
        s.copy(
            reading = frame.reading,
            activeString = active,
            locked = frame.locked,
            tunedStrings = if (frame.locked && active != null && active !in s.tunedStrings) s.tunedStrings + active else s.tunedStrings,
        )
    }

    // ---- Tuner actions ---------------------------------------------------------------------

    fun selectTuning(tuning: Tuning) {
        replaceTuning(tuning)
        preferences.saveTuningId(tuning.id)
        repository.touchRecent(tuning.id)
    }

    private fun replaceTuning(tuning: Tuning) {
        pipeline.reset()
        _tuner.update { TunerState(tuning = tuning, autoDetect = it.autoDetect) }
    }

    fun setAutoDetect(on: Boolean) {
        _tuner.update { it.copy(autoDetect = on, activeString = if (on) null else it.activeString) }
        preferences.saveAutoDetect(on)
        pipeline.reset()
    }

    fun selectString(number: Int) {
        _tuner.update { it.copy(autoDetect = false, activeString = number) }
        pipeline.reset()
    }

    // ---- Tuning library actions --------------------------------------------------------------

    fun newCustomId(): String = repository.newCustomId()
    fun saveCustomTuning(tuning: CustomTuning) = repository.upsert(tuning)
    fun duplicateCustomTuning(id: String) = repository.duplicate(id)
    fun deleteCustomTuning(id: String) = repository.delete(id)
    fun moveCustomTuning(id: String, delta: Int) = repository.move(id, delta)
    fun toggleFavorite(id: String) = repository.toggleFavorite(id)
    fun customTuning(id: String): CustomTuning? = repository.state.value.custom.firstOrNull { it.id == id }
    fun customNames(exceptId: String? = null): List<String> =
        repository.state.value.custom.filter { it.id != exceptId }.map { it.name }

    fun exportCustomTunings(): String = repository.exportJson()

    fun importCustomTunings(text: String): ImportOutcome = try {
        val (imported, skipped) = repository.importJson(text)
        ImportOutcome.Done(imported, skipped)
    } catch (e: IllegalArgumentException) {
        ImportOutcome.Failed(e.message ?: "Could not read file")
    }

    private fun resolveCatalogTuning(id: String): Tuning? =
        catalog.find(id) ?: TuningCatalog.LegacyIds[id]?.let(catalog::find)

    private fun buildLibraryUi(state: LibraryState): LibraryUi {
        val custom = state.custom.mapNotNull { c -> runCatching { c.toTuning() }.getOrNull() }
        val byId = custom.associateBy { it.id }
        fun resolve(id: String): Tuning? = byId[id] ?: catalog.find(id)
        return LibraryUi(
            catalog = catalog,
            custom = custom,
            favorites = state.favorites.mapNotNull(::resolve),
            recents = state.recents.mapNotNull(::resolve),
            favoriteIds = state.favorites.toSet(),
        )
    }

    // ---- Settings --------------------------------------------------------------------------------

    fun updateSettings(settings: SettingsState) {
        _settings.value = settings
        metronomeEngine.sound = settings.metronomeSound
        metronomeEngine.volume = settings.metronomeVolume
        preferences.saveSettings(settings)
        AppIconManager.updateLauncherIcon(getApplication(), settings.themeScheme, settings.customThemeHex)
    }

    // ---- Metronome -------------------------------------------------------------------------------

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

    fun setSubdivision(subdivision: Int) {
        val n = subdivision.coerceIn(1, 4)
        metronomeEngine.subdivision = n
        _metronome.update { it.copy(subdivision = n) }
        preferences.saveSubdivision(n)
    }

    fun toggleBeatAccent(index: Int) {
        _metronome.update { current ->
            val list = current.accents.toMutableList()
            if (index in list.indices) {
                list[index] = when (list[index]) {
                    BeatAccent.ACCENT -> BeatAccent.NORMAL
                    BeatAccent.NORMAL -> BeatAccent.MUTE
                    BeatAccent.MUTE -> BeatAccent.ACCENT
                }
                metronomeEngine.accents = list
                preferences.saveAccents(current.signature.beats, list)
                current.copy(accents = list)
            } else current
        }
    }

    fun setPlaying(on: Boolean) {
        if (on) {
            val m = _metronome.value
            metronomeEngine.bpm = m.bpm
            metronomeEngine.beatsPerMeasure = m.signature.beats
            metronomeEngine.accents = m.accents
            metronomeEngine.subdivision = m.subdivision
            metronomeEngine.start()
        } else {
            metronomeEngine.stop()
        }
        _metronome.update { it.copy(playing = on, beat = 0) }
    }

    fun tapTempo(nowMs: Long = SystemClock.elapsedRealtime()) {
        tapTempo.tap(nowMs)?.let(::setBpm)
    }

    // ---- Navigation / lifecycle ---------------------------------------------------------------------

    fun onDestinationSelected(destName: String, micGranted: Boolean) {
        preferences.saveLastDestination(destName)
        if (destName == "tuner") {
            setPlaying(false) // tuner and metronome never run together
            if (micGranted) startListening()
        } else {
            stopListening()
        }
    }

    /** Release the microphone whenever the UI is not visible (privacy + battery). */
    fun onAppPaused() = stopListening()

    fun onAppResumed(onTuner: Boolean, micGranted: Boolean) {
        if (onTuner && micGranted) startListening()
    }

    fun setDestination(destName: String) = preferences.saveLastDestination(destName)

    fun getLastDestination(): String = preferences.loadLastDestination()

    override fun onCleared() {
        super.onCleared()
        audioEngine.stop()
        metronomeEngine.release()
    }

    private companion object { const val TAG = "TunrViewModel" }
}
