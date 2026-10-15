# Es Teh Evren — Android

Native Android client untuk kasir Es Teh Evren (paritas web POS).
Spek lengkap: [`android.md`](./android.md).

## Build lokal

```bash
./gradlew assembleDebug
# Release lokal (perlu keystore + password):
./gradlew assembleRelease -PESTEH_SIGNING_PASSWORD=...
```

`API_BASE_URL` default `https://esteh.evrenhouse.online`.
Override: `-PAPI_BASE_URL=http://10.0.2.2:3006` (emulator).

## Rilis (CI)

Semua build rilis lewat GitHub Actions — jangan build manual untuk distribusi.

```bash
./bump-version.sh patch   # atau minor | major
# → commit + tag vX.Y.Z + push → workflow Release jalan →
#    APK + checksums.txt terbit di GitHub Releases
```

Workflow `release.yml` juga bisa dipicu manual (workflow_dispatch) dengan
tag / draft / prerelease opsional.

## Signing

- Keystore rilis: `app/keystore/release.jks` (gitignored).
- CI memulihkannya dari secret **`ESTEH_KEYSTORE_BASE64`**,
  password dari **`ESTEH_SIGNING_PASSWORD`**.
- Tanpa secret tersebut, `assembleRelease` memakai config debug
  (tetap bisa build, tidak untuk distribusi).

## OTA update

Aplikasi cek `GET /repos/sanixmon/esteh-mobile/releases/latest`
via `UpdateChecker` (lihat `update/UpdateChecker.kt`):
tag terbaru > `versionName` → tawarkan unduh APK dari `browser_download_url`.
Syarat OTA jalan: setiap rilis wajib melampirkan file `*.apk`
(workflow release sudah melakukannya otomatis).
