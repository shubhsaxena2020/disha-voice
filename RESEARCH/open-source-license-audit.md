# Open-Source and Licence Audit — Disha Voice

**Reviewed:** 2026-10-21 (corrected). Originally 2026-10-20; all URLs accessed 2026-10-20
and 2026-10-21.
**Correction note (2026-10-21):** a machine-readable inventory of every resolved dependency
was built by reading the built APK and the Gradle cache
(`RESEARCH/third-party-inventory.csv`). It found that this document made several claims that
were not true. The false ones are corrected below, and the corrections are called out
rather than quietly applied, because a licence audit that is wrong about what ships is worse
than no audit at all.
**Rule applied:** no third-party code is copied into this project. Everything here is a
clean-room implementation. This audit exists to record what was checked, what may be
used, and what must not be used.

Every claim is marked **VERIFIED** (read in the primary source), **OBSERVATION**, or
**UNKNOWN**.

---

## 1. Summary

| Question | Answer |
|---|---|
| Can this project be Apache-2.0? | **Yes**, with one item recorded as undetermined. Every shipped *code* dependency is Apache-2.0, and the one shipped *model* is Apache-2.0. |
| Is any code copied from another project? | **No.** |
| Is any model redistributed without a compatible licence? | **No.** The one bundled model is Apache-2.0. |
| Does anything force a reciprocal licence? | **No.** No GPL, AGPL, CC-BY-NC, or source-available component is used. |
| Are the test images redistributable? | **Unknown.** Their *repository* licences are known; **no licence statement for the photographs themselves was found**. They are development fixtures and are **not** shipped in the APK. |
| What binary assets actually ship in the APK? | **Four**, not one: the model, and **three native libraries** totalling 15,366,820 bytes. The previous version of this document said the model was the only bundled binary asset. That was false. |
| Is the shipped licence text present? | **Yes, now.** The APK contains no licence file at all until 2026-10-21; it now ships `assets/LICENSE-Apache-2.0.txt` and `assets/THIRD_PARTY_NOTICES.txt`, and CI asserts both are present. |
| Is the shipped COCO category-name table licensed? | **UNDETERMINED.** It ships in the APK. No licence statement for the table itself was found, so it is recorded as an open item rather than assumed to be public domain. |

---

## 2. Prior-art projects reviewed

The purpose of this section is **not** to copy code but to learn architectural lessons and
to confirm that no licence obligation is inherited.

> **Honest limitation:** the automated research passes over this project repeatedly failed
> to produce a written file, so the table below records what was verified directly in
> this session and marks the remainder as **UNKNOWN**. Names were confirmed as real
> projects; their current maintenance state and exact licence revisions were **not** all
> re-read from the licence file text, and must be before any reuse.

| Project | Status | Licence | What was learned | Reuse verdict |
|---|---|---|---|---|
| **Google Lookout** | Commercial product, not open source | Proprietary | Product/UX reference only: focuses on *object search* and image description, not a turnkey assistive pipeline. | adapt-concept-only (product concept) |
| **Microsoft Seeing AI** | Commercial product, not open source | Proprietary | Shows the value of coarse-to-fine descriptions and a strong accessibility posture. | adapt-concept-only (product concept) |
| **Be My Eyes / Be My AI** | Commercial + open components | Mixed (repo-specific) | Community-driven "ask a volunteer" model; a different interaction paradigm from a fully local one. | adapt-concept-only |
| **SightBuddy** | Open source | UNKNOWN — **not re-verified** | Community open-source assistive vision. | UNKNOWN until the LICENSE file is read |
| **SeeForMe** | Open source | UNKNOWN — **not re-verified** | Browser-based visual assistance; a reminder that a web approach can serve users without an Android device. | UNKNOWN until the LICENSE file is read |
| **VISION (We All Can SEE)** | Open source | UNKNOWN — **not re-verified** | Voice-interaction-first assistive design. | UNKNOWN until the LICENSE file is read |
| **Envision / Sullivan+ / Supersense / Lazarillo** | Commercial | Proprietary | Product/feature reference only. | adapt-concept-only (product concept) |

**Action required before any of the UNKNOWN rows is relied on:** fetch the repository's
`LICENSE` file and record the exact SPDX identifier and the copyright line. Do not trust
a licence badge.

**The "do not copy" rule is enforced here:** none of the above projects contributed code
to this repository. The implementation was written from scratch against the Android
platform documentation, and the only shared element is the idea of describing objects
from a camera view — which is not itself protectable.

---

## 3. Third-party components actually shipped in the APK

