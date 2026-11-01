package com.knownassurajit.tunr_tool.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.knownassurajit.tunr_tool.app.model.BeatAccent
import com.knownassurajit.tunr_tool.app.model.MetronomeSound
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Sample-accurate, zero-jitter continuous streaming metronome engine.
 *
 * Uses continuous PCM stream synthesis directly into AudioTrack to eliminate
 * buffer timeouts, underruns, and digital DAC clicks/pops.
 *
 * Generates four high-fidelity acoustic / digital sound profiles:
 * - Woodblock: Authentic physical wood strike with impact chirp (2600Hz -> 1600Hz)
 * - Digital: Crisp, electronic micro-sine tick
 * - Rimshot: Snappy acoustic snare-rim percussion tap
 * - Clave: Resonant Latin wooden clave strike
 */
class MetronomeEngine {
    private val sampleRate = 44100
    private val chunkSize = 1024 // ~23.2ms per chunk for responsive streaming

    private val scope = CoroutineScope(Dispatchers.Default)
    private var streamJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentBeat = MutableStateFlow(0)
    val currentBeat: StateFlow<Int> = _currentBeat.asStateFlow()

    @Volatile
    var bpm: Int = 120
        set(value) {
            field = value.coerceIn(30, 240)
        }

    @Volatile
    var beatsPerMeasure: Int = 4
        set(value) {
            field = value.coerceIn(1, 16)
        }

    @Volatile
    var sound: MetronomeSound = MetronomeSound.Woodblock

    @Volatile
    var volume: Float = 0.9f
        set(value) {
            field = value.coerceIn(0.0f, 1.0f)
        }

    @Volatile
    var accents: List<BeatAccent> = listOf(
        BeatAccent.ACCENT,
        BeatAccent.NORMAL,
        BeatAccent.NORMAL,
        BeatAccent.NORMAL,
    )

    // Pre-synthesized sound profiles with zero DC offset
    private val woodblockDown = synthesizeWoodblockClick(isDownbeat = true)
    private val woodblockSub = synthesizeWoodblockClick(isDownbeat = false)

    private val digitalDown = synthesizeDigitalClick(isDownbeat = true)
    private val digitalSub = synthesizeDigitalClick(isDownbeat = false)

    private val rimshotDown = synthesizeRimshotClick(isDownbeat = true)
    private val rimshotSub = synthesizeRimshotClick(isDownbeat = false)

