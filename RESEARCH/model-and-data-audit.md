# Model and Data Audit — Disha Voice

**Status:** decision locked for the P0 prototype, based on measurements against the actual model file and official documentation.
**Reviewed:** 2026-10-20. All URLs accessed 2026-10-20.

Every claim below is marked **VERIFIED** (I measured it or read it in a primary source),
**OBSERVATION** (behaviour seen in a run), or **UNKNOWN** (not established).

---

## 1. Decision

**Ship EfficientDet-Lite0 (int8, COCO-2017, 90 usable classes) as a bundled TFLite model,
run through the LiteRT interpreter, with no ML Kit dependency and no Google Play Services
model download.**

| Property | Value | Evidence |
|---|---|---|
| Model file | `efficientdet_lite0_detection.tflite` | in-repo at `android/app/src/main/assets/` |
| Size | 4,561,184 bytes (4.4 MB) | `ls -l` on the asset and `unzip -l` inside the built APK |
| SHA-256 | `33a3b622c7cac0762f96089353cd61495f3e993968d133af7871bfc2d5396704` | `sha256sum` on the asset |
| License | **Apache-2.0** | model card front-matter `license: apache-2.0` on `huggingface.co/litert-community/efficientdet` |
| Source | `https://huggingface.co/litert-community/efficientdet` → `efficientdet_lite0_detection.tflite` | fetched and hashed locally |
| Provenance | TFLite deployment of `tensorflow/efficientdet/lite0/detection/1` (same file as `lite0/int8/1`) | model card text |
| Training data | COCO 2017 | model card text |
| Object classes | 90 COCO categories (e.g. bottle, cup, chair, cell phone, laptop, tv, keyboard, scissors, toothbrush, potted plant) | COCO category id space |
| Runtime | LiteRT (`com.google.ai.edge.litert:litert:1.0.1`) | resolved artifact in the Gradle cache; TFLite Java relocated to LiteRT |
| Offline | Yes — model is inside the APK; no Play Services fetch | verified: model asset present in built APK |
| App permissions | `CAMERA` only; no `INTERNET` | `aapt2 dump permissions` on the built APK |

**The decision is auditable, not inherited from a blog post.** The tensor contract below was
measured by running the real file through LiteRT, not read from documentation.

---

## 2. Why not ML Kit's built-in object detector

This was the first choice considered and it was **rejected on verified grounds**, not taste.

| Issue | Detail | Evidence |
|---|---|---|
| Not offline-first | The base detector is an **unbundled** library: "This API uses an unbundled library that must be downloaded before use." The model is delivered by Google Play Services, so a first run on a device without it has no detection. | <https://developers.google.com/ml-kit/vision/object-detection/android> |
| Too few categories | The built-in coarse classifier covers **five** categories ("The default coarse classifier is built for five categories"). The prototype needs common household objects. | <https://developers.google.com/ml-kit/vision/object-detection> |
| Adds a Play Services dependency | Unbundled models imply a Play Services dependency in the critical path. | same as above |
| Custom path is workable but heavier | `object-detection-custom` does support a **bundled** model via `LocalModel`, but constrains the model to ML Kit's metadata/label-file conventions, and its published guidance still assumes a 300×300-style input and label metadata we would have to manufacture for a model that ships without any embedded labels. | <https://developers.google.com/ml-kit/vision/object-detection/custom-models/android> |

**Consequence:** the project owns preprocessing, postprocessing and the label table itself. That is
more code, but it is *our* code, unit tested, and it removes a network-dependent failure mode from
the core loop.

---

## 3. Verified model contract

Measured by running the actual `.tflite` with LiteRT (`benchmarks/scripts/inspect_model.py`):

```
INPUT
  name   = 'serving_default_images:0'
  shape  = [1, 320, 320, 3]
  dtype  = uint8
  quant  = (scale=0.0078125, zero_point=127)

OUTPUTS
  'StatefulPartitionedCall:3'  [1, 25, 4]  float32  -> boxes   (ymin, xmin, ymax, xmax, normalized)
  'StatefulPartitionedCall:2'  [1, 25]     float32  -> class indices
  'StatefulPartitionedCall:1'  [1, 25]     float32  -> scores
  'StatefulPartitionedCall:0'  [1]        float32  -> number of detections
```

