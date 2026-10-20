# Disha Voice: stationary field-evaluation protocol

**Document type:** Prospective protocol; no field experiment or participant study is reported here.  
**Status:** Data collection is blocked until a real Android build, supported detector/model, and test device are available and the pre-field gates below pass. Project status currently records no Android SDK/JDK and no attached device.  
**Intended use:** Evaluate limited, image-relative descriptions of supported objects for blind and low-vision users. Disha Voice is not a mobility, navigation, obstacle-avoidance, or route-clearance aid.

## 1. Evidence, design choices, and claims

This document distinguishes **Evidence** (external guidance or existing project facts) from **Protocol choice** (a proposed method or threshold that must be approved and fixed before collecting data). Protocol choices are not empirical findings. A 100-frame pilot can identify failures and guide iteration; it cannot establish safety, broad generalizability, or release readiness.

- **Evidence — risk and evaluation:** NIST's AI Risk Management Framework recommends evaluating validity, reliability, safety, and impacts under realistic, documented conditions; considering false positives/negatives, uncertainty, and disaggregated results; and managing risks across the system lifecycle. See [NIST AI RMF 1.0, Trustworthiness](https://airc.nist.gov/airmf-resources/airmf/3-sec-characteristics/) and [NIST publication](https://www.nist.gov/publications/artificial-intelligence-risk-management-framework-ai-rmf-10).
- **Evidence — accessible involvement:** W3C WAI advises involving people with disabilities in evaluation, accounting for varied users and interaction strategies, and combining user evaluation with other forms of evaluation rather than treating a small user study as proof of accessibility. See [W3C WAI: Involving Users in Evaluating Web Accessibility](https://www.w3.org/WAI/eval/users). The resource is about web accessibility; its inclusion principles are applied here by analogy, not as a Disha-specific test standard.
- **Evidence — ethics in India:** ICMR's [National Ethical Guidelines for Biomedical and Health Research Involving Human Participants (2017)](https://www.icmr.gov.in/icmrobject/custom_data/pdf/resource-guidelines/ICMR_Ethical_Guidelines_2017.pdf) describe voluntary informed consent, participant rights, ethics review, and applicability to biomedical and health research including social/behavioural research for health. The institution/ethics committee must determine applicability and approval requirements for this specific study; do not assume an exemption.
- **Evidence — detection evaluation:** The [COCO evaluation implementation](https://github.com/cocodataset/cocoapi/blob/master/PythonAPI/pycocotools/cocoeval.py) uses class-aware matching and evaluates bounding boxes across IoU thresholds from 0.50 to 0.95. This protocol uses one IoU threshold for a small pilot; that simplification is a **protocol choice**, not a COCO result or equivalence to full COCO evaluation.
- **Evidence — timing:** Android's [Macrobenchmark guidance](https://developer.android.com/codelabs/android-macrobenchmark-inspect) reports timing distributions including percentiles and notes run-to-run variation. The latency timestamps and reported percentiles below are **protocol choices** tailored to the camera-to-speech path, not Android performance targets.
- **Project evidence:** The project safety position says Disha is supplemental visual information only, can miss/misidentify objects or report stale positions, and must not be used for mobility or safety decisions ([SAFETY.md](../docs/safety/SAFETY.md)). The core zone code defines image-relative LEFT/CENTER/RIGHT using normalized box-center x cutoffs 0.38 and 0.62 ([AnnouncementPolicy.kt](../core/src/main/kotlin/org/dishavoice/core/AnnouncementPolicy.kt)). These are image regions, not world directions or distance.

## 2. Questions and boundaries

Primary questions:

1. In a small, controlled set of stationary scenes, how often does the locked build announce the correct supported object class?
2. How often does it announce a supported class when that object is absent, and how often does it fail to announce a visible supported object?
3. For correctly matched detections, how often does the spoken left/center/right region agree with the annotated image region?
4. What are the distributions of camera-frame-to-inference and camera-frame-to-TTS-start latency, including timeouts and failures?
5. Can blind and low-vision participants understand the limited, uncertain, image-relative output and its non-mobility boundary while seated?

This protocol does **not** evaluate real-world navigation, walking, obstacle avoidance, distance/depth estimation, emergency use, people recognition, OCR, medication/medical decisions, or hazard detection. Do not make claims about these capabilities from this pilot.

## 3. Pre-field gates

Do not recruit or collect participant data until all gates are signed off and documented:

1. A reproducible, real app build and a named test handset are available. Record app/build hash, OS version, device model, detector/model name, exact model artifact/version and license, class-label map, threshold/configuration, and whether inference is on-device. No mock, scripted labels, or demo fixture counts as a detector.
2. The exact supported-class roster is verified from the model label map and an on-device smoke test. Freeze the build and configuration for this run; changes require a new run ID.
3. Verify offline operation with Wi-Fi and mobile data disabled. Audit manifest, dependencies, network access, logging, crash reporting, backup/cache and frame handling. If image or result handling cannot be explained and controlled, do not use participant scenes. Offline-first intent is not proof that no data leaves the device.
4. Confirm accessible consent and task materials, a safe seated room/table arrangement, device mounting/placement, and an accessible stop/exit process. Do not require a participant to hold or operate the phone if they prefer the facilitator to do so.
5. Obtain institutional ethics review/determination and any required permissions before recruitment. Use accessible, understandable consent in the participant's preferred language and format. If ethics review or privacy controls are unresolved, stop at bench-only work.
6. Pre-register/approve the 100-frame manifest, matching rules, metrics, stop rules, data access, and retention plan. Do not tune thresholds on the evaluation frames and then report those same frames as an independent test.

## 4. Proposed 100-observation design

**Observation unit (protocol choice):** one unique, stationary camera frame captured in a pre-arranged scene, with one time-linked detector result/audio event. Collect exactly 100 frame observations if all gates pass. The unit is a frame, not a participant, walk, task success, or independent statistical sample. Annotate every visible eligible object and every detector output in each frame.

| Stratum | Count | Construction |
|---|---:|---|
| Target-present, routine | 48 | Eight candidate classes, six frames per class. For each class place the target in the left, center, and right image regions twice each, with ordinary, clearly visible presentation. |
| Target-present, controlled variation | 32 | Four additional frames per candidate class. Use safe, pre-specified variations such as mild partial occlusion, object orientation/scale, background clutter, and ordinary indoor lighting/contrast. Keep the object stationary and visible; do not use darkness, flashing light, glare that risks the participant, or hazardous scenes. |
| Target-absent | 20 | No object from the verified target roster in frame. Use 10 uncluttered non-target scenes and 10 scenes with harmless, non-target distractors/near-confusable household items. |
| **Total** | **100** | **80 target-present + 20 target-absent.** |

The counts above are **protocol choices** for a manageable diagnostic pilot, not a statistically powered sample. Each positive frame has one designated target from the roster; avoid additional roster objects so per-class quotas remain interpretable. Record all visible objects and any deviations regardless. Randomize capture order within practical constraints. Do not take repeated near-identical frames and count them as independent observations. Use multiple physical exemplars and at least three distinct tabletop/background arrangements if available; record when only one room, device, or exemplar is feasible.

### Candidate object classes

Proposed safe household candidates, **only if the exact verified model exposes these canonical labels**:

- bottle
- cup
- bowl
- book
- cell phone
- remote control
- chair
- backpack

The roster is conditional, not a claim about the current model. Before collection, match spelling/IDs exactly to the detector's label map. Do not silently map “mug” to “cup,” “bag” to “backpack,” or an unsupported object into a supported class. If a candidate is unsupported, replace it before the run with a documented, safe, model-supported household class; preserve eight classes only if the model actually supports eight suitable classes. If fewer safe classes are supported, revise the quotas and total plan before recruitment rather than inventing support. Exclude people, faces, vehicles, roads, stairs/drop-offs, knives, medication, machinery, flames, and any object or scene likely to prompt a safety-critical interpretation.

### Scene variation and sampling controls

Use safe, ordinary tabletop or seated-room scenes; the phone and participant remain stationary. Vary and record, without over-claiming coverage:

- image zone (left/center/right using the locked app cutoffs);
- room/table/background arrangement and clutter (plain vs. ordinary mild clutter);
- indoor illumination (ordinary bright vs. moderate shadow; no unsafe lighting);
- object orientation, visible fraction/occlusion, and apparent image size;
- approximate camera-to-object placement or measured distance, where practical;
- device orientation/configuration only if supported and explicitly intended by the build.

Do not claim these strata represent homes, streets, or the full user population. Convenience recruitment, a single device, staged props, repeated exemplars, and the limited class roster create substantial selection and domain bias. Keep participant/task sampling separate from object/frame quotas. Record exclusions and missed quotas; do not replace difficult frames silently.

## 5. Stationary procedure

1. Welcome the participant in their preferred language; explain the study, uncertainty, privacy controls, non-mobility boundary, optional participation, compensation, and stop/withdrawal rights. Obtain consent before camera access.
2. Offer the participant their preferred seating, phone placement, controls, audio volume, and accessible materials. A facilitator may place props and operate the device. The participant may decline any task or stop without penalty.
3. Keep the participant seated in a safe, familiar room. Mount or place the camera at the table; frame only the staged objects and background. Do not point it at people, private documents, screens, addresses, or bystanders. Confirm the scene is still and capture one frame for each manifest row.
4. In the optional user-feedback portion, ask only seated questions such as “What did you understand from that description?” and “Was the uncertainty clear?” Do not ask the participant to locate an object by walking, approach it, move around furniture, judge a path, or act on a detection.
5. Run offline as specified at the pre-field gate. Record app states and event timestamps, not ambient conversation. If TTS is unavailable or unintelligible, label the event as a speech failure; do not substitute a facilitator's spoken description as if the app said it.
6. At session end, confirm comfort, explain how to withdraw data within the approved limits, provide compensation regardless of completion, and offer contact details for questions or concerns.

### Why participants are not asked to walk

Walking would introduce mobility risk and would directly test or imply a mobility/navigation function the product explicitly does not provide. It could also encourage reliance on uncertain detections for movement, while this camera view cannot establish depth, full scene geometry, or route safety. The protocol therefore tests only static image descriptions and seated comprehension. Left/center/right is evaluated against the captured image, not against a participant's body, a physical direction, a distance, or a clear path. Safe zone variation is created by placing props in different parts of a stationary camera frame, never by moving the participant.

## 6. Annotation schema and matching

Use a pseudonymous `run_id`, `observation_id`, `scene_id`, and `participant_id` (where applicable). Do not put names, exact home addresses, face images, or unnecessary diagnoses in the dataset. Each frame row should include:

- **Run/device:** app version/build hash, OS/device, model/version/hash/license reference, class map, confidence threshold, offline state, capture order, and annotator IDs.
- **Scene:** room/table arrangement code, lighting category, background/clutter category, object orientation/scale/visible fraction, approximate placement/distance if recorded, intended primary class or target-absent, and protocol deviations.
- **Ground truth per object:** unique instance ID, canonical class or `non_target/unknown`, normalized bounding box `[x_min,y_min,x_max,y_max]` in `[0,1]`, occlusion/visibility (`clear/partial/ambiguous`), annotator confidence, and image-zone label from box-center x.
- **Prediction per detector output:** canonical emitted class, confidence, normalized box, image-zone label, result timestamp, whether spoken, exact app-generated output text/label if available, TTS status, and start timestamp. Do not store raw audio unless separately approved and essential.
- **Participant feedback (optional):** task completion by participant report, whether label/zone/uncertainty was understood, perceived delay/confusion, optional verbatim comment with consent, and any discomfort/withdrawal. This is qualitative usability feedback, not detector ground truth.

Two annotators independently label all 100 frames if feasible, without seeing the detector result during the first pass. Resolve class/box disagreements by documented adjudication before scoring. If dual annotation is infeasible, state that limitation and double-label a pre-specified subset; never hide disagreement. Mark truly ambiguous ground truth as `ignore/ambiguous` before unblinding; report its count and exclude only under the predeclared rule.

**Object matching (protocol choice):** within each frame and canonical class, match a prediction to at most one ground-truth instance using box IoU ≥ 0.50; match highest-confidence predictions first, using maximum IoU among unmatched ground-truth objects. This is a simple pilot rule, not a full COCO AP evaluation. A prediction with the wrong class or insufficient overlap is unmatched. Every unmatched prediction is a false positive, including duplicates; every unmatched eligible ground-truth object is a miss. Report class-confusion cases as both a miss for the true class and a false positive for the announced class, with a separate confusion table where possible.

**Image zones:** derive ground-truth and predicted zones independently from each box center x: LEFT if x < 0.38; CENTER if 0.38 ≤ x ≤ 0.62; RIGHT if x > 0.62. These are the current core-code boundaries and are not spatial directions in the world. If the build uses different boundaries, do not compare it to this protocol until the mismatch is documented and the protocol/implementation are reconciled.

## 7. Error definitions and metrics

- **True positive (TP):** an emitted detector class/box matched one-to-one to a visible, eligible ground-truth object under the locked matching rule.
- **False positive (FP):** an emitted eligible-class detection with no matched eligible object, including wrong-class announcements and duplicate detections. For target-absent frames, any eligible-class announcement is an FP.
- **Miss / false negative (FN):** an eligible visible ground-truth object with no matched emitted detection. Report partial/ambiguous objects separately as predeclared.
- **Class error:** emitted class differs from the ground-truth class. Report separately; it contributes one FN and one FP under class-aware matching.
- **Zone disagreement:** for a correctly class-matched detection, predicted-box zone differs from ground-truth-box zone. This is an image-relative disagreement only.
- **Stale/late result:** the result is not tied to the current captured frame or arrives after the scene/task is no longer current. Count as a timing/failure event and do not present it as current-frame correctness.

Report raw numerators and denominators, not just percentages:

1. Per-class and overall precision `TP/(TP+FP)` and recall/sensitivity `TP/(TP+FN)`; report no-denominator cases as `not estimable`, not zero.
2. False positives per frame and false-positive count/rate on the 20 target-absent frames; separately list repeated/duplicate announcements and wrong-class outputs.
3. Zone agreement among correctly class-matched detections, with a 3×3 ground-truth-zone × predicted-zone table; also report misses by ground-truth zone. Do not hide missed detections by calculating zone agreement only and implying total location accuracy.
4. If intervals are included, show the method and caveat that frames within a participant/scene are correlated. Provide per-class counts and uncertainty; do not interpret a small pilot interval as proof of safety.
5. Qualitatively summarize participant comprehension, confusion, perceived delay, and accessibility barriers without claiming population prevalence. Separate participant feedback from model metrics.

### Latency instrumentation

Use monotonic device-clock timestamps and one consistent clock for each event:

- `t0`: camera frame available to the app/detector;
- `t1`: detector result available;
- `t2`: TTS request submitted;
- `t3`: platform TTS utterance-start callback.

Report inference latency `t1−t0`, speech-queue/start latency `t3−t2`, and camera-to-TTS-start `t3−t0`, each as P50, P90, P95, maximum, and count/percentage timed out, missing, suppressed, or failed. Report latency by device/build and scene stratum. TTS callback is an app/platform event, not guaranteed acoustic onset or participant perception; label it accordingly. Do not use a human stopwatch as a substitute. Freeze a timeout definition before collection; if the product has no validated responsiveness requirement, report distributions and failures without inventing a pass threshold. Any output tied to an earlier frame is a stale-output safety event, not merely a slow successful detection.

## 8. Participant ethics, privacy, and accessibility

- Recruit adults who identify as blind or having low vision, with varied preferred language, residual vision/use of magnification, and comfort with mobile accessibility features. This purposive, small sample is for formative feedback, not representation of all blind/low-vision people. Do not request medical records or unnecessary diagnostic detail. If recruiting minors or adults unable to consent, stop and seek a separate approved protocol; they are out of scope here.
- Provide an accessible information sheet and consent process in the preferred language/format (for example, spoken explanation, screen-reader-compatible digital text, large print, or Braille where feasible). Check understanding without pressure. Participation is voluntary; participants can skip tasks, pause, withdraw, and request a support person. Compensation is not conditional on finishing or giving positive feedback.
- Seek ethics review/determination before recruitment and follow applicable institutional and Indian requirements. Obtain consent before recording or processing participant-linked data. Explain purpose, duration, foreseeable discomfort/privacy risk, data use, contact/complaint route, withdrawal limits after irreversible anonymization, and compensation.
- Stage objects in a controlled room. Exclude people and identifying/private content from frames. If an unintended person or private item enters view, stop capture and delete/quarantine that frame under the approved plan; do not retain it as a “negative” sample.
- The app is intended to process locally and not retain frames, but project documentation says this is not yet verified. Evaluation frame retention for annotation is a separate, explicit research activity: use approved encrypted local storage with access limited to the named study team, no cloud sync, and a fixed deletion date approved before collection. Prefer deletion of raw frames after adjudication and quality checks; retain only de-identified annotations/aggregate results as approved. If offline/network behavior or retention cannot be verified, do not collect participant imagery.
- Do not record ambient conversation or participant speech by default. Ask permission for any audio recording, justify necessity, and offer participation without it if feasible. Store no direct identifiers with annotations. Report small-cell results carefully to avoid re-identification.
- Provide breaks and stop for fatigue, distress, discomfort, inaccessible controls, or any request to stop. Do not ask participants to demonstrate or explain mobility practices.

## 9. Risk review and stop rules

| Risk | Control / response |
|---|---|
| False confidence or an output interpreted as navigation advice | Repeat the non-mobility boundary before and during the session; only staged static scenes; stop immediately if any output says or implies that a route is clear, an obstacle can be safely avoided, or movement is safe. Quarantine the build/output and review before any further participant work. |
| Missed, wrong, duplicate, or stale output | Describe outputs as uncertain; do not ask participants to act on them; log every event; stop the current task if the result is stale or confusing. Review any pattern that could invite unsafe reliance before resuming. |
| Participant movement/fall or unsafe setup | Seated tasks only; no walking/reaching beyond comfortable seated range; facilitator handles props and phone positioning; stop if someone needs to stand, move around, or feels unsafe. |
| Privacy or consent breach | Exclude people/private material; stop capture, quarantine/delete affected data, notify the study lead and ethics contact per approved process; resume only after containment and review. |
| Device/camera/TTS failure, heat, or app state ambiguity | Stop the trial; do not improvise a spoken answer; record failure state; resume only if participant agrees and the setup is safe. |
| Fatigue, distress, or withdrawal | Stop immediately on request or signs of discomfort; no persuasion; provide full approved compensation. |

**Immediate session stop:** participant asks, becomes uncomfortable, is not stationary, could be prompted to rely on the app for movement, a privacy/consent breach occurs, the device appears unsafe, output is stale/confusing, or the app provides a mobility/safety implication.  
**Study pause:** any serious adverse event, any credible unsafe reliance, any unconsented capture/data exposure, or any repeated/previously unrecognized failure mode that could plausibly cause harm. Notify the responsible lead/ethics body as required; investigate and document corrective actions. Restart only after explicit review/approval. A single serious event is sufficient to pause; no performance score overrides a stop rule.

## 10. Analysis, reporting, and decision use

1. Lock and archive the run manifest, model/build identifiers, labels, matching code/rules, and analysis script before scoring. Keep an audit trail of exclusions, missing frames, annotation disagreements, and protocol deviations.
2. Apply the one-to-one class-aware matching and zone rules in Section 6 to all evaluable observations. Independently check a sample of scoring outputs against hand calculations; preserve confusion cases and timeouts.
3. Produce a compact report with exact `n`, TP/FP/FN, precision/recall, FP per frame, per-class/stratum tables, zone confusion matrix/agreement, latency percentiles/failures, participant feedback themes, and deviations. State device, model, date, offline conditions, and all known sampling limitations.
4. Compare baseline vs. stress and per-class/per-zone results descriptively. Do not pool away a poor class or a severe failure with high-performing classes. Do not tune on these same observations and describe the result as independent validation; any retest after a change gets a new manifest and untouched observations where feasible.
5. Treat the pilot as a diagnostic gate for iteration, not a clinical/safety validation or statistical release threshold. No universal “acceptable” accuracy or latency threshold is claimed here. Product owners and accessibility/safety reviewers must define requirements and residual-risk tolerance before a go/no-go decision. Any serious stop-rule event blocks progression regardless of aggregate metrics.

## 11. Current execution status

This file specifies a future protocol only. No 100 observations have been collected, no participants have been recruited, and no performance, latency, accessibility, or safety result is asserted. Per the project status and task context, a target Android build/device/toolchain is not yet available; first complete the pre-field gates, then obtain ethical approval before any participant-facing study.
