package com.knownassurajit.tunr_tool.app.audio

import kotlin.math.sqrt

data class PitchResult(
    val frequencyHz: Float,
    val confidence: Float,
    val rms: Float,
    val isVoiced: Boolean
)

/**
 * Pure Kotlin pitch detection engine using Normalized Square Difference Function (NSDF)
 * with McLeod Pitch Method (MPM) parabolic sub-sample peak interpolation.
 *
 * Enhanced with:
 * - Subsonic DC-blocker high-pass filter (35 Hz cutoff) to eliminate handling thumps and draft rumble.
 * - Dual-criteria noise separation: RMS gate combined with harmonic periodicity clarity threshold
 *   to isolate musical instrument strings from background chatter and room noise.
 * - Attack/sustain gate hysteresis: Allows natural note decay to ring out smoothly without cutting off.
 * - Optimized running-sum-of-squares calculation for real-time mobile performance.
 *
 * Runs strictly in RAM with zero disk writes, ensuring complete user privacy.
 */
class PitchDetector(
    private val sampleRate: Int = 44100,
    private val minFrequency: Float = 35.0f,   // Bass B0/E1 register
    private val maxFrequency: Float = 1400.0f  // High guitar harmonics
) {
    private val minPeriod = (sampleRate / maxFrequency).toInt().coerceAtLeast(20)
    private val maxPeriod = (sampleRate / minFrequency).toInt().coerceAtMost(2000)

    // Pre-allocated internal buffer to avoid GC allocations during high-frequency audio callbacks
    private var filteredBuffer = FloatArray(4096)
    private var energySum = DoubleArray(4096 + 1)

    // High-pass filter state
    private var prevX = 0f
    private var prevY = 0f

    /**
     * Estimates pitch from 16-bit PCM mono samples.
     *
     * @param audioBuffer Audio sample buffer.
     * @param bufferLength Number of valid samples in the buffer.
     * @param noiseGateThreshold Minimum RMS amplitude to consider sound input.
     * @param harmonicFilter When enabled, rejects non-periodic sound (speech, room noise).
     * @param subsonicFilter When enabled, blocks DC bias and rumble below 35Hz.
     * @param wasVoiced Indicates if the previous audio frame was voiced (enables gate hysteresis).
     */
    fun detectPitch(
        audioBuffer: ShortArray,
        bufferLength: Int,
        noiseGateThreshold: Float = 0.012f,
        harmonicFilter: Boolean = true,
        subsonicFilter: Boolean = true,
        wasVoiced: Boolean = false
    ): PitchResult {
        if (bufferLength < maxPeriod * 2) {
            return PitchResult(0f, 0f, 0f, false)
        }

        if (filteredBuffer.size < bufferLength) {
            filteredBuffer = FloatArray(bufferLength)
            energySum = DoubleArray(bufferLength + 1)
        }

        // 1. High-Pass Pre-filtering (DC blocker: y[n] = x[n] - x[n-1] + R*y[n-1], R ~ 0.995, fc ~ 35Hz)
        val r = 0.995f
        var sumSquares = 0.0

        for (i in 0 until bufferLength) {
            val x = audioBuffer[i] / 32768.0f
            val y = if (subsonicFilter) {
                val out = x - prevX + r * prevY
                prevX = x
                prevY = out
                out
            } else {
                x
            }
            filteredBuffer[i] = y
            sumSquares += y * y
        }

        val rms = sqrt(sumSquares / bufferLength).toFloat()

        // 2. Dual-Criteria Noise Gate with Hysteresis
        // If already tracking a voiced note, allow RMS to decay lower as long as harmonicity holds
        val effectiveGate = if (wasVoiced && harmonicFilter) {
            noiseGateThreshold * 0.38f
        } else {
            noiseGateThreshold
        }

        if (rms < effectiveGate) {
            return PitchResult(0f, 0f, rms, false)
        }

        // 3. Normalized Square Difference Function (NSDF) with Running Energy Optimization
        val windowSize = bufferLength - maxPeriod
        if (windowSize <= 0) return PitchResult(0f, 0f, rms, false)

        // Pre-compute cumulative energy sums for O(1) sumSquare lookups
        energySum[0] = 0.0
        for (i in 0 until bufferLength) {
            val s = filteredBuffer[i].toDouble()
            energySum[i + 1] = energySum[i] + s * s
        }

        val sumSquare1 = energySum[windowSize] - energySum[0]
        val nsdf = FloatArray(maxPeriod)

        for (tau in minPeriod until maxPeriod) {
            var sumProd = 0.0
            for (j in 0 until windowSize) {
                sumProd += filteredBuffer[j] * filteredBuffer[j + tau]
            }

            val sumSquare2 = energySum[windowSize + tau] - energySum[tau]
            val denominator = sumSquare1 + sumSquare2
            if (denominator > 0.000001) {
                nsdf[tau] = (2.0 * sumProd / denominator).toFloat()
            } else {
                nsdf[tau] = 0f
            }
        }

        // 4. Peak picking with McLeod Pitch Method (MPM)
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

        // Harmonic Clarity Check:
        // Musical instruments (vibrating strings) produce high periodicity peaks (0.70 - 0.98).
        // Room chatter, speech, and ambient noise have low, diffuse peaks (< 0.55).
        val clarityThreshold = if (harmonicFilter) 0.62f else 0.48f
        if (maxPeakValue < clarityThreshold) {
            return PitchResult(0f, maxPeakValue.coerceAtLeast(0f), rms, false)
        }

        // Pick first peak that achieves at least 85% of the maximum peak
        // Effectively eliminates octave jumping (picking 2*tau or 3*tau)
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
