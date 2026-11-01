# TUNR — APP_STATE (read first; open only the files listed for your task)

Package `com.knownassurajit.tunr_tool.app` (`app/src/main/java/...`). Single module `:app`, minSdk 24, target/compile 36, Kotlin 2.3.20, Compose BOM 2026.03.01 + material3 1.5.0-beta01 (latest is beta02; not bumped without device QA). Build/verify: `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug` (needs Android SDK; in sandboxes add a Maven-Central mirror, Central returns HTTP 429). Compose reports: `-PcomposeMetrics`. Tokens/DSP rationale: `docs/RESEARCH.md`. NB: `docs/` is deployed to GitHub Pages (pages.yml) — these notes become public.

## 1. Architecture & modules
MVVM, one `TunrViewModel` → immutable `StateFlow`s. `audio/` and `model/` are Android-free (JVM-tested). Audio threads: `tunr-audio-in` (mic → `PitchDetector` → `TunerPipeline`), `tunr-metronome` (`MetronomeRenderer` → `AudioTrack`). UI never touches DSP.
`audio/`: Fft, PitchDetector, TunerPipeline, AudioEngine, MetronomeRenderer, ClickBank, MetronomeEngine, TapTempo, InTuneSoundPlayer · `model/`: Model (settings/enums/states), Notes, TuningModels, TuningCatalog, TuningLibrary · `data/`: TunrPreferences (SharedPreferences: settings, bpm, ids), TuningLibraryRepository (DataStore: custom/favorites/recents) · `theme/`: Color, Theme, Shape, Type, Motion · `ui/`.
Assets: `app/src/main/assets/tuning_catalog.json`; `app/src/main/baseline-prof.txt` (hand-written wildcards).

## 2. Screen map (route › composable › file › state)
- Tuner › `TunerScreen` › ui/tuner/TunerScreen.kt › `TunerState` (vm.tuner)
- Metronome › `MetronomeScreen` › ui/metronome/MetronomeScreen.kt › `MetronomeState` (vm.metronome)
- Tuning library sheet › `TuningLibrarySheet` › ui/sheets/TuningLibrarySheet.kt › `LibraryUi` (vm.library) + `LibraryActions`
- Custom tuning editor sheet › `CustomTuningEditor` › ui/sheets/CustomTuningEditor.kt › local state, saves via vm
- Calibration & settings sheet › `SettingsSheet` › ui/sheets/SettingsSheet.kt › `SettingsState` (vm.settings)
- Shell › `TunrApp` (nav bar/rail by window width ≥600dp, sheet routing, lifecycle, dim/timeout) › ui/TunrApp.kt. Shell observes only `activeTuning`, `hearingPitch`, `metronomePlaying`, `settings`; 21 Hz state is collected inside the destination.

