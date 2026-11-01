# Privacy Policy for Tunr

**Effective Date:** October 20, 2026  
**Last Updated:** October 21, 2026  
**Application:** Tunr (`com.knownassurajit.tunr_tool.app`)  
**Developer:** Surajit (@knownassurajit)

---

### 1. Introduction
Tunr is designed from the ground up with a strict **Privacy-First, Local-Only** architecture. We believe utility tools should respect user privacy unconditionally. Tunr does not track, store, profile, or sell any user information.

---

### 2. Information We Do NOT Collect
* **No Personal Data:** Tunr does not require an account, email address, name, phone number, or login credentials.
* **No Telemetry or Analytics:** We do not embed third-party analytics SDKs (e.g., Firebase Analytics, Mixpanel, Google Analytics).
* **No Advertising Identifiers:** Tunr contains zero advertising networks, tracking beacons, or ad IDs.
* **No Location Data:** We do not access GPS, network location, or IP-based geolocation.
* **No Filesystem Access:** We do not read or access personal files, media, contacts, or documents.

---

### 3. Permissions & Device Hardware Usage

#### A. Microphone Access (`RECORD_AUDIO`)
* **Purpose:** Tunr requires access to the microphone solely to analyze real-time acoustic sound waves emitted by your musical instruments (guitars, bass, ukulele, or chromatic sources).
* **Ephemeral Processing:** Incoming audio PCM buffers are analyzed in volatile system memory (RAM) using the McLeod Pitch Method (MPM) algorithm.
* **Zero Recording / Zero Persistence:** **Audio is never recorded, never written to flash/disk storage, never cached, and never transmitted over any network or cloud service.** Once the pitch frequency is calculated for the active frame, the audio buffer is immediately overwritten and discarded.

#### B. Vibration Access (`VIBRATE`)
* **Purpose:** Tunr triggers subtle, precise haptic motor clicks when an instrument string enters the exact in-tune zone (±1 to ±3 cents) and for metronome beat pulses.
* **Scope:** Used purely for physical sensory feedback. Can be toggled on or off at any time in Settings.

---

### 4. Network & Internet Usage
Tunr has **zero internet permissions** (`android.permission.INTERNET` is neither requested nor included in the manifest). Tunr operates 100% offline and is incapable of transmitting data outside your physical device.

---

### 5. Children's Privacy
Tunr is completely safe for musicians of all ages, including children under 13. We do not collect personal information from anyone.

---

### 6. Changes to This Privacy Policy
If any future update modifies how features operate, this document will be updated accordingly in the public repository.

---

### 7. Contact Us
If you have any questions or concerns regarding this Privacy Policy, please open an issue or reach out via:
* **Hosted policy (Play Console URL):** [https://knownassurajit.github.io/tunr/](https://knownassurajit.github.io/tunr/)
* **GitHub Repository:** [https://github.com/knownassurajit/tunr](https://github.com/knownassurajit/tunr)
* **Developer:** Surajit (@knownassurajit)
