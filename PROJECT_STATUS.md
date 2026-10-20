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

**Total across modules: 21 distinct tests, 0 failures, 0 errors, 0 skipped**
(12 in `:core` + 9 in `:android:app`). The app module's 9 tests run under both the
debug and release variants, so a build that tests both reports 30 *executions* of 21
distinct test methods.

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

**The contrast audit was rebuilt and re-run in 2026-10-20.** The first pass checked 9
hand-picked pairs. The replacement, `scripts/check-website-contrast.py`, resolves the
**rendered** result: it parses `website/index.html` into a DOM, matches every CSS rule
against every element with a real selector matcher, resolves the cascade by specificity
and source order, walks the ancestor chain to find the background a text node actually
sits on, and composites alpha. It checks **126 rendered text/background pairs** across
the base cascade and all three media-query breakpoints.

**17 real failures were found and fixed.** The worst was **not** a subtle one: the
"Read the full safety note" link inside the dark safety band rendered the site's green
(`#166b55`) on the band's dark green (`#25382e`) — **1.94:1, effectively invisible to a
low-vision user**. The first version of the auditor missed it because it did not resolve
`var(--green)`, and a second version missed it because it did not implement colour
inheritance. Both gaps were closed after the real browser disagreed with the script.

**Both implementations now agree exactly**: the script reports 0 failures, and a
Playwright run in Chromium reports **0 failures across 126 elements at both 1082 px and
390 px** viewports. The script is wired into CI so this cannot regress silently. The
lowest passing ratio is now 4.51:1.

The camera view on the page is a **CSS illustration**, labelled in the page as "Illustration only · not a live detection".

### 6. Research, docs, governance — COMPLETE
Present: `RESEARCH/model-and-data-audit.md` (written from first-hand measurement),
`RESEARCH/open-source-license-audit.md`, `RESEARCH/differentiation-hypothesis.md`,
`RESEARCH/benchmark-plan.md`, `RESEARCH/assistive-ux-requirements.md`,
`RESEARCH/skeptical-review-research.md` (adversarial review),
`RESEARCH/safety-and-claims-review.md` (claims matrix),
`RESEARCH/competitor-landscape.md`, `RESEARCH/source-register.md`,
`RESEARCH/feature-comparison.csv`, `RESEARCH/threat-model.md`,
`RESEARCH/speech-hindi-audit.md`, `TESTING/field-evaluation-protocol.md`,
`docs/privacy/privacy-policy.md`, `docs/safety/SAFETY.md`, `docs/DEMO_GUIDE.md`,
`docs/website-deployment-plan.md`, `SECURITY.md`, `CONTRIBUTING.md`,
`CODE_OF_CONDUCT.md`, `SUPPORT.md`, issue/PR templates, two CI workflows.

Every source consulted is registered in `RESEARCH/source-register.md` with its access
date, reliability rating, and the claim it supports — including the retrievals that
**failed**, which is what keeps the UNVERIFIED rows honest.

#### The finding that changed the project

**Google Lookout already ships Hindi, already works offline, and its Find mode already
gives direction and distance** — verified from Google's own support documentation and
Play listing (see `RESEARCH/competitor-landscape.md` §1). The project's original
differentiators (Hindi-first, offline) are therefore **table stakes, not advantages**,
and `RESEARCH/differentiation-hypothesis.md` has been corrected to say so.

The project has been re-positioned around what actually remains: **a narrow,
fully on-device app that says less, says it honestly, and publishes the measurements
that would show whether that is better.** The strongest supporting evidence is
unexpected and comes from a competitor's own user reviews: Lookout users complain that
it "is continually announcing 'No text in view'" and that announcements "cannot be turned
off" — which is **exactly the failure mode this project treats as its core hypothesis**.
The website now states plainly that the project is not the first.

A GitHub **project board** could not be created: the authenticated token lacks the `project`
scope, and `gh auth refresh -s project` requires interactive browser authorisation. The
issue tracker, labels, and three milestones cover the same ground and are all in place.

