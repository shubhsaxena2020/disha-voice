# Third-Party Notices

Disha Voice is licensed under the Apache License, Version 2.0 (see `LICENSE`).
This file lists the third-party components redistributed with, or used to build, this
project, and the notices they require.

---

## Bundled model

### EfficientDet-Lite0 (int8) — TFLite

- **File:** `android/app/src/main/assets/efficientdet_lite0_detection.tflite`
- **Version:** the TFLite deployment of `tensorflow/efficientdet/lite0/detection/1`
  (identical to `tensorflow/efficientdet/lite0/int8/1`)
- **Size:** 4,561,184 bytes
- **SHA-256:** `33a3b622c7cac0762f96089353cd61495f3e993968d133af7871bfc2d5396704`
- **Source:** <https://huggingface.co/litert-community/efficientdet>
- **Licence:** Apache License 2.0 (as declared in the model repository's `README.md` front matter)
- **Training data:** COCO 2017

This model is redistributed inside the application package so that object detection works
with no network access.

---

## Software dependencies (not redistributed as source; resolved at build time)

| Component | Version | Licence |
|---|---|---|
| LiteRT (`com.google.ai.edge.litert:litert`) | 1.0.1 | Apache-2.0 |
| AndroidX CameraX — `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` | 1.4.1 | Apache-2.0 |
| AndroidX Compose — `ui`, `ui-tooling`, `ui-tooling-preview` | 1.6.8 | Apache-2.0 |
| AndroidX Compose Material3 | 1.2.1 | Apache-2.0 |
| AndroidX Core KTX | 1.13.1 | Apache-2.0 |
| AndroidX Activity Compose | 1.9.3 | Apache-2.0 |
| AndroidX Lifecycle — `lifecycle-runtime-ktx`, `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose` | 2.8.7 | Apache-2.0 |
| Kotlin standard library / kotlin-test | 2.0.21 | Apache-2.0 |
| kotlinx-coroutines-test (test only) | 1.9.0 | Apache-2.0 |
| JUnit 4 (test only) | 4.13.2 | EPL-1.0 |
| AndroidX Test JUnit / Espresso / Compose UI test (test only) | 1.2.1 / 3.6.1 / 1.6.8 | Apache-2.0 |

Apache-2.0 components require that this notice and the licence text be retained, and that
modifications be marked. This project retains the original notices in each file's header
where the upstream project requires it.

---

## Data

### COCO 2017 category names

`android/app/src/main/java/org/dishavoice/app/detection/CocoLabels.kt` and
`benchmarks/model/coco_labels.py` contain the **COCO 2017 category id → name table**
(e.g. `1 → person`, `44 → bottle`, `47 → cup`).

- These are factual identifiers for object categories, not expressive content.
- **No COCO image is redistributed by this project.**
- The COCO dataset itself is distributed by its authors under its own terms
  (<https://cocodataset.org/>); those terms apply to the dataset, not to the category names.

---

## Development test images (NOT included in the application package)

These live in `benchmarks/testdata/` and are used only to verify the detector during
development. They are not compiled into the APK.

| File | Source | Terms |
|---|---|---|
| `yellow_labrador.jpg` | <https://storage.googleapis.com/download.tensorflow.org/example_images/YellowLabradorLooking_new.jpg> | Google example image published with the TensorFlow documentation |
| `samoyed.jpg` | <https://raw.githubusercontent.com/pytorch/hub/master/images/dog.jpg> | PyTorch Hub example image |
| `bus_street.jpg` | <https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/bus.jpg> | Example image from the Ultralytics repository. **No code from that repository is used or linked**; only the photograph is retained as a test fixture |
| `people_tie.jpg` | <https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/zidane.jpg> | as above |

---

## Attribution

EfficientDet was introduced in "EfficientDet: Scalable and Efficient Object Detection",
Zoph, Lin, Le, and Girshick, CVPR 2020 — <https://arxiv.org/abs/1911.09070>.
The backbone is EfficientNet, Tan and Le, ICML 2019 — <https://arxiv.org/abs/1905.11946>.
The COCO dataset is described in "Microsoft COCO: Common Objects in Context", Lin et al.,
ECCV 2014 — <https://arxiv.org/abs/1405.0312>.

---

*If you add a dependency, model, or asset, add it here in the same pull request that
introduces it, with its version, source URL, licence, and SHA-256 where applicable.*
