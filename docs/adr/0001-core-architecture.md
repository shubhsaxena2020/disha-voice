# ADR 0001 — Core architecture: a pure Kotlin/JVM policy module with the Android app as a thin platform adapter

- **Status:** Accepted
- **Date:** 2026-10-21
- **Deciders:** Disha Voice maintainers
- **Applies to:** `:core` (policy), `:android:app` (platform adapter), P0 prototype scope
- **Supersedes:** nothing
- **Amended by:** nothing yet

### Related documents

This ADR records one decision. Three other documents in the repository bear on it, and a
reader should read them together:

| Document | Relationship to this ADR |
|---|---|
| [`RESEARCH/architecture-options.md`](../../RESEARCH/architecture-options.md) | A wider survey of five architecture axes (where the policy lives, how detection runs, threading, speech, build/test strategy). It reaches the **same conclusion** on the `:core`/app split, and covers options this ADR does not evaluate — notably Kotlin Multiplatform, MediaPipe, and an explicit frame-sampling rate. |
| [`RESEARCH/platform-api-audit.md`](../../RESEARCH/platform-api-audit.md) | An audit of every platform API the app depends on, against the real model and the real built APK. It found the defect described in [Consequences](#negative) and documents four further findings that deliberately need a device. |
| [`RESEARCH/model-and-data-audit.md`](../../RESEARCH/model-and-data-audit.md) | The detector decision, the measured tensor contract, and why the raw confidence score cannot be treated as a safety signal — the reason the two-threshold policy in `:core` exists at all. |

---

## Context

Disha Voice is an offline-first Android prototype that describes what a blind or low-vision
user's camera can see, in Hindi, by speech. Its entire value is a single loop:

```
camera frame → object detection → decide what may be said → speak it
```

Two properties of that loop drive the architecture.

**First, the "decide what may be said" step is the product.** Not the detector and not the
speech synthesis. The decision that matters to a user is: *of everything the model saw, what
is worth saying out loud, how sure are we, and where in the image is it?* A continuously
running detector reports the same object across many consecutive frames, so speaking each
detection produces a near-continuous stream of short utterances. `RESEARCH/assistive-ux-requirements.md`
§2 treats this as the central research concern, not a performance detail: the domain policy
suppresses a repeat of the same `(label, image-zone)` pair for four seconds, and tentative
detections are worded differently from confident ones.

That step is also where the safety-critical wording decisions live — never assert distance,
never claim a route is clear, phrase the negative state as a statement about the model's view
rather than about the room. These are rules about *when to speak and what to say*, and they
are the rules most likely to be changed and most in need of tests.

**Second, the environment where that step must be verified is hostile to verification.**
The decision logic sits in the middle of CameraX, a TFLite interpreter, and
`android.speech.tts.TextToSpeech`. Testing it in situ means an emulator or a device. The
project has no physical Android device: `adb devices` is empty, and `PROJECT_STATUS.md`
records "No Android device" as the single largest gap. Every behaviour that matters most —
suppression, thresholds, zone boundaries, wording — would therefore be untested precisely
because the surrounding platform is what makes it hard to test.

The platform APIs also constrain the design directly:

- `TextToSpeech` is asynchronous — `onInit` reports success or failure later — and its
  Hindi availability is a property of the installed device, not of the app.
- CameraX delivers frames through `ImageProxy`, which must be closed or the pipeline stalls.
- TFLite's `Interpreter` must be created, held, and closed, and its output tensors have a
  specific layout.
- A detector's confidence score is a class posterior, **not** a probability that the label is
  correct in the user's situation, and it is not calibrated
  (`RESEARCH/model-and-data-audit.md` §5). It cannot be used as a safety signal, so the
  app needs a *policy* on top of it, and that policy needs to be separately testable.

The question this ADR settles is where the policy lives, and what is allowed to exist inside
the P0 boundary.

---

## Decision

### 1. The assistive-communication policy lives in a separate `:core` module that is pure Kotlin/JVM, with no Android dependencies

`:core` is a Gradle `kotlin("jvm")` module at the repository root. It compiles against
nothing but the Kotlin standard library.

Its entire production source is one file,
`core/src/main/kotlin/org/dishavoice/core/AnnouncementPolicy.kt`, which contains:

| Type | Responsibility |
|---|---|
| `ImageZone` (enum) | Horizontal region of the image: `LEFT`, `CENTER`, `RIGHT`. Documented as "never a world-space direction or distance". |
| `BoundingBox` (data class) | Image-relative rectangle normalised to `[0, 1]`. `isValid` rejects non-finite, out-of-range, or mis-ordered coordinates; `centerX` is `null` when the box is invalid. |
| `Detection` (data class) | A detector result in image coordinates: `label`, `confidence`, `bounds`. |
| `ConfidenceBand` (enum) | `CONFIDENT`, `TENTATIVE`, `IGNORE`. |
| `Decision` (sealed interface) | The policy's output: `Announce(label, zone, confidenceBand)`, `SuppressedLowConfidence`, `SuppressedInvalidDetection`, `SuppressedDuplicate`. |
| `zoneFor(bounds)` | Maps a box to a zone by horizontal centre, with boundaries at `0.38` and `0.62`. Returns `null` for an invalid box — it does not invent a direction. |
| `confidenceBand(confidence, confidentThreshold, tentativeThreshold)` | Classifies a score into a band. Thresholds default to `0.70` (confident) and `0.45` (tentative); below tentative is `IGNORE`. Non-finite or out-of-range scores are `IGNORE`. |
| `AnnouncementPolicy` (class) | The stateful repeat-suppression rule. Constructed per camera session with a `cooldownMillis` (default `4_000`). `decide(detection, nowMillis)` returns a `Decision`. |

Three properties of this class are worth stating explicitly, because they are the reason it
is the way it is:

- **Time is injected, not read.** `decide` takes `nowMillis` as a parameter. The class
  cannot call `SystemClock.elapsedRealtime()` because that is an Android API, and it must
  not read a wall clock because that would make tests non-deterministic. The app passes
  `android.os.SystemClock.elapsedRealtime()` at the call site
  (`DishaViewModel.handleDetections`).
- **Suppression is keyed on `(label, zone)`, case-insensitively.** The same label in a
  different image zone is a different key, so a user who pans the camera gets a fresh
  announcement. Labels are trimmed and lowercased for the key, but the *original* label
  casing is what is announced.
- **The policy never speaks.** It returns a `Decision`. The wording of that decision is a
  separate concern owned by the app module.

The module contains **no** Android or AndroidX types: `grep -rn "^import android\|^import
androidx" core/src/` returns nothing, and `:core:dependencies --configuration
compileClasspath` resolves to exactly `kotlin-stdlib` and `org.jetbrains:annotations`. Its
test runtime adds only `kotlin-test` and the JUnit 5 platform.

### 2. The Android app is a thin platform adapter over that policy

`:android:app` owns every platform API and contains no policy. Its packages are:

| Package | Types | Responsibility |
|---|---|---|
| `org.dishavoice.app` | `DishaViewModel`, `UiState`, `SessionState` | Session orchestration: camera binding, frame analysis, mapping `Decision` to speech. |
| `org.dishavoice.app.camera` | `YuvFrameConverter` | Converts a CameraX `ImageProxy` to a `Bitmap`, then recycles it. |
| `org.dishavoice.app.detection` | `Detector` (interface), `EfficientDetDetector`, `DetectionDecoder`, `InputTensorBuilder`, `CocoLabels` | On-device detection and the translation from model tensors to `org.dishavoice.core.Detection`. |
| `org.dishavoice.app.speech` | `Speaker`, `Phrasebook` | Text-to-speech and the wording. |
| `org.dishavoice.app.ui` | `DishaScreen`, `PreviewSurface` | Compose UI and accessibility semantics. |

The boundary is explicit in the type system and verifiable by inspection:

- `Detector` is the seam between platform and domain. It is declared in the app module
  (`org.dishavoice.app.detection.Detector`) and returns `List<org.dishavoice.core.Detection>`
  — the app depends on `:core`, and `:core` depends on nothing. Its own doc comment says
  implementations "must not persist frames".
- `DetectionDecoder` and `InputTensorBuilder` are deliberately kept free of TensorFlow and
  `Bitmap` types respectively, so the parts that decide a *convention* can be unit tested on
  the JVM. `DetectionDecoder` produces `org.dishavoice.core.BoundingBox` and
  `org.dishavoice.core.Detection` directly.
- `Phrasebook` consumes `org.dishavoice.core.ConfidenceBand` and
  `org.dishavoice.core.ImageZone` and turns a `Decision` into a sentence. It never computes
  a zone or a band; it only words the result.
- `DishaViewModel` is the only place that holds both halves. Its class comment states the
  intent: "The domain policy (:core) is the single place that decides what may be
  announced; this class only supplies it with detections and speaks the resulting phrase."
  It calls `policy.decide(it, now)` on every detection, filters for `Decision.Announce`,
  and selects one via `maxByOrNull { it.confidenceBand.ordinal * 1000 + it.zone.ordinal }`.

The dependency direction is therefore one-way: `:android:app` → `:core`. `core/build.gradle.kts`
declares no dependency on the app, and the app declares `implementation(project(":core"))`.

The `Decision` type is a *sealed* interface, which is what makes the boundary auditable: the
app's `handleDetections` currently branches on `Decision.SuppressedLowConfidence`, and the
compiler will flag any new `Decision` variant that the app has not handled.

### 3. Network, microphone, and cloud APIs are explicitly excluded from the P0 scope

This is a scope decision as much as an architectural one, and it is enforced by the build
rather than by convention:

- `android/app/src/main/AndroidManifest.xml` declares exactly one permission:
  `<uses-permission android:name="android.permission.CAMERA" />`. There is no
  `INTERNET`, no `RECORD_AUDIO`, no location, no storage, and no notification permission.
- `aapt2 dump permissions` on the built `app-debug.apk` confirms the same: `CAMERA` and
  nothing else from the platform set.
- The application element sets `android:allowBackup="false"`,
  `android:usesCleartextTraffic="false"`, and `android:fullBackupContent="false"`.
- The model ships **inside the APK** as
  `android/app/src/main/assets/efficientdet_lite0_detection.tflite` (4.4 MB), so detection
  needs no Play Services download and no network round-trip. `RESEARCH/model-and-data-audit.md`
  §2 records that ML Kit's built-in detector was rejected precisely because it is an
  *unbundled* library whose model must be fetched.
- No analytics, crash-reporting, or advertising SDK appears in `dependencies`.
- `RESEARCH/threat-model.md` T6 states the consequence directly: "No `INTERNET` permission is
  declared, so **the app literally cannot upload anything**." The data-flow diagram in that
  document has no edge leaving the device.

CI enforces this rather than trusting it. `.github/workflows/ci.yml` runs `aapt2 dump
permissions` on the freshly built APK and fails the build if `android.permission.INTERNET`
appears, requiring `android.permission.CAMERA` to be present.

The microphone is excluded for a specific reason rather than by omission: a screen-reader
user's phone is their primary input device, so requesting `RECORD_AUDIO` for a feature that
does not exist would impose a privacy cost on exactly the users this project serves
(`RESEARCH/threat-model.md` T17, §5). Voice commands are P1.

### 4. The policy is exercised by a deterministic JVM test suite, and the module is split so that CI can run it without the Android SDK

`core/build.gradle.kts` applies only `kotlin("jvm")` with `jvmToolchain(17)` and
`testImplementation(kotlin("test"))` — no Android Gradle Plugin, no Robolectric, no
emulator. The suite is in two files:

- `AnnouncementPolicyTest` — 8 behavioural tests: zone classification, deterministic
  boundaries at exactly `0.38` and `0.62`, rejection of invalid boxes rather than inventing
  a direction, the three confidence bands, cooldown suppression, the same label in a
  different zone *not* being suppressed, and suppression of low-confidence detections.
- `AnnouncementPolicyPropertyTest` — 4 property-based tests over a fixed seed
  (`0xD15A0`): 10,000 seeded valid boxes must map to the zone implied by their centre;
  confidence classification must be monotonic across a 10,000-step grid over `[0, 1]`;
  label-zone cooldown behaviour must be stable across 36 synthetic keys including
  whitespace-trimming and case variants; and invalid or low-confidence detections must not
  start a cooldown.

Because the tests are pure JVM, `:core:test` runs anywhere a JDK 17 exists. The Android SDK
is not needed — the README states this explicitly.

### 5. The app module additionally keeps its *platform-independent* logic in the app module but unit-testable, rather than pushing it down into `:core`

Two classes in the app module follow the same "pure decision, injected platform" pattern
without being moved into `:core`, because they are decoding concerns rather than
communication policy:

- `InputTensorBuilder.toRgbBytes(pixels, size)` decides the model's input convention and
  is a pure function over an `IntArray`. `buildFromBitmap` is the Android half.
- `DetectionDecoder.decode(...)` turns four flat `FloatArray`s into
  `List<org.dishavoice.core.Detection>` and holds no TensorFlow types.

Both are covered by JVM unit tests in `:android:app` (`InputTensorBuilderTest`,
`DetectionDecoderTest`), which run under `:android:app:testDebugUnitTest` in CI.

The reason these are not in `:core` is that the boundary is drawn around *what the module
is for*, not around *what is testable*. `:core` exists to hold the assistive-communication
policy. A decoder's job is to describe the shape of a particular model's output; it is a
property of that model and will change with the model. Moving it into `:core` would make
`:core` a general utilities module and dilute the decision this ADR records.

---

## Consequences

### Positive

1. **The policy is testable without a device, and it is tested.** `:core:test` was run and
   passes 12 tests with 0 failures, 0 errors, 0 skipped, reading the Gradle JUnit XML. The
   app module's own unit tests bring the repository total to 36 distinct test methods. The
   most safety-relevant behaviour in the product — when not to speak, how to qualify
   uncertainty, how to classify position — has executable assertions.
2. **The behaviour that matters is pinned against silent regression.** Several of the
   project's real bugs were of the "wrong but plausible" kind that no build would catch:
   the COCO label off-by-one that reported a Labrador as a "cat" at 0.84 confidence, and
   the input-convention drift that would degrade detection without breaking anything. The
   architecture gives these a place to live — a pure function with a unit test — and
   `DetectionDecoderTest` now pins all four label anchors.
3. **Deterministic property tests are possible at all.** Because time is a parameter
   rather than a system call, `AnnouncementPolicyPropertyTest` can sweep 10,000 seeded
   boxes and 10,000 confidence values and assert monotonicity and repeatability. A policy
   that read the clock itself could not be tested this way.
4. **The privacy guarantee is structural.** The no-network property is enforced by the
   manifest, verified by `aapt2`, and re-checked by CI on every build. It does not depend on
   any code being careful.
5. **The app module stays small and legible.** `DishaViewModel` is 234 lines and its
   orchestration is readable top to bottom: bind camera → analyse frame → `policy.decide` →
   speak. A reviewer can follow the control flow without an emulator.
6. **The dependency direction is enforced by the build.** `:core` cannot accidentally
   acquire an Android dependency, because `:core` is not an Android module and the AGP is
   not applied to it. The constraint is checked on every compile.
7. **The codebase states its own limits honestly.** `RESEARCH/model-and-data-audit.md`
   §5 and `docs/safety/SAFETY.md` both record that the thresholds are uncalibrated
   prototype defaults and that the prototype is not a mobility aid. Separating the policy
   made those claims easy to attach to specific constants (`DEFAULT_CONFIDENT_THRESHOLD`,
   `DEFAULT_TENTATIVE_THRESHOLD`) rather than to scattered literals.

### Negative

These are real costs, and they are not hypothetical.

1. **The policy cannot be exercised end-to-end without a device.** The JVM suite proves
   that `AnnouncementPolicy` maps detections to decisions. It does not prove that a camera
   frame ever reaches that policy, that a detection is ever produced from a real frame, or
   that a `Decision` ever becomes audible. The path from `CameraX` → `YuvFrameConverter` →
   `EfficientDetDetector` → `DetectionDecoder` → `AnnouncementPolicy` → `Phrasebook` →
   `Speaker` has no automated coverage at all, and there is no `androidTest` source set in
   the repository. The largest gap in this project is the one this architecture cannot
   close.
2. **The domain layer cannot talk to CameraX or TTS directly, and this is a genuine
   limitation, not a preference.** `:core` cannot request a permission, cannot observe a
   frame, cannot schedule work, and cannot speak. Anything that requires those capabilities
   *must* be expressed in the app module, which means the app module's tests need the
   Android SDK and are correspondingly slower and less deterministic than the `:core` suite.
   The cost of a change that touches the boundary — say, adding a new `Decision` variant, or
   changing how zones map to words — is paid in the app module, not in `:core`.
3. **A pure-JVM test suite does not prove on-device behaviour.** `:core:test` passing tells
   us the policy arithmetic is right. It says nothing about whether the app is usable: a
   policy that correctly suppresses a repeat is worthless if the app never announces the
   first detection, which is exactly the failure mode the concurrent work on
   `OutputTensorResolver` in this repository uncovered — a value-range test that classified
   the class-id tensor as the score tensor, causing the detector to return an empty list on
   every frame, silently, with the camera running. The class-id and score tensors of the
   shipped model have the *same shape*, so shape alone cannot separate them, and on a
   freshly allocated interpreter every output tensor reads as zero, so a value-range test
   cannot either. The correct fix is positional resolution in a pure, JVM-testable class —
   the same pattern this ADR prescribes. But note precisely what that fix does and does not
   establish: the bug was found by *reading the code and measuring the shipped asset*, not
   by any test, and it was not findable by the `:core` suite at all. It would not have been
   found by a device test either, because a detector returning an empty list on every frame
   looks identical, from the outside, to "no objects in view" — the failure surfaces only
   as the absence of speech. This is the sharpest illustration of what the `:core` suite
   cannot do for this project.
4. **The module boundary is a real ongoing tax.** Every new behaviour requires a decision
   about which module it belongs to, and the boundary is not obvious in every case — the
   `InputTensorBuilder` and `DetectionDecoder` cases above are the argument for a slightly
   fuzzy edge. Without a stated rule, this becomes an inconsistency generator over time.
5. **Two modules mean two build configurations, two test reports, and two places to look
   when something fails.** CI already aggregates both, but the aggregation is a Python
   script inside the workflow, and the distinct-test versus execution count distinction
   (the app module's tests run under both the debug and release variants) is already a
   documented source of confusion in `PROJECT_STATUS.md`.
6. **The `:core` suite cannot catch platform-contract drift.** The policy is pure, but the
   *data* it receives is produced by platform code. A change in the model, the label
   mapping, the input convention, or the tensor layout is invisible to `:core` and must be
   caught by the app module's tests or by manual benchmarking. The separation makes the
   policy safe and makes the pipeline around it the remaining risk.
7. **The exclusion of network and microphone APIs, while correct for P0, is a constraint
   that will be felt later.** It forecloses cloud captioning, remote model updates, and
   voice commands without a manifest change, a fresh consent and permission analysis, and
   a re-opening of the threat model (`RESEARCH/threat-model.md` T6, T17). That is the
   intended behaviour — the constraint is the feature — but it means a future networked
   feature cannot be added as a small change, and that is a cost the team has accepted
   deliberately rather than by accident.

---

## Alternatives considered

### A. Put the policy in the Android app module and use Robolectric

*Rejected.* The policy would then be exercised by Robolectric unit tests rather than plain
JVM tests, gaining Android APIs that the policy does not need (it needs none) while adding a
test dependency, a heavier test runtime, and a second way for the policy to acquire an
Android dependency. The central claim of this ADR — that the decision logic can be tested
deterministically with no Android SDK present — would no longer hold, and the module
boundary would become a convention rather than a build-enforced constraint.

### B. Put the policy in the app module and test it with instrumented (`androidTest`) tests

*Rejected.* This is the option that would have been chosen by default, and it is
unworkable in this project's current state: there is no device (`adb devices` is empty), the
`androidTest` source set is empty, and CI runs no emulator. Adopting it would mean the
policy's tests could not run at all in the current environment — the exact inversion of the
goal. It also couples the cost of running the policy's tests to emulator start-up, which is
minutes rather than seconds, and it would make the most safety-relevant behaviour in the
product dependent on the least reliable piece of infrastructure available.

### C. Keep everything in the app module but extract pure functions without a separate module

*Rejected, and this was the closest alternative.* The app module already demonstrates that
pure, JVM-testable functions are possible inside an Android module: `InputTensorBuilder`
and `DetectionDecoder` are both in `:android:app` and both are unit tested today. A
single module with pure functions would have delivered most of the testability benefit
without a second module. It was rejected because the `:core` module is not merely a
testing convenience: it is a **build-enforced** boundary. In a single module, nothing
prevents a future contributor from adding `import android.os.SystemClock` to the policy, or
from calling `TextToSpeech` from a decision function, and the boundary silently erodes one
convenient commit at a time. In a `kotlin("jvm")` module, that import does not compile.
The cost of the extra module is real — two build configurations, two test reports, and a
boundary that needs a stated rule — and it is paid to buy a constraint that holds
mechanically rather than by convention.

### D. Use a shared module with `androidx` test utilities and a `domain` package inside the app

*Rejected.* This is a naming variant of A and C: it puts the same pure code behind the same
non-enforced boundary, and it would tie the policy's tests to the Android build. The
benefit of a physically separate module is that its independence is checked by the
compiler and by the dependency graph, not by a package name.

### E. Put the policy in `:core` but let it depend on Android (i.e. an Android library module)

*Rejected.* This would allow the policy to call `SystemClock.elapsedRealtime()` directly
and to reach for platform utilities. It would destroy the determinism that the current
`decide(detection, nowMillis)` signature buys: the tests would need a device or a
shadow framework to supply a clock, and the property tests could not sweep time. The
injection of time is what makes the cooldown rule verifiable at all, and it is only
verifiable because `:core` cannot obtain a clock for itself.

### F. Use a cloud vision API for detection and/or description

*Rejected.* It breaks the offline-first design, and the offline-first design is the
project's stated differentiator (`README.md`, `RESEARCH/competitor-landscape.md`). It
would require the `INTERNET` permission, which is exactly the thing CI currently fails the
build over, and it would send a blind user's surroundings — their home layout, their
bystanders' faces, their medication, their mail — to a third party
(`RESEARCH/threat-model.md` T6, §1). It would also add a backend, accounts, and a consent
flow the prototype does not have. `RESEARCH/model-and-data-audit.md` §6 lists it as out of
scope for P0.

### G. Use ML Kit's built-in object detection instead of a bundled TFLite model

*Rejected on verified grounds, recorded here because it is a plausible future suggestion.*
`RESEARCH/model-and-data-audit.md` §2 documents that the base ML Kit detector is an
*unbundled* library whose model is delivered by Google Play Services, so a first run on a
device without it has no detection at all — which contradicts the offline-first requirement
— and that its coarse classifier covers only five categories, too few for common household
objects. The custom-object-detector path is workable but imposes metadata and label-file
conventions that would have to be fabricated for a model that ships with no embedded labels.
A bundled model keeps the entire detection path inside the process and inside the APK.

### H. Add the microphone now, for voice commands

*Rejected for P0.* `RESEARCH/threat-model.md` T17 and §5: requesting `RECORD_AUDIO` for a
feature that does not exist would impose an unjustified privacy cost on screen-reader users,
whose phone is their primary input device. Voice commands are P1 scope, and adding them
later is a deliberate, reviewed change with its own consent analysis.

### I. Adopt a fully offline architecture but let the policy read a wall clock internally

*Rejected.* `System.currentTimeMillis()` is not an Android API, so this would technically
fit inside a pure JVM module. It was rejected on correctness grounds: wall-clock time can
jump backwards (NTP correction, user changing the clock), and the cooldown rule is written
to handle that case by comparing `nowMillis >= lastTime` before applying the window. A
monotonic clock is the correct source for an interval, and `SystemClock.elapsedRealtime()`
is the Android equivalent — which is why the app passes it in rather than the policy
reaching for it.

---

## Status of the evidence

This section separates what is verified in code and in CI from what remains unverified
because no physical Android device was available. It is the honest boundary of this ADR.

### Verified in code, and confirmed by running the build and the tests on 2026-10-21

| Claim | How it was verified |
|---|---|
| `:core` is a pure Kotlin/JVM module with no Android dependency. | `core/build.gradle.kts` applies only `kotlin("jvm")` with `jvmToolchain(17)`. `grep -rn "^import android\|^import androidx" core/src/` returns nothing. `:core:dependencies --configuration compileClasspath` resolves to `kotlin-stdlib` and `org.jetbrains:annotations` only; `testRuntimeClasspath` adds only `kotlin-test` and the JUnit 5 platform. |
| The `:core` suite passes. | `./gradlew :core:test --rerun-tasks` → BUILD SUCCESSFUL. The JUnit XML reports `AnnouncementPolicyTest` tests=8 failures=0 errors=0 skipped=0 and `AnnouncementPolicyPropertyTest` tests=4 failures=0 errors=0 skipped=0 — **12 tests, 0 failures**. |
| The repository has 36 distinct test methods, all passing. | Counted in source: 8 + 4 in `:core`, 9 in `DetectionDecoderTest`, 7 in `InputTensorBuilderTest`, 8 in `OutputTensorResolverTest`. Confirmed by the Gradle JUnit XML: 12 in `:core` and 24 in `:android:app`, 0 failures, 0 errors, 0 skipped. |
| The app builds and produces a debug APK. | `./gradlew :android:app:assembleDebug` → BUILD SUCCESSFUL; `app-debug.apk`, 30,701,552 bytes. (It was 30,671,369 bytes at the commit this ADR was verified against; it grew by the third-party licence notices and the Apache-2.0 licence text, which the APK now carries — see `THIRD_PARTY_NOTICES.md`.) |
| The APK requests only the camera. | `aapt2 dump permissions` on the built APK: `android.permission.CAMERA` and no `INTERNET`. The manifest declares exactly one `uses-permission` element. |
| The dependency direction is one-way. | `android/app/build.gradle.kts` declares `implementation(project(":core"))`; `core/build.gradle.kts` declares no project dependency. |
| The app depends on the domain types and the domain depends on nothing. | `grep "import org.dishavoice.core"` in the app resolves to `AnnouncementPolicy`, `BoundingBox`, `ConfidenceBand`, `Decision`, `Detection`, `ImageZone`. No reverse import exists. |
| The `Decision` type is sealed and the app branches on it. | `sealed interface Decision` with four variants; `DishaViewModel.handleDetections` filters `Decision.Announce` and tests `Decision.SuppressedLowConfidence`. |
| Time is injected into the policy. | `AnnouncementPolicy.decide(detection: Detection, nowMillis: Long)`; the app passes `android.os.SystemClock.elapsedRealtime()`. `:core` contains no clock call. |
| The model is bundled, so detection needs no network. | `android/app/src/main/assets/efficientdet_lite0_detection.tflite` is present in the source tree and in the built APK. |
| CI enforces the no-network property. | `.github/workflows/ci.yml` runs `aapt2 dump permissions` on the built APK and exits non-zero if `android.permission.INTERNET` is present. |
| CI runs the `:core` suite without the Android SDK. | The `core-tests` job runs `./scripts/gradle-with-retry.sh :core:test` on `ubuntu-latest` with only JDK 17 set up — no Android SDK installation step precedes it. |
| There is no instrumented test coverage. | `android/app/src/androidTest/` contains zero files, and `.github/workflows/ci.yml` contains no emulator, device, or connected-check step. |
| No device is attached. | `adb devices` lists no devices. |
| The model's tensor contract and label offset are measured, not assumed. | `RESEARCH/model-and-data-audit.md` §3: input `[1, 320, 320, 3]` uint8; outputs boxes `[1,25,4]`, classes `[1,25]`, scores `[1,25]`, count `[1]`; `model_class_index + 1 == coco_category_id`, established with four independent anchors. `DetectionDecoderTest` pins all four. |

### Not verified, because no physical Android device was available

These are open. The architecture recorded in this ADR does not close any of them, and none
of them is closed by the 12 passing `:core` tests.

1. **The app has never been installed or launched on a handset.** `PROJECT_STATUS.md`
   records install/launch as untested and "No Android device" as the single largest gap.
   The APK is verified structurally — it builds, its permissions are confirmed, its payload
   is byte-reproducible — and that is all.
2. **The camera pipeline has never run on real hardware.** Whether `YuvFrameConverter`
   produces correct colours from a real `ImageProxy`, whether `STRATEGY_KEEP_ONLY_LATEST`
   backpressure behaves as intended under load, and whether the `ImageProxy` is always
   closed, are unverified. The YUV→ARGB conversion in `YuvFrameConverter.yuvToArgb` is a
   hand-written integer approximation of the BT.601 transform with no test at all.
3. **On-device detection is unverified on a phone.** The detection evidence in
   `RESEARCH/model-and-data-audit.md` §4 is **desktop-CPU** LiteRT output: dog 0.840, bus
   0.801, latencies of 25–44 ms. That document says so explicitly — "This is NOT a phone
   measurement" — and `PROJECT_STATUS.md` repeats it. Phone latency, memory, thermal
   behaviour, and battery impact are all unknown.
4. **Hindi `TextToSpeech` on a real handset is unverified.** `Speaker.configureLocale`
   checks `isLanguageAvailable` for `hi_IN` and falls back to English, reporting
   `isHindiAvailable` so the UI and `Phrasebook` can say so plainly. Whether target
   devices actually have a usable Hindi voice, and whether the Devanagari phrases are
   intelligible to the intended users, are unknown
   (`RESEARCH/model-and-data-audit.md` §8).
5. **The TalkBack audio collision is unverified and is the most likely total failure.**
   The app speaks through `TextToSpeech` on the media stream by default while a screen
   reader is likely using the accessibility stream. The OS does not arbitrate between them.
   `Speaker` does not set an audio attribute, does not request audio focus, and does not
   detect an active screen reader. This is unmitigated (tracked as issue #8), and no
   manual TalkBack review has been done.
6. **The end-to-end loop is unverified.** Whether a detection produced on a real frame
   actually becomes an audible, correctly-worded announcement is unknown. The concurrent
   work in this repository on `OutputTensorResolver` is a concrete illustration: a
   value-range test that separated the class-id tensor from the score tensor worked on
   paper and failed completely in practice, because on a freshly allocated interpreter
   every output tensor reads as zero. The result was a detector that returned an empty list
   on every frame — the camera running, nothing ever announced, and no error surfaced
   anywhere. That failure was invisible to the `:core` suite by construction, and it would
   have been invisible to a device test as well, because the only symptom is the absence
   of speech. It was found by reading the code and measuring the shipped asset
   (`RESEARCH/platform-api-audit.md` §2).
7. **The confidence thresholds are uncalibrated.** `0.70` and `0.45` are prototype
   defaults, chosen as a starting point. `RESEARCH/model-and-data-audit.md` §5 explains why
   the raw score is not a correctness probability, and `PROJECT_STATUS.md` lists
   calibration as an open blocker. No accuracy data exists: zero labelled observations have
   been collected, and no accuracy figure may be published.
8. **Image-relative position is unproven as a useful output.** "Left / centre / right *of the
   image*" is true about the picture; whether it helps a blind user act is an open research
   question (issue #3), with a standing recommendation to delete the feature if the field
   evaluation does not show it beating "no position".
9. **The accessibility acceptance checklist in `RESEARCH/assistive-ux-requirements.md` §5 is
   largely unticked.** The unchecked items include a manual screen-reader pass, dynamic-type
   handling at the largest system font scale, dark theme, reduced motion in the app UI, and
   the failure-state matrix. The ticked items are the ones that could be checked statically:
   the 160 dp primary control, the 64 dp secondary controls, and the WCAG AA contrast
   figures.

### The state of this document

This ADR was written on 2026-10-21 against commit `e63840a`, and every figure quoted
above was re-verified against the tree as it stood at that commit. `:core` was re-run with
`--rerun-tasks` immediately before this document was finalised.

| Suite | Tests | Failures | Errors | Skipped |
|---|---|---|---|---|
| `:core` — `AnnouncementPolicyTest` | 8 | 0 | 0 | 0 |
| `:core` — `AnnouncementPolicyPropertyTest` | 4 | 0 | 0 | 0 |
| `:android:app` — `DetectionDecoderTest` | 9 | 0 | 0 | 0 |
| `:android:app` — `InputTensorBuilderTest` | 7 | 0 | 0 | 0 |
| `:android:app` — `OutputTensorResolverTest` | 8 | 0 | 0 | 0 |
| **Total** | **36** | **0** | **0** | **0** |

**One thing this ADR does not cover, because it is not a decision about the architecture.**
While this ADR was being written, the same working tree was being used to fix a defect in
the *detector*, not in the policy: `EfficientDetDetector` had been separating the model's
class-id tensor from its score tensor by inspecting their values, which cannot work because
every output tensor reads as zero on a freshly initialised interpreter. That defect, its
measurement, and the fix are documented in `RESEARCH/platform-api-audit.md` section 2.

It is recorded here for one reason: it is the clearest available evidence for the
architectural claim this ADR makes. The failure was in the platform-facing layer, and it
was invisible to every test in `:core` — because `:core` does not own the decode path and
never will. A pure domain policy is testable to the last line, and the layer that
translates pixels into domain values is where silent, total failures live. That is an
argument for the `:core`/app split and an argument against pretending a green JVM test
suite says anything about the app.

The architectural claim this ADR makes — that `:core` is pure Kotlin/JVM, that the app
depends on `:core` one-way, and that the policy is testable without the Android SDK — is
structural and does not depend on the test counts above.
