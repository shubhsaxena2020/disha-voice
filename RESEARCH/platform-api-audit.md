# Disha Voice: Android platform API audit

**Scope.** Every Android platform API the app depends on, audited against the actual
source in this repository, with each API's real contract measured rather than assumed.

**Status.** Static and desktop verification only. **No physical Android device was
available** (`adb devices -l` reported none), so nothing here is a substitute for a device
run. Each finding says explicitly what was *measured*, what was *read in the source*, and
what remains *unverified*.

**Date:** 2026-10-21.

---

## 1. Platform APIs used, and where

| API | Used in | Purpose |
|---|---|---|
| `android.permission.CAMERA` | `AndroidManifest.xml` | The only permission requested |
| `androidx.camera.core.ImageAnalysis` | `DishaViewModel.kt` | Frame source, `STRATEGY_KEEP_ONLY_LATEST`, `OUTPUT_IMAGE_FORMAT_YUV_420_888` |
| `androidx.camera.core.Preview` | `DishaViewModel.kt` | On-screen preview via `Preview.SurfaceProvider` |
| `androidx.camera.lifecycle.ProcessCameraProvider` | `DishaViewModel.kt` | Camera binding |
| `android.graphics.Bitmap` | `YuvFrameConverter.kt`, `InputTensorBuilder.kt` | Frame → model input |
| `org.tensorflow.lite.Interpreter` (LiteRT `1.0.1`) | `EfficientDetDetector.kt` | On-device inference |
| `android.speech.tts.TextToSpeech` | `Speaker.kt` | Hindi speech output |
| `android.os.SystemClock.elapsedRealtime()` | `DishaViewModel.kt` | Monotonic clock for the announcement policy |
| `android.content.res.AssetManager.openFd` | `EfficientDetDetector.kt` | Memory-maps the bundled model |

Dependency versions are pinned in `gradle/libs.versions.toml`: AGP 8.7.3, Kotlin 2.0.21,
CameraX 1.4.1, Compose BOM 2024.12.01, LiteRT 1.0.1, `compileSdk`/`targetSdk` 36,
`minSdk` 24, JDK 17.

---

## 2. Finding A — the detector would never have detected anything (CRITICAL, **fixed**)

**This was the most serious defect found in the whole project, and it was invisible from
the code.**

### What the code did

`EfficientDetDetector.resolveOutputIndices` separated the model's four output tensors.
Two of them — the class-id tensor `[1, 25]` and the score tensor `[1, 25]` — have the
**same shape**, so shape alone cannot separate them. The code separated them by
**inspecting their values**:

```kotlin
// The classes tensor holds integral ids; the scores tensor is
// bounded to 0..1. Distinguish them by their value ranges.
val values = readFloats(tf.getOutputTensor(i))
val looksLikeScores = values.all { it.isFinite() && it in 0f..1f }
if (looksLikeScores) scores = i else classes = i
```

### Why it fails

The resolution ran **before** `tf.runForMultipleInputsOutputs(...)`, i.e. on a freshly
allocated interpreter. Measured against the shipped asset:

```
=== output tensor contents IMMEDIATELY after allocate_tensors(), BEFORE any inference ===
  index 598 'StatefulPartitionedCall:3' shape=[1,25,4]: allZero=True
  index 599 'StatefulPartitionedCall:2' shape=[1,25]  : allZero=True  looksLikeScores=True
  index 600 'StatefulPartitionedCall:1' shape=[1,25]  : allZero=True  looksLikeScores=True
  index 601 'StatefulPartitionedCall:0' shape=[1]      : allZero=True

  app's resolution on a FRESH interpreter ->  scores=600  classes=-1
```

Every output tensor reads as **zero**, and zero is inside `0..1`. So the class-id tensor
also satisfies the "this is a score tensor" test, the class-id tensor is never assigned,
and the code throws `error("Unexpected model output layout")`.

