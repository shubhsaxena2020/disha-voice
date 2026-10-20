# Source Register — Disha Voice

Every source consulted for the research in this repository, with what it was used for and how
much weight it carries. Access date is the date the source was actually read in this session.

Reliability key: **primary** = the organisation that makes the product or publishes the model;
**secondary** = reputable reporting or community documentation; **weak** = a single unconfirmed
source; **UNVERIFIED** = could not be confirmed.

---

| ID | Claim it supports | Source title | URL | Publisher | Accessed | Type | Reliability | Used in |
|---|---|---|---|---|---|---|---|---|
| S1 | Lookout supports Hindi, Bengali, Gujarati, Kannada, Marathi, Tamil, Telugu and 30+ languages; seven modes; Find mode gives direction and distance; Explore is beta and less accurate | Use Lookout to explore your surroundings | <https://support.google.com/accessibility/android/answer/9031274> | Google | 2026-10-20 | primary | high | `competitor-landscape.md`, `differentiation-hypothesis.md`, `feature-comparison.csv` |
| S2 | Lookout mode descriptions; Android 6.0+; user complaints of continual "No text in view" announcements and of the accessibility audio stream | Lookout - Assisted vision (Play listing) | <https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.reveal> | Google / Google Play | 2026-10-20 | primary | high | `competitor-landscape.md`, `differentiation-hypothesis.md` |
| S3 | Lookout works offline without Wi-Fi or data service (announced June 2022) | Explore, communicate and customize with Android | <https://blog.google/products-and-platforms/platforms/android/new-android-features-june-2022/> | Google | 2026-10-20 | primary | high | `competitor-landscape.md` |
| S4 | Lookout Find mode beta with seven object categories; direction and distance guidance | GAAD 2024: New AI accessibility updates | <https://blog.google/company-news/outreach-and-initiatives/accessibility/ai-accessibility-update-gaad-2024/> | Google | 2026-10-20 | primary | high | `competitor-landscape.md` |
| S5 | Lookout language count grew to 34; image Q&A in English | Stay productive and connected with these new Android features | <https://blog.google/products-and-platforms/platforms/android/new-android-features-september-2023/> | Google | 2026-10-20 | primary | high | `competitor-landscape.md` |
| S6 | Seeing AI is available on Android since Dec 2023; 18 languages at launch | Microsoft Launches an Improved Seeing AI App on Android Too | <https://www.thurrott.com/mobile/294072/microsoft-launches-an-improved-seeing-ai-app-on-android-too> | Thurrott.com | 2026-10-20 | secondary | medium | `competitor-landscape.md`, `feature-comparison.csv` |
| S7 | ML Kit's base object detector is an **unbundled** library that must be downloaded before use | Detect and track objects with ML Kit on Android | <https://developers.google.com/ml-kit/vision/object-detection/android> | Google | 2026-10-20 | primary | high | `model-and-data-audit.md` §2 |
| S8 | ML Kit's built-in coarse classifier covers only **five** categories; custom LiteRT models are supported | Object detection and tracking | <https://developers.google.com/ml-kit/vision/object-detection> | Google | 2026-10-20 | primary | high | `model-and-data-audit.md` §2 |
| S9 | A custom model can be **bundled** in the APK and works offline; `object-detection-custom:17.0.2` | Detect, track and classify objects with a custom classification model on Android | <https://developers.google.com/ml-kit/vision/object-detection/custom-models/android> | Google | 2026-10-20 | primary | high | `model-and-data-audit.md` §2 |
| S10 | Custom-model requirements: one input tensor, metadata, label map; ML Kit reads labels from model metadata | Custom models with ML Kit | <https://developers.google.com/ml-kit/custom-models> | Google | 2026-10-20 | primary | high | `model-and-data-audit.md` §2 |
| S11 | ML Kit model installation paths: unbundled (Play Services), bundled (in APK), dynamically downloaded | ML Kit model installation paths on Android | <https://developers.google.com/ml-kit/tips/installation-paths> | Google | 2026-10-20 | primary | high | `model-and-data-audit.md` §2 |
| S12 | EfficientDet-Lite0: 4.4 MB int8, 37 ms on a Pixel 4 (4 CPU threads), 25.69 % mAP on COCO 2017; Lite0–Lite4 size/latency/AP table | Object Detection with TensorFlow Lite Model Maker | <https://developers.google.com/edge/litert/libraries/modify/object_detection> | Google | 2026-10-20 | primary | high | `model-and-data-audit.md` §5, `benchmarks/` |
| S13 | The bundled model's licence is **Apache-2.0**; it is the TFLite deployment of `tensorflow/efficientdet/lite0/detection/1`, COCO 2017 trained | EfficientDet (model card) | <https://huggingface.co/litert-community/efficientdet> | litert-community on Hugging Face | 2026-10-20 | primary | high | `model-and-data-audit.md`, `THIRD_PARTY_NOTICES.md` |
| S14 | The model's real input is 320×320 uint8 and its outputs are boxes/classes/scores/count | Direct measurement with LiteRT against the shipped file | *(this repository: `benchmarks/scripts/inspect_model.py`)* | This project | 2026-10-20 | primary measurement | high | `model-and-data-audit.md` §3, `CocoLabels.kt` |
| S15 | The model emits `model_index + 1 == coco_id` (anchors: person→0, bus→5, dog→17, tie→31) | Direct measurement with LiteRT on four real images | *(this repository: `benchmarks/scripts/run_detection.py`)* | This project | 2026-10-20 | primary measurement | high | `model-and-data-audit.md` §3, `DetectionDecoderTest.kt` |
| S16 | TFLite Java relocated to **LiteRT** at `com.google.ai.edge.litert:litert:1.0.1` | Maven POM relocation notice | <https://repo1.maven.org/maven2/org/tensorflow/tensorflow-lite/2.17.0/tensorflow-lite-2.17.0.pom> | Maven Central | 2026-10-20 | primary | high | `gradle/libs.versions.toml`, `model-and-data-audit.md` |
| S17 | Real detections and CPU latency (25.9–35.9 ms median, 7 repeats) on four images | This project's benchmark run | *(this repository: `benchmarks/results/detector_benchmark.json`)* | This project | 2026-10-20 | primary measurement | high | `model-and-data-audit.md` §4, `README.md` |
| S18 | The built APK requests only CAMERA; no INTERNET; allowBackup=false; usesCleartextTraffic=false | `aapt2 dump permissions` / `dump xmltree` on the built APK | *(this repository: build output)* | This project | 2026-10-20 | primary measurement | high | `PROJECT_STATUS.md`, `threat-model.md`, `safety-and-claims-review.md` |
| S19 | The built APK contains 547 entries / 473 graph nodes / 0 integrity contradictions | REA `open_binary` + `inspect_artifact` on the built APK | *(this session, REA MCP server `rea-agents` 6.7.0)* | This project | 2026-10-20 | primary measurement | high | `PROJECT_STATUS.md` §4 |
| S20 | WCAG 2.2 SC 1.4.2 Audio Control exists because uncontrollable audio is an accessibility barrier | Web Content Accessibility Guidelines (WCAG) 2.2 | <https://www.w3.org/TR/WCAG22/> | W3C | 2026-10-20 | primary | high | `assistive-ux-requirements.md` §2 |
| S21 | Android apps targeting API 35+ must meet current Play target-API requirements | Play Console target API level requirement | <https://developer.android.com/google/play/requirements/target-sdk> | Google | 2026-10-20 | primary | high | `safety-and-claims-review.md` §4 |
| S22 | A privacy policy URL is required for Play listings that collect personal or sensitive data | Privacy policy requirement | <https://developer.android.com/google/play/requirements/privacy-policy> | Google | 2026-10-20 | primary | high | `safety-and-claims-review.md` §4 |
| S23 | Android 15+ devices require 16 KB page-size support for apps | Supporting 16 KB page sizes | <https://developer.android.com/guide/practices/page-sizes> | Google | 2026-10-20 | primary | high | `safety-and-claims-review.md` §4 |
| S25 | Google Guided Vision in Gemini Live: conversational camera description for blind and low-vision users, launched 2026-10-01; reframing cues; Aira training; TalkBack-menu integration; Android 9+ | <https://blog.google/innovation-and-ai/products/gemini-app/guided-vision-gemini-live> | primary (Google) | 2026-10-20 | `competitor-landscape.md` §1, `differentiation-hypothesis.md` |
| S26 | VisionAId: an offline-first on-device Android assistant for people with visual impairment, six ONNX models, Romanian TTS, calibrated metric depth, published measurements | <https://arxiv.org/abs/2607.02371> | primary (arXiv) | 2026-10-20 | `competitor-landscape.md` §2 |
| S27 | Andha-Dhun: the first dataset of human-authored Hindi audio descriptions; naive translation introduces artifacts and narrows diversity | <https://arxiv.org/abs/2607.06457> | primary (arXiv) | 2026-10-20 | `competitor-landscape.md` §3, `speech-hindi-audit.md` |
| S28 | Gemini supported languages (incl. Hindi and 12 other Indian languages) and supported countries | <https://support.google.com/gemini/answer/13575153> | primary (Google) | 2026-10-20 | `competitor-landscape.md` §5 |
| S29 | Gemini Live expanded to 40+ languages on Android | <https://blog.google/products-and-platforms/products/gemini/gemini-live-extensions-language-expansion/> | primary (Google) | 2026-10-20 | `competitor-landscape.md` §5 |
| S30 | Guided Vision launch routes (Gemini app, Accessibility shortcut, TalkBack menu) and Android 9+ availability | <https://gcn.com/google-gemini-live-guided-vision-android/22187> | secondary | 2026-10-20 | `competitor-landscape.md` §1 |
| S31 | Guided Vision was developed with feedback from blind and low-vision communities in India, Brazil, Singapore, Indonesia and Japan | <https://timesofindia.indiatimes.com/technology/tech-news/google-launches-guided-vision-for-gemini-live-what-is-it-and-how-it-works/amp_articleshow/134639412.cms> | secondary | 2026-10-20 | `competitor-landscape.md` §1 |
| S24 | WAI/WCAG reference material used for the accessibility acceptance checklist | Web Accessibility Initiative resources | <https://www.w3.org/WAI/standards-guidelines/wcag/> | W3C | 2026-10-20 | primary | high | `assistive-ux-requirements.md` §5 |

