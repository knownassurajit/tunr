package com.knownassurajit.tunr_tool.app.audio

import kotlin.math.sqrt

data class PitchResult(
    val frequencyHz: Float,
    val confidence: Float,
    val rms: Float,
    val isVoiced: Boolean
)

/**
 * Pure Kotlin pitch detection engine using Normalized Autocorrelation / YIN-style NSDF
 * with parabolic sub-sample peak interpolation.
 *
 * Runs entirely in memory with zero disk writes, ensuring maximum performance and user privacy.
 */
class PitchDetector(
    private val sampleRate: Int = 44100,
    private val minFrequency: Float = 35.0f,   // Bass B0/E1 register
    private val maxFrequency: Float = 1400.0f  // High guitar harmonics
) {
    private val minPeriod = (sampleRate / maxFrequency).toInt().coerceAtLeast(20)
    private val maxPeriod = (sampleRate / minFrequency).toInt().coerceAtMost(2000)

    /**
     * Estimates pitch from 16-bit PCM mono samples.
     * @param audioBuffer Audio sample buffer.
     * @param bufferLength Number of valid samples in the buffer.
     * @param noiseGateThreshold Minimum RMS amplitude to consider sound input.
     */
    fun detectPitch(
        audioBuffer: ShortArray,
        bufferLength: Int,
        noiseGateThreshold: Float = 0.012f
    ): PitchResult {
        if (bufferLength < maxPeriod * 2) {
            return PitchResult(0f, 0f, 0f, false)
        }

        // 1. Calculate RMS energy
        var sumSquares = 0.0
        for (i in 0 until bufferLength) {
            val normalized = audioBuffer[i] / 32768.0
            sumSquares += normalized * normalized
        }
        val rms = sqrt(sumSquares / bufferLength).toFloat()

        if (rms < noiseGateThreshold) {
            return PitchResult(0f, 0f, rms, false)
        }

        // 2. Normalized Square Difference Function (NSDF) / Autocorrelation
        val windowSize = bufferLength - maxPeriod
        if (windowSize <= 0) return PitchResult(0f, 0f, rms, false)

        val nsdf = FloatArray(maxPeriod)

        for (tau in minPeriod until maxPeriod) {
            var sumProd = 0.0
            var sumSquare1 = 0.0
            var sumSquare2 = 0.0

            for (j in 0 until windowSize) {
                val s1 = audioBuffer[j].toDouble()
                val s2 = audioBuffer[j + tau].toDouble()
                sumProd += s1 * s2
                sumSquare1 += s1 * s1
                sumSquare2 += s2 * s2
            }

            val denominator = sumSquare1 + sumSquare2
            if (denominator > 0.000001) {
                nsdf[tau] = (2.0 * sumProd / denominator).toFloat()
            } else {
                nsdf[tau] = 0f
            }
        }

        // 3. Peak picking with McLeod Pitch Method (MPM)
        // Collect local maxima that occur after nsdf has descended through zero
        data class Peak(val exactPeriod: Float, val value: Float)
        val peaks = mutableListOf<Peak>()
        var hasGoneNegative = false

        for (tau in 1 until (maxPeriod - 1)) {
            if (nsdf[tau] < 0f) {
                hasGoneNegative = true
            }

            if (hasGoneNegative && nsdf[tau] > 0f) {
                val curr = nsdf[tau]
                val prev = nsdf[tau - 1]
                val next = nsdf[tau + 1]

                if (curr > prev && curr >= next) {
                    // Parabolic 3-point interpolation for sub-sample accuracy
                    val alpha = prev
                    val beta = curr
                    val gamma = next
                    val denom = 2.0f * (alpha - 2.0f * beta + gamma)
                    val delta = if (denom != 0.0f) {
                        (alpha - gamma) / denom
                    } else {
                        0.0f
                    }
                    val exactPeriod = tau + delta
                    peaks.add(Peak(exactPeriod, curr))
                }
            }
        }

        if (peaks.isEmpty()) {
            return PitchResult(0f, 0f, rms, false)
        }

        val maxPeakValue = peaks.maxOf { it.value }
        if (maxPeakValue < 0.55f) {
            return PitchResult(0f, maxPeakValue.coerceAtLeast(0f), rms, false)
        }

        // Pick first peak that achieves at least 85% of the maximum peak
        // This effectively eliminates octave errors (picking 2*tau or 3*tau)
        val cutoff = 0.85f * maxPeakValue
        val chosenPeak = peaks.firstOrNull { it.value >= cutoff } ?: peaks.first()

        val fundamentalFrequency = sampleRate / chosenPeak.exactPeriod

        return PitchResult(
            frequencyHz = fundamentalFrequency,
            confidence = maxPeakValue.coerceIn(0f, 1f),
            rms = rms,
            isVoiced = true
        )
    }
}