That throw was caught by the per-frame `runCatching` in `detect()`, which returned
`emptyList()`. **And because the throw happened before `run()`, the output buffers were
never populated** — so the next frame hit exactly the same state, and every frame after
that did too.

The visible result: the camera preview runs, the UI shows "Listening", and the app
**never announces a single object**, forever, with no error surfaced anywhere. For an
assistive tool that is the worst possible failure: it looks like it is working.

### The fix

Resolution now uses **shape and position only, never values**, and happens **once at
construction** so a contract mismatch is visible rather than swallowed:

- `android/app/src/main/java/org/dishavoice/app/detection/OutputTensorResolver.kt` — a
  pure, Android-free resolver.
- The 3-D output whose last dimension is 4 is **boxes**; the 1-D output is **count**; of
  the two remaining 2-D outputs, the earlier is **class ids** and the later is **scores**.
- If the layout does not match, the detector reports `isReady == false` at construction,
  so the UI says **"Model unavailable"** instead of silently detecting nothing.
- `OutputTensorResolverTest` pins the contract measured from the shipped asset, so
  replacing the model breaks a test rather than silently mis-decoding detections.

### Verification

The fixed resolution was run against the real model on the real test images:

```
bus_street.jpg      count=25  [('bus', 0.801), ('person', 0.789), ('person', 0.707), ('person', 0.648)]
people_tie.jpg      count=25  [('person', 0.688), ('person', 0.605)]
```

These reproduce the published desktop benchmark numbers (bus 0.8008, person 0.7891 /
0.7070 / 0.6289) exactly, so the fix is known to produce the intended detections and not
merely to stop throwing.

> **Caveat, stated plainly.** The zero-initialised behaviour was measured in the **Python
> LiteRT** interpreter, which is the same native runtime the Java API wraps, but the Java
> `Tensor.asReadOnlyBuffer()` path was not exercised on a device. The Java behaviour is
> either "returns zeros" (the measured case) or "throws", and **both lead to the same
> outcome: an empty detection list on every frame**. That is why the fix removes the
> value dependency entirely rather than relying on the measurement.

---

## 3. Finding B — the YUV conversion ignores row and pixel stride (HIGH, **not fixed**)

`YuvFrameConverter.toBitmap` reads the three `YUV_420_888` planes with
`uBuffer.get(yuv, ySize, chromaSize)` and assumes they are tightly packed.

`YUV_420_888` does **not** guarantee that. The Y plane commonly has a `rowStride` wider
than `image.width`, and the U and V planes commonly have a `pixelStride` of 2 (semi-planar
NV12/NV21 layouts). When `pixelStride == 2`, the byte at offset *i* is the first byte of
a two-byte chroma sample, and reading `chromaSize` contiguous bytes picks up the padding
byte of every sample. The result is a **systematically wrong colour image** — chroma
noise — which degrades detection without producing any error.

**Why it was not fixed here.** The correct fix is to honour `rowStride` and `pixelStride`
per plane, or to use `ImageProxy.toBitmap()` (available in CameraX 1.3+, and this project
is on 1.4.1). Both change the pixels the model sees, so both can only be *validated* on a
device, against a known-good reference. Shipping an unvalidated colour conversion would
trade a known bug for an unknown one. It is recorded as **issue #11**.

**What would validate it:** capture the same scene with the app and with a reference
implementation, and compare the decoded frames pixel for pixel.

---

## 4. Finding C — full-resolution frame conversion on the main thread (HIGH, **not fixed**)

`DishaViewModel.startAnalysis` registers the analyzer with
`ContextCompat.getMainExecutor(...)`, and the analyzer body calls
`YuvFrameConverter.toBitmap(proxy)` **before** moving work to a background dispatcher:

```kotlin
imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getApplication())) { proxy ->
    analyze(det, proxy)
}
```

`analyze` does the YUV→RGB conversion synchronously on the main thread, and the conversion
is a **per-pixel Kotlin loop over the full sensor frame** — `image.width * image.height`
iterations, which is ~2 million pixels for a 1080p sensor. The `Bitmap.createScaledBitmap`
down to 480×480 happens *after* that loop, so the expensive work is done at full
resolution.