---

## Sources consulted and found insufficient

Recorded because a negative result is still a result, and because the project should not
imply coverage it does not have.

| Attempt | Outcome |
|---|---|
| Wikimedia Commons test images | **Blocked** — the environment received a Wikimedia error page instead of the image. Abandoned in favour of TensorFlow / PyTorch / Ultralytics example images. |
| `storage.googleapis.com/download.tensorflow.org/models/tflite/task_library/.../lite0_efficientdet_lite0.tflite` | **Returned an XML error, not a model.** Abandoned; the Hugging Face copy (S13) was used instead and independently hash-verified. |
| Finding a standalone Hindi-first assistive app or a 2024–2026 Indian hackathon project | **NOT FOUND — but see the second pass below.** The first pass recorded this as a weak negative. A second, more thorough search on 2026-10-20 **overturned the general conclusion**: it found Google Guided Vision (a Google product, not a standalone app) and VisionAId (a research system, in Romanian). The narrow question — a *standalone Hindi-first* product — still stands as not found, and remains a weak negative. Tracked as issue #4. |
| Licences of SightBuddy, SeeForMe, and VISION | **NOT VERIFIED** — the licence files were not read in this session. Recorded as UNVERIFIED rather than guessed. Issue #4. |
| Hindi support in Seeing AI, Be My Eyes, Envision, Supersense, and Lazarillo | **NOT VERIFIED** from official sources. Recorded as UNVERIFIED in the comparison table. |
| Independent latency measurements for any competitor | **NONE FOUND.** No competitor publishes comparable on-device latency numbers, which is itself a finding (see `differentiation-hypothesis.md` H5). |
| Whether the open-source prior art is already Hindi-first | **NOT ESTABLISHED.** |

