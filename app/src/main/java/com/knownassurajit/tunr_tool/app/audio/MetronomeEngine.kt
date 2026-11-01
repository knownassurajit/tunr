package com.knownassurajit.tunr_tool.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import com.knownassurajit.tunr_tool.app.model.BeatAccent
import com.knownassurajit.tunr_tool.app.model.MetronomeSound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Streams [MetronomeRenderer] output into a LOW_LATENCY AudioTrack from a dedicated
 * URGENT_AUDIO thread. No Handler/Timer/coroutine delay is involved in timing: the click spacing is
 * decided purely by frame arithmetic in the renderer, and the blocking write paces the thread.
 *
 * [currentBeat] is published when a click's frame passes AudioTrack.playbackHeadPosition, i.e. when
 * it is audible, so UI flashes and haptics line up with the sound instead of leading it by the
 * buffer length.
 */
class MetronomeEngine {
    private val sampleRate = 44100
    private val chunkFrames = 256 // 5.8 ms
    private val renderer = MetronomeRenderer(sampleRate)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** The most recently *heard* beat click (subdivision clicks do not change it). */
    class BeatTick(val beat: Int, val serial: Int)

    private val _currentBeat = MutableStateFlow(BeatTick(0, 0))
    val currentBeat: StateFlow<BeatTick> = _currentBeat.asStateFlow()
    private var serial = 0

    var bpm: Int
        get() = renderer.bpm
        set(v) { renderer.bpm = v }
    var beatsPerMeasure: Int
        get() = renderer.beatsPerMeasure
        set(v) { renderer.beatsPerMeasure = v }
    var subdivision: Int
        get() = renderer.subdivision
        set(v) { renderer.subdivision = v }
    var sound: MetronomeSound
        get() = renderer.sound
        set(v) { renderer.sound = v }
    var volume: Float
        get() = renderer.volume
        set(v) { renderer.volume = v }
    var accents: List<BeatAccent>
        get() = renderer.accents.toList()
        set(v) { renderer.accents = v.toTypedArray() }

    @Volatile private var running = false
    private var thread: Thread? = null

    @Synchronized
    fun start() {
        if (thread?.isAlive == true) return
        val track = createTrack() ?: return
        renderer.reset()
        running = true
        _isPlaying.value = true
        thread = Thread({ loop(track) }, "tunr-metronome").also { it.start() }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.let { try { it.join(300) } catch (_: InterruptedException) { Thread.currentThread().interrupt() } }
        thread = null
        _isPlaying.value = false
        _currentBeat.value = BeatTick(0, ++serial)
    }

    fun release() = stop()

    private fun loop(track: AudioTrack) {
        try { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) } catch (_: Exception) {}
        val chunk = ShortArray(chunkFrames)
        try {
            // Pre-roll two chunks so the first click is not delayed by an underrun.
            repeat(2) { renderer.render(chunk, chunkFrames); track.write(chunk, 0, chunkFrames) }
            track.play()
            while (running) {
                renderer.render(chunk, chunkFrames)
                val written = track.write(chunk, 0, chunkFrames, AudioTrack.WRITE_BLOCKING)
                if (written < 0) break
                // playbackHeadPosition is an unsigned 32-bit frame count; one track lives < 13 h.
                val head = track.playbackHeadPosition.toLong() and 0xFFFFFFFFL
                while (renderer.peekEventFrame() <= head) {
                    renderer.popEvent()
                    if (renderer.eventSub == 0) _currentBeat.value = BeatTick(renderer.eventBeat, ++serial)
                }
            }
        } catch (_: Exception) {
        } finally {
            running = false
            try { track.pause(); track.flush() } catch (_: Exception) {}
            track.release()
            _isPlaying.value = false
        }
    }

    private fun createTrack(): AudioTrack? = try {
        val minBytes = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val builder = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBytes * 2, chunkFrames * 2 * 6))
            .setTransferMode(AudioTrack.MODE_STREAM)
        // AAudio fast path (API 26+); older devices keep the default mixer path.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) builder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
        builder.build()
    } catch (_: Exception) {
        null
    }
}
