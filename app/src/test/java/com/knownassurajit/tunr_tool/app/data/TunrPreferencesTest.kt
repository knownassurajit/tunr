package com.knownassurajit.tunr_tool.app.data

import android.content.SharedPreferences
import com.knownassurajit.tunr_tool.app.model.AppThemeScheme
import com.knownassurajit.tunr_tool.app.model.BeatAccent
import com.knownassurajit.tunr_tool.app.model.MetronomeSound
import com.knownassurajit.tunr_tool.app.model.NotationPreference
import com.knownassurajit.tunr_tool.app.model.Precision
import com.knownassurajit.tunr_tool.app.model.ScreenDimTimeout
import com.knownassurajit.tunr_tool.app.model.ScreenTimeout
import com.knownassurajit.tunr_tool.app.model.SettingsState
import com.knownassurajit.tunr_tool.app.model.TimeSignature
import com.knownassurajit.tunr_tool.app.model.TunerSensitivity
import com.knownassurajit.tunr_tool.app.model.Tunings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying that all settings, configurations, instrument tunings,
 * and metronome preferences persist and restore accurately without data loss.
 */
class TunrPreferencesTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var preferences: TunrPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        preferences = TunrPreferences(fakePrefs)
    }

    @Test
    fun testDefaultSettingsLoading() {
        val defaultSettings = preferences.loadSettings()
        assertEquals(Precision.High, defaultSettings.precision)
        assertEquals(TunerSensitivity.Balanced, defaultSettings.sensitivity)
        assertEquals(AppThemeScheme.Emerald, defaultSettings.themeScheme)
        assertEquals("#B9F23C", defaultSettings.customThemeHex)
        assertEquals(440f, defaultSettings.a4Hz, 0.01f)
        assertEquals(0.5f, defaultSettings.noiseGate, 0.01f)
        assertTrue(defaultSettings.harmonicFilter)
        assertTrue(defaultSettings.subsonicFilter)
        assertEquals(NotationPreference.Sharps, defaultSettings.notation)
        assertEquals(MetronomeSound.Woodblock, defaultSettings.metronomeSound)
        assertEquals(0.9f, defaultSettings.metronomeVolume, 0.01f)
        assertTrue(defaultSettings.metronomeHaptics)
        assertFalse(defaultSettings.metronomeFlash)
        assertTrue(defaultSettings.keepScreenAwake)
        assertEquals(ScreenTimeout.AlwaysOn, defaultSettings.screenTimeout)
        assertEquals(ScreenDimTimeout.TwoMinutes, defaultSettings.screenDimTimeout)
        assertTrue(defaultSettings.haptics)
        assertTrue(defaultSettings.soundFeedback)
    }

    @Test
    fun testCustomSettingsPersistence() {
        val custom = SettingsState(
            precision = Precision.Ultra,
            sensitivity = TunerSensitivity.Smooth,
            themeScheme = AppThemeScheme.Custom,
            customThemeHex = "#FF5722",
            a4Hz = 432.0f,
            noiseGate = 0.75f,
            harmonicFilter = false,
            subsonicFilter = true,
            notation = NotationPreference.Flats,
            metronomeSound = MetronomeSound.Clave,
            metronomeVolume = 0.6f,
            metronomeHaptics = false,
            metronomeFlash = true,
            keepScreenAwake = false,
            screenTimeout = ScreenTimeout.FiveMinutes,
            screenDimTimeout = ScreenDimTimeout.ThirtySeconds,
            haptics = false,
            soundFeedback = false,
        )

        preferences.saveSettings(custom)
        val loaded = preferences.loadSettings()

        assertEquals(Precision.Ultra, loaded.precision)
        assertEquals(TunerSensitivity.Smooth, loaded.sensitivity)
        assertEquals(AppThemeScheme.Custom, loaded.themeScheme)
        assertEquals("#FF5722", loaded.customThemeHex)
        assertEquals(432.0f, loaded.a4Hz, 0.01f)
        assertEquals(0.75f, loaded.noiseGate, 0.01f)
        assertFalse(loaded.harmonicFilter)
        assertTrue(loaded.subsonicFilter)
        assertEquals(NotationPreference.Flats, loaded.notation)
        assertEquals(MetronomeSound.Clave, loaded.metronomeSound)
        assertEquals(0.6f, loaded.metronomeVolume, 0.01f)
        assertFalse(loaded.metronomeHaptics)
        assertTrue(loaded.metronomeFlash)
        assertFalse(loaded.keepScreenAwake)
        assertEquals(ScreenTimeout.FiveMinutes, loaded.screenTimeout)
        assertEquals(ScreenDimTimeout.ThirtySeconds, loaded.screenDimTimeout)
        assertFalse(loaded.haptics)
        assertFalse(loaded.soundFeedback)
    }

    @Test
    fun testTuningPresetPersistence() {
        // Defaults to Standard
        assertEquals(Tunings.Standard.id, preferences.loadTuning().id)

        // Select Drop D
        preferences.saveTuning(Tunings.DropD)
        assertEquals(Tunings.DropD.id, preferences.loadTuning().id)

        // Select Ukulele
        preferences.saveTuning(Tunings.Ukulele)
        assertEquals(Tunings.Ukulele.id, preferences.loadTuning().id)
    }

    @Test
    fun testAutoDetectPersistence() {
        assertTrue(preferences.loadAutoDetect())

        preferences.saveAutoDetect(false)
        assertFalse(preferences.loadAutoDetect())

        preferences.saveAutoDetect(true)
        assertTrue(preferences.loadAutoDetect())
    }

    @Test
    fun testMetronomeBpmPersistence() {
        assertEquals(120, preferences.loadBpm())

        preferences.saveBpm(168)
        assertEquals(168, preferences.loadBpm())

        // Values clamped between 30 and 240
        preferences.saveBpm(10)
        assertEquals(30, preferences.loadBpm())

        preferences.saveBpm(300)
        assertEquals(240, preferences.loadBpm())
    }

    @Test
    fun testTimeSignatureAndAccentsPersistence() {
        val defaultSig = preferences.loadSignature()
        assertEquals(4, defaultSig.beats)
        assertEquals(4, defaultSig.unit)

        val customSig = TimeSignature(3, 4)
        preferences.saveSignature(customSig)
        assertEquals(3, preferences.loadSignature().beats)

        // Test custom beat accents persistence per beats count
        val custom34Accents = listOf(BeatAccent.ACCENT, BeatAccent.MUTE, BeatAccent.NORMAL)
        preferences.saveAccents(3, custom34Accents)
        val loadedAccents = preferences.loadAccents(3)

        assertEquals(3, loadedAccents.size)
        assertEquals(BeatAccent.ACCENT, loadedAccents[0])
        assertEquals(BeatAccent.MUTE, loadedAccents[1])
        assertEquals(BeatAccent.NORMAL, loadedAccents[2])
    }

    @Test
    fun testDestinationPersistence() {
        assertEquals("tuner", preferences.loadLastDestination())

        preferences.saveLastDestination("metronome")
        assertEquals("metronome", preferences.loadLastDestination())
    }
}

/**
 * Lightweight in-memory fake SharedPreferences for JVM unit testing.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(data)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val target: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val staging = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) staging[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) staging[key] = values
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) staging[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) staging[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) staging[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) staging[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) staging[key] = this
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearRequested) target.clear()
            for ((k, v) in staging) {
                if (v === this) target.remove(k) else target[k] = v
            }
        }
    }
}