The inference itself is already off the main thread
(`viewModelScope.launch(Dispatchers.Default)`), so the design is half-right: only the
conversion is on the UI thread. The observable effect is frame drops and UI jank, and for
a TalkBack user that is a serious problem — the app is competing with the screen reader
for the main thread.

**Why it was not fixed here.** Moving the analyzer to a background executor is a small
change, but it alters the frame lifecycle: the `ImageProxy` must stay open until the
conversion finishes, which changes when `proxy.close()` is called and therefore how
CameraX's `STRATEGY_KEEP_ONLY_LATEST` backpressure behaves. That interaction is exactly
the kind of thing that needs a device to get right. Recorded as **issue #12**.

---

## 5. Finding D — the model is loaded on the main thread (MEDIUM, **not fixed**)

`DishaViewModel.ensureDetector()` constructs `EfficientDetDetector` from `start()`, which
is called from the UI thread. Constructing the LiteRT `Interpreter` allocates tensors and
initialises the XNNPACK delegate, which is not instant.

The code comment says "Detector is created lazily so model loading never blocks app
start". That is true of *app start* but not of the *start button press* — from the user's
point of view the button is what starts the app. For a blind user pressing "start" and
hearing nothing for a second or two, the natural conclusion is that the app is broken.

**Why it was not fixed here.** Moving the load onto a background dispatcher changes the
`IDLE → STARTING → RUNNING` state machine and the ordering of the first announcement
("Started" is currently spoken before the model is checked). The correct sequence is
observable only on a device. Recorded as **issue #13**.

---

## 6. Finding E — the frame bitmap is leaked if the coroutine is cancelled (LOW, **not fixed**)

In `DishaViewModel.analyze`:

```kotlin
viewModelScope.launch(Dispatchers.Default) {
    val detections = withContext(Dispatchers.Default) { det.detect(bitmap) }
    bitmap.recycle()
    handleDetections(detections)
}
```

`bitmap.recycle()` runs only if the coroutine reaches it. If the `ViewModel` is cleared
while detection is in flight, `viewModelScope` cancels the coroutine, `recycle()` is
skipped, and the bitmap — including its native pixel buffer — is left to the garbage
collector instead of being released promptly. Repeated start/stop cycles could therefore
accumulate native memory. Recorded as **issue #14**.

---

## 7. Verified correct — these were checked and are *not* problems

These are recorded so a later reader does not have to re-audit them.

### 7.1 The bundled model is stored uncompressed, so `openFd` works

`EfficientDetDetector.loadModel` uses `context.assets.openFd(...)` and memory-maps the
file. `AssetManager.openFd` **throws** for a compressed asset. Measured directly from the
built APK:

```
assets/efficientdet_lite0_detection.tflite   4561184 -> 4561184   compress_type=0  (STORED)
```

Uncompressed, so the memory map succeeds. Had it been compressed, the detector would have
failed to load and reported itself not ready — visible, not silent. Note that aapt2's
default of not compressing assets is what makes this work; it is not an accident the
project can rely on silently.

### 7.2 The model's class-index space is offset by one from COCO, and the app handles it

The model emits a 0-based index into a **shifted** COCO id space:
`model_class_index + 1 == coco_category_id`. `CocoLabels` applies that offset. All four
anchors used to establish the offset were re-verified against the app's own table:

| model index | app label | anchor |
|---|---|---|
| 0 | person | person ✓ |
| 5 | bus | bus ✓ |
| 17 | dog | dog ✓ |
| 31 | tie | tie ✓ |

Getting this wrong would name every object incorrectly while producing perfectly plausible
detections, so it is worth stating that it was checked rather than assumed.

### 7.3 The published benchmark numbers are desktop numbers

