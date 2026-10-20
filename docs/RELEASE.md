# Release process

This project currently ships a **debug** APK. A signed release is deliberately **not**
automated, because a release key is the operator's decision and a key that lives in a
repository is a key that leaks.

## Status

| Artifact | State |
|---|---|
| `app-debug.apk` (debug-signed) | Built and structurally verified |
| Signed release APK | **Not produced** — no release keystore has been configured |
| GitHub Release | **Not created** |
| Website deployment | **Not deployed** |

## 1. Build a debug APK (what CI does)

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:ANDROID_HOME = "C:\path\to\Android\Sdk"
.\gradlew.bat :android:app:assembleDebug
```

Output: `android/app/build/outputs/apk/debug/app-debug.apk`

Debug APKs are signed with the standard Android debug key. They install fine on a
device for testing, but they are **not** distributable as a release and must not be
presented as one.

## 2. Create a release keystore (one time, done by the operator)

The keystore and its passwords must never enter the repository.

```powershell
keytool -genkeypair -v `
  -keystore "$env:USERPROFILE\.disha-voice\release.jks" `
  -alias disha-voice `
  -keyalg RSA -keysize 4096 -validity 10000
```

Store the keystore and its passwords in a password manager or an OS keystore, not in the
repo and not in a chat message.

> Use a **separate** keystore from your Android Studio debug key, and consider Google's
> Play App Signing for Play distribution so that the upload key can be rotated.

## 3. Configure signing (outside version control)

Create `~/.gradle/gradle.properties` (user-level, never committed) or use environment
variables:

```properties
DISHA_UPLOAD_STORE_FILE=/absolute/path/to/release.jks
DISHA_UPLOAD_STORE_PASSWORD=...
DISHA_UPLOAD_KEY_ALIAS=disha-voice
DISHA_UPLOAD_KEY_PASSWORD=...
```

Then add to `android/app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(providers.gradleProperty("DISHA_UPLOAD_STORE_FILE").get())
        storePassword = providers.gradleProperty("DISHA_UPLOAD_STORE_PASSWORD").get()
        keyAlias = providers.gradleProperty("DISHA_UPLOAD_KEY_ALIAS").get()
        keyPassword = providers.gradleProperty("DISHA_UPLOAD_KEY_PASSWORD").get()
    }
}
```

and set `signingConfig = signingConfigs.getByName("release")` on the `release` build type.

Because those values come from Gradle properties rather than the build script, the
repository never contains a key or a password.

## 4. Build and verify the signed release

```powershell
.\gradlew.bat :android:app:assembleRelease
```

Verify the signature and record the checksum:

```powershell
$apk = "android\app\build\outputs\apk\release\app-release.apk"
& "$env:ANDROID_HOME\build-tools\36.0.0\apksigner.bat" verify --print-certs $apk
(Get-FileHash $apk -Algorithm SHA256).Hash.ToLower() | Out-File "SHA256SUMS.txt" -Encoding ascii
```

## 5. Publish

- Tag the release: `git tag -a v0.1.0 -m "..."` and push the tag.
- Create a GitHub Release and attach the APK plus `SHA256SUMS.txt`.
- Record in the release notes: version, build type, signing status, SHA-256, and the
  supported Android version range.
- Update the website's download section with the same facts — version, checksum,
  signing status, and the permissions the app requests.

## 6. Before calling anything a release

Run the gate in `PROJECT_STATUS.md`. In particular these cannot be skipped:

- the build works from a **clean checkout**;
- automated tests and CI pass;
- the APK installs and launches on the target Android version;
- camera, detection and speech are verified **on a real device**;
- no secrets are tracked in Git;
- the manifest permissions are justified and minimal;
- accessibility checks and a **manual TalkBack review** are documented;
- the website builds, its links are checked, and a deploy URL is claimed only after a
  successful deployment;
- licences, model rights, third-party notices, version, changelog and release hash are
  recorded;
- limitations and the non-mobility safety warning are prominent.

## 7. What a release must never claim

- that the app measures distance, detects hazards, or clears a path;
- that detection is "accurate" without a published, protocol-based evaluation;
- that it works "offline" unless it has been verified on a device with networking disabled;
- that it is "private" beyond what the audited implementation actually does;
- that it is a substitute for a cane, a guide dog, mobility training, or a trusted guide.
