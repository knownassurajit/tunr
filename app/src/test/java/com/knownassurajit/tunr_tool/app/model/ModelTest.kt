package com.knownassurajit.tunr_tool.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ModelTest {

    @Test
    fun testStandardTuningStrings() {
        val standard = Tunings.Standard
        assertEquals(6, standard.strings.size)
        // High E4 is string 1, Low E2 is string 6
        assertEquals("E", standard.strings[0].note)
        assertEquals(2, standard.strings[0].octave)
        assertEquals(6, standard.strings[0].number)

        val a4 = 440f
        // String 1: E4 ~ 329.6 Hz
        val stringE4 = standard.strings.first { it.number == 1 }
        assertEquals(329.63f, stringE4.hz(a4), 0.5f)

        // String 5: A2 ~ 110.0 Hz
        val stringA2 = standard.strings.first { it.number == 5 }
        assertEquals(110.0f, stringA2.hz(a4), 0.1f)
    }

    @Test
    fun testTuneStatusLockWindow() {
        val standardPrecision = Precision.Standard // lockCents = 3f
        val readingExact = PitchReading("A", 4, 440f, 440f, 0.5f)
        assertEquals(TuneStatus.InTune, readingExact.status(standardPrecision.lockCents))

        val readingNear = PitchReading("A", 4, 442f, 440f, 8f)
        assertEquals(TuneStatus.Near, readingNear.status(standardPrecision.lockCents))

        val readingFar = PitchReading("A", 4, 450f, 440f, 25f)
        assertEquals(TuneStatus.Far, readingFar.status(standardPrecision.lockCents))

        val readingNull: PitchReading? = null
        assertEquals(TuneStatus.Idle, readingNull.status(standardPrecision.lockCents))

        val ultraPrecision = Precision.Ultra // lockCents = 0.5f, decimals = 2
        assertEquals(0.5f, ultraPrecision.lockCents, 0.001f)
        assertEquals(2, ultraPrecision.decimals)
        val readingUltraLocked = PitchReading("A", 4, 440.1f, 440f, 0.35f)
        assertEquals(TuneStatus.InTune, readingUltraLocked.status(ultraPrecision.lockCents))
        val readingUltraFar = PitchReading("A", 4, 440.3f, 440f, 0.8f)
        assertEquals(TuneStatus.Near, readingUltraFar.status(ultraPrecision.lockCents))
    }

    @Test
    fun testSettingsStateGranularControls() {
        val defaultSettings = SettingsState()
        assertEquals(Precision.High, defaultSettings.precision)
        assertEquals("High", defaultSettings.precision.label)
        assertEquals("±1¢", defaultSettings.precision.thresholdLabel)
        assertEquals(TunerSensitivity.Balanced, defaultSettings.sensitivity)
        assertEquals(AppThemeScheme.Emerald, defaultSettings.themeScheme)
        assertEquals(440f, defaultSettings.a4Hz, 0.01f)
        assertEquals(0.5f, defaultSettings.noiseGate, 0.01f)
        assertTrue("Harmonic filter default true", defaultSettings.harmonicFilter)
        assertTrue("Subsonic filter default true", defaultSettings.subsonicFilter)
        assertEquals(NotationPreference.Sharps, defaultSettings.notation)
        assertEquals(MetronomeSound.Woodblock, defaultSettings.metronomeSound)
        assertEquals(0.9f, defaultSettings.metronomeVolume, 0.01f)
        assertTrue("Metronome haptics default true", defaultSettings.metronomeHaptics)
        assertTrue("Keep screen awake default true", defaultSettings.keepScreenAwake)
        assertTrue("Sound feedback default true", defaultSettings.soundFeedback)

        val custom = defaultSettings.copy(
            sensitivity = TunerSensitivity.Smooth,
            notation = NotationPreference.Flats,
            metronomeSound = MetronomeSound.Clave,
            metronomeVolume = 0.75f,
            noiseGate = 0.8f,
            harmonicFilter = false,
            soundFeedback = false,
        )
        assertEquals(TunerSensitivity.Smooth, custom.sensitivity)
        assertEquals(NotationPreference.Flats, custom.notation)
        assertEquals(MetronomeSound.Clave, custom.metronomeSound)
        assertEquals(0.75f, custom.metronomeVolume, 0.01f)
        assertFalse(custom.soundFeedback)
    }

    @Test
    fun testTunerSensitivityOptions() {
        assertEquals(3, TunerSensitivity.entries.size)
        assertEquals("Smooth", TunerSensitivity.Smooth.label)
        assertEquals("Standard", TunerSensitivity.Balanced.label)
        assertEquals("Fast", TunerSensitivity.Responsive.label)
        assertTrue(TunerSensitivity.Smooth.smoothingFactor < TunerSensitivity.Responsive.smoothingFactor)
        assertTrue(TunerSensitivity.Smooth.deadbandCents > TunerSensitivity.Responsive.deadbandCents)
    }

    @Test
    fun testTimeSignatures() {
        val sig44 = TimeSignature(4, 4)
        assertEquals("4/4", sig44.label)

        val sig34 = TimeSignature(3, 4)
        assertEquals("3/4", sig34.label)
    }

    @Test
    fun testBeatAccents() {
        val defaultMetronome = MetronomeState()
        assertEquals(4, defaultMetronome.accents.size)
        assertEquals(BeatAccent.ACCENT, defaultMetronome.accents[0])
        assertEquals(BeatAccent.NORMAL, defaultMetronome.accents[1])
        assertEquals(BeatAccent.NORMAL, defaultMetronome.accents[2])
        assertEquals(BeatAccent.NORMAL, defaultMetronome.accents[3])

        val customAccents = listOf(BeatAccent.ACCENT, BeatAccent.MUTE, BeatAccent.NORMAL, BeatAccent.MUTE)
        val updated = defaultMetronome.copy(accents = customAccents)
        assertEquals(BeatAccent.MUTE, updated.accents[1])
    }

    @Test
    fun testAppThemeSchemeAndCustomHex() {
        assertEquals(6, AppThemeScheme.entries.size)
        assertTrue(AppThemeScheme.entries.contains(AppThemeScheme.Custom))
        assertEquals("Custom", AppThemeScheme.Custom.label)

        val settings = SettingsState()
        assertEquals("#B9F23C", settings.customThemeHex)
        assertEquals(ScreenTimeout.AlwaysOn, settings.screenTimeout)
        assertEquals(ScreenDimTimeout.TwoMinutes, settings.screenDimTimeout)
    }

    @Test
    fun testScreenTimeoutAndDimOptions() {
        assertEquals(5, ScreenTimeout.entries.size)
        assertEquals("Always on", ScreenTimeout.AlwaysOn.label)
        assertEquals("System", ScreenTimeout.SystemDefault.label)

        assertEquals(5, ScreenDimTimeout.entries.size)
        assertEquals("Off", ScreenDimTimeout.Never.label)
        assertEquals("2 min", ScreenDimTimeout.TwoMinutes.label)
        assertEquals(120, ScreenDimTimeout.TwoMinutes.seconds)
    }
}
