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
(`AllowedAPKSigningKeys`) for F-Droid. If the key is ever rotated, update that value in the same
commit.

## Rotating the key

Rotation changes the app's identity:

* **Android 9+**: `apksigner rotate` with a lineage file lets a new key continue an existing
  install (`--lineage`), provided the new key is also distributed inside the APK.
* **Android 8.0 / 8.1** (our `minSdk` is 26): APK Signature Scheme v3 is unavailable, so those
  users must uninstall the old build first.
* **F-Droid / Play**: a new key needs new `AllowedAPKSigningKeys`; Play requires Play App Signing.

The current key was committed to this public repository up to version 1.1.0-beta.16, which means it
must be treated as **publicly known** and should be rotated before any store distribution.
