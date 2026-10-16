package com.knownassurajit.tunr_tool.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * High-precision synthesized metronome engine.
 * Generates audio clicks on the fly via PCM synthesis and AudioTrack.
 */
class MetronomeEngine {
    private val sampleRate = 44100
    private val scope = CoroutineScope(Dispatchers.Default)
    private var loopJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    @Volatile
    var bpm: Int = 120
        set(value) {
            field = value.coerceIn(30, 300)
        }

    @Volatile
    var beatsPerMeasure: Int = 4
        set(value) {
            field = value.coerceIn(1, 12)
        }

    // Pre-generate short, crisp acoustic synthesized clicks (downbeat and subbeat)
    private val highClick = generateClick(frequencyHz = 1600.0, durationMs = 20)
    private val lowClick = generateClick(frequencyHz = 900.0, durationMs = 15)

    private var audioTrack: AudioTrack? = null

    init {
        initAudioTrack()
    }

    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build().apply {
                play()
            }
    }

    private fun generateClick(frequencyHz: Double, durationMs: Int): ShortArray {
        val sampleCount = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(sampleCount)
        val decayRate = 5.0 / sampleCount // Exponential decay

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate
            val envelope = exp(-i * decayRate)
            val sample = sin(2.0 * PI * frequencyHz * t) * envelope
            buffer[i] = (sample * 28000).toInt().coerceIn(-32767, 32767).toShort()
        }
        return buffer
    }

    fun start() {
        if (_isPlaying.value) return
        _isPlaying.value = true

        loopJob = scope.launch {
            var beatIndex = 0
            while (isActive && _isPlaying.value) {
                _currentBeat.value = beatIndex

                val click = if (beatIndex == 0) highClick else lowClick
                audioTrack?.write(click, 0, click.size)

                val intervalMs = (60_000.0 / bpm).toLong()
                delay(intervalMs)

                beatIndex = (beatIndex + 1) % beatsPerMeasure
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        loopJob?.cancel()
        loopJob = null
        _currentBeat.value = 0
    }

    fun release() {
        stop()
        try {
            audioTrack?.apply {
                pause()
                flush()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }
}
