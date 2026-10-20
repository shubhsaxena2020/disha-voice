# Third-Party Notices

Disha Voice is licensed under the Apache License, Version 2.0 (see `LICENSE`).
This file lists the third-party components redistributed with, or used to build, this
project, and the notices they require.

> **This file was substantially corrected on 2026-10-21.** An inventory of every
> resolved dependency was built by reading the built APK and the Gradle cache
> (`RESEARCH/third-party-inventory.csv`), and that inventory found that the
> previous version of this file was wrong in several ways: it named about eleven
> dependency families when **eighty-four** Maven coordinates actually ship; it
> listed **no native libraries at all**, although native code is half the APK; it
> gave the wrong version for coroutines and for the Compose runtime; it omitted
> the LiteRT **Caffe attribution**; and it stated that the APK required the
> licence text to be retained while the APK contained no licence file whatsoever.
> The corrections are listed in the changelog entry for 2026-10-21.

---

## The notices travel with the application

The licence text and these notices are **bundled inside the APK**, as
`assets/LICENSE-Apache-2.0.txt` and `assets/THIRD_PARTY_NOTICES.txt`, and are
reachable from the app's **Open source licences** screen.

Shipping them inside the artifact is what Apache-2.0 section 4(a) requires of a
redistribution. The screen exists because the users of this app use a screen
reader, and a screen reader cannot read a file inside an APK: the obligation is
only genuinely met if the notices are reachable by the person receiving them.

CI asserts on every build that the APK contains both files, that the licence
text really is the Apache-2.0 licence, and that the LiteRT Caffe attribution
has not been dropped.

---

## Bundled model

### EfficientDet-Lite0 (int8) — LiteRT

- **File:** `android/app/src/main/assets/efficientdet_lite0_detection.tflite`
- **Version:** the LiteRT deployment of `tensorflow/efficientdet/lite0/detection/1`
  (identical to `tensorflow/efficientdet/lite0/int8/1`)