**Still unverified, and stated as such in the documents:** the licences of the
open-source prior art (SightBuddy, SeeForMe, VISION), Hindi support in Seeing AI /
Be My Eyes / Envision, and whether any standalone Hindi-first assistive app or 2024–2026
Indian hackathon project already exists. The last of these is the largest unverified
risk in the project (issue #4).

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
4. **The TalkBack audio collision is unverified and is the most likely complete failure.**
   The app speaks through `TextToSpeech` (media stream by default) while the user is
   probably running TalkBack (accessibility stream). The OS does not arbitrate between
   them, so the likely outcome is two voices at once — for exactly the users this app
   exists to serve. **The market leader has the same problem**: a Lookout review reports
   that its voice announcements use the accessibility stream and that the volume
   controls are not exposed. Mitigation (an explicit speech toggle, correct audio
   attributes, screen-reader detection) is **not implemented**. Tracked as issue #8.
5. **Image-relative position may be useless.** "Left / centre / right **of the image**"
   is true about the picture but may be useless for acting — a blind user does not know
   which way they are facing. The wording is safe; the *utility* is unproven. The
   standing recommendation is to **delete the feature if the field evaluation shows it
   does not beat "no position"** (issue #3).
6. **Prior art and licence gap.** The licences of SightBuddy, SeeForMe, and VISION are
   unverified, and no search has confirmed whether a standalone Hindi-first assistive app
   already exists. The negative result is weak: it is "not found", not "verified absent"
   (issue #4).
7. **No accuracy data.** Zero labelled observations have been collected. The field
   protocol is a plan, not a result. No accuracy percentage may be published.

**Repository, website deployment, and the pre-release ARE published** — see the links at
the top of this document.

---

## Release gate — current standing

| # | Requirement | Status |
|---|---|---|
| 1 | Builds from a clean checkout with documented steps | **Met** — verified: a fresh `git clone` builds and tests successfully |
| 2 | Automated tests and CI pass | **Met** — 21/21 distinct local tests pass; both GitHub Actions workflows green. CI now also runs the app module's tests and rebuilds the APK, and asserts it requests `CAMERA` and not `INTERNET` |
| 3 | APK exists, installable, launches | **Partial** — APK builds and is verified structurally; **install/launch untested** |
| 4 | Core detection and TTS verified on a real device | **Not met** — no device (issue #1) |
| 5 | No secrets tracked; permissions justified | **Met** — CAMERA only, no INTERNET, `allowBackup=false`; secret scan clean |
| 6 | Accessibility checked and TalkBack reviewed | **Partial** — website audited automatically against WCAG AA (126 rendered pairs, verified in a real browser at two viewports, enforced in CI); app semantics written; **no manual TalkBack review**, and the audio-collision risk is unmitigated (issue #8) |
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
- **2026-10-20** — Produced and independently verified `app-debug.apk`; ran 21 unit tests (all pass); ran a 7-repeat CPU benchmark; audited the website with Playwright and computed WCAG contrast, fixing one failing pair.
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
  published repository builds, runs 21/21 tests green, and produces a debug APK whose
  SHA-256 is **identical** to the published artifact
  (`96bab1f33062922add352ea4709bac080bd3f2266f19d582e53928346f3dc7b0`).
- **2026-10-20** — Release asset round-trip verified: the APK downloaded from the GitHub
  release matches the local build byte for byte.
- **2026-10-20** — **Competitor research completed and it changed the project.** Google
  Lookout already ships Hindi, already works offline, and already gives direction and
  distance (verified from Google's own support docs and Play listing). The original
  differentiators are table stakes. `RESEARCH/differentiation-hypothesis.md` was
  corrected, the positioning statement was rewritten, and the website now states plainly
  that the project is not the first. The unexpected win: Lookout's own user reviews
  complain about exactly the announcement-spam failure this project targets, which is
  independent support for the core hypothesis.
- **2026-10-20** — Wrote the remaining research: assistive-UX requirements (with the
  repetition problem argued from sources, a testable accessibility acceptance
  checklist, and user-testing ethics), an adversarial self-review, a claims matrix that
  states what may and may not be said and why, a source register, and a feature-comparison
  CSV. Each document separates evidence from inference and names what could not be
  verified.
- **2026-10-20** — **The website contrast audit was rebuilt and found 17 real failures.**
  The new auditor resolves the rendered result from the DOM and CSS rather than
  approximating the cascade from stylesheet text, which is what let it catch the
  1.94:1 green-on-green link inside the dark safety band — invisible to exactly the
  users the project serves. The auditor and a real Chromium run were cross-checked
  against each other; **both now report 0 failures across 126 elements at 1082 px and
  390 px**, and the check is wired into CI.
