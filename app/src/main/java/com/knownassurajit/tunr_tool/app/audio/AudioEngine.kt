package com.knownassurajit.tunr_tool.app.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log

/** Receives mono PCM16 blocks on the audio thread. Must not allocate or block. */
fun interface AudioSink {
    fun onSamples(buffer: ShortArray, count: Int)
}

/**
 * Microphone capture on a dedicated URGENT_AUDIO thread: one preallocated buffer, blocking reads,
 * no coroutines, no per-block allocation. Prefers the UNPROCESSED source (no AGC / noise
 * suppression distorting level and periodicity), falling back to VOICE_RECOGNITION, then MIC.
 *
 * STRICT PRIVACY: samples live only in that one buffer in RAM and are overwritten by the next read.
 * Nothing is persisted, cached or transmitted.
 */
class AudioEngine(
    context: Context,
    private val sampleRate: Int = 44100,
    private val hopSamples: Int = 2048,
) {
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    @Volatile private var running = false
    private var thread: Thread? = null

    val isRecording: Boolean get() = running && thread?.isAlive == true

    @Synchronized
    @SuppressLint("MissingPermission")
    fun start(sink: AudioSink, onFailure: () -> Unit = {}) {
        if (isRecording) return
        val record = createRecord() ?: run { onFailure(); return }
        try {
            record.startRecording()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "startRecording failed", e)
            record.release()
            onFailure()
            return
        }
        running = true
        thread = Thread({ loop(record, sink) }, "tunr-audio-in").also { it.start() }
    }

    @Synchronized
    fun stop() {
        running = false
        thread?.let { t ->
            try { t.join(250) } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
        }
        thread = null
    }

    private fun loop(record: AudioRecord, sink: AudioSink) {
        try { Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO) } catch (_: Exception) {}
        val buffer = ShortArray(hopSamples)
        try {
            while (running) {
                val n = record.read(buffer, 0, hopSamples, AudioRecord.READ_BLOCKING)
                if (n > 0) sink.onSamples(buffer, n) else if (n < 0) break
            }
        } catch (e: Exception) {
            Log.e(TAG, "capture loop failed", e)
        } finally {
            running = false
            try { record.stop() } catch (_: Exception) {}
            record.release()
        }
    }

    @SuppressLint("MissingPermission")
    private fun createRecord(): AudioRecord? {
        val minBytes = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBytes <= 0) return null
        val bytes = maxOf(minBytes * 2, hopSamples * 2 * 4)
        val unprocessed = audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val sources = buildList {
            if (unprocessed) add(MediaRecorder.AudioSource.UNPROCESSED)
            add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            add(MediaRecorder.AudioSource.MIC)
        }
        for (source in sources) {
            try {
                val r = AudioRecord.Builder()
                    .setAudioSource(source)
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build(),
                    )
                    .setBufferSizeInBytes(bytes)
                    .build()
                if (r.state == AudioRecord.STATE_INITIALIZED) return r
                r.release()
            } catch (e: Exception) {
                Log.w(TAG, "audio source $source unavailable", e)
            }
        }
        return null
    }

    private companion object { const val TAG = "AudioEngine" }
}
