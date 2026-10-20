# Changelog

All notable changes to this project are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) with the
caveat that the project is pre-1.0 and currently a **prototype**.

## [Unreleased]

Nothing yet.

## [0.1.0-prototype] — 2026-10-20

First working prototype. **This is a prototype, not a release.** No device testing, no
signed release, and no store distribution have been performed.

### Added

- **Pure Kotlin domain core** (`core/`) — image-relative zone classification
  (left/centre/right), two-threshold confidence banding, and repeated-announcement
  suppression keyed on (label, zone). No Android dependencies, so it is unit-testable on
  a desktop JVM.
- **Android application** (`android/app/`) — Kotlin + Jetpack Compose + CameraX + LiteRT
  + Android TextToSpeech.
  - Lifecycle-aware CameraX analysis with `STRATEGY_KEEP_ONLY_LATEST` backpressure;
    frames are converted and released immediately and are never written to disk.
  - On-device object detection using a **bundled** EfficientDet-Lite0 model
    (Apache-2.0, COCO-2017, 90 classes, 4.4 MB) — no network, no Play Services model
    download.
  - Hindi speech output via `TextToSpeech` with `hi-IN`, and an explicit spoken notice
    plus English fallback when the device has no usable Hindi voice.
  - Accessible Compose UI: one large start/stop control, live-region status line, a
    repeat-last-message action, help action, and a permanent safety note.
- **Companion website** (`website/`) — static, dependency-free, no external requests.
  Its camera view is explicitly labelled as an illustration, not a live detection.
- **Reproducible benchmark harness** (`benchmarks/`) — model inspection and a repeatable
  detection benchmark that writes machine-readable JSON.
- **Environment check script** (`scripts/check-environment.ps1`) and a full
  `docs/DEVELOPMENT_SETUP.md`.
- Documentation: privacy policy, safety statement, demo guide, release process, model
  and data audit, benchmark plan, field-evaluation protocol.
- Repository governance: `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`,
  `SUPPORT.md`, issue and pull-request templates, and CI workflows.

### Verified

- `:core:test` and `:android:app:testDebugUnitTest` — **21 distinct tests, 0 failures**
  (12 in `:core`, 9 in `:android:app`; the app tests also run under the release variant).
- `:android:app:assembleDebug` — produces `app-debug.apk` (30,671,369 bytes,
  SHA-256 `96bab1f33062922add352ea4709bac080bd3f2266f19d582e53928346f3dc7b0`),
  minSdk 24 / targetSdk 36.
- APK inspected independently: **CAMERA permission only, no `INTERNET` permission**,
  `allowBackup=false`, `usesCleartextTraffic=false`, model present in `assets/`.
- Detector validated on real images (dog, bus, people) with correct labels and
  image-relative zones.
- Website audited with a headless browser: one `h1`, correct heading order, all
  landmarks, skip link, no unlabelled controls, no positive tabindex, reduced-motion
  supported, no console errors, no horizontal overflow, all colour pairs pass WCAG AA.
- **Published** as a public GitHub repository with branch protection, three milestones and
  ten tracked issues; the website is deployed and verified live at
  <https://shubhsaxena2020.github.io/disha-voice/>.
- **Clean-checkout reproduction:** a fresh clone of the published repository builds, passes
  21/21 tests, and produces a debug APK whose SHA-256 is identical to the published
  artifact.

### Known limitations

- **No real-device verification.** Camera behaviour, Hindi TTS on a handset, install and
  launch, TalkBack, thermal behaviour, and on-device latency are all unverified — no
  Android device was available.
- **Confidence thresholds are uncalibrated.** 0.70 and 0.45 are prototype defaults, not
  values derived from measured data.
- **Desktop-CPU latency only.** 25.9–35.9 ms median on the development machine; this is
  not a phone number.
- **Image-relative position is unvalidated as useful.** Whether "left/centre/right in the
  image" helps a blind user without egocentric grounding is an open research question.
- **A debug-signed APK only.** No release keystore has been configured.

[Unreleased]: https://github.com/shubhsaxena2020/disha-voice/compare/v0.1.0-prototype...HEAD
[0.1.0-prototype]: https://github.com/shubhsaxena2020/disha-voice/releases/tag/v0.1.0-prototype
