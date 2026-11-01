package com.knownassurajit.tunr_tool.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Lightweight, zero-latency synthesizer for the pitch lock chime.
 * Synthesizes a soft, crystal harmonic chime (A5 880Hz + octave harmonics)
 * directly into static PCM memory. Plays instantly when the note locks in tune.
 */
class InTuneSoundPlayer {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null

    init {
        try {
            val durationMs = 150
            val numSamples = (sampleRate * durationMs) / 1000
            val pcmBuffer = ShortArray(numSamples)

            val f1 = 880.0  // A5 fundamental
            val f2 = 1760.0 // A6 octave
            val f3 = 2640.0 // E7 fifth harmonic sparkle

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                // 6ms smooth linear attack to avoid click, followed by natural exponential decay
                val attack = (t / 0.006).coerceAtMost(1.0)
                val decay = exp(-t / 0.038)
                val envelope = attack * decay

                val wave = 0.65 * sin(2.0 * PI * f1 * t) +
                           0.25 * sin(2.0 * PI * f2 * t) +
                           0.10 * sin(2.0 * PI * f3 * t)

                val sample = (wave * envelope * 0.60 * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcmBuffer[i] = sample.toShort()
            }

            val bufferSizeInBytes = pcmBuffer.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            val written = track.write(pcmBuffer, 0, pcmBuffer.size)
            if (written > 0) {
                audioTrack = track
            } else {
                track.release()
            }
        } catch (e: Exception) {
            Log.w("InTuneSoundPlayer", "Could not initialize in-tune chime AudioTrack: ${e.message}")
        }
    }

    /**
     * Plays the in-tune confirmation chime with near-zero latency.
     */
    fun play() {
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                        track.stop()
                    }
                    track.reloadStaticData()
                    track.play()
                }
            }
        } catch (e: Exception) {
            Log.w("InTuneSoundPlayer", "Failed to play in-tune chime: ${e.message}")
        }
    }

    fun release() {
        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
        } catch (_: Exception) {
        } finally {
            audioTrack = null
        }
    }
}
