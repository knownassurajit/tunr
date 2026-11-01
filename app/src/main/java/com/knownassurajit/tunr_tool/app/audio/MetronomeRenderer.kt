package com.knownassurajit.tunr_tool.app.audio

import com.knownassurajit.tunr_tool.app.model.BeatAccent
import com.knownassurajit.tunr_tool.app.model.MetronomeSound

/**
 * Sample-accurate metronome scheduler/mixer with no Android dependency.
 *
 * Time is an absolute frame counter. Tick k of a constant tempo lands on frame
 * base + ceil(k·sampleRate·60 / (bpm·subdivision)) computed in exact Long arithmetic, so there is no
 * cumulative drift at any BPM (an integer samples-per-beat would lose up to 1 frame per beat, and
 * accumulating a Double would round differently from the exact rational). Tempo, meter,
 * subdivision, accents and sound can change at any time; they apply from the next tick without a
 * phase reset (the schedule is simply re-based at that tick).
 *
 * [render] is allocation-free. Each tick also queues an event (frame, beat, sub) so the caller can
 * publish it when that frame is actually *audible* (AudioTrack playback head), not when written.
 */
class MetronomeRenderer(
    private val sampleRate: Int = 44100,
    private val clicks: ClickBank = ClickBank(sampleRate),
) {
    @Volatile var bpm: Int = 120
        set(v) { field = v.coerceIn(MIN_BPM, MAX_BPM) }
    @Volatile var beatsPerMeasure: Int = 4
        set(v) { field = v.coerceIn(1, 16) }
    /** Clicks per beat (1..4). */
    @Volatile var subdivision: Int = 1
        set(v) { field = v.coerceIn(1, 4) }
    @Volatile var sound: MetronomeSound = MetronomeSound.Woodblock
    @Volatile var volume: Float = 0.9f
        set(v) { field = v.coerceIn(0f, 1f) }
    /** Replace the whole array to change accents (never mutate in place). */
    @Volatile var accents: Array<BeatAccent> = arrayOf(BeatAccent.ACCENT, BeatAccent.NORMAL, BeatAccent.NORMAL, BeatAccent.NORMAL)

    /** Absolute index of the next frame [render] will produce. */
    var framesRendered: Long = 0L
        private set

    private var nextTickFrame = 0L
    private var baseFrame = 0L
    private var ticksSinceBase = 0L
    private var tickDenominator = 0L // bpm * subdivision the current schedule was built for
    private var lastSubs = 1
    private var tickInMeasure = 0
    private var click: ShortArray? = null
    private var clickPos = 0
    private var clickGain = 0f

    // Event ring (single producer = render thread, single consumer = same thread after write)
    private val evFrame = LongArray(EVENTS)
    private val evBeat = IntArray(EVENTS)
    private val evSub = IntArray(EVENTS)
    private var evHead = 0
    private var evTail = 0
    var eventBeat = 0
        private set
    var eventSub = 0
        private set

    fun reset() {
        framesRendered = 0L; nextTickFrame = 0L; baseFrame = 0L; ticksSinceBase = 0L; tickDenominator = 0L
        lastSubs = 1; tickInMeasure = 0
        click = null; clickPos = 0; evHead = 0; evTail = 0
    }

    /** Fills [out] with [frames] mono samples and advances time. */
    fun render(out: ShortArray, frames: Int) {
        val currentSound = sound
        val vol = volume
        val subs = subdivision
        val beats = beatsPerMeasure
        val acc = accents
        val ticksPerMeasure = beats * subs
        val denominator = bpm.toLong() * subs

        for (i in 0 until frames) {
            val f = framesRendered + i
            if (f >= nextTickFrame) {
                if (subs != lastSubs) { // subdivision changed live: restart at the start of the current beat
                    tickInMeasure = (tickInMeasure / lastSubs) * subs
                    lastSubs = subs
                }
                if (tickInMeasure >= ticksPerMeasure) tickInMeasure = 0
                val beat = tickInMeasure / subs
                val sub = tickInMeasure % subs
                if (sub == 0) {
                    when (acc.getOrNull(beat) ?: if (beat == 0) BeatAccent.ACCENT else BeatAccent.NORMAL) {
                        BeatAccent.ACCENT -> { click = clicks.accent(currentSound); clickGain = vol }
                        BeatAccent.NORMAL -> { click = clicks.normal(currentSound); clickGain = vol }
                        BeatAccent.MUTE -> click = null
                    }
                } else {
                    click = clicks.normal(currentSound); clickGain = vol * SUB_GAIN
                }
                clickPos = 0
                push(f, beat, sub)
                tickInMeasure++

                if (denominator != tickDenominator) { // tempo / subdivision changed: re-base at this tick
                    tickDenominator = denominator
                    baseFrame = nextTickFrame
                    ticksSinceBase = 0
                }
                ticksSinceBase++
                nextTickFrame = baseFrame + (ticksSinceBase * sampleRate * 60L + denominator - 1) / denominator
            }
            val c = click
            if (c != null) {
                out[i] = (c[clickPos] * clickGain).toInt().coerceIn(-32767, 32767).toShort()
                if (++clickPos >= c.size) click = null
            } else {
                out[i] = 0
            }
        }
        framesRendered += frames
    }

    /** Frame of the oldest unconsumed event, or [Long.MAX_VALUE] if none. */
    fun peekEventFrame(): Long = if (evTail == evHead) Long.MAX_VALUE else evFrame[evTail]

    /** Pops the oldest event into [eventBeat]/[eventSub]. */
    fun popEvent() {
        eventBeat = evBeat[evTail]; eventSub = evSub[evTail]
        evTail = (evTail + 1) % EVENTS
    }

    private fun push(frame: Long, beat: Int, sub: Int) {
        val next = (evHead + 1) % EVENTS
        if (next == evTail) evTail = (evTail + 1) % EVENTS // drop oldest if the consumer stalled
        evFrame[evHead] = frame; evBeat[evHead] = beat; evSub[evHead] = sub
        evHead = next
    }

    companion object {
        const val MIN_BPM = 30
        const val MAX_BPM = 240
        private const val EVENTS = 64
        private const val SUB_GAIN = 0.55f
    }
}