---

## Source register conventions for future work

- Add a row **in the same pull request** that introduces a claim, not later.
- Record the **access date**, because product pages change.
- Prefer a **primary** source (the vendor's own documentation) over reporting about it.
- Mark anything inferred as an inference, with the reasoning.
- When a source could not be reached, record that too — a failed retrieval is evidence about
  coverage, and it is what keeps the UNVERIFIED rows honest.

---

## Second pass, 2026-10-20 — what changed

A second, more thorough search was run after the first pass. It was not a formality: it
**overturned a conclusion**. The first pass recorded "no prior art found" for an
offline-first assistive Android app. The second pass found two confirmed collisions:

| New source | What it is | Effect on the project |
|---|---|---|
| **Google Guided Vision in Gemini Live** (S25) | Conversational camera description for blind and low-vision users, launched **2026-10-01**, on Android 9+, in Gemini's 40+ languages including Hindi, in India, integrated with the TalkBack menu. Requires a network connection. | **A closer competitor than Lookout.** The "feature-level differentiation is close to zero" claim was too generous. The remaining ground is now: on-device, offline, non-conversational, and measured. |
| **VisionAId** (S26) | Offline-first on-device Android assistant for people with visual impairment; six ONNX models; Romanian speech synthesis; calibrated metric depth; published mAP@50 and depth-error figures. | **Confirms prior art for the technical thesis** (issue #4's largest risk). Not a Hindi collision. Also shows calibrated depth is achievable, which reframes the project's permanent refusal to estimate distance. |
| **Andha-Dhun** (S27) | The first dataset of human-authored Hindi audio descriptions; finds that naive translation introduces artifacts and narrows diversity. | **Cites the project's decision** to write original Hindi rather than translate English. Establishes that Hindi assistive description is an acknowledged research area. |

**The lesson worth keeping:** the first pass was careful about *claiming* absence — it
labelled the negative result weak — and yet the document still read as more confident than
the evidence allowed. "Not found" was written in a way that a reader would take as "does not
exist". The second pass found the strongest competitor within one day of publication.