- **Size:** 4,561,184 bytes
- **SHA-256:** `33a3b622c7cac0762f96089353cd61495f3e993968d133af7871bfc2d5396704`
- **Source:** <https://huggingface.co/litert-community/efficientdet>
- **Licence:** Apache License 2.0 (as declared in the model repository's `README.md` front matter)
- **Training data:** COCO 2017

This model is redistributed inside the application package so that object detection works
with no network access.

---

## Native libraries shipped in the APK

This is the part the previous version of this file omitted entirely. **15,366,820 bytes
of the 30,701,552-byte APK is native code**, across four ABIs.

| Library | Version | ABIs | Licence | SHA-256 (arm64-v8a) |
|---|---|---|---|---|
| `libtensorflowlite_jni.so` (LiteRT runtime) | 1.0.1 | arm64-v8a, armeabi-v7a, x86, x86_64 | Apache-2.0 **plus** the Caffe BSD-3-Clause attribution below | `cfecb1f2da022a555dc916fd890df385221034cc3bd27365706a3f91847a1990` |
| `libimage_processing_util_jni.so` (CameraX) | 1.4.1 | arm64-v8a, armeabi-v7a, x86, x86_64 | Apache-2.0 | `3e0071afcdc7e321f839e196d266014182f18d0988e4455acc9857a085b2184e` |
| `libsurface_util_jni.so` (CameraX) | 1.4.1 | arm64-v8a, armeabi-v7a, x86, x86_64 | Apache-2.0 | `ee3f0d56f81cc0a301db32dc4fd51274c3216733b4c40172768e38bc5049a265` |

### The LiteRT Caffe attribution

The `LICENSE` file inside `com.google.ai.edge.litert:litert:1.0.1` carries a notice
**in addition to** the Apache-2.0 licence, for TensorFlow code derived from Caffe. It is
reproduced in full in `assets/THIRD_PARTY_NOTICES.txt` inside the APK, and it is a
BSD-3-Clause notice, which carries its own retention condition.

---

## Software dependencies shipped in the APK

Eighty-four Maven coordinates resolve into the shipped APK. The authoritative list, with
the exact SHA-256 of every shipped artifact and where each is used, is
**`RESEARCH/third-party-inventory.csv`** — machine-readable, and regenerated from the
built APK and the Gradle cache.

The versions below are the ones the **APK itself** reports, read from the
`META-INF/*.version` files inside it, not from the version catalog. That distinction
matters: two of them are not what the catalog says.

| Component | Version | Type | Licence |
|---|---|---|---|
| `androidx.activity:activity` | 1.9.3 | androidx-library | Apache-2.0 |
| `androidx.activity:activity-compose` | 1.9.3 | androidx-library | Apache-2.0 |
| `androidx.activity:activity-ktx` | 1.9.3 | androidx-library | Apache-2.0 |
| `androidx.annotation:annotation` | 1.8.1 | androidx-library | Apache-2.0 |
| `androidx.annotation:annotation-experimental` | 1.4.1 | androidx-library | Apache-2.0 |
| `androidx.appcompat:appcompat` | 1.1.0 | androidx-library | Apache-2.0 |
| `androidx.appcompat:appcompat-resources` | 1.1.0 | androidx-library | Apache-2.0 |
| `androidx.arch.core:core-common` | 2.2.0 | androidx-library | Apache-2.0 |
| `androidx.arch.core:core-runtime` | 2.2.0 | androidx-library | Apache-2.0 |
| `androidx.autofill:autofill` | 1.0.0 | androidx-library | Apache-2.0 |
| `androidx.camera:camera-camera2` | 1.4.1 | androidx-library | Apache-2.0 |
| `androidx.camera:camera-core` | 1.4.1 | androidx-library | Apache-2.0 |
| `androidx.camera:camera-lifecycle` | 1.4.1 | androidx-library | Apache-2.0 |
| `androidx.camera:camera-view` | 1.4.1 | androidx-library | Apache-2.0 |
| `androidx.collection:collection` | 1.4.0 | androidx-library | Apache-2.0 |
| `androidx.collection:collection-ktx` | 1.4.0 | androidx-library | Apache-2.0 |
| `androidx.compose.animation:animation` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.animation:animation-core` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.foundation:foundation` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.foundation:foundation-layout` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.material3:material3` | 1.2.1 | androidx-library | Apache-2.0 |
| `androidx.compose.material:material` | 1.6.0 | androidx-library | Apache-2.0 |
| `androidx.compose.material:material-icons-core` | 1.6.0 | androidx-library | Apache-2.0 |
| `androidx.compose.material:material-ripple` | 1.6.0 | androidx-library | Apache-2.0 |
| `androidx.compose.runtime:runtime` | 1.7.1 | androidx-library | Apache-2.0 |
| `androidx.compose.runtime:runtime-saveable` | 1.7.1 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-geometry` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-graphics` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-test-manifest` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-text` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-tooling` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-tooling-data` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-tooling-preview` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-unit` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-util` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.concurrent:concurrent-futures` | 1.1.0 | androidx-library | Apache-2.0 |
| `androidx.concurrent:concurrent-futures-ktx` | 1.1.0 | androidx-library | Apache-2.0 |
| `androidx.core:core` | 1.13.1 | androidx-library | Apache-2.0 |
| `androidx.core:core-ktx` | 1.13.1 | androidx-library | Apache-2.0 |
| `androidx.cursoradapter:cursoradapter` | 1.0.0 | androidx-library | Apache-2.0 |
| `androidx.customview:customview` | 1.0.0 | androidx-library | Apache-2.0 |
| `androidx.customview:customview-poolingcontainer` | 1.0.0 | androidx-library | Apache-2.0 |
| `androidx.drawerlayout:drawerlayout` | 1.0.0 | androidx-library | Apache-2.0 |
| `androidx.emoji2:emoji2` | 1.3.0 | androidx-library | Apache-2.0 |
| `androidx.exifinterface:exifinterface` | 1.3.2 | androidx-library | Apache-2.0 |
| `androidx.fragment:fragment` | 1.1.0 | androidx-library | Apache-2.0 |
| `androidx.interpolator:interpolator` | 1.0.0 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-common` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-common-java8` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata-core` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-livedata-core-ktx` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-process` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | 2.8.7 | androidx-library | Apache-2.0 |
| `androidx.loader:loader` | 1.0.0 | androidx-library | Apache-2.0 |
| `androidx.profileinstaller:profileinstaller` | 1.3.1 | androidx-library | Apache-2.0 |
| `androidx.savedstate:savedstate` | 1.2.1 | androidx-library | Apache-2.0 |
| `androidx.savedstate:savedstate-ktx` | 1.2.1 | androidx-library | Apache-2.0 |
| `androidx.startup:startup-runtime` | 1.1.1 | androidx-library | Apache-2.0 |
| `androidx.tracing:tracing` | 1.2.0 | androidx-library | Apache-2.0 |
| `androidx.tracing:tracing-ktx` | 1.2.0 | androidx-library | Apache-2.0 |
| `androidx.vectordrawable:vectordrawable` | 1.1.0 | androidx-library | Apache-2.0 |
| `androidx.vectordrawable:vectordrawable-animated` | 1.1.0 | androidx-library | Apache-2.0 |
| `androidx.versionedparcelable:versionedparcelable` | 1.1.1 | androidx-library | Apache-2.0 |
| `androidx.viewpager:viewpager` | 1.0.0 | androidx-library | Apache-2.0 |
| `com.google.ai.edge.litert:litert` | 1.0.1 | androidx-library | Apache-2.0 |
| `com.google.ai.edge.litert:litert-api` | 1.0.1 | androidx-library | Apache-2.0 |
| `com.google.auto.value:auto-value-annotations` | 1.6.3 | java-library | Apache-2.0 |
| `com.google.guava:listenablefuture` | 1.0 | java-library | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.0.21 | java-library | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-common` | 2.0.21 | java-library | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk7` | 1.8.20 | java-library | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib-jdk8` | 1.8.20 | java-library | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.7.3 | java-library | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | 1.7.3 | java-library | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` | 1.7.3 | java-library | Apache-2.0 |
| `org.jetbrains:annotations` | 23.0.0 | java-library | Apache-2.0 |
| `COCO 2017 category id-to-name table` | 2017 | model | **UNDETERMINED** |
| `EfficientDet-Lite0 int8 detection model (TFLite)` | tensorflow/efficientdet/lite0/detection/1 | model | Apache-2.0 |
| `CameraX native image-processing JNI (libimage_processing_util_jni.so, 4 ABIs)` | 1.4.1 | native-library | Apache-2.0 |
| `CameraX native surface utility JNI (libsurface_util_jni.so, 4 ABIs)` | 1.4.1 | native-library | Apache-2.0 |
| `LiteRT native runtime (libtensorflowlite_jni.so, 4 ABIs)` | 1.0.1 | native-library | Apache-2.0 |

### Version corrections

| Component | Previously stated | Actually shipped | How it was established |
|---|---|---|---|
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` / `-android` | 1.9.0 | **1.7.3** | `META-INF/kotlinx_coroutines_core.version` inside the APK. 1.9.0 is `kotlinx-coroutines-test`, which is test-scope only and does not ship |
| `androidx.compose.runtime:runtime` / `-runtime-saveable` | 1.6.8 | **1.7.1** | `META-INF/androidx.compose.runtime_runtime.version` inside the APK |
| `androidx.compose.material:material`, `material-icons-core`, `material-ripple` | omitted | **1.6.0** | `META-INF/*.version` inside the APK |

---

## Build-time dependencies (not redistributed in the APK)

These are used to build the project and are not shipped in the artifact, but they are
listed because the file's stated purpose is "components redistributed with, or used to
build" this project.

| Component | Version | Licence |
|---|---|---|
| Android Gradle Plugin | 8.7.3 | Apache-2.0 |
| Kotlin Gradle plugin | 2.0.21 | Apache-2.0 |
| Gradle (wrapper-pinned) | 8.14.3 | Apache-2.0 |
| Android SDK Build-Tools | 36.0.0 | Android SDK Terms |
| `org.jetbrains.kotlin:kotlin-stdlib` | 2.0.21 | Apache-2.0 |

---

## Dependencies not shipped in the APK

Resolved but not packaged: the unit-test and instrumented-test classpaths, and the
desktop benchmarking tools.

| Component | Version | Type | Licence |
|---|---|---|---|
| `androidx.compose.ui:ui-test` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.compose.ui:ui-test-junit4` | 1.6.8 | androidx-library | Apache-2.0 |
| `androidx.test.espresso:espresso-core` | 3.6.1 | androidx-library | Apache-2.0 |
| `androidx.test.espresso:espresso-idling-resource` | 3.6.1 | androidx-library | Apache-2.0 |
| `androidx.test.ext:junit` | 1.2.1 | androidx-library | Apache-2.0 |
| `androidx.test.services:storage` | 1.5.0 | androidx-library | Apache-2.0 |
| `androidx.test:core` | 1.6.1 | androidx-library | Apache-2.0 |
| `androidx.test:monitor` | 1.7.1 | androidx-library | Apache-2.0 |
| `androidx.test:runner` | 1.6.1 | androidx-library | Apache-2.0 |
| `Android Build Tools (aapt2, d8, dexdump, apksigner)` | 36.0.0 | gradle-plugin | Apache-2.0 |
| `Gradle (build tool + wrapper)` | 8.14.3 | gradle-plugin | Apache-2.0 |
| `com.android.application:com.android.application.gradle.plugin` | 8.7.3 | gradle-plugin | Apache-2.0 |
| `com.android.tools.build:gradle` | 8.7.3 | gradle-plugin | Apache-2.0 |
| `org.jetbrains.kotlin.android:org.jetbrains.kotlin.android.gradle.plugin` | 2.0.21 | gradle-plugin | Apache-2.0 |
| `org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin` | 2.0.21 | gradle-plugin | Apache-2.0 |
| `org.jetbrains.kotlin.plugin.compose:org.jetbrains.kotlin.plugin.compose.gradle.plugin` | 2.0.21 | gradle-plugin | Apache-2.0 |
| `org.jetbrains.kotlin:compose-compiler-gradle-plugin` | 2.0.21 | gradle-plugin | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-gradle-plugin` | 2.0.21 | gradle-plugin | Apache-2.0 |
| `com.google.code.findbugs:jsr305` | 3.0.2 | java-library | Apache-2.0 |
| `com.google.errorprone:error_prone_annotations` | 2.18.0 | java-library | Apache-2.0 |
| `javax.inject:javax.inject` | 1 | java-library | Apache-2.0 |
| `junit:junit` | 4.13.2 | java-library | EPL-1.0 |
| `org.hamcrest:hamcrest-core` | 1.3 | java-library | BSD-3-Clause |
| `org.hamcrest:hamcrest-library` | 1.3 | java-library | BSD-3-Clause |
| `org.jetbrains.kotlin:kotlin-test` | 2.0.21 | java-library | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-test-junit5` | 2.0.21 | java-library | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.9.0 | java-library | Apache-2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test-jvm` | 1.9.0 | java-library | Apache-2.0 |
| `org.junit.jupiter:junit-jupiter-api` | 5.10.1 | java-library | EPL-2.0 |
| `org.junit.jupiter:junit-jupiter-engine` | 5.10.1 | java-library | EPL-2.0 |
| `org.junit.platform:junit-platform-commons` | 1.10.1 | java-library | EPL-2.0 |
| `org.junit.platform:junit-platform-engine` | 1.10.1 | java-library | EPL-2.0 |
| `org.junit.platform:junit-platform-launcher` | 1.10.1 | java-library | EPL-2.0 |
| `org.junit:junit-bom` | 5.10.1 | java-library | EPL-2.0 |
| `org.opentest4j:opentest4j` | 1.3.0 | java-library | Apache-2.0 |
| `benchmarks/testdata/bus_street.jpg` | n/a | model | **UNDETERMINED** |
| `benchmarks/testdata/people_tie.jpg` | n/a | model | **UNDETERMINED** |
| `benchmarks/testdata/samoyed.jpg` | n/a | model | **UNDETERMINED** |
| `benchmarks/testdata/yellow_labrador.jpg` | n/a | model | **UNDETERMINED** |
| `ai-edge-litert` | 2.3.0 (latest on PyPI at 2026-10-21; not pinned by this repo) | python-tool | Apache-2.0 |
| `jsonschema` | 4.26.0 (latest on PyPI at 2026-10-21; installed by CI as 'python3 -m pip install jsonschema') | python-tool | MIT |
| `numpy` | 2.5.4 (latest on PyPI at 2026-10-21; not pinned by this repo) | python-tool | BSD-3-Clause AND 0BSD AND MIT AND Zlib AND CC0-1.0 |
| `pillow` | 13.0.0 (latest on PyPI at 2026-10-21; not pinned by this repo) | python-tool | MIT-CMU |
| `playwright` | 1.64.0 (latest on PyPI at 2026-10-21; installed by CI as 'pip install playwright') | python-tool | Apache-2.0 |

### The platform text-to-speech engine

The app's entire spoken output depends on `android.speech.tts.TextToSpeech`, which is a
**platform API, not a bundled component**. No TTS engine is shipped in the APK; the
device's own engine provides the voice, and its licence is the device manufacturer's.
This is recorded here because the app's output path depends on it completely, and a
reader of the notices would otherwise find no mention of it.

---

## Data

### COCO 2017 category names — licence UNDETERMINED

`android/app/src/main/java/org/dishavoice/app/detection/CocoLabels.kt` and
`benchmarks/model/coco_labels.py` contain the **COCO 2017 category id → name table**
(e.g. `1 → person`, `44 → bottle`, `47 → cup`). **This table ships in the APK.**

- **No COCO image is redistributed by this project.**
- The COCO dataset is published at <https://cocodataset.org/> under its own terms.
  Those terms govern the dataset; **no separate licence statement for the category
  name table itself was found**, so the table's licence is recorded as
  **UNDETERMINED** rather than assumed to be Apache-2.0 or public domain.
- The previous version of this file asserted the table was "factual identifiers, not
  expressive content" and therefore unencumbered. That was an argument, not a licence.
  It has been downgraded to an open item.

### Development test images — licences UNDETERMINED (not shipped)

These live in `benchmarks/testdata/` and are used only to verify the detector during
development. **They are not compiled into the APK.**

| File | Source | Terms |
|---|---|---|
| `yellow_labrador.jpg` | <https://storage.googleapis.com/download.tensorflow.org/example_images/YellowLabradorLooking_new.jpg> | **UNDETERMINED** — published as TensorFlow documentation material, but no explicit licence statement for the photograph was found |
| `samoyed.jpg` | <https://raw.githubusercontent.com/pytorch/hub/master/images/dog.jpg> | **UNDETERMINED** — the `pytorch/pytorch` repository is BSD-3-Clause, but that licence covers the repository's code, not the sample photograph |
| `bus_street.jpg` | <https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/bus.jpg> | **UNDETERMINED** — the `yolov5` repository is AGPL-3.0, but that licence covers the software, not the photograph. **No code from that repository is used or linked**; only the photograph is retained as a test fixture |
| `people_tie.jpg` | <https://raw.githubusercontent.com/ultralytics/yolov5/master/data/images/zidane.jpg> | as above |

The previous version of this file listed the *repository* licences in the "Terms" column,
which read as though they covered the photographs. They do not. The distinction is now
stated where it matters.

---

## Attribution

EfficientDet was introduced in "EfficientDet: Scalable and Efficient Object Detection",
Zoph, Lin, Le, and Girshick, CVPR 2020 — <https://arxiv.org/abs/1911.09070>.
The backbone is EfficientNet, Tan and Le, ICML 2019 — <https://arxiv.org/abs/1905.11946>.
The COCO dataset is described in "Microsoft COCO: Common Objects in Context", Lin et al.,
ECCV 2014 — <https://arxiv.org/abs/1405.0312>.

---

*If you add a dependency, model, or asset, add it here in the same pull request that
introduces it, with its version, source URL, licence, and SHA-256 where applicable — and
add the row to `RESEARCH/third-party-inventory.csv` in the same change, so the two
cannot drift apart again.*