**Two documented assumptions were wrong and had to be corrected against the file itself:**

1. The input is **320×320**, not the 300×300 that the EfficientDet-Lite documentation implies.
2. The input is **uint8**, not float32 — so the frame must be resized and fed as raw RGB bytes
   with no float scaling.

Both are encoded in `EfficientDetDetector.buildInputBuffer` and in the Python harness.

### Label index space (an off-by-one that was caught, not shipped)

The model has **no embedded label map** (verified: the strings `person`, `bicycle`, `zebra`,
`toothbrush` do not occur anywhere in the binary). The label table must be owned by the app.

The model emits a **0-based index into the COCO-2017 91-slot id space**:

```
model_class_index + 1 == coco_category_id
```

This was established empirically with four independent anchors on real images, all consistent:

| Object in image | model index | COCO id | Mapping |
|---|---|---|---|
| person | 0 | 1 | +1 |
| bus | 5 | 6 | +1 |
| dog | 17 | 18 | +1 |
| tie | 31 | 32 | +1 |

An earlier draft of the label table used the raw model index as the COCO id and therefore
reported a Labrador as a "cat" with 0.84 confidence. The table in
`android/app/src/main/java/org/dishavoice/app/detection/CocoLabels.kt` applies the offset, and
`benchmarks/model/coco_labels.py` mirrors it. `DetectionDecoderTest` asserts all four anchors so
the regression cannot come back silently.

---

## 4. End-to-end detection evidence (real runs, CPU, this machine)

Produced by `benchmarks/scripts/run_detection.py` on the real model file:

| Image | Result | Zone (0.38 / 0.62 boundaries) |
|---|---|---|
| `yellow_labrador.jpg` | **dog** 0.840, box (0.056, 0.057)–(0.909, 0.943) | centre_x 0.483 → CENTRE |
| `samoyed.jpg` | **dog** 0.723, box (0.084, 0.038)–(0.943, 0.962) | centre_x 0.514 → CENTRE |
| `bus_street.jpg` | **bus** 0.801 (centre_x 0.500 → CENTRE); **person** 0.789 (0.173 → LEFT); **person** 0.707 (0.357 → LEFT); **person** 0.629 (0.922 → RIGHT) | see left |
| `people_tie.jpg` | **person** 0.648 (0.723 → RIGHT); **person** 0.605 (0.410 → CENTRE) | see left |

The images were independently inspected with a vision model to confirm ground truth: the first is a
yellow Labrador, the second a white Samoyed, the third a street bus with people, the fourth two men.

**Latency:** 25–44 ms per frame on this desktop CPU (LiteRT with the XNNPACK delegate).
**This is NOT a phone measurement.** Phone latency is unmeasured and must be benchmarked on real
hardware before any "real-time" claim.

**Image sources** (permissive, used only for development testing):
- `YellowLabradorLooking_new.jpg` — <https://storage.googleapis.com/download.tensorflow.org/example_images/YellowLabradorLooking_new.jpg>
- `dog.jpg` (Samoyed) — <https://raw.githubusercontent.com/pytorch/hub/master/images/dog.jpg>
- `bus.jpg`, `zidane.jpg` — <https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/>

---

## 5. Why raw confidence is not accuracy

The detector emits a per-detection score, but it is **not** the probability that the label is
correct in the user's situation, and it is **not** a safety signal. Three distinct reasons:

1. **It is a class posterior, not a correctness estimate.** The score is the softmax output of the
   classification head over 90 classes. It says "of the classes the model considered, this one
   scored highest by this much" — not "there is a 0.84 probability that a bottle is present".