    private val claveDown = synthesizeClaveClick(isDownbeat = true)
    private val claveSub = synthesizeClaveClick(isDownbeat = false)

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
        val bufferSizeBytes = maxOf(minBufferSize * 4, chunkSize * 4 * 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSizeBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    /**
     * Synthesizes an authentic acoustic woodblock strike:
     * - Fast pitch sweep (chirp) for physical impact.
     * - Warm second harmonic for resonant wood tone.
     * - Micro-transient stick noise in the first 1ms.
     */
    private fun synthesizeWoodblockClick(isDownbeat: Boolean): ShortArray {
        val durationSec = if (isDownbeat) 0.035 else 0.024
        val sampleCount = (sampleRate * durationSec).toInt()

        val f0 = if (isDownbeat) 2600.0 else 1900.0
        val f1 = if (isDownbeat) 1600.0 else 1150.0
        val decayRate = if (isDownbeat) 120.0 else 170.0
        val attackSamples = (0.0006 * sampleRate).toInt()
        val tailTaperSamples = (0.004 * sampleRate).toInt()

        val random = Random(42)
        val raw = FloatArray(sampleCount)
        var phase = 0.0

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate
            val instFreq = f1 + (f0 - f1) * exp(-t / 0.003)
            phase += 2.0 * PI * instFreq / sampleRate

            val body = sin(phase) + 0.35 * sin(2.0 * phase)
            val noise = (random.nextFloat() * 2f - 1f) * exp(-t / 0.001)

            val envelope = when {
                i < attackSamples -> i.toFloat() / attackSamples
                else -> exp(-decayRate * (i - attackSamples) / sampleRate).toFloat()
            }

            var sampleVal = ((body + 0.45 * noise) * envelope).toFloat()

            if (i >= sampleCount - tailTaperSamples) {
                val taper = (sampleCount - 1 - i).toFloat() / tailTaperSamples
                sampleVal *= taper
            }

            raw[i] = sampleVal
        }

        var peak = 0.0001f
        for (v in raw) {
            val a = abs(v)
            if (a > peak) peak = a
        }

        val maxAmp = if (isDownbeat) 28000f else 21000f
        val result = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            result[i] = ((raw[i] / peak) * maxAmp).toInt().coerceIn(-32767, 32767).toShort()
        }
        return result
    }

    /**
     * Synthesizes a crisp electronic digital click (pure sine burst with ultra-fast decay).
     */
    private fun synthesizeDigitalClick(isDownbeat: Boolean): ShortArray {
        val durationSec = if (isDownbeat) 0.018 else 0.012
        val sampleCount = (sampleRate * durationSec).toInt()

        val freq = if (isDownbeat) 1800.0 else 1200.0
        val decayRate = if (isDownbeat) 190.0 else 260.0
        val attackSamples = (0.0003 * sampleRate).toInt()
        val tailTaperSamples = (0.002 * sampleRate).toInt()

        val raw = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate
            val phase = 2.0 * PI * freq * t
            val envelope = when {
                i < attackSamples -> i.toFloat() / attackSamples
                else -> exp(-decayRate * (i - attackSamples) / sampleRate).toFloat()
            }
            var sampleVal = (sin(phase) * envelope).toFloat()
            if (i >= sampleCount - tailTaperSamples) {
                val taper = (sampleCount - 1 - i).toFloat() / tailTaperSamples
                sampleVal *= taper
            }
            raw[i] = sampleVal
        }

        val maxAmp = if (isDownbeat) 27000f else 20000f
        val result = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            result[i] = (raw[i] * maxAmp).toInt().coerceIn(-32767, 32767).toShort()
        }
        return result
    }

    /**
     * Synthesizes a punchy drum rimshot / cross-stick tap.
     */
    private fun synthesizeRimshotClick(isDownbeat: Boolean): ShortArray {
        val durationSec = if (isDownbeat) 0.028 else 0.018
        val sampleCount = (sampleRate * durationSec).toInt()

        val f0 = if (isDownbeat) 3200.0 else 2400.0
        val decayRate = if (isDownbeat) 140.0 else 210.0
        val attackSamples = (0.0004 * sampleRate).toInt()
        val tailTaperSamples = (0.003 * sampleRate).toInt()

        val random = Random(99)
        val raw = FloatArray(sampleCount)

        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate
            val phase = 2.0 * PI * f0 * t
            val tone = sin(phase) + 0.5 * sin(2.2 * phase)
            val noise = (random.nextFloat() * 2f - 1f) * exp(-t / 0.002)

            val envelope = when {
                i < attackSamples -> i.toFloat() / attackSamples
                else -> exp(-decayRate * (i - attackSamples) / sampleRate).toFloat()
            }

            var sampleVal = ((tone * 0.7f + noise * 0.7f) * envelope).toFloat()
            if (i >= sampleCount - tailTaperSamples) {
                val taper = (sampleCount - 1 - i).toFloat() / tailTaperSamples
                sampleVal *= taper
            }
            raw[i] = sampleVal
        }

        var peak = 0.0001f
        for (v in raw) {
            val a = abs(v)
            if (a > peak) peak = a
        }

        val maxAmp = if (isDownbeat) 27000f else 20000f
        val result = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            result[i] = ((raw[i] / peak) * maxAmp).toInt().coerceIn(-32767, 32767).toShort()
        }
        return result
    }

    /**
     * Synthesizes a resonant wooden Latin clave strike.
     */
    private fun synthesizeClaveClick(isDownbeat: Boolean): ShortArray {
        val durationSec = if (isDownbeat) 0.032 else 0.022
        val sampleCount = (sampleRate * durationSec).toInt()

        val f0 = if (isDownbeat) 2350.0 else 1650.0
        val decayRate = if (isDownbeat) 110.0 else 160.0
        val attackSamples = (0.0004 * sampleRate).toInt()
        val tailTaperSamples = (0.003 * sampleRate).toInt()

        val raw = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / sampleRate
            val phase = 2.0 * PI * f0 * t
            val tone = sin(phase) + 0.25 * sin(3.0 * phase)
            val envelope = when {
                i < attackSamples -> i.toFloat() / attackSamples
                else -> exp(-decayRate * (i - attackSamples) / sampleRate).toFloat()
            }
            var sampleVal = (tone * envelope).toFloat()
            if (i >= sampleCount - tailTaperSamples) {
                val taper = (sampleCount - 1 - i).toFloat() / tailTaperSamples
                sampleVal *= taper
            }
            raw[i] = sampleVal
        }

        val maxAmp = if (isDownbeat) 28000f else 21000f
        val result = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            result[i] = (raw[i] * maxAmp).toInt().coerceIn(-32767, 32767).toShort()
        }
        return result
    }

    fun start() {
        if (_isPlaying.value) return
        _isPlaying.value = true

        streamJob = scope.launch {
            try {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            } catch (_: Exception) {}

            try {
                if (audioTrack?.state != AudioTrack.STATE_INITIALIZED) {
                    initAudioTrack()
                }
                audioTrack?.play()
            } catch (e: Exception) {
                _isPlaying.value = false
                return@launch
            }

            val buffer = ShortArray(chunkSize)
            var activeClick: ShortArray? = null
            var activeClickOffset = 0
            var samplesUntilNextBeat = 0
            var beatIndex = 0

            while (isActive && _isPlaying.value) {
                val currentBpm = bpm
                val currentSignature = beatsPerMeasure
                val currentSound = sound
                val currentVol = volume
                val samplesPerBeat = ((sampleRate * 60.0) / currentBpm).toInt()

                // Clamp in case tempo was raised
                if (samplesUntilNextBeat > samplesPerBeat) {
                    samplesUntilNextBeat = samplesPerBeat
                }

                buffer.fill(0)

                val currentAccents = accents

                for (i in 0 until chunkSize) {
                    if (samplesUntilNextBeat <= 0) {
                        val currentAccent = currentAccents.getOrNull(beatIndex)
                            ?: if (beatIndex == 0) BeatAccent.ACCENT else BeatAccent.NORMAL

                        activeClick = when (currentAccent) {
                            BeatAccent.ACCENT -> when (currentSound) {
                                MetronomeSound.Woodblock -> woodblockDown
                                MetronomeSound.Digital -> digitalDown
                                MetronomeSound.Rimshot -> rimshotDown
                                MetronomeSound.Clave -> claveDown
                            }
                            BeatAccent.NORMAL -> when (currentSound) {
                                MetronomeSound.Woodblock -> woodblockSub
                                MetronomeSound.Digital -> digitalSub
                                MetronomeSound.Rimshot -> rimshotSub
                                MetronomeSound.Clave -> claveSub
                            }
                            BeatAccent.MUTE -> null
                        }
                        activeClickOffset = 0
                        _currentBeat.value = beatIndex
                        beatIndex = (beatIndex + 1) % currentSignature
                        samplesUntilNextBeat = samplesPerBeat
                    }
                    samplesUntilNextBeat--

                    if (activeClick != null && activeClickOffset < activeClick.size) {
                        val sample = (activeClick[activeClickOffset] * currentVol).toInt()
                        buffer[i] = sample.coerceIn(-32767, 32767).toShort()
                        activeClickOffset++
                        if (activeClickOffset >= activeClick.size) {
                            activeClick = null
                        }
                    }
                }

                // Write chunk directly to AudioTrack with blocking semantics
                audioTrack?.write(buffer, 0, chunkSize)
            }
        }
    }

    fun stop() {
        _isPlaying.value = false
        streamJob?.cancel()
        streamJob = null
        _currentBeat.value = 0

        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    pause()
                    flush()
                }
            }
        } catch (_: Exception) {}
    }

    fun release() {
        stop()
        try {
            audioTrack?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }
}