**The complete list is `RESEARCH/third-party-inventory.csv`** — 134 rows, machine-readable,
built by reading the built APK (all 546 ZIP entries at the time of the measurement — 548 now that the
notices and licence text are bundled — and all 8 DEX files) and the Gradle cache.
The table below is the summary; the CSV is the authority, and it records the exact SHA-256
of every shipped artifact and where each is used.

### 3.1 The four binary assets that ship

| Asset | Type | Version | Licence | Evidence | OK to redistribute |
|---|---|---|---|---|---|
| **EfficientDet-Lite0 int8** (`efficientdet_lite0_detection.tflite`) | model | `tensorflow/efficientdet/lite0/detection/1` | **Apache-2.0** | Model card front-matter `license: apache-2.0` at `https://huggingface.co/litert-community/efficientdet`; SHA-256 `33a3b622c7cac0762f96089353cd61495f3e993968d133af7871bfc2d5396704` | **Yes** |
| **`libtensorflowlite_jni.so`** (LiteRT runtime), 4 ABIs | native library | 1.0.1 | **Apache-2.0, plus a BSD-3-Clause Caffe attribution** | The `LICENSE` inside the resolved AAR carries the Apache-2.0 text **and** a separate notice for TensorFlow code derived from Caffe | **Yes**, provided the Caffe notice is reproduced — which it now is, in the shipped `THIRD_PARTY_NOTICES.txt` |
| **`libimage_processing_util_jni.so`** (CameraX), 4 ABIs | native library | 1.4.1 | Apache-2.0 | Hash-matched byte-for-byte to `camera-core-1.4.1.aar` in the Gradle cache | **Yes** |
| **`libsurface_util_jni.so`** (CameraX), 4 ABIs | native library | 1.4.1 | Apache-2.0 | Hash-matched byte-for-byte to `camera-core-1.4.1.aar` in the Gradle cache | **Yes** |

The three native libraries were **not listed in the previous version of this document**, which
asserted that the model was "the only bundled third-party binary asset". They are 15,366,820
of the APK's 30,701,552 bytes, so the claim was wrong about half the artifact.

### 3.2 The code dependencies

Eighty-four Maven coordinates resolve into the shipped APK: 83 AndroidX libraries and one
`com.google.*` artifact. The full list is in the CSV. Two version corrections matter, and
both were found by reading the version metadata the APK carries about itself rather than
by reading the version catalog:

