# Disha Voice

**Early accessibility research prototype · Android · Hindi-first concept**

Disha Voice explores whether a phone can offer concise Hindi descriptions of selected objects visible in its camera image, while reducing repetitive announcements and being explicit about uncertainty.

> **Context, stated up front.** Google ships two apps in this space and both are more
> capable than this one: **Lookout** (offline, direction and distance) and **Guided Vision
> in Gemini Live** (launched 1 October 2026 — conversational camera description, in Hindi,
> in India, integrated into TalkBack, and requiring a network connection). This project
> does not try to out-feature either. It is narrower on purpose: **nothing leaves the
> phone**, it **says less**, and it **publishes the measurements** that would show whether
> saying less is better. See
> [`RESEARCH/competitor-landscape.md`](RESEARCH/competitor-landscape.md).

> **Safety:** This project is not a mobility aid, navigation or collision-avoidance system, hazard detector, distance estimator, or path-clearance tool. It may miss or misidentify objects. Silence does not mean that nothing is present. Do not rely on it for independent travel, street crossing, stairs, or safety-critical decisions. Continue using established mobility aids, training, and practices.

| | |
|---|---|
| **Repository** | https://github.com/shubhsaxena2020/disha-voice |
| **Website** | https://shubhsaxena2020.github.io/disha-voice/ |
| **Pre-release (debug APK)** | https://github.com/shubhsaxena2020/disha-voice/releases/tag/v0.1.1-prototype |
| **Status** | Prototype. **Not release-ready** and **not tested on a physical device.** |
| **Licence** | Apache-2.0 (see [`LICENSE`](LICENSE), [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)) |

## What is verified today

- **Pure Kotlin domain core** — image-relative left/centre/right classification, confidence
  banding, and repeated-announcement suppression. **12 tests passing.**
- **Android app builds** — `:android:app:assembleDebug` produces a 30,671,369-byte APK
  (SHA-256 `b80108a1…f83c5`), minSdk 24 / targetSdk 36, requesting **only** the `CAMERA`
  permission, with **no** `INTERNET` permission. **16 tests passing.**
  The input-tensor convention is pinned by a unit test, so a silent change to the
  pre-processing cannot degrade detection without failing the build.
  The build is **payload-reproducible**: a CI build of the same commit has a
  byte-identical payload (all 546 ZIP entries, including `classes.dex`, the manifest and
  the bundled model). Only the debug signing certificate differs, because Gradle
  generates a per-machine debug keystore. CI asserts this on every run.
- **On-device detection works** — a bundled Apache-2.0 EfficientDet-Lite0 model
  (COCO-2017, 90 classes) detects real objects: a Labrador as *dog* at 0.840, a street bus
  as *bus* at 0.801 alongside three people, with correct image-relative zones. Desktop-CPU
  latency: 25.9–35.9 ms median over 7 repeats per image. **These are not phone numbers.**
- **Website** — static, no external requests, no JavaScript, deployed via GitHub Pages.
  Checked in CI by three scripts: a link check, a **WCAG 2.2 AA contrast audit that
  resolves the rendered result from the DOM and CSS** (152 rendered pairs, 0 failures),
  and a **real-browser layout audit at 390, 900 and 1440 px**. The layout audit was
  verified by deliberately breaking the site and confirming it failed.

## What is NOT verified

- **No physical Android device was available.** Camera behaviour, Hindi
  TextToSpeech on a handset, install/launch, TalkBack, thermal behaviour, and on-device
  latency are all unverified.
- The confidence thresholds (0.70 / 0.45) are **prototype defaults, not calibrated values.**
- Whether image-relative left/centre/right is genuinely *useful* to a blind user is an
  **open research question** — see issue #3.
- The artifact published for download is **debug-signed**, not a production release.
- **The competitive position is narrow.** Guided Vision in Gemini Live is a closer and far
  more capable competitor than this project, released nineteen days ago. The remaining
  ground is offline, non-conversational, Hindi-specific, and measured — see the competitor
  landscape.

`PROJECT_STATUS.md` tracks the full release gate, item by item, with what is still blocking.

## Repository layout

- `core/` — pure Kotlin domain policy, deliberately independent of Android APIs.
- `android/` — the Android application (CameraX, LiteRT, Compose, TextToSpeech).
- `website/` — dependency-free static companion site.
- `benchmarks/` — reproducible model inspection and CPU benchmark harness, with results.
- `RESEARCH/` — the research, published in full (see below).
- `TESTING/` — field-evaluation protocol (a plan; **no results collected yet**).
- `docs/` — setup, release, safety, privacy, and demo documentation.
- `scripts/` — the website checks that run in CI (links, contrast, layout).
- `PROJECT_STATUS.md` — acceptance-gate dashboard and verified blockers.

## The research, in full

All of this is published on the [website](https://shubhsaxena2020.github.io/disha-voice/)
under `docs/research/`, and is written to separate what was **measured** from what was
**inferred**, and to name what could not be verified.

| Document | What it settles |
|---|---|
| [`competitor-landscape.md`](RESEARCH/competitor-landscape.md) | Google Guided Vision (launched 2026-10-01) and Lookout, plus VisionAId, an offline-first research system. What is actually left to differentiate on. |
| [`skeptical-review-research.md`](RESEARCH/skeptical-review-research.md) | An adversarial pass on this project's own claims, including where they may be wrong. |
| [`safety-and-claims-review.md`](RESEARCH/safety-and-claims-review.md) | What may and may not be said, and the reasoning behind each restriction. |
| [`assistive-ux-requirements.md`](RESEARCH/assistive-ux-requirements.md) | The repetition problem, speech design rules, and a testable accessibility checklist. |
| [`model-and-data-audit.md`](RESEARCH/model-and-data-audit.md) | First-hand measurement of the shipped model file, its licence, and its real limits. |
| [`threat-model.md`](RESEARCH/threat-model.md) | What the app can be made to do to a user, and what the design prevents. |
| [`speech-hindi-audit.md`](RESEARCH/speech-hindi-audit.md) | Phrase-by-phrase wording, locale handling, and the no-Hindi-voice fallback. |
| [`source-register.md`](RESEARCH/source-register.md) | Every source consulted, with access dates, reliability ratings, and the retrievals that failed. |
| [`field-evaluation-protocol.md`](TESTING/field-evaluation-protocol.md) | A stationary, do-no-harm protocol for testing with blind and low-vision users. |
| [`differentiation-hypothesis.md`](RESEARCH/differentiation-hypothesis.md) | The falsifiable claims this project rests on, and the test for each. |
| [`open-source-license-audit.md`](RESEARCH/open-source-license-audit.md) | Licence status of the prior-art projects (partly unresolved). |
| [`benchmark-plan.md`](RESEARCH/benchmark-plan.md) | How the measurements will be made, so they can be reproduced. |

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

A connected device is required for camera, speech, install/launch, TalkBack, thermal, and performance checks. See `docs/DEVELOPMENT_SETUP.md` for the build environment, `TESTING/field-evaluation-protocol.md` for the field protocol, and `docs/DEMO_GUIDE.md` for demo steps.

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
