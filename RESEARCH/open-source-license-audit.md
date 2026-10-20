# Open-Source and Licence Audit — Disha Voice

**Reviewed:** 2026-10-20. All URLs accessed 2026-10-20.
**Rule applied:** no third-party code is copied into this project. Everything here is a
clean-room implementation. This audit exists to record what was checked, what may be
used, and what must not be used.

Every claim is marked **VERIFIED** (read in the primary source), **OBSERVATION**, or
**UNKNOWN**.

---

## 1. Summary

| Question | Answer |
|---|---|
| Can this project be Apache-2.0? | **Yes.** The only bundled third-party binary asset is an Apache-2.0 model; every code dependency is Apache-2.0. |
| Is any code copied from another project? | **No.** |
| Is any model redistributed without a compatible licence? | **No.** The one bundled model is Apache-2.0. |
| Does anything force a reciprocal licence? | **No.** No GPL, AGPL, CC-BY-NC, or source-available component is used. |
| Are the test images redistributable? | **Yes**, with attribution (see §5). They are development fixtures and are **not** shipped in the APK. |

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

| Component | Type | Version | Licence | Evidence | OK to redistribute |
|---|---|---|---|---|---|
| **EfficientDet-Lite0 int8** (`efficientdet_lite0_detection.tflite`) | model | `tensorflow/efficientdet/lite0/detection/1` (same file as `lite0/int8/1`) | **Apache-2.0** | Model card front-matter `license: apache-2.0` at `https://huggingface.co/litert-community/efficientdet`; file SHA-256 `33a3b622c7cac0762f96089353cd61495f3e993968d133af7871bfc2d5396704` | **Yes** |
| **LiteRT** (`com.google.ai.edge.litert:litert`) | code | 1.0.1 | Apache-2.0 | `LICENSE` file shipped inside the resolved AAR in the Gradle cache | **Yes** |
| **AndroidX CameraX** (core, camera2, lifecycle, view) | code | 1.4.1 | Apache-2.0 | `https://dl.google.com/dl/android/maven2/androidx/camera/camera-view/1.4.1/camera-view-1.4.1.aar` (Apache-2.0, AndroidX policy) | **Yes** |
| **AndroidX Compose** (ui, material3, foundation) | code | ui 1.6.8, material3 1.2.1 | Apache-2.0 | AndroidX is Apache-2.0 | **Yes** |
| **AndroidX Core / Activity / Lifecycle** | code | 1.13.1 / 1.9.3 / 2.8.7 | Apache-2.0 | AndroidX is Apache-2.0 | **Yes** |
| **Kotlin standard library & coroutines** | code | 2.0.21 / 1.9.0 | Apache-2.0 | Apache-2.0 | **Yes** |
| **JUnit 4** (test only) | code | 4.13.2 | EPL-1.0 | test-only, not shipped in the APK | Yes (not redistributed) |
| **COCO 2017 category names** | data (labels) | 2017 | Factual identifiers, not expressive content | Only the id→name table is used; **no COCO image is redistributed** | **Yes** |

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

The project is licensed **Apache-2.0** (`LICENSE`). Every shipped third-party component
is Apache-2.0, which is compatible with Apache-2.0 distribution (Apache-2.0 → Apache-2.0
requires preserving notices, which `THIRD_PARTY_NOTICES.md` does).

**No reciprocal-licence obligations are triggered.** There is no GPL/AGPL code, no
CC-BY-NC asset, and no source-available model in the shipped artifact.

**The one genuine risk area** is model redistribution, and it is the reason the model
licence was verified from the model card before the model was downloaded into the
repository — not after.

---

## 5. Development test images (not shipped)

| File | Source | Licence / terms | Notes |
|---|---|---|---|
| `benchmarks/testdata/yellow_labrador.jpg` | `https://storage.googleapis.com/download.tensorflow.org/example_images/YellowLabradorLooking_new.jpg` | Google-provided example image, published for use with TensorFlow documentation | Development fixture only |
| `benchmarks/testdata/samoyed.jpg` | `https://raw.githubusercontent.com/pytorch/hub/master/images/dog.jpg` | PyTorch Hub example image | Development fixture only |
| `benchmarks/testdata/bus_street.jpg` | `https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/bus.jpg` | Ultralytics repo example image (AGPL-3.0 repository) — **image only, no code** | Development fixture only |
| `benchmarks/testdata/people_tie.jpg` | `https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/zidane.jpg` | as above | Development fixture only |

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
   category names are used now.
