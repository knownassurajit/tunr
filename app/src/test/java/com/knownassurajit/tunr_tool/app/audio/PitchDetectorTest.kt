package com.knownassurajit.tunr_tool.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class PitchDetectorTest {

    private val sampleRate = 44100
    private val detector = PitchDetector(sampleRate = sampleRate)

    /**
     * Synthesizes a mono 16-bit PCM buffer with a pure sine wave at the given frequency.
     */
    private fun generateSineWave(frequencyHz: Float, durationSamples: Int = 4096, amplitude: Double = 0.8): ShortArray {
        val buffer = ShortArray(durationSamples)
        for (i in 0 until durationSamples) {
            val t = i.toDouble() / sampleRate
            val sample = sin(2.0 * PI * frequencyHz * t) * amplitude
            buffer[i] = (sample * 32767).toInt().toShort()
        }
        return buffer
    }

    @Test
    fun testDetectConcertA4_440Hz() {
        val targetFreq = 440.0f
        val buffer = generateSineWave(targetFreq)
        val result = detector.detectPitch(buffer, buffer.size)

        assertTrue("Should be voiced", result.isVoiced)
        assertTrue("Confidence should be high", result.confidence > 0.85f)
        assertEquals("Detected pitch should be close to 440Hz", targetFreq, result.frequencyHz, 0.4f)
    }

    @Test
    fun testDetectGuitarLowE2_82Hz() {
        val targetFreq = 82.41f
        val buffer = generateSineWave(targetFreq)
        val result = detector.detectPitch(buffer, buffer.size)

        assertTrue("Should be voiced", result.isVoiced)
        assertTrue("Confidence should be high", result.confidence > 0.85f)
        assertEquals("Detected pitch should be close to 82.4Hz", targetFreq, result.frequencyHz, 0.5f)
    }

    @Test
    fun testDetectGuitarA2_110Hz() {
        val targetFreq = 110.0f
        val buffer = generateSineWave(targetFreq)
        val result = detector.detectPitch(buffer, buffer.size)

        assertTrue("Should be voiced", result.isVoiced)
        assertEquals("Detected pitch should be close to 110Hz", targetFreq, result.frequencyHz, 0.4f)
    }

    @Test
    fun testDetectGuitarHighE4_329Hz() {
        val targetFreq = 329.63f
        val buffer = generateSineWave(targetFreq)
        val result = detector.detectPitch(buffer, buffer.size)

        assertTrue("Should be voiced", result.isVoiced)
        assertEquals("Detected pitch should be close to 329.6Hz", targetFreq, result.frequencyHz, 0.4f)
    }

    @Test
    fun testNoiseGateRejectionOnSilence() {
        val silence = ShortArray(4096) { 0 }
        val result = detector.detectPitch(silence, silence.size, noiseGateThreshold = 0.01f)

        assertFalse("Silent buffer should not be voiced", result.isVoiced)
        assertEquals(0f, result.frequencyHz, 0.001f)
    }
}