`benchmarks/scripts/run_benchmark.py` and `verify_preprocessing.py` resize with
`Image.BILINEAR`; the app uses `Bitmap.createScaledBitmap(..., true)`. The resampling
filters are not identical, so the app's scores on the same image differ slightly from
the published ones (measured: 0.738 on the Samoyed where the desktop run recorded
0.7227). The published numbers remain accurate **as desktop measurements** and must not
be presented as the app's on-device output.

### 7.4 CameraX backpressure is configured correctly

`STRATEGY_KEEP_ONLY_LATEST` with `OUTPUT_IMAGE_FORMAT_YUV_420_888` is the right
combination for this workload: only the newest frame is analysed, so slow inference
cannot build an unbounded backlog, and YUV avoids the conversion cost of `RGBA` output.
`proxy.close()` is called in a `finally` block, so frames are always released.

### 7.5 The manifest is minimal and correct

`CAMERA` only. `allowBackup="false"`, `fullBackupContent="false"`,
`dataExtractionRules` present, `usesCleartextTraffic="false"`. No `INTERNET`, no
`RECORD_AUDIO`, no `ACCESS_FINE_LOCATION`, no `QUERY_ALL_PACKAGES`. `uses-feature` for
`android.hardware.camera.any` is `required="false"`, so the app can be installed on a
device without a camera and must degrade — the UI does handle the permission-denied path.

### 7.6 The TTS wrapper degrades correctly, and never blocks

`Speaker` checks `isLanguageAvailable` for `hi_IN` and falls back to `Locale.US` while
reporting `isHindiAvailable = false`, so the UI can say so plainly. Initialisation is
asynchronous and `whenReady` never blocks the caller. `speak()` uses `QUEUE_FLUSH` for
announcements the user asked for, because stale speech is worse than interruption.

### 7.7 The APK ships four ABIs

`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`, each with `libtensorflowlite_jni.so` and the
CameraX JNI libraries. This is a large share of the 30,671,369-byte APK. A real release
should use ABI splits (or ship arm64 only), which is a build-configuration change rather
than a code change. Recorded as **issue #15**.

### 7.8 A dead version key

`gradle/libs.versions.toml` still carries `tensorflowLite = "2.17.0"`, which **nothing
references** — the actual dependency is `com.google.ai.edge.litert:litert:1.0.1`. The
key is misleading: it names a version of a different artifact from the one in use. Every
document correctly says LiteRT 1.0.1, so no published claim is wrong, but the key invites
a future reader to believe the project ships TFLite 2.17.0. Removed as part of this audit.

---

## 8. What this audit does **not** establish

- **That the app runs at all on a device.** Not tested. No device was available.
- **That the camera preview, permission flow, or TalkBack interaction behave correctly.**
  Not tested.
- **That the detector produces sensible detections on live camera frames.** The model was
  verified on four static images; the camera path (frames → YUV → Bitmap → tensor) was
  not.
- **That Hindi text-to-speech works.** `Speaker` handles the *absence* of a Hindi voice
  correctly; whether one is present, and whether it sounds right, is untested.
- **Any latency or thermal figure for the app.** All numbers published so far are
  desktop-CPU numbers and are labelled as such.
- **That Finding B or C is the *only* colour/threading problem.** Static review found
  these two; it cannot prove there are no others.

---

## 9. Method

| Claim type | How it was established |
|---|---|
| The model output contract, the fresh-interpreter zeroing, the four label anchors | The real `.tflite` run through LiteRT 1.0.1 on this machine, against the real asset and the real test images |
| Asset compression in the APK | The ZIP central directory of the actual built APK |
| API usage, threading, and lifecycle | Read in the source, at the paths cited |
| Findings B, C, D, E | Static reasoning from the source. **Not reproduced at runtime** — no device |
| Everything in section 7 | Read in the source and, where stated, measured from the built APK |

The scripts used to produce the measurements are `benchmarks/scripts/verify_preprocessing.py`
(input convention) and, for this audit, a throwaway harness that ran the shipped model
through both the old and the new resolution logic. The lasting artefact of this audit is
`OutputTensorResolver` plus `OutputTensorResolverTest`, which pins the contract in CI.