| Component | Previously stated | Actually shipped | How it was established |
|---|---|---|---|
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` / `-android` | 1.9.0 | **1.7.3** | `META-INF/kotlinx_coroutines_core.version` inside the APK. 1.9.0 is `kotlinx-coroutines-test`, which is test-scope only |
| `androidx.compose.runtime:runtime` / `-runtime-saveable` | 1.6.8 | **1.7.1** | `META-INF/androidx.compose.runtime_runtime.version` inside the APK |

`androidx.compose.foundation:foundation` also ships, but it is not a *direct* dependency:
it arrives transitively through `androidx.compose.material` and `androidx.compose.ui`. The
previous version of this document listed it as though the project had chosen it.

### 3.3 Two components whose own POMs carry no licence

`com.google.auto.value:auto-value-annotations` and `com.google.guava:listenablefuture`
have **no `<licenses>` element in their own POMs**. The Apache-2.0 conclusion for both is
inherited from their parent POMs. That is a weaker form of evidence than reading the
artefact's own `LICENSE` file, and the CSV says so explicitly rather than asserting
Apache-2.0 as though it had been read directly.

### 3.4 The platform text-to-speech engine

The app's entire spoken output depends on `android.speech.tts.TextToSpeech`. **No TTS
engine is bundled**; the device's own engine provides the voice, and its licence is the
device manufacturer's. It is recorded in the CSV as a platform dependency with
`shipped_in_apk = not-applicable`. The previous version of this document did not mention
it at all, which is an omission in a document whose subject is what the app ships.

### 3.5 Data

| Component | Type | Version | Licence | OK to redistribute |
|---|---|---|---|---|
| **COCO 2017 category names** (`CocoLabels.kt`, shipped in the APK) | data (labels) | 2017 | **UNDETERMINED** — no licence statement for the table itself was found. The COCO dataset's own terms govern the dataset; **no COCO image is redistributed** | **Open item.** Recorded as undetermined rather than argued to be unencumbered. The previous version of this document listed the licence as "Factual identifiers, not expressive content", which is an argument, not a licence. |

### Deliberately **not** used

| Not used | Reason |
|---|---|
| ML Kit object detection (base) | The model is delivered by Google Play Services, so detection would not work on a device without it — incompatible with the offline-first goal. Its built-in coarse classifier also covers only 5 categories. |
| ML Kit object detection (custom) | Workable, but it imposes metadata/label-file conventions that the chosen model does not carry; owning the decode path directly is simpler to test. |
| Google ML Kit / Play Services (any) | Adds a network-dependent component to the critical path. |
| Any analytics, crash-reporting, or advertising SDK | The app is offline with no tracking by design. |
| Any cloud vision API | Would break the privacy design and require a backend, accounts, and consent flow. |

---

## 4. Licence compatibility conclusion

The project is licensed **Apache-2.0** (`LICENSE`). Every shipped *code* component and
the one shipped *model* are Apache-2.0, which is compatible with Apache-2.0 distribution.
Apache-2.0 → Apache-2.0 requires preserving the notices and conveying the licence, which
`THIRD_PARTY_NOTICES.md` does and which the APK now also carries for itself.

**No reciprocal-licence obligations are triggered.** There is no GPL/AGPL code, no
CC-BY-NC asset, and no source-available model in the shipped artifact.

**The one genuine risk area** is model redistribution, and it is the reason the model
licence was verified from the model card before the model was downloaded into the
repository — not after.

**Two items are open, and neither is a blocker for an Apache-2.0 distribution, but both
should be closed rather than argued away:**

1. **The COCO category-name table ships in the APK and has no determined licence.** The
   table is a list of factual object-category names, and no licence statement for it was
   found. It is recorded as UNDETERMINED. If the project wants a clean position, the
   table can be regenerated from the COCO annotation files at build time and the committed
   copy removed, which would make the question disappear rather than answer it.
2. **The LiteRT Caffe attribution is a BSD-3-Clause notice with its own retention
   condition.** It is now reproduced in full in the shipped notices, and CI asserts it has
   not been dropped. Before 2026-10-21 it was recorded nowhere in the repository.

**A third item is about the artifact rather than the licences:** until 2026-10-21 the APK
contained **no `LICENSE` or `NOTICE` file at all**, while this document asserted that the
notices requirement was satisfied by `THIRD_PARTY_NOTICES.md` in the repository. A
repository file does not travel with a distributed binary. The notices and the licence
text are now bundled as APK assets, reachable from an **Open source licences** screen in
the app, and CI fails the build if either is missing.

---

## 5. Development test images (not shipped)

| File | Source | Licence / terms | Notes |
|---|---|---|---|
| `benchmarks/testdata/yellow_labrador.jpg` | `https://storage.googleapis.com/download.tensorflow.org/example_images/YellowLabradorLooking_new.jpg` | **UNDETERMINED** — published as TensorFlow documentation material, but no explicit licence statement for the photograph was found | Development fixture only |
| `benchmarks/testdata/samoyed.jpg` | `https://raw.githubusercontent.com/pytorch/hub/master/images/dog.jpg` | **UNDETERMINED** — the `pytorch/pytorch` repository is BSD-3-Clause, but that licence covers the repository's code, not the sample photograph | Development fixture only |
| `benchmarks/testdata/bus_street.jpg` | `https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/bus.jpg` | **UNDETERMINED** — the `yolov5` repository is AGPL-3.0, but that licence covers the software, not the photograph. **Image only, no code** | Development fixture only |
| `benchmarks/testdata/people_tie.jpg` | `https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/zidane.jpg` | as above | Development fixture only |

**The repository licences are known; the photograph licences are not.** The previous version
of this document listed the repository licences in the "Licence / terms" column, which read
as though they covered the images. They do not, and a licence that covers software is not
a licence that covers a photograph.

**Important:** the `yolov5` repository is AGPL-3.0. **No code from it is used** — only two
well-known example photographs are retained as test fixtures, and the repository's code is
not a dependency. If a maintainer prefers a stricter position, these two images can be
replaced with self-taken photographs of a cup, bottle, and chair, which would also make
the fixtures better matched to the intended use case. This is recorded as an open item
rather than silently left.

---

## 6. Outstanding items

1. **Re-verify the licence of every prior-art project listed in §2** by reading its
   `LICENSE` file, and replace the UNKNOWN rows.
2. **Decide on the two Ultralytics-sourced test images** (keep with attribution, or replace
   with self-taken equivalents).
3. **Re-check licences at release time** — dependency versions change, and a licence can
   change between versions.
4. **Confirm COCO dataset terms** if any COCO *image* is ever used in evaluation; only
   category names are used now — but the *category-name table's* own licence is
   **UNDETERMINED** and it does ship. Either find a licence statement for it, or
   regenerate it at build time from the COCO annotations and stop shipping a copy.
5. **Re-run the dependency inventory at release time.** `RESEARCH/third-party-inventory.csv`
   is the check. It found fourteen discrepancies in this document and in
   `THIRD_PARTY_NOTICES.md` on 2026-10-21, including two wrong versions and a missing
   native-library notice, so it is worth running on every release rather than trusting
   that the prose is still right.