2. **It is not calibrated.** Published EfficientDet-Lite numbers are mAP on COCO validation
   (EfficientDet-Lite0: 25.69 % mAP, 4.4 MB, 37 ms on a Pixel 4 with 4 CPU threads — Google,
   <https://developers.google.com/edge/litert/libraries/modify/object_detection>). COCO mAP is an
   aggregate over classes and thresholds; it says nothing about the score→accuracy mapping for the
   handful of classes this app cares about.
3. **The operating conditions are different.** The model was trained on COCO's everyday photos.
   A blind user's camera pointed at a dim room, a close-up object, or a cluttered shelf is a
   different distribution, and partial occlusion by a hand is common in real use.

**Therefore the app gates on a two-threshold policy rather than treating score as truth**
(`core/.../AnnouncementPolicy.kt`): at or above 0.70 a detection is stated plainly; between 0.45
and 0.70 it is announced as tentative ("लगता है …" / "it looks like …"); below 0.45 it is not
spoken at all. The thresholds are a **starting point chosen for a first prototype**, not
calibrated values, and `PROJECT_STATUS.md` records calibration as outstanding work.

**Thresholds must be revisited with real data** from the field protocol in
`TESTING/field-evaluation-protocol.md`, using a reliability curve on the collected labels.

---

## 6. Alternatives considered and not chosen

| Option | Why not | Status |
|---|---|---|
| ML Kit base object detection | unbundled → not offline; 5 coarse categories only | rejected on verified grounds |
| ML Kit custom object detector | bundled-capable, but imposes metadata/label conventions we would have to fabricate for a model with no embedded labels | not chosen |
| MobileNet-SSD / other TFLite detectors | same Apache-2.0 family, comparable size; EfficientDet-Lite0 has the better published mAP/size point of the Lite family | alternative, not required |
| ONNX Runtime Mobile | viable, but adds a second runtime for no measured benefit in this prototype | not chosen |
| On-device VLM for scene description (Gemma 3n, Smondrel et al.) | P1 scope; size, latency and thermal cost on a mid-range phone are not established here, and a cloud captioner raises a bystander-privacy problem the MVP deliberately avoids | **UNKNOWN** — needs its own evaluation before any commitment |
| Cloud vision API | would break the offline-first and privacy design, and requires a backend, accounts, and consent flow the prototype does not have | out of scope for P0 |

---

## 7. Third-party assets actually shipped

| Asset | Type | Version | License | Distribution OK | Notes |
|---|---|---|---|---|---|
| `efficientdet_lite0_detection.tflite` | model | `tensorflow/efficientdet/lite0/detection/1` | Apache-2.0 | yes | SHA-256 recorded above; model card is the license source |
| COCO 2017 category names | data (labels) | 2017 | COCO images are CC-licensed; the **category names** are factual identifiers, not expressive content | yes | Only the id→name table is used, not any image |
| LiteRT (`com.google.ai.edge.litert:litert:1.0.1`) | code | 1.0.1 | Apache-2.0 | yes | Runtime, resolved in the Gradle cache |
| AndroidX CameraX / Compose / Lifecycle | code | pinned in `gradle/libs.versions.toml` | Apache-2.0 | yes | |
| Test images (4 files in `benchmarks/testdata/`) | data | — | permissive public sources listed in §4 | yes, with attribution | Development fixtures only; not shipped in the APK |

**Not used:** no ML Kit, no Google Play Services model fetch, no analytics SDK, no crash-reporting
SDK, no advertising SDK, no network permission.

---

## 8. Outstanding unknowns

- **UNKNOWN:** on-device latency, memory, thermal behaviour and battery impact on real handsets.
- **UNKNOWN:** accuracy and confidence calibration for this specific object set in real homes.
- **UNKNOWN:** behaviour of the detector with partial occlusion, motion blur, and low light.
- **UNKNOWN:** whether a Hindi voice is present on typical target devices, and how intelligible the
  Devanagari phrases are to intended users.
- **UNKNOWN:** scene-description (P1) feasibility at all on a mid-range phone.
- **UNKNOWN:** energy cost of continuous camera + inference over a long session.
