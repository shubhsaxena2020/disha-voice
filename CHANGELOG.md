# Changelog

All notable changes to this project are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this
project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html) with the
caveat that the project is pre-1.0 and currently a **prototype**.

## [0.1.1-prototype] - 2026-10-20

**Published pre-release:** https://github.com/shubhsaxena2020/disha-voice/releases/tag/v0.1.1-prototype

- SHA-256 `b80108a1196e6cc64ee641ab20fb63b79794e5c2e8ab969b3c452331805f83c5`, 30,671,369 bytes.
- **No user-visible change** from v0.1.0-prototype. Republished because the source changed in
  a way that matters for trust:
  - The input-tensor construction is now a testable unit (`InputTensorBuilder`), with **7 new
    unit tests** pinning the model's input convention: raw unsigned 0-255 channel values in
    R, G, B order, no float scaling, no mean/std subtraction, no requantisation. The
    convention was previously only assumed; it has now been **measured** against the real
    model on the real test images under three candidates, and raw uint8 is the correct one.
  - The test count is now **28 distinct tests**, 0 failures (was 21).
- CI now retries transient dependency-resolution failures (a shared-runner Maven Central 403
  took down a green build) and pins line endings and the executable bit via `.gitattributes`.
- The CI payload check now compares against this release.

## [Unreleased]

### Added
- `benchmarks/scripts/verify_preprocessing.py`: runs the bundled model on the real test
  images under three input conventions and reports what each produces. The Android app and
  the desktop benchmark both feed **raw uint8 RGB**; that they agree is reassuring but not
  proof, because if the model wanted a different convention both would be wrong together
  and every benchmark number would still look plausible. Measured: `raw` and `norm11`
  agree closely (this model's input quantisation maps the uint8 range onto [-1, 1]), while
  ImageNet mean/std normalisation is clearly worse (bus 0.801 -> 0.668, dog 0.840 -> 0.801).
  The app's convention is confirmed correct, and the check is now in CI.
- **A second-pass competitor finding, and the corrections it forced.**
  - **Google Guided Vision in Gemini Live** (launched **2026-10-01**) is a closer and far
    more capable competitor than Lookout: conversational camera description for blind and
    low-vision users, in Gemini's 40+ languages including Hindi, in India, on Android,
    trained with Aira on tens of thousands of hours, stress-tested by 1,000+ Aira Trusted
    Testers, and launched from the Google TalkBack menu. It requires a network connection.
  - **VisionAId** (arXiv 2607.02371) is an offline-first on-device Android assistant for
    people with visual impairment, in Romanian, with published measurements (mAP@50
    0.986; calibrated metric depth below 1 cm error within 3 m).
  - **Andha-Dhun** (arXiv 2607.06457) is the first dataset of human-authored Hindi audio
    descriptions, and finds that naive translation introduces artifacts and narrows
    diversity — which cites this project's decision to write original Hindi rather than
    translate English.
  - `RESEARCH/competitor-landscape.md` was substantially rewritten; the source register,
    the differentiation hypothesis, the skeptical review, the assistive-UX requirements, the
    README, `PROJECT_STATUS.md` and the website were all corrected to match.
  - Issues #3, #4 and #8 were updated with the prior art, which answers two of this
    project's open questions: Google's **reframing cue** ("pan slowly to the right, tilt
    downward, or step back") is prior art for the image-relative position problem, and
    shipping a feature **inside the TalkBack menu** is prior art for the audio collision.
  - The remaining ground is now stated precisely: **offline, non-conversational,
    Hindi-specific, and measured**.
- CI artefacts: the debug APK and the unit-test reports are uploaded from every run,
  the APK SHA-256 is recorded in the run summary, and a test-results table is printed.
- The app module's unit tests and an APK build now run in CI, and CI asserts the APK
  requests `CAMERA` and not `INTERNET`.
- `scripts/check-website-layout.py`: a real-browser layout audit at 390, 900 and
  1440 px, asserting one `h1`, heading order, landmarks, a resolvable skip link, no
  positive `tabindex`, accessible names, `alt` text, no horizontal overflow, no console
  errors, and that every in-page anchor resolves.
- `scripts/check-website-contrast.py`: a WCAG 2.2 AA audit that resolves the rendered
  result from the DOM and CSS, rather than approximating the cascade from stylesheet
  text. 152 rendered pairs checked.
- The website now publishes all 12 research documents under `docs/research/`, linked
  from a new "Read the working out" section. The published file count went from 8 to 20.

### Added
- `scripts/compare-apk-payload.py`: compares two APKs by payload, ignoring the
  per-machine debug certificate. CI now asserts that a fresh build has a
  byte-identical payload (all 546 ZIP entries) to the published pre-release.
  The previous whole-file SHA-256 comparison could never have passed across
  machines, because Gradle generates a different debug keystore on each one.

### Fixed
- `RESEARCH/feature-comparison.csv` was malformed: 9 of its 11 original rows had the wrong
  number of columns because of unquoted commas. It has been rebuilt with a proper CSV
  writer and now parses cleanly at 23 columns across 14 rows.
- The CI layout check no longer runs `apt-get update` via
  `playwright install --with-deps`, which failed the whole job when
  `packages.microsoft.com` returned 403. It now uses the system Chrome that
  GitHub's runners already ship, with a tested fallback chain.
- 17 text/background pairs were below WCAG AA. The worst was the "Read the full safety
  note" link inside the dark safety band, which rendered the site's green on the band's
  dark green at **1.94:1** - effectively invisible to a low-vision user.
- A 7 px horizontal scroll on mobile viewports, caused by a decorative ring in the hero
  illustration.
- The published test count said 30; it double-counted the app tests, which run under
  both the debug and release variants. The true count of distinct tests is 28.
- The README and PROJECT_STATUS claimed the build is "byte-identical" to the published
  APK. That holds only for a build on the same machine. The corrected claim is
  payload-identical across machines, with only the debug certificate differing - and
  CI now verifies it on every run.

### Changed
- The project's differentiation claim was corrected after verifying that Google Lookout
  already ships Hindi, already works offline, and already provides direction and
  distance. The website now states plainly that the project is not the first.



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

- `:core:test` and `:android:app:testDebugUnitTest` — **28 distinct tests, 0 failures**
  (12 in `:core`, 16 in `:android:app`; the app tests also run under the release variant).
- `:android:app:assembleDebug` — produces `app-debug.apk` (30,671,369 bytes,
  SHA-256 `b80108a1196e6cc64ee641ab20fb63b79794e5c2e8ab969b3c452331805f83c5`),
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
  28/28 tests, and produces a debug APK whose SHA-256 is identical to the published
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

[Unreleased]: https://github.com/shubhsaxena2020/disha-voice/compare/v0.1.1-prototype...HEAD
[0.1.0-prototype]: https://github.com/shubhsaxena2020/disha-voice/releases/tag/v0.1.1-prototype
