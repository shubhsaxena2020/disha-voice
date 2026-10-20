# Disha Voice

**Early accessibility research prototype · Android · Hindi-first concept**

Disha Voice explores whether a phone can offer concise Hindi descriptions of selected objects visible in its camera image, while reducing repetitive announcements and being explicit about uncertainty.

> **Safety:** This project is not a mobility aid, navigation or collision-avoidance system, hazard detector, distance estimator, or path-clearance tool. It may miss or misidentify objects. Silence does not mean that nothing is present. Do not rely on it for independent travel, street crossing, stairs, or safety-critical decisions. Continue using established mobility aids, training, and practices.

## Current status

- The pure Kotlin core implements image-relative left/centre/right classification, confidence bands, and repeated-announcement suppression.
- The pure core currently has automated unit tests. See `PROJECT_STATUS.md` for the current verified count and work in progress.
- Android camera, the actual detector/model, Hindi TextToSpeech on a handset, and TalkBack behavior have not yet passed a real-device evaluation.
- The website is a static, local-only concept page in `website/` and marks its illustration as illustrative. No public site, APK, release, or remote repository is claimed here.
- Model selection is conditional on checking exact model provenance, object labels, distribution rights, and device behavior. A domain-policy test is not an object-detection test.

## Repository layout

- `core/` — pure Kotlin domain policy, deliberately independent of Android APIs.
- `android/` — Android application work (in progress).
- `website/` — dependency-free static companion site.
- `RESEARCH/` — sourced competitor, API, model, UX, privacy, and differentiation research.
- `TESTING/` — test protocol and results (results only after execution).
- `docs/` — setup, safety, accessibility, privacy, and release docs.
- `PROJECT_STATUS.md` — acceptance-gate dashboard and verified blockers.

## Build the core tests

Requirements: JDK 17 or newer for Gradle 8.14.3. The Android SDK is not needed for the pure `core` tests.

Windows PowerShell:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :core:test
```

macOS / Linux / WSL:

```sh
export JAVA_HOME=/path/to/jdk-17
./gradlew :core:test
```

## Android build

Android packaging needs JDK 17+, Android SDK Platform 36 and Android Build-Tools 36.0.0 (current local configuration). Once the app module is integrated:

```powershell
$env:JAVA_HOME = "C:\path\to\jdk-17"
$env:ANDROID_HOME = "C:\path\to\Android\Sdk"
.\gradlew.bat :android:app:assembleDebug
```

A connected device is required for camera, speech, install/launch, TalkBack, thermal, and performance checks. See `docs/DEVELOPMENT_SETUP.md` and `docs/TESTING.md` when available.

## Website preview

Run a local static server from the repository root:

```powershell
py -m http.server 5181 --bind 127.0.0.1
```

Then open `http://127.0.0.1:5181/website/`. The page currently uses only local HTML/CSS; its camera view is an illustration, not a live app capture or detection.

## Privacy intent vs verified behavior

The P0 intent is session-only, on-device processing with no account, analytics, microphone permission, or image upload. These are requirements until verified from the shipped APK, manifest, dependency graph, network inspection, and runtime tests. Do not represent intentions as audit findings. No user images or recordings are requested for development or testing.

## Contributing and responsible use

Please see `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, and `SECURITY.md`. Do not contribute claims that imply safe walking, guaranteed detection, medical certification, or unverified accuracy. Report security issues privately through GitHub's private vulnerability reporting only if it is enabled for the eventual repository; otherwise use the documented safe contact path.

## License

The project license will be selected after research and third-party/model license review. Until `LICENSE` and `THIRD_PARTY_NOTICES.md` are complete, treat this repository as all-rights-reserved; do not assume code, models, data, or assets are freely redistributable.
