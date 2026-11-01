# TUNR research notes (decided once; do not re-research)

## 1. Pitch detection

| Method | Low strings (E2, B0 = 30.9 Hz) | Octave errors | Latency | CPU | Noise | Verdict |
|---|---|---|---|---|---|---|
| YIN (de Cheveigné & Kawahara 2002) | Good; needs ≥2-3 periods | Few; absolute threshold is level-sensitive | 2-3 periods | O(N·τ) naive | Good | Solid fallback |
| **MPM / NSDF** (McLeod & Wyvill 2005) | Good; normalised, level-independent | Few; "key maximum" rule picks the first peak ≥ k·max | 2-3 periods | O(N log N) with FFT autocorrelation | Peak height is a built-in periodicity score | **Chosen** |
| pYIN (Mauch & Dixon 2014) | Good | Fewest (HMM over candidates) | Needs look-ahead / Viterbi smoothing | Several × YIN | Best | Overkill for one monophonic string; adds latency |
| CREPE (Kim et al. 2018) | Good | Fewest | 1024-sample frames + NN inference | Highest (≈22 M parameters, full model) | Best | Rejected: battery, APK size, no benefit for a single clean string |
| HPS / cepstrum | Poor: many harmonics + long FFT needed | Frequent (harmonic / sub-harmonic) | One long FFT | Low | Weak | Rejected |
| FFT peak + parabolic | Poor below ~100 Hz: 4096-pt @44.1k = 10.8 Hz/bin; picks the strongest partial, not f0 | n/a | 1 FFT | Low | Fair | Rejected |

**Chosen: MPM with FFT autocorrelation on a 4×-decimated stream.** Parameters (all in `PitchDetector`):
- High-pass DC blocker at **15 Hz** (the old 35 Hz corner attenuated B0 = 30.9 Hz); 4th-order Butterworth low-pass 3.5 kHz; decimate 44.1 → 11.025 kHz (every tunable fundamental ≤ 1.8 kHz survives; ~16× less work than full rate).
- Frame 1536 decimated samples (139 ms), hop 512 (46 ms, ~21 updates/s); lag range = fs/1800 … fs/25 (6 … 441). Linear autocorrelation through a 2048-pt FFT: 1536 + 441 ≤ 2048, so no wrap-around.
- NSDF(τ) = 2r(τ)/m(τ). Key maxima = highest sample of each positive lobe; pick the first with value ≥ **0.90**·max (McLeod suggests 0.8-1.0).
- Voicing: clarity ≥ **0.62** (0.48 with the harmonic filter off) AND RMS above the user gate (release at 0.38× once a note is sounding, so decays ring out).
- Precision: 3-point parabolic vertex of the first peak, then least-squares refinement through the later multiples of the period (P = Σk·τk/Σk²). Measured on synthetic strings: ≤ 0.06¢ from B0 to B5 (clean), ≤ 0.3¢ at ~31 dB SNR; a single frame at ~15 dB SNR on E2 can still be ~5¢ off, which the tracker's median/EMA absorbs.
- Octave safety: lobe-based key maximum handles most cases; weak-fundamental E2 (fundamental 10-15 dB below the strongest partial) resolves correctly in tests. In the tuner, a pinned string folds deviations into ±600¢ and the median-of-N discards isolated octave frames. Auto-detect matches strings in **cents** (log), not Hz.

## 2. Audio stack and smoothing
- **Oboe not adopted (yet).** Tuner latency is the analysis window (60-140 ms), not the device path (10-40 ms). `AudioRecord` blocking reads on a dedicated URGENT_AUDIO thread and `AudioTrack` with `PERFORMANCE_MODE_LOW_LATENCY` (API 26+) already use AAudio underneath. A native module adds NDK/CMake, APK size and device-quirk surface for no audible tuner gain. Revisit only for measured metronome-latency problems.
- Input source UNPROCESSED when supported (no AGC / noise suppression distorting level and periodicity), else VOICE_RECOGNITION, else MIC. 44.1 kHz mono PCM16, 2048-sample reads, hardware buffer ≥ 4 hops.
- No window function: NSDF is time-domain and level-normalised.
- Smoothing chosen over Kalman: median of 5/3/1 frames in log-frequency (Smooth/Standard/Fast) → EMA on cents (0.12/0.28/0.60 per frame, jumps > 25¢ follow at ≥0.65, small deltas dead-banded) → debounced lock (3 frames in, leave at 1.6× the window) so colour, haptics and chime do not chatter.

## 3. Metronome
- Render on a dedicated URGENT_AUDIO thread straight into `AudioTrack` (MODE_STREAM, 256-frame chunks = 5.8 ms). No Handler/Timer/coroutine delay participates in timing.
- Ticks are scheduled in absolute frames with exact integer arithmetic: tick k = base + ⌈k·sampleRate·60 / (bpm·subdivision)⌉. Zero cumulative drift at any tempo (tested over 60 s at 61-203 BPM); tempo, meter, accents and subdivision changes re-base at the next tick without a phase jump.
- Subdivisions 1-4 per beat; accent / normal / mute per beat; subdivision clicks at 0.55 gain.
- UI sync: beat events are released when `AudioTrack.playbackHeadPosition` passes their frame (audible time), not when written — the write leads the speaker by the buffer length.
- Tap tempo: median of up to 6 inter-tap intervals, restart after 2 s, clamp 30-240 BPM.
