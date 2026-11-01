# Release signing

Place `tunr-release.jks` here locally. The file is gitignored.

Create a keystore once:

```bash
keytool -genkeypair -v \
  -keystore keystore/tunr-release.jks \
  -alias tunr \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype JKS
```

Then copy `keystore.properties.example` to `keystore.properties` (also gitignored) or export:

- `TUNR_KEYSTORE_FILE=keystore/tunr-release.jks`
- `TUNR_KEYSTORE_PASSWORD`
- `TUNR_KEY_ALIAS`
- `TUNR_KEY_PASSWORD`

GitHub Actions expects the same values as repository secrets, plus `TUNR_KEYSTORE_BASE64` (`base64 -i keystore/tunr-release.jks`) and `PLAY_SERVICE_ACCOUNT_JSON`.
