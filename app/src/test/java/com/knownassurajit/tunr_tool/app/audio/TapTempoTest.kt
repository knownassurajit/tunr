package com.knownassurajit.tunr_tool.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TapTempoTest {
    @Test fun needsTwoTaps() {
        val t = TapTempo()
        assertNull(t.tap(0))
        assertEquals(120, t.tap(500))
    }

    @Test fun medianIgnoresAnOutlierTap() {
        val t = TapTempo()
        var bpm: Int? = null
        for (ms in listOf(0L, 500, 1000, 1900, 2400, 2900)) bpm = t.tap(ms)   // one late tap
        assertEquals(120, bpm)
    }

    @Test fun pauseStartsOver() {
        val t = TapTempo()
        t.tap(0); t.tap(500)
        assertNull(t.tap(10_000))
        assertEquals(60, t.tap(11_000))
    }

    @Test fun clampsToSupportedRange() {
        assertEquals(240, TapTempo().also { it.tap(0) }.tap(100))
        assertEquals(30, TapTempo().also { it.tap(0) }.tap(2000))   // exactly 30 BPM
        assertEquals(30, TapTempo(resetMs = 10_000).also { it.tap(0) }.tap(5000))
    }

    @Test fun keepsOnlyTheMostRecentTaps() {
        val t = TapTempo()
        for (i in 0..5) t.tap(i * 1000L)           // 60 BPM
        var bpm: Int? = null
        for (i in 0..8) bpm = t.tap(6000L + i * 500L) // then 120 BPM
        assertEquals(120, bpm)
    }
}
