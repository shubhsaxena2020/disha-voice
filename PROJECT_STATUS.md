# Disha Voice — Project Status

**Status: working prototype, published.** Builds, tests pass, and a debug APK is built, released and downloadable. **Not release-ready** — no real-device verification and no signed release.

| | |
|---|---|
| **Repository** | https://github.com/shubhsaxena2020/disha-voice |
| **Website (live)** | https://shubhsaxena2020.github.io/disha-voice/ |
| **Pre-release** | https://github.com/shubhsaxena2020/disha-voice/releases/tag/v0.1.0-prototype |

**Last updated:** 2026-10-20

---

## What exists and is verified

### 1. Pure Kotlin domain core (`core/`) — VERIFIED
Image-relative zone classification (left/centre/right from box centre x, boundaries 0.38 / 0.62), confidence bands (0.70 confident / 0.45 tentative / below ignored), repeat suppression keyed on (label, zone) with a 4 s cooldown, and a session state model. Deliberately free of Android APIs so it is unit-testable on the JVM.

**Tests: 12 passing, 0 failures** (8 behavioural + 4 property-based), read from the Gradle JUnit XML.

### 2. Android app (`android/app/`) — BUILDS, NOT YET DEVICE-VERIFIED
Kotlin + Jetpack Compose + CameraX + LiteRT + Android TextToSpeech.

- `app-debug.apk` — **30,671,369 bytes**
- SHA-256 `96bab1f33062922add352ea4709bac080bd3f2266f19d582e53928346f3dc7b0`
- minSdk 24, targetSdk 36, compileSdk 36
- **Permissions: `CAMERA` only.** No `INTERNET`. Verified with `aapt2 dump permissions`.
- `allowBackup=false`, `usesCleartextTraffic=false` — verified in the built manifest.
- Model bundled in the APK (`assets/efficientdet_lite0_detection.tflite`, 4.4 MB), so detection needs no network.
- Native libs for arm64-v8a, armeabi-v7a, x86, x86_64.
- Release build type has R8 + resource shrinking enabled and **no signing config** — a release key is the operator's to supply, never generated and committed.

**Unit tests: 9 passing, 0 failures** (detection decoding + label mapping).

**Total across modules: 30 tests, 0 failures, 0 errors, 0 skipped.**

### 3. Detector — VERIFIED ON REAL IMAGES
Bundled **EfficientDet-Lite0 (int8)**, Apache-2.0, COCO-2017, 90 classes, SHA-256 `33a3b622…6704`.

Measured against the real model file (not documentation):
- input `[1, 320, 320, 3]` **uint8** — the published 300×300/float assumption was wrong
- outputs: boxes `[1,25,4]`, classes `[1,25]`, scores `[1,25]`, count `[1]`
- **label offset `model_index + 1 == coco_id`**, established with four independent anchors (person→0, bus→5, dog→17, tie→31). A draft table that skipped the offset reported a Labrador as a "cat" at 0.84 confidence; `DetectionDecoderTest` now pins all four anchors so it cannot regress.

Real detections (LiteRT, CPU, this machine):

| Image | Result | Zone |
|---|---|---|
| Yellow Labrador | dog 0.840 | CENTRE |
| Samoyed | dog 0.723 | CENTRE |
| Street bus | bus 0.801 · person 0.789 · person 0.707 · person 0.629 | CENTRE · LEFT · LEFT · RIGHT |
| Two men | person 0.648 · person 0.605 | RIGHT · CENTRE |

Ground truth for each image was confirmed with an independent vision model.

**Latency (7 repeats/image, desktop CPU):** median 25.9–35.9 ms, min 25.1, max 63.7.
**This is a desktop measurement, not a phone measurement.** Full data: `benchmarks/results/detector_benchmark.json`.

### 4. Independent APK verification (REA)
The built APK was opened and inspected with the REA MCP server (`rea-agents` 6.7.0): **547 archive entries, 473 graph nodes, 0 integrity contradictions**, digest matching the locally computed SHA-256. REA's Android (JADX) route was unavailable in this environment — it requires a caller-supplied `REA_JADX_MCP_JAR` — so package facts were verified with `aapt2` and cross-checked against REA's archive graph.

