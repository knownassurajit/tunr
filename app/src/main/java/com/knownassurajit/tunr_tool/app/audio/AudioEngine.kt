package com.knownassurajit.tunr_tool.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Captures real-time PCM audio from the device microphone for pitch estimation.
 * 
 * STRICT PRIVACY GUARANTEE:
 * Captured audio buffers exist exclusively in volatile memory (RAM) for immediate mathematical analysis
 * and are instantly overwritten. No audio data is ever persisted to local storage, cache, or external servers.
 */
class AudioEngine(
    private val sampleRate: Int = 44100,
    private val bufferSize: Int = 4096
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _audioBufferFlow = MutableSharedFlow<ShortArray>(extraBufferCapacity = 2)
    val audioBufferFlow: SharedFlow<ShortArray> = _audioBufferFlow.asSharedFlow()

    private val minHardwareBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    val isRecording: Boolean
        get() = recordingJob?.isActive == true && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    @SuppressLint("MissingPermission")
    fun start(onPermissionRequired: () -> Unit) {
        if (isRecording) return

        try {
            val recordBufferSize = maxOf(minHardwareBufferSize * 2, bufferSize * 2)
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufferSize
            )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioEngine", "AudioRecord failed to initialize")
                record.release()
                onPermissionRequired()
                return
            }

            audioRecord = record
            record.startRecording()

            recordingJob = scope.launch {
                val tempBuffer = ShortArray(bufferSize)
                while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val readSamples = record.read(tempBuffer, 0, bufferSize, AudioRecord.READ_BLOCKING)
                    if (readSamples > 0) {
                        val bufferCopy = tempBuffer.copyOf(readSamples)
                        _audioBufferFlow.tryEmit(bufferCopy)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("AudioEngine", "SecurityException starting audio recording", e)
            onPermissionRequired()
        } catch (e: Exception) {
            Log.e("AudioEngine", "Exception starting audio recording", e)
        }
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.apply {
                if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioEngine", "Error stopping AudioRecord", e)
        } finally {
            audioRecord = null
        }
    }
}
