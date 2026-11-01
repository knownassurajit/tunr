# Tunr

Minimalist Android instrument tuner and metronome (`com.knownassurajit.tunr_tool.app`). 100% offline: microphone audio is processed in RAM and never stored or uploaded.

## Layout

```text
.github/workflows/   CI, signed Play release, listing sync, GitHub Pages
app/                 Android application module
docker + compose     Local CI image (JDK 17 + Android SDK 36)
fastlane/            Play Console metadata and upload lanes
keystore/            Local release JKS only (gitignored except README)
store/               Play questionnaire, what's new, marketing art
docs/                Privacy policy (GitHub Pages) + APP_STATE.md / RESEARCH.md engineering notes
```

## Development

```bash
./gradlew testDebugUnitTest assembleDebug
```

Containerized equivalent:

```bash
docker compose run --rm ci
```

## Production bundle

Copy `keystore.properties.example` to `keystore.properties`, or export `TUNR_KEYSTORE_*` and `TUNR_VERSION_*`. Then:

```bash
./gradlew bundleRelease
# or
docker compose run --rm release
```

## GitHub Actions

| Workflow | Trigger | Purpose |
| --- | --- | --- |
| `CI` | pull requests and `main` | unit tests, lint, debug APK |
| `Release` | `v*.*.*` tags or manual | signed AAB, GitHub Release, Play upload |
| `Play store listing` | manual | Fastlane metadata/graphics sync |
| `GitHub Pages` | `docs/` on `main` | privacy policy at GitHub Pages |

Required repository secrets for release (Settings → Secrets and variables → Actions):

- `TUNR_KEYSTORE_BASE64`
- `TUNR_KEYSTORE_PASSWORD`
- `TUNR_KEY_ALIAS`
- `TUNR_KEY_PASSWORD`
- `PLAY_SERVICE_ACCOUNT_JSON`

Create GitHub Environments named `internal`, `alpha`, `beta`, and `production` so the release workflow can target Play tracks.

Play Developer API: create a Google Cloud service account, enable the Android Publisher API, and invite the account as a Play Console user with Release to production / Release to testing.

## Privacy

Public policy: https://knownassurajit.github.io/tunr/
