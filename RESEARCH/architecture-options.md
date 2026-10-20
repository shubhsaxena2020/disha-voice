# Architecture Options Analysis — Disha Voice (Android)

**Date:** 2026-10-21
**Scope:** the P0 Android prototype's architecture, and the options that were genuinely live
when the architecture was decided, plus the ones that remain open.
**Status:** a research document. **It is not evidence that any of these options was tested on
a device.** No physical Android device was available to this project at any point
(`PROJECT_STATUS.md`, "Verified environment" → "Android device: None attached — `adb devices`
is empty"; `RESEARCH/benchmark-plan.md` §"Current verified boundary"). Every claim below
is either **VERIFIED** (read directly out of a file in this repository, or measured by a
command whose output is recorded here) or **INFERRED** (a reading of that evidence that goes
beyond what the file literally says).

**Reference state.** This document was written against commit `afc027c` and updated to
commits `e63840a` ("fix: the detector would never have detected anything") and `0a55de5`
("docs: publish the architecture decision record"), both of which landed while the
document was being written. Every file/line citation was re-verified against `e63840a`
after those commits landed. Where a claim is about a specific commit, the commit is
named. §3.6 and §7.1 explain the one place where this matters materially.

**Companion documents.** This is not the project's only architecture record. Read it with:

- `RESEARCH/model-and-data-audit.md` — the detector decision, measured against the shipped
  model file.
- `RESEARCH/speech-hindi-audit.md` — the TTS decision and the Hindi-voice platform
  dependency.
- `RESEARCH/threat-model.md` — the privacy boundary that rules several options out.
- `RESEARCH/assistive-ux-requirements.md` — the UX requirements that constrain the speech
  and threading options.
- `docs/adr/0001-core-architecture.md` — the accepted decision record for the module
  boundary (committed in `e63840a`; see §7.1 for how the two relate).
- `RESEARCH/platform-api-audit.md` — a platform-API audit of the Android layer (committed in
  `e63840a`; see §7.1).

---

## 0. How to read this document

**Evidence labels.** Each substantive statement carries one of:

| Label | Meaning |
|---|---|
| **VERIFIED** | Read directly from a file in this repository (cited by path), or produced by a command run in this session (command and output recorded). |
| **INFERRED** | My reading of that evidence. Not a measurement. Could be wrong. |
| **NOT EVALUATED** | No serious attempt was made. Stated so it is not mistaken for a considered rejection. |

**What this document is not.** It is not a decision record and it does not claim the
architecture is optimal. It reconstructs what the options were, what the repository actually
shows about them, and what remains open. Where the repository's own documentation and the
repository's code disagree, **the code wins** and the disagreement is recorded.

---

## 1. The verified baseline

Everything in this section is **VERIFIED** by direct file reads. These are the facts every
option below is measured against.

### 1.1 Module graph and build

| Fact | Evidence |
|---|---|
| Two modules: `:core` (pure Kotlin/JVM) and `:android:app` (Android application). | `settings.gradle.kts` lines 16-17: `include(":core")`, `include(":android:app")` |
| `:core` applies only `kotlin("jvm")`; it has no Android plugin and no Android dependency. | `core/build.gradle.kts` lines 1-15: `plugins { kotlin("jvm") }`, `jvmToolchain(17)`, one `testImplementation(kotlin("test"))` |
| `:android:app` depends on `:core` as a project dependency. | `android/app/build.gradle.kts` line 64: `implementation(project(":core"))` |
| AGP 8.7.3, Kotlin 2.0.21, Gradle 8.14.3 (wrapper committed). | `gradle/libs.versions.toml` lines 2-3; `gradle/wrapper/gradle-wrapper.properties` |
| minSdk 24, targetSdk 36, compileSdk 36, Java/JVM target 17. | `android/app/build.gradle.kts` lines 9, 13-14, 37-45 |
| The app declares **only** `android.permission.CAMERA`. No `INTERNET`, no `RECORD_AUDIO`. | `android/app/src/main/AndroidManifest.xml` line 8; confirmed in the merged manifest at `android/app/build/outputs/logs/manifest-merger-debug-report.txt` |
| The bundled model is committed in-repo as an asset. | `android/app/src/main/assets/efficientdet_lite0_detection.tflite`, 4,561,184 bytes, SHA-256 `33a3b622c7cac0762f96089353cd61495f3e993968d133af7871bfc2d5396704` (recomputed in this session) |
| The inference runtime is LiteRT, not the old `org.tensorflow` coordinate. | `gradle/libs.versions.toml` line 33: `tensorflow-lite = { group = "com.google.ai.edge.litert", name = "litert", version = "1.0.1" }` |
| CI runs `:core:test`, `:android:app:testDebugUnitTest` and `:android:app:assembleDebug`, and **asserts the APK has no `INTERNET` permission**. | `.github/workflows/ci.yml` lines 30-47 |
| There is **no** `androidTest` source set in the repository. | `android/app/src/androidTest` exists as an empty directory; `git ls-files` shows no instrumented test file |
| There is **no** Robolectric, no `mockk`, no Android test harness of any kind. | `gradle/libs.versions.toml` has no such entry; `android/app/build.gradle.kts` has none |

### 1.2 The domain policy in `:core`

**VERIFIED**, from `core/src/main/kotlin/org/dishavoice/core/AnnouncementPolicy.kt`:

- `ImageZone { LEFT, CENTER, RIGHT }`, `BoundingBox` with normalized `[0,1]` coordinates and an
  `isValid` guard, `Detection(label, confidence, bounds)` (lines 4-26).
- Zone boundaries `LEFT_ZONE_END = 0.38f`, `RIGHT_ZONE_START = 0.62f` (lines 31-32).
- Confidence bands: `DEFAULT_CONFIDENT_THRESHOLD = 0.70f`, `DEFAULT_TENTATIVE_THRESHOLD =
  0.45f`, and an `IGNORE` band below (lines 33-34, 46-59).
- `DEFAULT_COOLDOWN_MILLIS = 4_000L` (line 35).
- `AnnouncementPolicy` is stateful and keyed on `(label.lowercase(), zone)` (lines 72-101).
- `Decision` is a sealed interface with four cases: `Announce`, `SuppressedLowConfidence`,
  `SuppressedInvalidDetection`, `SuppressedDuplicate` (lines 61-66).
- The file imports **nothing** from `android.*` — the module is genuinely platform-free.

**VERIFIED test counts**, read from the JUnit XML the build produced in this session
(`core/build/test-results/test/*.xml`):

- `AnnouncementPolicyTest` — 8 tests, 0 failures.
- `AnnouncementPolicyPropertyTest` — 4 tests, 0 failures (seeded at `0xD15A0`, 10,000
  synthetic boxes, a 10,001-point confidence grid, 36 synthetic label/zone keys).

**VERIFIED**: `:core:test` passed when I ran it in this session
(`./gradlew --offline :core:test :android:app:testDebugUnitTest`).

### 1.3 The Android layer

**VERIFIED**, from the app module:

| Concern | Where it lives | Detail |
|---|---|---|
| Camera binding | `DishaViewModel.bindCamera` (`DishaViewModel.kt` lines 91-127) | `ProcessCameraProvider`, `Preview` + `ImageAnalysis` bound to the lifecycle, `CameraSelector.DEFAULT_BACK_CAMERA` |
| Backpressure | `DishaViewModel.kt` line 108 | `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` |
| Frame format | `DishaViewModel.kt` line 109 | `OUTPUT_IMAGE_FORMAT_YUV_420_888` |
| YUV→Bitmap | `android/app/src/main/java/org/dishavoice/app/camera/YuvFrameConverter.kt` | Hand-written BT.601 conversion in Kotlin, then scaled to 480×480 |
| Analyzer executor | `DishaViewModel.kt` lines 151-153 | `ContextCompat.getMainExecutor(getApplication())` — **the main thread** |
| Inference dispatch | `DishaViewModel.kt` lines 160-163 | `viewModelScope.launch(Dispatchers.Default)` then `withContext(Dispatchers.Default) { det.detect(bitmap) }` |
| Detector interface | `android/app/src/main/java/org/dishavoice/app/detection/Detector.kt` | `isReady`, `inputResolution`, `detect(bitmap): List<Detection>`, `AutoCloseable` |
| Model loading | `EfficientDetDetector.kt` lines 126-137 | `context.assets.openFd` → `FileChannel.map(READ_ONLY)` → `MappedByteBuffer` |
| Interpreter options | `EfficientDetDetector.kt` line 34 | `setNumThreads(2).setUseXNNPACK(true)` |
| Decoding | `DetectionDecoder.kt` | Tensor-free; `DEFAULT_SCORE_FLOOR = 0.25f` |
| Labels | `CocoLabels.kt` | `model_class_index + 1 == coco_category_id`; 80 mapped COCO ids |
| Speech | `android/app/src/main/java/org/dishavoice/app/speech/Speaker.kt` | `android.speech.tts.TextToSpeech`, `hi-IN` preferred, English fallback |
| Wording | `android/app/src/main/java/org/dishavoice/app/speech/Phrasebook.kt` | Every product state has a Hindi and an English line |

### 1.4 The measured model contract

**VERIFIED in this session**, independently of the repository's own claims, by loading the
shipped asset with `ai_edge_litert` in a scratch virtualenv and printing the interpreter's
tensor details:

```
=== INPUT ===
  name='serving_default_images:0' shape=[1 320 320 3] dtype=uint8 quant=(0.0078125, 127)

=== OUTPUTS (interpreter ordinal order) ===
  ordinal 0: name='StatefulPartitionedCall:3' shape=[1 25 4] dtype=float32
  ordinal 1: name='StatefulPartitionedCall:2' shape=[1 25]    dtype=float32
  ordinal 2: name='StatefulPartitionedCall:1' shape=[1 25]    dtype=float32
  ordinal 3: name='StatefulPartitionedCall:0' shape=[1]       dtype=float32
```

This matches `RESEARCH/model-and-data-audit.md` §3 exactly (input `[1,320,320,3]` uint8 with
quantisation `(scale=0.0078125, zero_point=127)`; outputs boxes `[1,25,4]`, classes `[1,25]`,
scores `[1,25]`, count `[1]`), and it confirms the two "documented assumptions were wrong"
corrections the audit records: the input is **320×320**, not 300×300, and it is **uint8**,
not float32.

**Also VERIFIED in this session:** the two `[1, 25]` outputs share a shape, so shape alone
cannot separate class ids from scores.

### 1.5 What is *not* verified, anywhere in the repository

- **No physical device.** `adb devices` returns an empty list (re-run in this session).
  Camera behaviour, Hindi TTS on a handset, install/launch, TalkBack behaviour, thermal
  behaviour, and on-device inference latency are all unverified.
- **No on-device latency number exists.** The only inference timings in the repo are
  desktop-CPU numbers from `benchmarks/results/detector_benchmark.json`, whose own
  `limitations` array says: *"Desktop CPU measurement. Not a phone; do not present as device
  latency."*
- **No accuracy data.** Zero labelled observations; `TESTING/field-evaluation-protocol.md`
  is a plan, not a result.
- **No instrumented test has ever run.** The `androidTest` source set is empty.

---

## 2. Axis 1 — Where the domain policy lives

### 2.1 Option A (chosen): a pure Kotlin/JVM `:core` module — **VERIFIED as built**

**What it is.** The "decide what may be said" step — zone classification, confidence banding,
repeat suppression — lives in a separate Gradle module that applies only `kotlin("jvm")`
and imports no Android API. The Android module depends on it and supplies it with
detections.

**Evidence for.** `core/build.gradle.kts` contains three meaningful lines: `kotlin("jvm")`,
`jvmToolchain(17)`, and `testImplementation(kotlin("test"))`. `settings.gradle.kts` includes
it as `:core`. `AnnouncementPolicy.kt` has zero Android imports. `DishaViewModel.kt` lines
24-26 import `AnnouncementPolicy`, `Decision`, `Detection` from `org.dishavoice.core` and
the class comment at lines 41-46 states the intent directly: *"The domain policy (:core) is
the single place that decides what may be announced; this class only supplies it with
detections and speaks the resulting phrase."*

**What it would have cost.** One extra Gradle module and one extra `implementation(project(...))`
line. That is the entire cost, and it is paid in `settings.gradle.kts` lines 16-17 and
`android/app/build.gradle.kts` line 64.

**Why it was chosen.** The repository's own record of this reasoning is
`docs/adr/0001-core-architecture.md` (written concurrently with this document): the decision
step is the product, and the environment where it must be verified is hostile to
verification — with no device available, logic embedded in the Android layer would be
exactly the logic that goes unverified. The decisive concrete fact is the test matrix:
**`:core:test` needs no Android SDK at all** (`core/build.gradle.kts` has no Android
plugin), so the policy suite runs anywhere JDK 17 is installed. I confirmed this in this
session: `:core:test` passed with `--offline`.

**Evidence against / cost of the choice.** The split has a price. Three things that are
conceptually domain logic are *not* in `:core` and are therefore tested less:

- `Phrasebook` lives in the app module (`android/app/src/main/java/org/dishavoice/app/speech/Phrasebook.kt`),
  not in `:core`, even though it is pure Kotlin with no Android imports. It is therefore
  **untested** — there is no `PhrasebookTest`. `RESEARCH/assistive-ux-requirements.md` §3
  and `RESEARCH/speech-hindi-audit.md` §5 both treat the wording rules as safety-critical,
  and the rules are currently enforced only by convention and by review.
- `DetectionDecoder` and `InputTensorBuilder` are deliberately kept in the app module rather
  than pushed down (`DetectionDecoder.kt` line 9: *"Kept free of TensorFlow types so it
  can be unit tested on the JVM"*). They are pure Kotlin and JVM-testable, and they are
  tested — 24 app-module tests — but they are not in `:core`, so they cannot run in a
  JDK-only CI job.
- The *policy about which of several detections to speak* — the `maxByOrNull` tie-break at
  `DishaViewModel.kt` line 178 and the "nothing detected" logic at lines 183-189 — lives in
  the ViewModel and is **not unit-tested at all**.

**VERIFIED**: `git ls-files` shows no test file for `Phrasebook`, `Speaker`, `DishaViewModel`,
`YuvFrameConverter`, `DishaScreen` or `MainActivity`. The only app-module test files are
`DetectionDecoderTest.kt` and `InputTensorBuilderTest.kt`.

### 2.2 Option B: all policy inside the Android app module — **genuinely evaluated, rejected**

**What it is.** One module. `AnnouncementPolicy` becomes a class in `:android:app`, tested
with Robolectric or with instrumented `androidTest` tests.

**Evidence in this repo for/against.** The infrastructure for it is **absent**: no
Robolectric dependency in `gradle/libs.versions.toml`, no `robolectric` block in
`android/app/build.gradle.kts`, and an **empty** `android/app/src/androidTest` directory. The
`androidTestImplementation` dependencies that *are* declared
(`android/app/build.gradle.kts` lines 87-90) cover nothing, because there are no
instrumented test sources to use them. `docs/adr/0001-core-architecture.md` §"Alternatives
considered" A and B record this option as evaluated and rejected.

**What it would have cost.** Two things. First, a test-tooling dependency: Robolectric
would have to be added, which means the policy suite would download and boot an Android
runtime on every CI run. Second — and more seriously — an **instrumented test suite would
require a device or emulator**, and this project has neither. Under Option B, the 12 tests
in `:core` would become tests that cannot run at all in the current environment. That is the
decisive cost: it converts 12 running tests into 0 running tests.

**Why rejected.** The policy is the part of the system that most needs tests and least needs
Android. Embedding it in the app module makes its testability a function of device
availability, which the repository records as its single largest gap.

### 2.3 Option C: Kotlin Multiplatform / shared native core — **NOT EVALUATED**

**What it is.** `:core` becomes a Kotlin Multiplatform module with a `jvm()` (or
`androidTarget()`) plus native targets, so the same policy source also builds for iOS,
desktop-native, or a shared C++ target via the Kotlin/Native or Kotlin/Swift interop.

**Evidence in this repo.** **There is none, and that is the finding.** A repository-wide
search for `multiplatform`, `kmp`, `expect`/`actual`, and any non-JVM target found no
matches in any source, build, or documentation file. The build is configured for exactly
one non-JVM ecosystem: `settings.gradle.kts` includes two modules, one of which is an
Android application.

**What it would have cost.** `kotlin("multiplatform")` instead of `kotlin("jvm")`; the
Kotlin Gradle plugin's multiplatform source-set layout (`commonMain`/`commonTest`); an
expect/actual split for anything platform-specific; a materially more complex CI matrix;
and, for native targets, a Kotlin/Native toolchain and a much longer first build. It also
risks the thing this project most values: a `:core` that is no longer
"`./gradlew :core:test` and nothing else", which is the property that made the policy
suite runnable without the Android SDK.

**Why not chosen.** Not chosen **because it was never seriously evaluated**, not because it
was measured and found inferior. The reason is contextual: Disha Voice is an Android-only
prototype with no iOS or desktop target, no roadmap for one, and no second consumer of the
policy code. Adopting KMP would be paying a real complexity cost for a portability benefit
that has no identified user. `docs/adr/0001-core-architecture.md` records the same conclusion
in weaker form.

**Honest caveat.** A future reader should not read this section as "KMP was tried and
failed". It was not tried. The honest statement is: *the portability was not needed by
anything that exists, so it was not paid for.*

### 2.4 Option D: an Android library module for the policy — **NOT EVALUATED separately**

**What it is.** `:core` applies `com.android.library` instead of `kotlin("jvm")`, so the
policy can use Android APIs (e.g. `SystemClock.elapsedRealtime()`) directly.

**Evidence in this repo.** `core/build.gradle.kts` uses `kotlin("jvm")` with no Android
plugin, and `AnnouncementPolicy.kt` takes the clock as a **parameter**
(`decide(detection: Detection, nowMillis: Long)`, line 85) rather than reading it. The
class comment at lines 68-71 is explicit: *"call with monotonic elapsed time (e.g. Android
SystemClock.elapsedRealtime()); it stores labels/zones/timestamps only."* The Android caller
supplies exactly that: `DishaViewModel.kt` line 174, `android.os.SystemClock.elapsedRealtime()`.

**Why this is not a real option.** It was never a live choice, because clock injection is
what removes the need for it. Making `:core` an Android library would trade the ability to
run `:core:test` without an SDK for convenience the code already does not need. This is
listed because the injected-clock design is the reason it never had to be evaluated.

---

## 3. Axis 2 — How the detector runs

### 3.1 Option A (chosen): bundled TFLite/LiteRT EfficientDet-Lite0, run in-process — **VERIFIED as built**

**What it is.** A 4.4 MB Apache-2.0 EfficientDet-Lite0 int8 model, COCO-2017-trained, 90
usable classes, committed to the repository as an APK asset and executed by the LiteRT
`Interpreter` on the device's CPU.

**Evidence for.**

- The asset is in the repo: `android/app/src/main/assets/efficientdet_lite0_detection.tflite`.
- The runtime is a normal Gradle dependency: `gradle/libs.versions.toml` line 33 pins
  `com.google.ai.edge.litert:litert:1.0.1`.
- The interpreter is created, configured (`setNumThreads(2)`, `setUseXNNPACK(true)`) and
  memory-mapped from the asset: `EfficientDetDetector.kt` lines 31-36 and 126-137.
- It works offline by construction: the model is inside the APK and the app has no
  `INTERNET` permission (`AndroidManifest.xml` line 8).
- The tensor contract was measured against the real file — by the project
  (`RESEARCH/model-and-data-audit.md` §3) and independently by me in this session (§1.4).
- The preprocessing convention is pinned by a JVM unit test:
  `android/app/src/test/java/org/dishavoice/app/detection/InputTensorBuilderTest.kt` asserts
  raw uint8 R,G,B bytes, no scaling, no mean/std subtraction, and full 0-255 range survival.

**What it would have cost.** Roughly what the repository now contains:
`EfficientDetDetector.kt` (146 lines), `DetectionDecoder.kt` (60), `InputTensorBuilder.kt`
(68), `CocoLabels.kt` (~95), plus 16 app-module unit tests and the desktop Python harness
in `benchmarks/scripts/`. The model asset itself is 4.4 MB of APK payload.

**Why it was chosen.** `RESEARCH/model-and-data-audit.md` §1 states the decision and §2
gives the grounds: offline-first was non-negotiable, and the alternatives were rejected on
verified rather than aesthetic grounds (§6 of that document). The strongest argument for
owning the decode path is the one the repository learned the hard way — see §3.6 below.

**The honest cost of this choice.** The project owns preprocessing, postprocessing and the
label table, and `RESEARCH/model-and-data-audit.md` §2 says so plainly: *"That is more
code, but it is our code, unit tested."* The repository has since paid a further cost
that the audit did not anticipate: a class of bug that a batteries-included pipeline would
not have had (see §3.6).

### 3.2 Option B: ML Kit / Google Play Services Object Detection API — **genuinely evaluated, rejected on verified grounds**

**What it is.** `com.google.mlkit:object-detection`, either the base API (unbundled model
delivered by Play Services) or the custom-model API (`object-detection-custom`, bundled
model via `LocalModel`).

**Evidence in this repo.** This is the best-documented rejection in the project.
`RESEARCH/model-and-data-audit.md` §2 and §6, `RESEARCH/open-source-license-audit.md` §3
("Deliberately not used"), and `RESEARCH/source-register.md` S7-S11 all record it.

The rejection rests on two **verified** facts, both quoted from Google's own documentation in
that audit:

1. The base detector is **unbundled**: *"This API uses an unbundled library that must be
   downloaded before use."* The model comes from Google Play Services, so a first run on a
   device without it has **no detection at all**.
2. The built-in coarse classifier covers **five** categories — inadequate for "common
   household objects", which is the product premise.

The custom-model path was considered and marked "not chosen": it is bundled-capable, but
*"constrains the model to ML Kit's metadata/label-file conventions"*, and the shipped model
has no embedded label map (the audit verifies the strings `person`, `bicycle`, `zebra`,
`toothbrush` do not occur anywhere in the binary).

**What it would have cost.** A Play Services dependency in the critical path (breaking
offline-first), plus — for the custom path — manufacturing metadata and a label file for a
model that ships without them. In exchange: a maintained, batteries-included decode path
that would have avoided §3.6 entirely.

**Why rejected.** Not offline-first, and too few categories in the base form.

**Honest caveat about this rejection.** The custom-model variant is a *near miss*, and the
repository's own wording ("not chosen", not "rejected") is the accurate one. It would have
been bundled and offline. The blocker is a conventions mismatch on a model that carries no
metadata, which is a *packaging* problem, not a fundamental one. A different model with
embedded metadata would have made this option viable.

### 3.3 Option C: MediaPipe Tasks Vision Object Detector — **NOT EVALUATED**

**What it is.** Google's MediaPipe Tasks library (`com.google.mediapipe:tasks-vision`)
provides an `ObjectDetector` that wraps a bundled TFLite model, with its own preprocessing
and postprocessing, intended for on-device use.

**Evidence in this repo.** **None.** A repository-wide search for `mediapipe`, across source,
build files, version catalog, documentation and the new ADR and platform audit, returns no
matches whatsoever. The word does not appear in `RESEARCH/model-and-data-audit.md` §6
("Alternatives considered and not chosen"), which lists ML Kit base, ML Kit custom,
MobileNet-SSD, ONNX Runtime Mobile, on-device VLM, and cloud vision — but not MediaPipe.

**What it would have cost.** A new dependency (`com.google.mediapipe:tasks-vision`,
substantially larger than the LiteRT runtime because it pulls in the MediaPipe graph
runtime), and the model's preprocessing would have to be re-expressed through MediaPipe's
`ImageProcessingOptions` rather than the current `InputTensorBuilder`. In exchange: a
maintained, correct-by-construction decode path, and — critically — it would have **avoided
the §3.6 bug class entirely**, because the output-tensor layout would be MediaPipe's problem
and not the project's.

**Why it was not chosen.** It was not evaluated, and the most likely explanation is
contextual: MediaPipe Tasks is a comparatively newer surface, the project had already
committed to the LiteRT runtime by the time it would have been considered, and the
LiteRT-direct route had the advantage of being measurable immediately against the real model
file. **This is a gap in the option space, not a considered rejection**, and it should be
re-examined if the project revisits the detector.

### 3.4 Option D: a TFLite Support / metadata-driven pipeline — **NOT EVALUATED**

**What it is.** The TFLite Support library (`tensorflow-lite-support`), or specifically the
`Task Library` and the metadata writer, embeds a model's input/output conventions, label
map, and postprocessing parameters *inside* the `.tflite` file, so that a generic runtime
can load it and decode it correctly without bespoke code.

**Evidence in this repo.** **None.** A repository-wide search for `tflite-support`,
`Task Library`, `TensorMetadata`, `LabelMap`, and `metadata_writer` returns no matches in any
file. The project instead owns the label table directly in `CocoLabels.kt` and the
preprocessing directly in `InputTensorBuilder.kt`.

**What it would have cost.** Adding the metadata writer to the build (or a one-off script),
converting the shipped asset to carry a metadata section, and depending on the Support
library's runtime to read it. The **payoff** would be precisely the class of bug in §3.6:
the label map and the tensor layout would become part of the artifact rather than
hand-maintained tables in two places (`CocoLabels.kt` and `benchmarks/model/coco_labels.py`),
and the output-tensor ambiguity would be resolved by the library rather than by hand.

**Why it was not chosen.** Not evaluated. The project chose the cheaper path — write the
decode logic directly — and, given the repository's own evidence, that choice has now cost
it at least one critical defect (§3.6).

### 3.5 Option E: a cloud vision API — **genuinely evaluated, rejected**

**What it is.** Send frames to a hosted vision service (Google Cloud Vision, AWS Rekognition,
or an LLM captioner) and speak the returned description.

**Evidence in this repo.** Rejected on three independent grounds, all recorded:
`RESEARCH/model-and-data-audit.md` §6 ("would break the offline-first and privacy design,
and requires a backend, accounts, and consent flow the prototype does not have"),
`RESEARCH/threat-model.md` §5 (the `INTERNET` permission is the mechanism that makes "no
upload is even possible" a guarantee rather than a promise — T6), and
`RESEARCH/safety-and-claims-review.md`'s claims matrix.

**What it would have cost.** The `INTERNET` permission, which is currently the single
strongest privacy property in the product (CI enforces its absence, `.github/workflows/ci.yml`
lines 35-47). Plus a backend, accounts, an API key, a consent flow, and — for a *captions*
model specifically — a different safety analysis entirely, because a captioning model
generates free text that would need to be treated as untrusted content
(`RESEARCH/threat-model.md` T12).

**Why rejected.** It would have removed the project's only defensible ground. The
`RESEARCH/differentiation-hypothesis.md` §3 positioning depends on the app being offline
and non-conversational.

### 3.6 The detector option that was actually chosen has produced one critical defect — **VERIFIED in this session, now fixed**

This belongs in an options document because it is the strongest argument *against* the
chosen option, and because it shows the cost of owning the decode path.

**The defect.** At commit `afc027c`, `EfficientDetDetector.resolveOutputIndices` separated
the class-id tensor from the score tensor by **inspecting tensor values** and testing
whether they all fell in `0f..1f`:

```kotlin
shape.size == 2 -> {
    val values = readFloats(tf.getOutputTensor(i))
    val looksLikeScores = values.all { it.isFinite() && it in 0f..1f }
    if (looksLikeScores) scores = i else classes = i
}
```

**Why that fails.** On a freshly allocated interpreter, **every** output tensor reads as
zero, and zero is inside `0f..1f`. I verified this directly against the shipped asset in
this session:

```
=== RAW TENSOR VALUES on a FRESH interpreter (before any run()) ===
  ordinal 0: min=0 max=0 all_in_0_1=True
  ordinal 1: min=0 max=0 all_in_0_1=True
  ordinal 2: min=0 max=0 all_in_0_1=True
  ordinal 3: min=0 max=0 all_in_0_1=True
```

I then simulated `afc027c`'s exact loop against the measured layout:

```
  i=0 shape=[1,25,4] -> boxes=0
  i=1 shape=[1,25] values all in 0..1 -> scores=1
  i=2 shape=[1,25] values all in 0..1 -> scores=2
  i=3 shape=[1] -> count=3

  final: boxes=0 classes=-1 scores=2 count=3
  -> error("Unexpected model output layout")  ==> detect() returns emptyList() every frame
```

**Consequence.** The `error(...)` was thrown inside `detect()`'s `runCatching` block, so it
was swallowed into `emptyList()`. The camera preview would run, `isReady` would still be
`true` (the interpreter loaded fine), and **the app would never announce a single object,
with no error surfaced anywhere.** The only user-visible symptom would be silence. For an
assistive tool, that is the worst possible failure mode, because it looks like it is
working.

**Why this matters for the options analysis.** A batteries-included pipeline — ML Kit
custom (§3.2), MediaPipe Tasks (§3.3), or a metadata-driven one (§3.4) — would not have had
this failure mode, because the output layout would not be the project's problem. The
project chose the "own the decode path" option, and that ownership produced a
**critical, silent, total failure** that shipped in the published v0.1.1 pre-release.

**The fix (committed in `e63840a`).** A new pure-Kotlin
`android/app/src/main/java/org/dishavoice/app/detection/OutputTensorResolver.kt` resolves
the four tensors by **shape and ordinal position, never by value**: the one 3-D output
whose last dimension is 4 is the boxes tensor; the one 1-D output is the count tensor; of
the two remaining 2-D outputs, the earlier is the class ids and the later is the scores.
`EfficientDetDetector` now resolves the layout **once at construction**
(`EfficientDetDetector.kt` lines 51-56), and `isReady` requires the resolution to have
succeeded (line 57), so a layout mismatch now makes the detector report *not ready* and
the UI say "Model unavailable" (`DishaViewModel.kt` lines 134-142) instead of silently
detecting nothing. The measured layout in §1.4 confirms the positional rule is correct
for this asset. `OutputTensorResolverTest` (8 tests) pins the contract against the
measured shapes.

**Status of the fix, verified by me in this session.** `:core:test` and
`:android:app:testDebugUnitTest` were re-run with `--rerun-tasks` against `e63840a`:
**BUILD SUCCESSFUL, 36 tests, 0 failures** (see §6.1 for the breakdown).

**The lesson, which is the actual point of this section.** The defect was found by
*reading*, not by a test — no test would have caught it, and the 28 tests that existed at
`afc027c` all passed against a detector that could never have detected anything. That is
the concrete cost of the "own the decode path" choice, and it is the strongest argument
for re-examining §3.3 and §3.4.

### 3.7 Option F: a different bundled TFLite detector — **genuinely evaluated as an alternative, not required**

**What it is.** MobileNet-SSD, or another EfficientDet-Lite tier (Lite1-Lite4), or a
YOLO-derived TFLite model, swapped into the same asset slot.

**Evidence in this repo.** `RESEARCH/model-and-data-audit.md` §6 lists it as "alternative,
not required": same Apache-2.0 family, comparable size, and EfficientDet-Lite0 has the
better published mAP/size point of the Lite family.

**What it would have cost.** Very little — the asset slot, the label table, the input
convention, and the decode path are all model-shaped but the swap itself is a file
replacement plus a label-table change. The desktop benchmark harness makes this cheap to
evaluate: `benchmarks/scripts/run_benchmark.py` takes `--model` as an argument.

**Why not chosen.** Lite0 was a defensible size/accuracy point. **This option remains
open**: if field data shows the class set or the accuracy is insufficient for the target
household scenarios, the harness exists to evaluate alternatives without touching the app.

---

## 4. Axis 3 — Threading and frame-sampling strategy

### 4.1 Option A (chosen): CameraX `ImageAnalysis` with `STRATEGY_KEEP_ONLY_LATEST` — **VERIFIED as built**

**What it is.** CameraX delivers frames to an analyzer; the analyzer converts the frame and
dispatches inference to a background dispatcher; the newest frame wins and stale frames are
dropped by the framework.

**Evidence for.**

- `DishaViewModel.kt` line 108: `.setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)`,
  with the rationale in the comment at lines 106-107: *"Bounded backpressure: only the newest
  frame is analysed, so slow inference cannot build an unbounded backlog."*
- `DishaViewModel.kt` lines 151-153: `imageAnalysis.setAnalyzer(executor) { proxy -> analyze(det, proxy) }`.
- `DishaViewModel.kt` lines 166-169: the `finally` block always calls `proxy.close()`,
  commented *"Always release the frame: this is what keeps memory bounded."*
- `YuvFrameConverter.kt` lines 50-66: the source bitmap is recycled when a scaled copy is
  made (`if (scaled !== bitmap) bitmap.recycle()`).
- `RESEARCH/platform-api-audit.md` §7.4 records this as one of the things "checked and are
  *not* problems".

**What it would have cost.** A CameraX dependency (`androidx.camera.core`, `camera2`,
`lifecycle`, `view` — `gradle/libs.versions.toml` lines 28-31), plus the `ImageProxy`
lifecycle discipline that the `finally` block demonstrates.

**Why it was chosen.** It gives bounded memory and bounded latency for free from the
framework, which is exactly what a continuously-running camera app needs. It also composes
with the product's central hypothesis: the app is supposed to report the *latest* view, not
to process a backlog.

### 4.2 Option B: polling a camera API directly — **NOT EVALUATED**

**What it is.** Open the camera via `Camera2`/`CameraManager` directly and pull frames on a
timer (`Handler.postDelayed`, a `ScheduledExecutorService`, or a coroutine `while` loop with
a `delay`), rather than being pushed frames by CameraX.

**Evidence in this repo.** **None.** There is no `CameraManager`, no `CameraDevice`, no
`ImageReader` and no timer-based polling anywhere in the source; the only camera surface
is `androidx.camera.*`. `RESEARCH/model-and-data-audit.md`, `RESEARCH/threat-model.md` and
`RESEARCH/assistive-ux-requirements.md` do not discuss polling.

**What it would have cost.** Substantially more code: `Camera2` session setup, permission
and lifecycle handling, `ImageReader` surface management, and a hand-written frame-rate
governor. It would also forfeit CameraX's backpressure, so the project would have to
re-implement "keep only the latest" itself — including the case where inference is slower
than the frame rate, which is the case that matters.

**Why it was not chosen.** Not evaluated. The most defensible reading is that CameraX is the
platform-blessed path and this option would only have been chosen to obtain a specific
capability (e.g. a frame rate CameraX does not expose) that the project never needed. It
is listed because "polling" is a genuinely common alternative in camera-app architecture,
and this repository has no record of ever having considered it.

### 4.3 Option C: a dedicated inference worker thread / executor — **partially adopted, and the current design is weak here**

**What it is.** A single long-lived worker thread (or a bounded single-thread dispatcher)
that owns the `Interpreter` and processes frames one at a time, with the camera feeding it.

**Evidence for.** The project has *parts* of this. `EfficientDetDetector.kt` line 34 sets
`setNumThreads(2)` on the interpreter, and `DishaViewModel.kt` lines 160-163 dispatch
inference to `Dispatchers.Default` (an elastic pool, not a dedicated single-thread executor).

**Evidence against — and this is a real weakness in the chosen design.**
`RESEARCH/platform-api-audit.md` §4 and §5 (written concurrently with this document)
identify two related problems in the current implementation:

- **Finding C (HIGH, not fixed):** the analyzer runs on
  `ContextCompat.getMainExecutor(...)` — the **main thread** — and the YUV→Bitmap conversion
  in `YuvFrameConverter.toBitmap` is a hand-written per-pixel Kotlin loop over the whole
  frame (`YuvFrameConverter.kt` lines 50-62), executed on that main thread.
- **Finding D (MEDIUM, not fixed):** the model is loaded on the main thread, because
  `ensureDetector()` is called from `start()` on the main thread
  (`DishaViewModel.kt` lines 130-146).

**A correctness concern I can state from the code, distinct from the audit's performance
framing.** `DishaViewModel.analyze` (lines 156-170) launches a coroutine per frame on
`Dispatchers.Default`, and the `Interpreter` is a **single shared instance** used from
whichever pool thread happens to run the coroutine. Nothing serialises those calls. The
LiteRT `Interpreter` is documented as not being safe for concurrent `run()` calls. The
`STRATEGY_KEEP_ONLY_LATEST` backpressure bounds how many frames are *pending in the
framework*, but it does not bound how many coroutines are already in flight against the
interpreter — each `analyze` call launches a new coroutine and returns immediately. **This
is INFERRED from reading the code; I did not reproduce a concurrent-inference failure, and
no test covers it.** A dedicated single-threaded inference executor would make the
interpreter's single-threaded contract structurally true rather than incidentally true.

**What a dedicated worker would have cost.** One `ExecutorService` or
`Executors.newSingleThreadExecutor()`, one dispatcher backed by it, and routing the analyzer
body to it. Small in isolation; the reason it was not done appears to be that nobody had
measured the main-thread cost yet — which is exactly what §4.5 says is missing.

### 4.4 Option D: an explicit frame-sampling rate — **NOT EVALUATED, and arguably a gap**

**What it is.** Detecting on every frame CameraX delivers, versus detecting at a fixed
cadence (say 2 Hz) or only when the scene has changed.

**Evidence in this repo.** **None.** There is no frame counter, no timestamp gate, no
"skip if the last frame was N ms ago", and no scene-change heuristic in
`DishaViewModel.analyze` (lines 156-170) or anywhere else. The *only* rate control in the
system is downstream of detection, in the policy: the 4-second `(label, zone)` cooldown in
`AnnouncementPolicy` (`AnnouncementPolicy.kt` line 35).

**What it would have cost.** A small amount of code — a timestamp comparison — plus a
measured justification for the chosen rate.

**Why it was not chosen.** Not evaluated. The consequence is worth stating plainly: the
app currently runs **continuous inference at full camera frame rate** and relies entirely
on the *speech* layer to control what the user hears. Whether that is an acceptable
battery and thermal cost is unknown, and `RESEARCH/platform-api-audit.md` §5 and
`RESEARCH/benchmark-plan.md` §3 both record on-device thermal and latency behaviour as
unmeasured. This is a live trade-off, not a settled one.

### 4.5 Option E: none of the above, verified on device — **the missing measurement**

**What would have to happen.** `RESEARCH/benchmark-plan.md` §3 specifies the on-device
protocol: a real device, a release-like profileable build, AndroidX Microbenchmark for the
inference path and Macrobenchmark for the app journey, at least three independent
invocations, with `SystemClock.elapsedRealtimeNanos()` and thermal-status sampling.

**Current state.** The plan exists; **no device result exists**. The document is careful
about this: *"No benchmark result dataset is created by this plan."* `adb devices` is empty
in this session.

**The honest consequence for this axis.** Every claim about which threading option is
*better* — main thread vs. dedicated worker vs. sampled frames — is currently
**unmeasured**. The chosen option is defensible on memory-boundedness grounds, which are
verifiable by reading the code, and **not** on latency, thermal or battery grounds, which
are not.

---

## 5. Axis 4 — How spoken output is produced

### 5.1 Option A (chosen): `android.speech.tts.TextToSpeech`, on-device — **VERIFIED as built**

**What it is.** The platform TTS engine, driven with `Locale("hi", "IN")` preferred and
`Locale.US` as fallback, `QUEUE_FLUSH` for user-triggered speech.

**Evidence for.**

- `Speaker.kt` line 33: `TextToSpeech(context.applicationContext, this)`.
- `Speaker.kt` lines 49-65: `configureLocale` checks `isLanguageAvailable` and accepts
  `LANG_AVAILABLE`, `LANG_COUNTRY_AVAILABLE`, `LANG_COUNTRY_VAR_AVAILABLE`; otherwise it
  falls back to `Locale.US` and returns `false`.
- `Speaker.kt` lines 80-85: `speak(text, flush = true)` → `QUEUE_FLUSH` by default.
- `DishaViewModel.kt` lines 70-82: `ensureSpeaker` waits for readiness and, if Hindi is
  unavailable, speaks `phrasebook.speechUnavailable()` — an **English** line announcing the
  fallback (`Phrasebook.kt` lines 75-78).
- `Phrasebook.kt` has a Hindi and an English variant for every state, and a
  `HINDI_OBJECT_NAMES` map for 57 COCO classes.
- `RESEARCH/speech-hindi-audit.md` §1 documents the platform mechanics and the
  `isLanguageAvailable` contract; §5 documents the phrasebook.

**What it would have cost.** A wrapper class (~100 lines) plus the honest-degradation
logic, which is the expensive part: the app must detect the missing-voice case and
*announce it in English*, which is a design commitment, not a code commitment.

**Why it was chosen.** Zero APK size, zero native dependency, zero licence burden, and
on-device by construction. `RESEARCH/speech-hindi-audit.md` §1 states the core fact
plainly: the Hindi voice is *data installed by the engine*, not something the app ships, so
the "Hindi-first" promise is a platform dependency either way.

### 5.2 Option B: bundle a TTS engine — **NOT EVALUATED**

**What it is.** Ship a neural or formant TTS engine inside the APK (sherpa-onnx with a
Hindi voice model, eSpeak-NG with a Devanagari voice, Coqui/VITS, or a Google TTS
dependency) so the app controls the voice, its quality, and its availability.

**Evidence in this repo.** **None as a speech-synthesis option.** `sherpa-onnx` appears
exactly once in the whole repository — in `RESEARCH/speech-hindi-audit.md` line 137 — and
it is listed as a **speech-recognition** (keyword-spotting) candidate for P1 voice
commands, not as a TTS engine. There is no TTS-engine option recorded anywhere.

**What it would have cost.** Substantially. The `RESEARCH/speech-hindi-audit.md` §3 table
gives the same engines' weights in the ASR context: sherpa-onnx ~10-30 MB, Vosk ~40 MB,
Porcupine small but commercial. For TTS the comparable costs apply: a voice model
(typically tens of MB), a native runtime, an asset-licensing review, and a native build for
four ABIs (`x86_64`, `arm64-v8a`, `armeabi-v7a`, `x86` are already shipped — confirmed in
`PROJECT_STATUS.md` §2 and `RESEARCH/platform-api-audit.md` §7.7), plus the loss of the
zero-dependency property that makes the current APK's dependency story auditable.

**What it would have bought.** Control over Hindi quality and availability — which is
precisely the project's biggest platform risk. `PROJECT_STATUS.md` "Open blockers" item 1
and `RESEARCH/speech-hindi-audit.md` §1 both record that whether a usable `hi-IN` voice
exists on a target handset is **unknown**, and `RESEARCH/differentiation-hypothesis.md` H4
calls the Hindi voice "a platform dependency, not ours."

**Why it was not chosen.** Not evaluated. Given the APK is already 30.7 MB, a 10-30 MB
voice model is not obviously disqualifying, and the project's own honest position is that
"offline-first" is one of only four defensible grounds. **This is the largest untested
option in the document**, and it is the one with the clearest motivation in the
repository's own findings.

### 5.3 Option C: pre-recorded audio files — **NOT EVALUATED as an architecture; mentioned once as a demo mitigation**

**What it is.** Ship recorded WAV/MP3 clips for a fixed phrase set (and for a fixed set of
object names), and play them with `MediaPlayer`/`SoundPool` instead of synthesising speech.

**Evidence in this repo.** One mention, in `RESEARCH/skeptical-review-research.md` line
131, and it is a **demo** mitigation, not an architecture: *"Demo on a prepared device with
the voice installed, and **pre-record a fallback clip**."* There is no audio asset in the
repository, no `MediaPlayer`/`SoundPool` usage, and no `res/raw` directory.

**What it would have cost.** A recording and editing pipeline, per-locale voice actor
management (Hindi *and* English), and — decisively — **the loss of open-ended speech**.
The phrasebook is a template: `Phrasebook.objectFound` composes a position, a certainty
word and an object name at runtime, from 90 COCO classes and three zones and two confidence
bands. A recorded-clip design would need a combinatorial clip set, or a small number of
clips stitched together, which is a materially worse product than the current one.

**Why it was not chosen.** The product requirement — arbitrary object names in arbitrary
positions — rules it out for the main announcement path. It remains viable for a small
fixed set (the "nothing detected" line, the permission-denied line, the safety note), which
is exactly the demo use the skeptical review suggests.

**The one place it would help most, and the reason it matters:** `RESEARCH/speech-hindi-audit.md` §1's
worst case is a device with **no Hindi voice at all**, where the app must fall back to
English — and the English fallback is itself a TTS-rendered Devanagari-free line, so it
works. But a *pre-recorded* Hindi fallback would guarantee the Hindi experience on a device
with no Hindi voice data. That is a genuinely open question and is listed in §8.

### 5.4 Option D: cloud TTS — **rejected by the same reasoning as the cloud vision API**

Not separately evaluated; excluded by `RESEARCH/threat-model.md` T6 and the `INTERNET`-free
design. Same cost and same rejection as §3.5.

### 5.5 The unresolved speech problem, stated by the project's own documents

**VERIFIED**: `RESEARCH/speech-hindi-audit.md` §4 and `RESEARCH/skeptical-review-research.md`
§3 both record that `TextToSpeech.speak()` goes to the **media stream** by default while
TalkBack uses the **accessibility stream**, and that the OS does not arbitrate between them.
The likely default is therefore **two voices at once** — for exactly the users the project
exists to serve.

**VERIFIED**: `Speaker.kt` never calls `setAudioAttributes`, never requests audio focus, and
never queries the accessibility manager. A repository-wide search for `setAudioAttributes`,
`AudioFocus`, `audioAttributes`, `USAGE_` and `STREAM_` across the Kotlin sources returns
**no matches**.

**VERIFIED**: `RESEARCH/assistive-ux-requirements.md` §4 records the mitigations that are
*not* implemented: an explicit speech toggle, audio attributes, screen-reader detection.
`PROJECT_STATUS.md` "Open blockers" item 4 calls this "the most likely complete failure"
(tracked as issue #8).

**Consequence for this axis.** The TTS *mechanism* (Option A) is settled and defensible.
The TTS *configuration* is not. This is the most consequential open item in the whole
architecture, and it is not an architectural-alternative question — it is a defect in the
chosen option. It is listed in §8.

---

## 6. Axis 5 — Build and test strategy

### 6.1 Option A (chosen): two modules, JVM-only policy tests, no instrumented tests — **VERIFIED as built**

**What it is.** `:core` is a plain JVM module with a `kotlin("test")` suite that needs no
Android SDK. `:android:app` carries a small number of JVM unit tests for its
platform-independent logic. CI builds the debug APK and asserts the permission set.

**Evidence for.**

- `core/build.gradle.kts`: `kotlin("jvm")`, `jvmToolchain(17)`, `testImplementation(kotlin("test"))`,
  `tasks.test { useJUnitPlatform() }`.
- `android/app/build.gradle.kts` line 59: `unitTests.isReturnDefaultValues = true` — this is
  what lets the app-module JVM tests touch `android.*` classes without Robolectric.
- `.github/workflows/ci.yml` lines 29-32: `:core:test` then `:android:app:testDebugUnitTest`,
  both through `scripts/gradle-with-retry.sh`.
- `.github/workflows/ci.yml` lines 33-47: assemble the debug APK and **fail the build** if
  `android.permission.INTERNET` appears in `aapt2 dump permissions`.
- `.github/workflows/ci.yml` lines 85-96: compare the freshly built APK's payload against
  the published pre-release with `scripts/compare-apk-payload.py`, which compares ZIP
  entries rather than whole-file hashes because the debug certificate is per-machine.
- `scripts/gradle-with-retry.sh` distinguishes a network failure from a build failure and
  refuses to retry a real test failure — *"A broken test is never retried into a pass."*
- `docs/DEVELOPMENT_SETUP.md` §2 documents that the pure domain tests need **no Android
  SDK**: *"Pure domain tests (no Android SDK needed)"*.

**Test totals, VERIFIED by me in this session**, from the JUnit XML the build produced
(`./gradlew --offline :core:test :android:app:testDebugUnitTest --rerun-tasks` against
`e63840a`: **BUILD SUCCESSFUL**):

| Suite | Tests | Failures |
|---|---|---|
| `:core` `AnnouncementPolicyTest` | 8 | 0 |
| `:core` `AnnouncementPolicyPropertyTest` | 4 | 0 |
| `:android:app` `DetectionDecoderTest` | 9 | 0 |
| `:android:app` `InputTensorBuilderTest` | 7 | 0 |
| `:android:app` `OutputTensorResolverTest` | 8 | 0 |
| **Total at `e63840a`** | **36** | **0** |

At the earlier commit `afc027c` the total was **28** (12 in `:core`, 16 in `:android:app`);
the eight `OutputTensorResolverTest` tests were added by the detector fix described in
§3.6, taking the total to **36**. `PROJECT_STATUS.md` and `CHANGELOG.md` were updated to
the same 36 in the same commit (`e63840a`), so the project does not publish two different
test counts.

**What it would have cost.** Little: two modules, one version-catalog file, one retry
wrapper script. The expensive part of the strategy is what it *buys*: a test suite that
runs on any JDK-17 machine with no emulator, no device, and no Android SDK.

**Why it was chosen.** The repository's reasoning is explicit in `PROJECT_STATUS.md`
"Release gate" item 2 and in `docs/adr/0001-core-architecture.md` §4: because there is no
device, the policy suite must run without one, or the project's most safety-critical logic
would be the least tested.

### 6.2 Option B: Robolectric for the app module — **NOT EVALUATED**

**What it is.** Add `org.robolectric:robolectric` so the app module's JVM tests can
exercise Android framework classes (a fake `TextToSpeech`, a fake `ImageProxy`, a fake
`Context`) without a device.

**Evidence in this repo.** **None.** No Robolectric dependency, no `robolectric` config
block, no `@RunWith(RobolectricTestRunner::class)` anywhere.

**What it would have cost.** A heavy test dependency, slower JVM test runs, and Robolectric's
own Android-runtime emulation. In exchange: the ability to test `Speaker`'s locale fallback
logic, `YuvFrameConverter`'s conversion, and — if desired — the ViewModel's detection-to-
speech path, on the JVM.

**Why it was not chosen.** Not evaluated. The project's current answer to "how do we test
the Android layer" has been "we don't, yet" — the 24 app-module tests deliberately test only
the parts that were written to be TensorFlow-free. Given that `Phrasebook` (pure Kotlin,
safety-critical wording) and `Speaker` (the fallback logic) are both untested, **this is
the most obvious available test-coverage gap** in the current strategy.

### 6.3 Option C: instrumented (`androidTest`) tests on a device or emulator — **NOT EVALUATED, and currently impossible**

**What it is.** `androidTest` sources run on a device or emulator via
`AndroidJUnitRunner` — the `testInstrumentationRunner` is already configured
(`android/app/build.gradle.kts` line 17) and the `androidTestImplementation` dependencies
are already declared (lines 87-90), but **the source set is empty** and no such test has
ever run.

**Evidence in this repo.** `android/app/src/androidTest` exists and is **empty**; `git ls-files`
shows no instrumented test file. `PROJECT_STATUS.md` release-gate item 3 records "APK
exists, installable, launches" as **Partial**, with "install/launch untested", and item 4
("core detection and TTS verified on a real device") as **Not met**.

**What it would have cost.** A device or emulator in CI — which is the expensive part. An
emulator in GitHub Actions needs hardware acceleration (KVM), which is not available on
standard hosted runners; a physical-device farm is a real budget line. `RESEARCH/benchmark-plan.md`
§3 additionally specifies the conditions such a run would need (release-like profileable
build, fixed thermal state, three independent invocations, recorded per-invocation outputs).

**Why it was not chosen.** Not evaluated — and, honestly, not yet *possible*, because the
project has no device and no CI device infrastructure.

### 6.4 Option D: a desktop Python harness alongside the app — **VERIFIED as built, and unusually well matched to this project**

**What it is.** A separate Python toolchain in `benchmarks/` that loads the same `.tflite`
asset and runs the same inference, so that model-level questions can be answered without
an Android device.

**Evidence for.** `benchmarks/scripts/inspect_model.py`, `run_benchmark.py`,
`run_detection.py`, `verify_preprocessing.py`, and `benchmarks/model/coco_labels.py`;
`benchmarks/results/detector_benchmark.json`; `benchmarks/benchmark-schema.json` (a JSON
Schema that makes a fabricated or zero-filled measurement structurally invalid).

**What it cost.** A second implementation of the model's contract in Python, and the
obligation to keep the two in sync — `CocoLabels.kt`'s own header comment says so:
*"The same table and offset live in `benchmarks/model/coco_labels.py`; keep the two in
sync."*

**Why it was chosen / what it bought.** Two things. First, it answered the questions that
§1.4 and §3.6 show are answerable without a device: the real tensor contract, the label
offset, and the input convention. Second — and this is the genuinely clever part — it
supplied the *control* that made the preprocessing convention verifiable rather than
assumed. `RESEARCH/model-and-data-audit.md` §3 explains that the app and the benchmark
sharing a convention is "reassuring but **not proof**", so the model was run on real images
under three candidate conventions and the app's was shown to be the best. Without a
separate implementation there is no independent check at all.

**VERIFIED limitation of this strategy, stated honestly.** `RESEARCH/model-and-data-audit.md`
§3 notes the test images are **deliberately not committed** (third-party fixtures;
`.gitignore` excludes `*.jpg`), so the three-convention comparison **cannot run in CI**.
The repository's own documentation claims it does — `RESEARCH/model-and-data-audit.md` §3
says the comparison "stays reproducible locally with
`benchmarks/scripts/verify_preprocessing.py`", while `PROJECT_STATUS.md` line 75 says "now
in CI". **The CI workflow does not run it**: `.github/workflows/ci.yml` contains no
reference to `verify_preprocessing.py` or to any benchmark script; the only `benchmarks/`
reference in either workflow is the JSON-Schema validation at lines 176-183. The guard that
*does* run is the JVM unit test on `InputTensorBuilder`. **This documentation-versus-CI
discrepancy is recorded here rather than silently repeated** (see also §7.1).

### 6.5 Option E: a shared-module + `androidx` test-utilities layout — **NOT EVALUATED**

**What it is.** A `:core` that depends on `androidx` test artifacts, with a `domain` package
inside the app module rather than a separate module.

`docs/adr/0001-core-architecture.md` §"Alternatives considered" D records this as evaluated
and rejected in favour of the current split.

### 6.6 Option F: Kotlin Multiplatform's common-test source set — **NOT EVALUATED**

The natural KMP analogue of §2.3 applied to testing. Since KMP itself was never evaluated
(§2.3), neither was this.

---

## 7. Scope, honesty, and how this document relates to the concurrent work

### 7.1 Relationship to `docs/adr/0001-core-architecture.md` and `RESEARCH/platform-api-audit.md`

**VERIFIED**: while this document was being written, the working tree was **dirty** with
changes I did not make — a new `OutputTensorResolver.kt` and `OutputTensorResolverTest.kt`,
a modified `EfficientDetDetector.kt`, a new `docs/adr/0001-core-architecture.md`, a new
`RESEARCH/platform-api-audit.md`, and edits to `PROJECT_STATUS.md`, `CHANGELOG.md`,
`gradle/libs.versions.toml`, `website/index.html`, `scripts/check-website-links.py` and
`.github/workflows/website.yml`. Those changes were subsequently committed as `e63840a`
("fix: the detector would never have detected anything") and `0a55de5` ("docs: publish the
architecture decision record") while this document was still being written.

**I did not make, modify, or revert any of those changes.** This document was written
against the committed tree, and every factual claim in it was re-checked against
`e63840a`. The later commit `0a55de5` touched only documentation and the website
workflow — no source, build or CI file — so it does not affect any citation here.

**Two observations from that concurrent work that this document can make, because it was
written against the committed state:**

1. It **confirms** the "own the decode path" cost (§3.6): the fix required a new
   position-based resolver and a new test file, because the original value-range
   heuristic was unsound. The defect was found by *reading*, not by a test.
2. `RESEARCH/platform-api-audit.md` §4/§5 independently corroborate the §4.3 analysis
   above: main-thread conversion and main-thread model load are unfixed, and the
   on-device measurement that would settle the choice has not been made. That audit
   explicitly leaves them unfixed, because each needs a device to validate against a
   known-good reference.

**Where the two documents overlap — the `:core`/app split, the ML Kit rejection, the
cloud rejection — they agree.** The ADR is the authoritative *decision record* for the
module boundary; this document is the *options analysis* behind and around it, and adds
the alternatives the ADR does not cover (MediaPipe, TFLite Support metadata, Camera2
polling, explicit frame sampling, a bundled TTS engine, pre-recorded audio, Robolectric).

**The ADR's closing note independently reaches the same conclusion this document reaches
in §3.6, from the other direction.** Its stated argument: *"The failure was in the
platform-facing layer, and it was invisible to every test in `:core` — because `:core`
does not own the decode path and never will. A pure domain policy is testable to the last
line, and the layer that translates pixels into domain values is where silent, total
failures live."* That is the same trade this document names: the split buys a policy that
is testable to the last line, and the price is that the pixel-to-domain layer is the
project's own problem and can fail silently. **Both documents agree, and neither treats
the fix in `e63840a` as evidence that the chosen architecture is risk-free.**

**One discrepancy this document records rather than repeats.** `RESEARCH/model-and-data-audit.md`
§3 and `PROJECT_STATUS.md` line 75 both state that the three-convention preprocessing
comparison is "now in CI". **It is not.** `.github/workflows/ci.yml` contains no reference
to `verify_preprocessing.py` or to any benchmark script; the only `benchmarks/` reference
in either workflow is the JSON-Schema validation at lines 176-183. The guard that *does*
run is the JVM unit test on `InputTensorBuilder`. This is recorded here because a research
document that repeats an unverified CI claim is worse than one that names it.

### 7.2 Options that were never seriously evaluated

Listed together, so that a reader does not mistake silence for a considered rejection:

| Option | Axis | Why it was probably skipped |
|---|---|---|
| Kotlin Multiplatform / shared native core | Policy location | No second platform consumer exists |
| Android library module for the policy | Policy location | Moot: the clock is already injected |
| MediaPipe Tasks Vision Object Detector | Detector | Newer surface; LiteRT-direct was already measurable |
| TFLite Support / metadata-driven pipeline | Detector | The cheaper path (write the decode directly) was available |
| Direct Camera2 with polled frames | Threading | CameraX covers the need; polling grants nothing |
| Explicit frame-sampling rate | Threading | Nobody had measured the cost of not sampling |
| Bundled TTS engine with a Hindi voice | Speech | Not attempted despite being the clearest answer to the project's biggest platform risk |
| Pre-recorded audio for the fixed phrase set | Speech | Viable only for a small fixed set; ruled out for object announcements |
| Robolectric for the app module | Build/test | The app layer was deliberately left thin |
| Instrumented tests | Build/test | No device, no emulator, no CI device infrastructure |
| Kotlin Multiplatform common-test source set | Build/test | Follows from KMP never being evaluated |

### 7.3 Trade-offs that remain open

| # | Open trade-off | Where it is recorded | What would settle it |
|---|---|---|---|
| O1 | Main-thread YUV conversion and model load vs. a dedicated inference worker | `RESEARCH/platform-api-audit.md` §4, §5; §4.3 above | On-device profiling (Microbenchmark) on real hardware |
| O2 | Full-frame-rate inference vs. explicit frame sampling | §4.4 above | Thermal + battery measurement over a long session |
| O3 | Concurrent access to a single shared `Interpreter` | §4.3 above (**INFERRED**, not reproduced) | A serialisation guarantee, or a stress test |
| O4 | TalkBack audio collision — media stream vs. accessibility stream | `PROJECT_STATUS.md` blocker 4; `RESEARCH/speech-hindi-audit.md` §4; §5.5 above | A manual TalkBack pass on a real device, with audio recorded |
| O5 | Missing Hindi voice: platform dependency vs. bundling an engine | `RESEARCH/speech-hindi-audit.md` §1; §5.2 above | Survey target devices; then a costed comparison |
| O6 | Confidence thresholds 0.70/0.45 are uncalibrated | `PROJECT_STATUS.md` blocker 3; `RESEARCH/model-and-data-audit.md` §5 | The field protocol in `TESTING/field-evaluation-protocol.md` |
| O7 | Image-relative position may be useless; may need deleting | `RESEARCH/assistive-ux-requirements.md` §7 H3; `PROJECT_STATUS.md` blocker 5 | The stationary reaching task |
| O8 | Whether the "nothing detected" line should be spoken at all | `RESEARCH/assistive-ux-requirements.md` §3 (issue #7) | Field data on announcement burden |
| O9 | `Phrasebook` wording is untested despite being safety-critical | §2.1 above | A unit test over the phrase table, plus native-speaker review (issue #7) |
| O10 | Detector head choice if the class set or accuracy proves insufficient | §3.7 above | The existing `benchmarks/` harness, then a field evaluation |

---

## 8. What would have to be true for a different option to win

For each option, the specific, checkable condition under which it would beat what is
built. These are written so they can be falsified.

### 8.1 For KMP / a shared native core (§2.3) to win

- A **second, non-Android consumer** of the policy must exist — an iOS app, a desktop
  tool, a shared library — with a committed owner. Today there is none.
- The shared module must still let `./gradlew :core:test` run on a JDK-only machine. If the
  KMP migration makes the policy suite require the Android SDK or a native toolchain, the
  migration has destroyed the property that justified the current split, and the trade is
  not worth it.
- The `:core` test count must not drop. 12 running policy tests → any number less is a
  regression.

### 8.2 For the detector to move off the bundled LiteRT path (§3.1)

- **To ML Kit custom:** the chosen model must be replaced by, or converted into, a model
  that carries ML Kit-compatible metadata *and* a label map — removing the "we would have to
  manufacture metadata for a model that ships without any" objection in
  `RESEARCH/model-and-data-audit.md` §2. And the Play-Services-free path must be preserved
  (the *custom* API is bundled; the base API is not).
- **To MediaPipe Tasks (§3.3):** MediaPipe's `ObjectDetector` must be shown to decode this
  model correctly with no loss of detection quality, and the resulting APK size increase
  must be accepted. MediaPipe would also have to be shown not to reintroduce a
  network-dependent model fetch.
- **To a metadata-driven TFLite Support pipeline (§3.4):** the asset must be re-issued with
  an embedded metadata section, and the label table must move from `CocoLabels.kt` into the
  model — eliminating the two-place sync obligation the code comments call out.
- **For any of them to be worth the switch:** the §3.6 class of defect must be shown to be
  impossible under the new pipeline, *and* the field data must show the current detector's
  accuracy or class set to be insufficient (§3.7, O10).

### 8.3 For a cloud API (§3.5) to win

- The offline-first property must be formally abandoned, and the `INTERNET` permission's
  absence must stop being the project's strongest privacy guarantee — which
  `RESEARCH/threat-model.md` T6 says is the *entire* mechanism of that guarantee.
- A consent flow, a backend, accounts, and an API key must exist and be operated.
- The safety analysis must be redone for free-text captions (`RESEARCH/threat-model.md`
  T12), and a bystander-privacy position must be published
  (`RESEARCH/assistive-ux-requirements.md` §6 rule 4 forbids collecting bystander imagery).
- And, honestly: the positioning in `RESEARCH/differentiation-hypothesis.md` §3 would have to
  be rewritten, because "nothing leaves the phone" is one of only four remaining grounds.

### 8.4 For a bundled TTS engine (§5.2) to win

- A **target-device survey** must show that a usable `hi-IN` voice is missing on a
  substantial fraction of the phones this project would serve — the premise that makes
  "Hindi-first" hollow. The repository currently records this as UNKNOWN, not as rare.
- The APK cost must be accepted: a voice model plus a native runtime, against an APK that
  is already 30.7 MB.
- The licence of the chosen engine and voice model must be audited to the standard
  `RESEARCH/open-source-license-audit.md` sets for everything else in the APK.
- The `Speaker` abstraction must survive the swap. It is currently thin but real
  (`Speaker.kt` is a wrapper with `speak`/`stop`/`close`), and `DishaViewModel` depends on
  it — but the interface would need to accommodate synthesis and pre-recorded audio
  uniformly, since §5.3's clip-based fallback may also be wanted.

### 8.5 For the threading strategy to change (§4.3, §4.4)

- An **on-device measurement** must exist: `RESEARCH/benchmark-plan.md` §3's protocol, run
  on real hardware, showing that the current main-thread conversion (Finding C) or the
  continuous full-rate inference (O2) is measurably too expensive — in latency, thermal
  terms, or battery.
- If the answer is yes, the **minimum** change is to move the analyzer to a dedicated
  executor and serialise inference against the shared `Interpreter` (O3). The **maximum**
  change is to sample frames at a fixed cadence. The current code is 6 lines away from the
  first and would be ~10 lines away from the second; the reason neither was done is that
  nobody has the measurement that would justify either.

### 8.6 For the test strategy to change (§6.2, §6.3)

- **Robolectric** wins if the team wants `Phrasebook` and `Speaker` covered without a
  device. That is a coverage gain on the two most safety-critical untested units (O9), at
  the cost of a heavy test dependency.
- **Instrumented tests** win if a device or emulator becomes available — which is blocked
  on `PROJECT_STATUS.md` blocker 1, and on the CI hardware story in §6.3.

---

## 9. Summary of the chosen architecture

**VERIFIED as built.** For reference, the architecture this analysis describes is:

```
:core  (pure Kotlin/JVM, no Android imports)          :android:app  (Android application)
─────────────────────────────────────────────         ─────────────────────────────────────
ImageZone / BoundingBox / Detection                    CameraX Preview + ImageAnalysis
ConfidenceBand                                         └─ STRATEGY_KEEP_ONLY_LATEST, YUV_420_888
zoneFor() / confidenceBand()                           └─ analyzer on main executor
AnnouncementPolicy (4 s cooldown on (label, zone))      └─ inference on Dispatchers.Default
Decision (sealed)                                      └─ proxy always closed
                                                       EfficientDetDetector (LiteRT, 2 threads, XNNPACK)
                                                       └─ MappedByteBuffer from the APK asset
                                                       └─ output layout resolved once, by shape+position
                                                       DetectionDecoder (score floor 0.25)
                                                       CocoLabels (model_index + 1 == coco_id)
                                                       YuvFrameConverter (hand-written BT.601)
                                                       Speaker (TextToSpeech, hi-IN → en fallback)
                                                       Phrasebook (Hindi + English per state)
                                                       DishaScreen (Compose, 160 dp control,
                                                       live region, permanent safety note)

  36 JVM tests (8+4 core, 9+7+8 app), 2 Gradle modules, CI asserts CAMERA-only and no INTERNET
  (:core's 12 tests need no Android SDK at all)
```

**The honest one-paragraph version.** The chosen architecture is deliberately small: a
policy module that runs and tests without an Android SDK, a detector the project owns
end-to-end so it can be measured and pinned, a platform TTS engine with an honest fallback,
and a build that fails if the app ever gains a network permission. Its costs are equally
deliberate and equally real — the app layer is untested, the phrasebook is untested, the
threading is measurably unproven, the model-decoding ownership has already produced one
critical latent defect, and the two capabilities that matter most to a blind user (a
Hindi voice on the device, and coexistence with TalkBack) depend on platform behaviour that
has never been observed on hardware. **None of this document is device evidence.**