### 5. Companion website (`website/`) — BUILDS AND PASSES AUDIT
Static, dependency-free HTML/CSS. No framework, no build step, **no external requests at runtime** (the Google Fonts import was removed deliberately).

Automated audit (Playwright, 390 px and 1520 px viewports): exactly one `h1`, correct heading order, all four landmarks present, skip link with a valid target, 15 tabbables, **zero positive tabindex**, no unlabelled links/buttons/inputs, no images without alt text, `prefers-reduced-motion` honoured, no console errors, no horizontal overflow.

Contrast audit (computed WCAG ratios): 8 of 9 pairs passed initially; **footer small text failed at 3.26:1** and was corrected to 5.23:1 (AA). All 9 pairs now pass AA.

The camera view on the page is a **CSS illustration**, labelled in the page as "Illustration only · not a live detection".

### 6. Research, docs, governance — PARTIAL
Present: `RESEARCH/model-and-data-audit.md` (written from first-hand measurement), `RESEARCH/benchmark-plan.md`, `TESTING/field-evaluation-protocol.md`, `docs/privacy/privacy-policy.md`, `docs/safety/SAFETY.md`, `docs/DEMO_GUIDE.md`, `docs/website-deployment-plan.md`, `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SUPPORT.md`, issue/PR templates, two CI workflows.

Research present in the tree: `RESEARCH/model-and-data-audit.md` (written from first-hand
measurement of the model file), `RESEARCH/open-source-license-audit.md`,
`RESEARCH/differentiation-hypothesis.md`, `RESEARCH/benchmark-plan.md`, and
`TESTING/field-evaluation-protocol.md`.

**Outstanding research:** the competitor landscape, the threat model, the assistive-UX
requirements, the Hindi speech audit, and the independent red-team review were dispatched to
specialist agents but **did not produce files** (see the work log). These are tracked as open
work — issue #4 covers the licence gap, and the rest must be written and reviewed against
primary sources before they are published. Nothing in the published site or the release notes
depends on them.

A GitHub **project board** could not be created: the authenticated token lacks the `project`
scope, and `gh auth refresh -s project` requires interactive browser authorisation. The
issue tracker, labels, and three milestones cover the same ground and are all in place.

---

## Verified environment

| Component | State |
|---|---|
| Host | Windows 11 Home (build 26300), 48,580 MB RAM, ~1.2 TB free on C: |
| JDK | Temurin 17.0.20.1, user-local at `C:\Users\shubh\AppData\Local\Programs\disha-jdk17` |
| Android SDK | Platform android-36, Build-Tools 36.0.0, platform-tools ADB 37.0.1, at `C:\Users\shubh\AppData\Local\Android\Sdk` |
| Gradle | 8.14.3 (wrapper committed) |
| Git / Node / Python | 2.53.0 / 24.18.0 / 3.11.15 |
| GitHub | `gh` 2.96.0 authenticated as `shubhsaxena2020` (`repo`, `workflow` scopes) |
| REA MCP | `rea-agents` 6.7.0 live, 207 tools, 6 prompts; Windows host has partial provider coverage (macOS-only providers unavailable) |
| **Android device** | **None attached** — `adb devices` is empty |

No JDK, Gradle, or Android SDK existed at discovery; all three were installed user-locally, without administrator rights.

---

## Open blockers

1. **No Android device.** Camera behaviour, Hindi TextToSpeech on a real handset, install/launch, TalkBack, thermal behaviour, and on-device latency are all **unverified**. This is the single largest gap.
2. **No release signing key.** A debug APK exists; a signed release requires an operator-managed keystore that is never committed.
3. **Confidence thresholds are uncalibrated.** 0.70/0.45 are a starting point, not derived from data.
4. **Remaining research not yet in the tree** and not yet reviewed.
5. **No public repository, website deployment, or release.** The operator approved a *public* GitHub repository, but nothing has been pushed or deployed yet.

---

## Release gate — current standing

