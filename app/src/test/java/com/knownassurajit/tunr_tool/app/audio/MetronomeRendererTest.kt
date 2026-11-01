package com.knownassurajit.tunr_tool.app.audio

import com.knownassurajit.tunr_tool.app.model.BeatAccent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.management.ManagementFactory
import kotlin.math.abs

class MetronomeRendererTest {
    private val sr = 44100

    private fun exactTickFrame(k: Int, ticksPerMinute: Int): Long = (k.toLong() * sr * 60 + ticksPerMinute - 1) / ticksPerMinute

    private class Tick(val frame: Long, val beat: Int, val sub: Int)

    /** Renders [seconds] in [chunk]-frame blocks, returning every tick event and the PCM. */
    private fun run(r: MetronomeRenderer, seconds: Double, chunk: Int = 256, onChunk: (Long) -> Unit = {}): Pair<List<Tick>, ShortArray> {
        val total = (seconds * sr).toInt() / chunk * chunk
        val pcm = ShortArray(total)
        val buf = ShortArray(chunk)
        val ticks = ArrayList<Tick>()
        var done = 0
        while (done < total) {
            onChunk(r.framesRendered)
            r.render(buf, chunk)
            System.arraycopy(buf, 0, pcm, done, chunk)
            while (r.peekEventFrame() != Long.MAX_VALUE) { val f = r.peekEventFrame(); r.popEvent(); ticks += Tick(f, r.eventBeat, r.eventSub) }
            done += chunk
        }
        return ticks to pcm
    }

    @Test fun noDriftAtAwkwardTempos() {
        for (bpm in listOf(120, 97, 133, 61, 203)) {
            val r = MetronomeRenderer(sr).also { it.bpm = bpm }
            val (ticks, _) = run(r, 60.0)
            assertTrue("bpm $bpm produced ${ticks.size} ticks", ticks.size >= bpm - 1)
            ticks.forEachIndexed { k, t ->
                assertEquals("bpm $bpm tick $k", exactTickFrame(k, bpm), t.frame)   // ceil(k·sr·60/bpm), exact
            }
            // After a full minute the clock is still within one sample of ideal (an integer
            // samples-per-beat would have drifted by tens of ms at 97 BPM).
            val last = ticks.last()
            assertTrue(abs(last.frame - (ticks.size - 1) * (sr * 60.0 / bpm)) < 1.0)
        }
    }

    @Test fun beatAndSubdivisionNumbering() {
        val r = MetronomeRenderer(sr).also { it.bpm = 100; it.beatsPerMeasure = 3; it.subdivision = 3 }
        val (ticks, _) = run(r, 6.0)
        ticks.forEachIndexed { k, t ->
            assertEquals(exactTickFrame(k, 300), t.frame)
            assertEquals("beat of tick $k", (k % 9) / 3, t.beat)
            assertEquals("sub of tick $k", k % 3, t.sub)
        }
    }

    @Test fun mutedBeatIsSilentAndOthersAreNot() {
        val r = MetronomeRenderer(sr).also {
            it.bpm = 120; it.beatsPerMeasure = 4
            it.accents = arrayOf(BeatAccent.ACCENT, BeatAccent.MUTE, BeatAccent.NORMAL, BeatAccent.NORMAL)
        }
        val (_, pcm) = run(r, 2.0)
        val beat = 22050
        fun peak(from: Int) = (from until from + 800).maxOf { abs(pcm[it].toInt()) }
        assertTrue(peak(0) > 10000)
        assertEquals(0, peak(beat))
        assertTrue(peak(2 * beat) > 5000)
        assertTrue("accent louder than normal", peak(0) > peak(2 * beat))
    }

    @Test fun subdivisionClicksAreSofterThanBeats() {
        val r = MetronomeRenderer(sr).also { it.bpm = 60; it.subdivision = 2 }
        val (_, pcm) = run(r, 2.0)
        val beat = (0 until 800).maxOf { abs(pcm[it].toInt()) }
        val half = (sr / 2 until sr / 2 + 800).maxOf { abs(pcm[it].toInt()) }
        assertTrue(half in 1 until beat)
    }

    @Test fun tempoChangeKeepsPhaseAndAppliesFromTheNextInterval() {
        val r = MetronomeRenderer(sr).also { it.bpm = 120 }
        var changed = false
        val (ticks, _) = run(r, 6.0) { frames ->
            if (!changed && frames >= 2 * sr) { r.bpm = 60; changed = true }
        }
        val gaps = ticks.zipWithNext { a, b -> b.frame - a.frame }
        assertTrue(gaps.first() in 22049..22051)
        assertTrue(gaps.last() in 44099..44101)
        assertTrue("never a gap shorter than the fast tempo or longer than the slow one", gaps.all { it in 22049..44101 })
    }

    @Test fun eventsAreOrderedAndReportAudibleFrames() {
        val r = MetronomeRenderer(sr).also { it.bpm = 240; it.subdivision = 4 }
        val (ticks, _) = run(r, 5.0)
        assertTrue(ticks.zipWithNext().all { (a, b) -> b.frame > a.frame })
    }

    @Test fun renderDoesNotAllocate() {
        val bean = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
        if (!bean.isThreadAllocatedMemorySupported) return
        val r = MetronomeRenderer(sr).also { it.bpm = 180; it.subdivision = 3 }
        val buf = ShortArray(256)
        repeat(2000) { r.render(buf, 256); while (r.peekEventFrame() != Long.MAX_VALUE) r.popEvent() }
        val id = Thread.currentThread().id
        val before = bean.getThreadAllocatedBytes(id)
        repeat(2000) { r.render(buf, 256); while (r.peekEventFrame() != Long.MAX_VALUE) r.popEvent() }
        val allocated = bean.getThreadAllocatedBytes(id) - before
        assertTrue("allocated $allocated bytes", allocated < 1024)
    }

    @Test fun parametersAreClamped() {
        val r = MetronomeRenderer(sr)
        r.bpm = 5; assertEquals(30, r.bpm)
        r.bpm = 999; assertEquals(240, r.bpm)
        r.subdivision = 9; assertEquals(4, r.subdivision)
        r.beatsPerMeasure = 0; assertEquals(1, r.beatsPerMeasure)
    }
}
