package com.knownassurajit.tunr_tool.app.audio

import com.knownassurajit.tunr_tool.app.model.MetronomeSound
import java.util.Random
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pre-synthesised click waveforms (accent + normal per sound). Built once, read-only afterwards,
 * so the audio thread only copies samples. Pure Kotlin: unit-testable on the JVM.
 *
 * - Woodblock: physical wood strike with impact chirp (2600 Hz -> 1600 Hz)
 * - Digital: crisp electronic micro-sine tick
 * - Rimshot: snappy cross-stick tap
 * - Clave: resonant Latin wooden strike
 */
class ClickBank(private val sampleRate: Int = 44100) {
    private val woodblockDown = synthesizeWoodblockClick(isDownbeat = true)
    private val woodblockSub = synthesizeWoodblockClick(isDownbeat = false)
    private val digitalDown = synthesizeDigitalClick(isDownbeat = true)
    private val digitalSub = synthesizeDigitalClick(isDownbeat = false)
    private val rimshotDown = synthesizeRimshotClick(isDownbeat = true)
    private val rimshotSub = synthesizeRimshotClick(isDownbeat = false)
    private val claveDown = synthesizeClaveClick(isDownbeat = true)
    private val claveSub = synthesizeClaveClick(isDownbeat = false)

    fun accent(sound: MetronomeSound): ShortArray = when (sound) {
        MetronomeSound.Woodblock -> woodblockDown
        MetronomeSound.Digital -> digitalDown
        MetronomeSound.Rimshot -> rimshotDown
        MetronomeSound.Clave -> claveDown
    }

    fun normal(sound: MetronomeSound): ShortArray = when (sound) {
        MetronomeSound.Woodblock -> woodblockSub
        MetronomeSound.Digital -> digitalSub
        MetronomeSound.Rimshot -> rimshotSub
        MetronomeSound.Clave -> claveSub
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
}
