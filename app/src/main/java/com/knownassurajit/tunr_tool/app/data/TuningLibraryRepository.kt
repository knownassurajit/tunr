package com.knownassurajit.tunr_tool.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.knownassurajit.tunr_tool.app.model.CustomTuning
import com.knownassurajit.tunr_tool.app.model.LibraryOps
import com.knownassurajit.tunr_tool.app.model.LibraryState
import com.knownassurajit.tunr_tool.app.model.TuningExchange
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.UUID

/** Minimal blob storage so the repository is testable without Android. */
interface LibraryStorage {
    suspend fun read(): String?
    suspend fun write(text: String)
}

private val Context.libraryDataStore by preferencesDataStore(name = "tuning_library")

/** Tuning library persisted in a Preferences DataStore (one JSON document under one key). */
class DataStoreLibraryStorage(context: Context) : LibraryStorage {
    private val store = context.applicationContext.libraryDataStore
    private val key = stringPreferencesKey("library_v1")
    override suspend fun read(): String? = store.data.map { it[key] }.first()
    override suspend fun write(text: String) { store.edit { it[key] = text } }
}

/**
 * Single source of truth for custom tunings, favorites and recents. All edits are pure
 * [LibraryOps] transforms applied atomically to [state]; writes are serialised and coalesced so the
 * UI thread never waits on disk.
 */
class TuningLibraryRepository(
    private val storage: LibraryStorage,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val newId: () -> String = { "custom." + UUID.randomUUID().toString().take(8) },
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; encodeDefaults = true }
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val writeLock = Mutex()
    private var pendingWrite: Job? = null

    private val queuedBeforeLoad = ArrayList<(LibraryState) -> LibraryState>()

    /** Loads persisted state. A corrupt document is ignored rather than crashing the app. */
    suspend fun load() {
        val text = runCatching { storage.read() }.getOrNull()
        val parsed = text?.let { runCatching { json.decodeFromString(LibraryState.serializer(), it) }.getOrNull() }
        val hadQueued: Boolean
        synchronized(queuedBeforeLoad) {
            var s = parsed ?: LibraryState()
            hadQueued = queuedBeforeLoad.isNotEmpty()
            queuedBeforeLoad.forEach { s = it(s) }
            queuedBeforeLoad.clear()
            _loaded.value = true // before state: collectors that see the loaded state must see loaded = true
            _state.value = s
        }
        if (hadQueued) schedulePersist()
    }

    fun newCustomId(): String = newId()

    /** Edits made before [load] finishes are replayed on top of the persisted state, never lost. */
    fun edit(transform: (LibraryState) -> LibraryState) {
        val queued = synchronized(queuedBeforeLoad) {
            if (!_loaded.value) { queuedBeforeLoad += transform; true } else { _state.update(transform); false }
        }
        if (!queued) schedulePersist()
    }

    fun upsert(tuning: CustomTuning) = edit { LibraryOps.upsert(it, tuning) }
    fun duplicate(id: String) = edit { LibraryOps.duplicate(it, id, newId()) }
    fun delete(id: String) = edit { LibraryOps.delete(it, id) }
    fun move(id: String, delta: Int) = edit { LibraryOps.move(it, id, delta) }
    fun toggleFavorite(id: String) = edit { LibraryOps.toggleFavorite(it, id) }
    fun touchRecent(id: String) = edit { LibraryOps.touchRecent(it, id) }

    fun exportJson(): String = TuningExchange.export(_state.value.custom)

    /** @return number imported and number skipped. Throws IllegalArgumentException for foreign files. */
    fun importJson(text: String): Pair<Int, Int> {
        val result = TuningExchange.import(text, _state.value.custom.map { it.name }, newId)
        if (result.tunings.isNotEmpty()) {
            edit { s -> result.tunings.fold(s) { acc, t -> LibraryOps.upsert(acc, t) } }
        }
        return result.tunings.size to result.skipped
    }

    private fun schedulePersist() {
        pendingWrite?.cancel()
        pendingWrite = scope.launch(io) {
            writeLock.withLock {
                val text = json.encodeToString(LibraryState.serializer(), _state.value)
                runCatching { storage.write(text) }
            }
        }
    }

    /** Test hook: waits for the last scheduled write. */
    suspend fun flush() { pendingWrite?.join() }
}