## 3. Instrument/tuning data model
Catalog JSON: Category › Instrument › Variant › Tuning; strings = scientific pitch (`"Eb2"`, `"F#3"`) listed thickest→thinnest (number = count−index). Tuning id `<instrument>.<variant>.<tuning>` (`chromatic` is bare). 103 tunings: fretted (guitar acoustic/classical/electric 6-7-8/12-string, bass 4-5-6, ukulele S/C/T/B, mandolin family, banjo, bouzouki, cavaquinho, balalaika), bowed (violin, viola, cello, double bass), chromatic. Unison courses are listed once. `TuningCatalog.parse` validates (notes, range A0–G6, ≤12 strings, unique ids); `search` AND-matches tokens (♯/♭ ≡ #/b). `LegacyIds` migrates pre-library saved ids.
Custom: `CustomTuning(id,name,strings[CustomString(note,octave,cents ±50)])`; `TuningValidation` (name ≤40 & unique, 1–12 strings, range, cents). `LibraryOps` pure transforms (upsert/duplicate/delete/move/favorite/recents ≤8, ≤60 custom). `TuningExchange` JSON import/export (`tunr.custom-tunings` v1; invalid entries skipped, fresh ids). Persistence: `TuningLibraryRepository` + `DataStoreLibraryStorage` (one JSON under `library_v1`), early edits replayed after load, corrupt file → empty.

## 4. DSP pipeline + parameters
44.1 kHz mono PCM16, source UNPROCESSED→VOICE_RECOGNITION→MIC, 2048-sample reads (46 ms hop). `PitchDetector`: DC-block HP 15 Hz → 4th-order Butterworth LP 3.5 kHz → ÷4 (11.025 kHz) → frame 1536 → FFT(2048) autocorrelation → NSDF → lobe maxima, first ≥0.90·max, clarity ≥0.62 (0.48 harmonic filter off), RMS gate with release ×0.38 → parabolic peak, refined by least squares over later period multiples. Range 25–1800 Hz. Measured (synthetic): ≤0.06¢ B0–B5 clean; ≤0.3¢ at 31 dB SNR; −5¢ worst case at 15 dB SNR on E2 (single frame). `TunerPipeline`: median 5/3/1 (Smooth/Standard/Fast) in log-f → nearest string in cents (pinned string folds octave errors to ±600¢) / nearest semitone → EMA 0.12/0.28/0.60 (fast-follow α≥0.65 beyond 25¢, dead-band) → lock: 3 frames in, exit at 1.6× window; hold 4 frames across dropouts. Metronome: `MetronomeRenderer` exact Long-arithmetic tick schedule (no drift), 256-frame chunks, subdivisions 1–4 (sub gain 0.55), accents/mute, beat events released at `playbackHeadPosition`; `TapTempo` median of ≤6 intervals, reset >2 s.

## 5. Design tokens & motion
`MaterialExpressiveTheme` + `MotionScheme.expressive()` + `TunrShapes` (8/12/18/24/32dp; now applied). AMOLED black dark palettes (Emerald default, Azure, Monochrome, Amber, Violet, Custom hex). Springs: default/fast effects & spatial from the motion scheme; **needle** ζ0.70 k280, **dial rotation** ζ0.65 k180 (`TunrMotion`); morph Cookie9Sided→Circle on lock. Reduced motion: `LocalReducedMotion` (animator scale 0) → snap. In-tune = colour + shape morph + "In tune" text + haptic `Confirm` + chime. A11y: dial spoken description, meter `stateDescription` (not live), beat cells 48dp with per-beat labels, dial text pinned against font scale. Predictive back enabled in manifest.

## 6. Open issues / TODO
- Not verified on device/emulator (none in sandbox): UI visuals, real-mic latency, UNPROCESSED gain on weak-mic devices (raise noise-gate sensitivity if quiet).
- Light theme and Material You dynamic colour not implemented (AMOLED dark only).
- Strings are hard-coded (no `strings.xml` / i18n); `TunrTypography` and the Pill/Bento/Card shape tokens are still unused.
- Settings still on SharedPreferences (migrate via `SharedPreferencesMigration` → DataStore).
- Baseline profile is wildcard-only: record a real one with Macrobenchmark; measure cold start/frame time/battery on hardware.
- Metronome keeps playing in background without a foreground service (may be killed); mic stops on pause.
- Oboe deferred (see RESEARCH). Drag-to-reorder for custom tunings (currently Move up/down). material3 beta02 bump pending QA.

## 7. Changelog (latest 10)
1. DSP: FFT-based MPM on ÷4 stream, 25 Hz floor (B0), multiples refinement, zero-alloc; replaces O(N·τ) detector.
2. TunerPipeline: median+EMA, log-space string match, octave fold, debounced lock, dropout hold.
3. AudioEngine: dedicated URGENT_AUDIO thread, UNPROCESSED source, no per-block allocation; mic released on pause.
4. Metronome: exact-schedule `MetronomeRenderer`, audible-time beat events, subdivisions, LOW_LATENCY (API26+), `TapTempo`.
5. Tuning library: JSON catalog (103 tunings), search, favorites, recents, custom CRUD + reorder, import/export, DataStore.
6. UI: library sheet + custom editor replace PresetsSheet; subdivision toggle; compact-height scroll in metronome.
7. Design: shapes applied, spring-tuned needle/dial, reduced motion, TalkBack semantics, 48dp beat targets, window-size layout.
8. Perf: shell no longer recomposes at 21 Hz; Compose metrics switch; baseline-prof + profileinstaller.
9. Removed dead `TuningPreset/InstrumentType/TuningMode/MusicalNote`; subsonic filter 35→15 Hz.
10. Tests: 88 JVM tests (detector accuracy/noise/alloc, pipeline, end-to-end chain, renderer drift, catalog, library, tap tempo).