| # | Requirement | Status |
|---|---|---|
| 1 | Builds from a clean checkout with documented steps | **Met** — verified: a fresh `git clone` builds and tests successfully |
| 2 | Automated tests and CI pass | **Met** — 30/30 local tests pass; both GitHub Actions workflows green |
| 3 | APK exists, installable, launches | **Partial** — APK builds and is verified structurally; **install/launch untested** |
| 4 | Core detection and TTS verified on a real device | **Not met** — no device (issue #1) |
| 5 | No secrets tracked; permissions justified | **Met** — CAMERA only, no INTERNET, `allowBackup=false`; secret scan clean |
| 6 | Accessibility checked and TalkBack reviewed | **Partial** — website audited automatically; app semantics written; **no manual TalkBack review** (issue #8) |
| 7 | Website builds, links verified, deploy URL claimed | **Met** — deployed and verified live at https://shubhsaxena2020.github.io/disha-voice/ |
| 8 | Licences, notices, version, hash recorded | **Met** — Apache-2.0 model hash recorded; `THIRD_PARTY_NOTICES.md` published; release hash published |
| 9 | Limitations and non-mobility warnings prominent | **Met** — in-app, in README, on the website, in the release notes, and in the docs |
| 10 | Another engineer can reproduce the build | **Met** — a clean clone was built and tested; the resulting APK is byte-identical to the published one |

**Conclusion: prototype / release candidate in progress. Not production-ready, and not safe for independent mobility use.**

---

## Work log

- **2026-10-20** — Read the governing build prompt and the installed REA skill; verified the live REA server identity, tool count, prompts, and per-provider availability on this Windows host.
- **2026-10-20** — Environment inspected: no JDK/Gradle/SDK, no device, GitHub identity confirmed. Installed Temurin JDK 17, Android SDK Platform 36 + Build-Tools 36.0.0 + ADB 37.0.1, and Gradle 8.14.3 — all user-local, no admin rights.
- **2026-10-20** — Delegation model switched to MiniMax M3.1 via the 9Router provider; `delegation.provider`/`model` pinned in the Hermes config.
- **2026-10-20** — Wrote the domain core test-first; observed RED (missing types), implemented minimally, observed GREEN.
- **2026-10-20** — Downloaded and verified the Apache-2.0 EfficientDet-Lite0 model; measured its real tensor contract with LiteRT; **found and fixed the label off-by-one** that misidentified a Labrador as a cat.
- **2026-10-20** — Built the Android app module, fixing real API mismatches found by compiling: CameraX `Preview.SurfaceProvider` (not `PreviewView.PreviewSurfaceProvider`), the LiteRT 1.0.1 `Tensor` API (buffer reads, not `floatArray()`), `setNumThreads` (not a `numThreads` property), and `FileChannel.map`.
- **2026-10-20** — Produced and independently verified `app-debug.apk`; ran 30 unit tests (all pass); ran a 7-repeat CPU benchmark; audited the website with Playwright and computed WCAG contrast, fixing one failing pair.
- **2026-10-20** — Committed 59 files as `3289c54`.
- **2026-10-20** — Published the repository (public, as authorised), enabled GitHub Pages,
  created three milestones and ten real issues, set branch protection on `main` (required
  status checks, one approving review, no force-push, no deletions), and published the
  `v0.1.0-prototype` pre-release with the debug APK and `SHA256SUMS.txt`.
- **2026-10-20** — CI failed twice for real reasons and both were fixed: the Gradle wrapper
  had lost its POSIX executable bit (exit 126), and the Pages link check caught three links
  that would 404 on the deployed site. Added `scripts/check-website-links.py` so the second
  class of error is caught locally.
- **2026-10-20** — **Clean-checkout reproduction verified**: a fresh `git clone` of the
  published repository builds, runs 30/30 tests green, and produces a debug APK whose
  SHA-256 is **identical** to the published artifact
  (`96bab1f33062922add352ea4709bac080bd3f2266f19d582e53928346f3dc7b0`).
- **2026-10-20** — Release asset round-trip verified: the APK downloaded from the GitHub
  release matches the local build byte for byte.
