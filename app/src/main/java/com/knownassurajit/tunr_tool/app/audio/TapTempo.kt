package com.knownassurajit.tunr_tool.app.audio

import kotlin.math.roundToInt

/** Tap-tempo estimator: median of the last inter-tap intervals; a pause > [resetMs] starts over. */
class TapTempo(private val maxTaps: Int = 7, private val resetMs: Long = 2000L) {
    private val taps = LongArray(maxTaps)
    private var count = 0
    private val intervals = LongArray(maxTaps - 1)

    fun reset() { count = 0 }

    /** @return BPM clamped to 30..240, or null until two taps are in. */
    fun tap(nowMs: Long): Int? {
        if (count > 0 && nowMs - taps[count - 1] > resetMs) count = 0
        if (count == maxTaps) { System.arraycopy(taps, 1, taps, 0, maxTaps - 1); count-- }
        taps[count++] = nowMs
        if (count < 2) return null
        val n = count - 1
        for (i in 0 until n) intervals[i] = taps[i + 1] - taps[i]
        java.util.Arrays.sort(intervals, 0, n)
        val median = if (n % 2 == 1) intervals[n / 2].toDouble() else (intervals[n / 2 - 1] + intervals[n / 2]) / 2.0
        if (median <= 0.0) return null
        return (60_000.0 / median).roundToInt().coerceIn(MetronomeRenderer.MIN_BPM, MetronomeRenderer.MAX_BPM)
    }
}
