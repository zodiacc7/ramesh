# Release signing

Ramesh is signed with a single RSA 4096 key. Android refuses to install a release signed with a
different key over an existing one, so **this key must be used for every future release**.

The key is **not** in this repository. It lives in GitHub Actions secrets, and
`.github/workflows/release.yml` decodes it into the runner before building.

## Where the key material lives

| Secret | Contents |
| --- | --- |
| `SIGNING_KEYSTORE` | the `.p12` file, base64-encoded, no line breaks required |
| `SIGNING_STORE_PASSWORD` | store password |
| `SIGNING_KEY_ALIAS` | `ramesh` |
| `SIGNING_KEY_PASSWORD` | key password |

**The key was rotated on 2026-11-01.** The current certificate SHA-256 is:

```
C1:7B:EA:F6:B5:FC:6A:DA:59:9E:85:5D:6D:A8:00:B0:D9:C6:D0:75:CB:5F:71:C7:5A:88:3D:45:97:D2:A1:AC
```

The previous key had been public in this repository since `35d9657`, so it is **burned**: it stays in
the git history and must never sign another release. Keep it only as an archive copy.

`app/build.gradle.kts` reads those as environment variables (`SIGNING_STORE_FILE`,
`SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, `SIGNING_STORE_TYPE`).
The workflow writes `SIGNING_STORE_FILE` and `SIGNING_STORE_TYPE` after decoding the secret; the
other three come straight from secrets.

If `SIGNING_STORE_FILE` is missing the release build has **no** signing config, Gradle emits
`app-release-unsigned.apk`, and the workflow's `Verify APK is signed` step fails on purpose.

## Building a signed release locally

```bash
export SIGNING_STORE_FILE=$PWD/ramesh-release.p12   # your offline copy
export SIGNING_STORE_TYPE=PKCS12
export SIGNING_STORE_PASSWORD='…'
export SIGNING_KEY_ALIAS=ramesh
export SIGNING_KEY_PASSWORD='…'
./gradlew assembleRelease
"$ANDROID_HOME"/build-tools/<version>/apksigner verify --print-certs \
    app/build/outputs/apk/release/app-release.apk
```

Never commit the `.p12` or the passwords: `keystore/*.p12`, `keystore/*.jks` and
`keystore/keystore.properties` are listed in `.gitignore` for exactly this reason.

## Backups

Keep at least two offline copies of the `.p12` **and** both passwords (password manager, encrypted
USB stick, printed paper in a safe). Losing the key means no existing installation can ever be
updated again — every user would have to uninstall and reinstall.

## Key fingerprint

The APK signing certificate SHA-256 is recorded in `metadata/com.ramesh.sms.yml`
(`AllowedAPKSigningKeys`) for F-Droid, and it is updated in the same commit as any rotation. Check a
downloaded APK against it with:

```bash
apksigner verify --print-certs ramesh-<version>-universal.apk
```

## Rotating the key

### Rotation log

| Date | Release | Reason |
| --- | --- | --- |
| 2026-11-01 | 1.0.0 (first previews were 1.1.0-beta.18 and later) | The original key had been committed to this public repository. Wallet-app scans flagged builds signed with it, and anyone could sign an APK Android accepts as an update of Ramesh. |

That rotation was a clean break (no lineage file): every installation made before it has to be
uninstalled once, because Android refuses an update signed with a different key. Use
`apksigner rotate --lineage` next time if existing installs must be preserved — AGP has no lineage
support, so the APK has to be re-signed after Gradle.

Rotation changes the app's identity:

* **Android 9+**: `apksigner rotate` with a lineage file lets a new key continue an existing
  install (`--lineage`), provided the new key is also distributed inside the APK.
* **Android 8.0 / 8.1** (our `minSdk` is 26): APK Signature Scheme v3 is unavailable, so those
  users must uninstall the old build first.
* **F-Droid / Play**: a new key needs new `AllowedAPKSigningKeys`; Play requires Play App Signing.

The pre-release key was committed to this public repository, which means it must be treated as
**publicly known**. It is retired; `1.0.0` and every later build use the key described above.
