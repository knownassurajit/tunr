# Google Play Console Enlistment Guide & Store Listing Kit

This document provides all verified assets, exact character-counted text copy, and step-by-step instructions to enlist **Tunr** on the Google Play Console.

---

## 1. App Identity & Basic Details

| Field | Value | Constraints |
| :--- | :--- | :--- |
| **App Name** | `Tunr - Minimalist Tuner` | 23 / 30 characters |
| **Short Description** | `Ultra-precise instrument tuner and metronome. AMOLED matte UI with zero latency.` | 78 / 80 characters |
| **Package Name** | `com.knownassurajit.tunr_tool.app` | Unique Application ID |
| **Application Type** | App | App / Game |
| **Primary Category** | `Music & Audio` | Standard Play Category |
| **Tags** | `Tuner`, `Guitar Tuner`, `Metronome`, `Audio`, `Music` | Max 5 tags |

---

## 2. Full Store Description

Copy and paste the text block below directly into the **Full description** field (up to 4000 characters):

```text
Tunr is a focused, distraction-free instrument tuner and metronome built entirely with modern Jetpack Compose and Material 3 Expressive. Engineered for acoustic guitars, electric guitars, bass, ukuleles, and chromatic instruments, Tunr pairs sub-cent pitch detection with a pure AMOLED matte aesthetic.

KEY FEATURES

• Sub-Cent MPM Precision
Powered by the McLeod Pitch Method (MPM) with Normalized Square Difference Function (NSDF) analysis. Tunr delivers instantaneous pitch detection with sub-millihertz frequency accuracy and zero octave jumping.

• Dual-Precision Tuning Modes
Switch effortlessly between Standard Mode (±3¢ threshold for fast onstage tuning) and High-Precision Mode (±1¢ threshold with 0.1¢ granular telemetry for studio-grade intonation setup).

• Integrated Tactile Metronome
Practice with a high-accuracy, zero-latency synthesized audio metronome. Features 30 to 280 BPM range, standard time signatures (2/4, 3/4, 4/4, 6/8), tap-tempo, rhythmic subdivisions, and synchronized visual beat indicators.

• Curated Instrument Tunings & Silhouettes
Includes bespoke, Pinterest-inspired vector silhouettes and preset tunings:
- Acoustic Guitar (Standard, Drop D, DADGAD, Open D)
- Electric Guitar (Standard, Drop C, Half Step Down)
- Bass Guitar (4-String Standard, 5-String Standard, Drop D)
- Ukulele (Standard Soprano/Concert/Tenor GCEA, Baritone DGBE)
- Full Chromatic Mode (C0 to B8 auto-detection)

• Pitch Reference Calibration
Calibrate concert pitch A4 continuously from 432 Hz to 446 Hz (default standard 440 Hz).

• Pure AMOLED Matte UI
Designed specifically for OLED and AMOLED displays with a true-black (#060709) background, subtle monochromatic slate tones, and gentle emerald & sky-blue in-tune visual cues that eliminate battery drain and stage glare.

• In-Tune Haptic Clicks
Subtle haptic pulses notify you when your instrument hits exact pitch without needing to stare at the screen.

STRICT PRIVACY PLEDGE
Tunr is built with a zero-compromise privacy architecture:
• 100% Offline: Tunr does not request internet access (`android.permission.INTERNET` is omitted).
• Pure RAM Audio Processing: Microphone audio buffers are analyzed exclusively in volatile RAM and immediately discarded.
• Zero Recordings: Audio is NEVER recorded, saved to disk, or sent to external servers.
• Zero Trackers: No ads, no analytics SDKs, and no account requirements.

Permissions Used:
• RECORD_AUDIO: Required exclusively for live microphone pitch detection.
• VIBRATE: Required for in-tune and metronome tactile pulses.

Tune cleanly. Play in time. Without clutter.
```

---

## 3. Play Store Graphical Assets Inventory

All graphics comply with the latest Google Play Store specifications:

| Asset Name | Resolution / Specs | Local File Path | Play Store Requirements |
| :--- | :--- | :--- | :--- |
| **High-Res App Icon** | 512 x 512 px, 32-bit PNG | [`fastlane/metadata/android/en-US/images/icon.png`](../fastlane/metadata/android/en-US/images/icon.png) | 512x512, max 1024 KB, RGBA |
| **Feature Graphic** | 1024 x 500 px, 24-bit PNG | [`fastlane/metadata/android/en-US/images/featureGraphic.png`](../fastlane/metadata/android/en-US/images/featureGraphic.png) | 1024x500, max 15 MB, RGB/RGBA |
| **Phone Screenshot 1** | 1080 x 2400 px, PNG | [`fastlane/.../phoneScreenshots/1.png`](../fastlane/metadata/android/en-US/images/phoneScreenshots/1.png) | Tuner HUD & String Matrix |
| **Phone Screenshot 2** | 1080 x 2400 px, PNG | [`fastlane/.../phoneScreenshots/2.png`](../fastlane/metadata/android/en-US/images/phoneScreenshots/2.png) | Custom Instrument Silhouettes |
| **Phone Screenshot 3** | 1080 x 2400 px, PNG | [`fastlane/.../phoneScreenshots/3.png`](../fastlane/metadata/android/en-US/images/phoneScreenshots/3.png) | High-Precision & A4 Calibration |
| **Phone Screenshot 4** | 1080 x 2400 px, PNG | [`fastlane/.../phoneScreenshots/4.png`](../fastlane/metadata/android/en-US/images/phoneScreenshots/4.png) | Minimalist Low-Latency Metronome |

---

## 4. Release Android App Bundle (AAB)

* **Generated Bundle:** CI artifact from `.github/workflows/release.yml` (`./gradlew bundleRelease`)
* **Version Code:** `TUNR_VERSION_CODE` (GitHub `run_number` in CI; local default `1`)
* **Version Name:** `TUNR_VERSION_NAME` (git tag without `v`; local default `1.0.0`)
* **Min SDK:** 24 (Android 7.0 Nougat)
* **Target / Compile SDK:** 36 (Android 16 preview / latest Android releases)

---

## 5. Play Console Questionnaire Answers

### A. Privacy Policy URL
* URL: `https://knownassurajit.github.io/tunr/` (GitHub Pages; markdown copy lives in `PRIVACY_POLICY.md`).

### B. App Access
* Select: **"All functionality is available without special access"** (No credentials or subscription required).

### C. Ads
* Select: **"No, my app does not contain ads"**.

### D. Content Rating (IARC)
* Category: **Consumer / Utility / Audio Tool**
* Violence, sexual content, profanity, drugs, gambling: **NO to all**
* Location sharing or physical contact: **NO**
* Digital purchases: **NO**
* Resulting Rating: **PEGI 3 / ESRB Everyone / USK 0**

### E. Target Audience and Content
* Target age groups: **13-15, 16-17, 18 and over** (utility app suitable for all musicians).
* Appeal to children: **No**.

### F. Data Safety Declaration
* **Does your app collect or share any of the required user data types?**  
  Select: **"No"**
* **Audio Data:**  
  Because microphone audio is processed strictly ephemerally in RAM and never saved or transmitted, check **"No audio data collected or shared"** or declare ephemeral processing under Audio if prompted.

### G. Government Apps / Financial Features / Health Apps
* Select: **"No"** to all.

---

## 6. Step-by-Step Enlistment Checklist

1. **Sign in to Google Play Console:**
   - Go to [play.google.com/console](https://play.google.com/console).
2. **Create App:**
   - App name: `Tunr - Minimalist Tuner`
   - Default language: English (United States)
   - App or game: App
   - Free or paid: Free
   - Accept Declarations & Create.
3. **Set Up Store Listing:**
   - Navigate to **Grow** -> **Store presence** -> **Main store listing**.
   - Paste Short description and Full description from Section 2.
   - Upload `app_icon_512.png` to **App icon**.
   - Upload `feature_graphic_1024x500.png` to **Feature graphic**.
   - Upload the 4 screenshots in `screenshots/` under **Phone screenshots**.
   - Save changes.
4. **Complete Policy & App Content:**
   - Navigate to **Policy** -> **App content**.
   - Fill out Privacy policy, Ads, App access, Content ratings, Target audience, and Data safety using the pre-filled answers in Section 5.
5. **Create & Roll Out Release:**
   - Navigate to **Release** -> **Production** (or **Testing** -> **Internal testing** / **Closed testing**).
   - Click **Create new release**.
   - Upload the signed AAB from GitHub Actions (`tunr-release-aab` artifact) or `app/build/outputs/bundle/release/`.
   - Enter Release notes:
     ```text
     Initial release of Tunr v1.0.
     - McLeod Pitch Method (MPM) sub-cent precision tuning engine
     - High-Precision mode (±1¢) and Standard mode (±3¢)
     - AMOLED true-black minimalist interface with custom instrument silhouettes
     - Integrated zero-latency synthesized metronome
     - 100% offline, zero audio recording storage
     ```
   - Click **Review release** and **Start rollout**.
