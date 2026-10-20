# Disha Voice Android P0: reproducible benchmark plan

**Purpose.** Define measurements that can be made now without an Android handset and measurements that must wait for a real device. This is a protocol and test plan, not evidence that an APK, inference benchmark, or thermal run has been completed.

## Current verified boundary

- `adb devices -l` reported no attached devices in this session.
- No APK was present in the repository, and the current Gradle project includes `:core`, a Kotlin/JVM policy module, but no Android app module. Therefore APK size, inference latency, and device thermal results do not exist yet and must not be estimated.
- The Android-free `:core:test` suite, including the deterministic synthetic tests added with this plan, ran successfully using Gradle 8.14.3 and JDK 17. This is a host test result, not an app performance result.
- There is currently no tracked Gradle Wrapper in the repository. To make future reruns independent of this machine's scratch Gradle installation, add and commit a wrapper pinned to the selected Gradle distribution before treating test wall-clock timings as reproducible. Keep the JDK version fixed and record both versions.

No benchmark result dataset is created by this plan. The companion `benchmarks/benchmark-schema.json` defines what an actual future run must record. Omit measurements that have not been collected; do not insert zeros, `N/A`, guessed values, or synthetic latency/thermal readings.

## Measurement matrix

| Measurement | Now, without a phone | Later, on a real Android device |
|---|---|---|
| APK artifact size | Only after a real APK exists. Record exact file bytes, SHA-256, build variant, and tool output. No APK is currently available. | Same procedure; compare only like-for-like build variants and APK splits. |
| Synthetic policy correctness | Yes: deterministic Kotlin/JVM unit/property tests with generated inputs and recorded seed/case counts. | Same tests remain Android-free; do not substitute them for device validation. |
| Policy test runner wall time | Optional host-side Gradle wall time, at least 3 independent executions. Label it as test-suite execution time only; it is not inference, app startup, or user-perceived latency. | May also be captured on the build host, separately from on-device results. |
| Model inference latency | No. Host/JVM timings, fake detectors, emulators, and synthetic policy tests are not valid device inference measurements. | Yes, using the exact distributable model/runtime on the target device and a fixed input. Keep model-only inference distinct from preprocessing or end-to-end camera/TTS timing. |
| Thermal status | No. | Yes, record Android `PowerManager` thermal status samples alongside the workload. Do not report fabricated status or infer degrees Celsius from a status value. |

## 1. Android-free synthetic domain policy tests

### Test suite

The tests are under `core/src/test/kotlin/org/dishavoice/core/` and run with the existing Kotlin/JVM test task; they do not import Android APIs or need an SDK, emulator, camera, model, network, or phone.

`AnnouncementPolicyPropertyTest.kt` uses a fixed seed (`0xD15A0`) and checks:

1. 10,000 generated, ordered, normalized boxes stay valid and classify to the zone implied by their center; classifying the same box twice is stable.
2. Confidence bands do not regress as confidence increases across a 10,001-point normalized grid.
3. 36 synthetic label/zone keys test whitespace/case normalization, per-key duplicate suppression, and allowance exactly at the 4,000 ms cooldown boundary.
4. Invalid and below-floor detections do not speak or start a cooldown.

Existing example-based `AnnouncementPolicyTest` cases continue to cover fixed zone boundaries, invalid input, confidence thresholds, and duplicate policy behavior. These are policy correctness checks only: they do not assess real detector precision/recall, speech quality, accessibility, or speed.

### Reproducible execution and optional timing

1. Use the same checkout/commit, clean working tree, Gradle Wrapper distribution, JDK vendor/version, host power mode, and test command for the full comparison. Record whether the tree is dirty; do not compare dirty and clean runs as if identical.
2. Run the full test task three times as separate Gradle process invocations, forcing task execution each time. From the repository root, once a wrapper is committed, use:

   ```sh
   ./gradlew :core:test --no-daemon --rerun-tasks
   ```

   On Windows, use `gradlew.bat :core:test --no-daemon --rerun-tasks`. For each execution, retain the actual test report and record pass/fail/skip totals, command, repeat index, and elapsed wall time if measured. Use a monotonic host timer around the command; identify the host OS, CPU, JDK, Gradle, and power mode.
3. Do not aggregate away a failing repeat. Any failure invalidates the suite result until diagnosed and rerun; preserve the failing output.
4. If the command wall time is reported, call it **host Gradle test-suite wall time**, not policy runtime or Android performance. Gradle startup, compilation, cache state, CPU load, and filesystem state affect it. Keep the same cache policy across repeats (prefer a warmed, unchanged cache for all measured runs) and avoid concurrent CPU-heavy tasks. Build/test wall time is informational only and should not be used as a user-facing latency gate.
5. Do not make tests depend on nondeterministic random seeds, real images, wall-clock time, network access, Android SDK state, or a phone. A future property test must log and fix its seed and generated-case count.

## 2. APK size (only when an APK artifact exists)

1. Build the intended release-like variant and identify the exact APK file(s), ABI/split, version, variant, commit, and build configuration. Do not use a debug APK as a release-size proxy. If the app is delivered as split APKs, report each artifact and the explicitly defined total; do not silently treat one split as the complete install.
2. Record the exact artifact byte count from the file system and SHA-256 of that same file. Record the command/tool and version. An APK's exact file size is not a timing metric, so timing repeats are unnecessary; repeat a read only to detect an artifact being replaced, and require the hash and byte count to remain identical.
3. Optionally record `apkanalyzer apk download-size <file.apk>` as a separate estimate, never as the APK's exact file size. If available, use APK Analyzer for composition (DEX/resources/native libraries) and retain the output; do not add component sizes in a way that double-counts ZIP entries.
4. Store no APK-size result until a build actually produces an APK. APK Analyzer documentation explains the distinction between raw/compressed file size and estimated download size and supports artifact comparison. [APK Analyzer](https://developer.android.com/studio/build/apk-analyzer); [apkanalyzer CLI](https://developer.android.com/studio/command-line/apkanalyzer).

## 3. On-device inference latency (future; physical device only)

### Eligibility and scope

- Run only after a real target model and Android app/runtime exist, on the intended physical Android device. Record device manufacturer/model, Android build fingerprint/API, ABI, app commit/build variant, model identifier/version/SHA-256, inference runtime/version, delegate/backend, thread count, tensor input shape/dtype, and preprocessing configuration.
- Use the real production model. Do not benchmark an empty stub, a fake detector, desktop/JVM execution, or an emulator and call it handset inference.
- Define the measured stage exactly. The primary metric is model invocation latency with the model already loaded and a fixed, versioned local input tensor. Measure model load, image decode/resize/preprocessing, camera capture, and speech/TTS separately if needed; do not combine them into “inference latency.” Record whether setup/teardown is outside the timed region.
- Use AndroidX Microbenchmark for an isolated hot Kotlin/Java inference path, following its warmup and result-output behavior. If measuring app startup, UI interaction, or a larger user journey, use Macrobenchmark instead and name that metric separately. Use a release-like, non-debuggable, profileable target for detailed profiling; record any profiling/benchmark build differences. Android docs recommend Microbenchmark for frequently executed small code paths and Macrobenchmark for larger app use cases. [Microbenchmark](https://developer.android.com/studio/profile/microbenchmark-overview); [Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview); [`<profileable>`](https://developer.android.com/guide/topics/manifest/profileable-element).

### Fixed conditions and repeats

1. Freeze the model/app/input hashes, runtime/delegate, input shape, thread count, and benchmark code. Use the same device and OS build across a comparison; if either changes, start a separately identified run.
2. Keep the app foreground and screen/orientation fixed. Disable battery saver, use the same network state (prefer offline/local inputs when network is not part of the metric), and keep charging state and display brightness constant across repeats. Record the actual settings rather than assuming them. Do not change clocks or use thermal overrides to make a result look faster.
3. Begin from the same defined state each time (e.g. model already loaded for inference-only). Allow device thermal state to settle to the predeclared starting condition and record it; if it drifts during a run, retain the result with the drift recorded or invalidate and rerun under the predeclared rule. Do not selectively discard slow measurements.
4. Perform at least **3 independent benchmark invocations** on the device. For Macrobenchmark, use at least **5 iterations per invocation** and preserve the generated JSON/trace outputs. For an inference-only harness, use the benchmark library’s documented warmup, then retain its actual per-invocation result and sample/iteration count. The same three-repeat minimum applies to any manually instrumented timing. Never present three calls within one loop as three independent runs.
5. Time elapsed inference with a monotonic clock. On Android, `SystemClock.elapsedRealtimeNanos()` is documented as monotonic and includes deep sleep; use it consistently if timing directly, or retain the benchmark library's reported metric and identify the timer/tool. Record units and the timed region. [SystemClock](https://developer.android.com/reference/android/os/SystemClock).
6. Preserve every repeat's raw benchmark result/trace and report each repeat separately. Summaries (min/median/max) may be computed from measured data but must not replace the per-repeat source results. Do not report an invented aggregate if a repeat failed.

## 4. On-device thermal observation (future; physical device only)

- Sample Android `PowerManager.getCurrentThermalStatus()` before, during, and after the same defined workload; a thermal-status listener may be used to retain transitions. Record timestamps relative to the run, Android API level, API/source, and status enum name for every observed sample. Keep thermal observations linked to the corresponding inference run.
- Capture at least the three inference repeats above and record status for each. If the device/API cannot provide the requested observation, omit the unavailable measurement and document the limitation in the run notes; do not replace it with a guessed value or `THERMAL_STATUS_NONE`.
- Thermal status is a platform throttling category, not a direct temperature-in-degrees reading. Do not claim a temperature in °C unless a separate calibrated sensor actually measured it and its provenance is recorded. Android documents `PowerManager` thermal status APIs and the thermal status categories. [PowerManager API](https://developer.android.com/reference/android/os/PowerManager); [Android thermal API guidance](https://developer.android.com/games/optimize/adpf/thermal).

## Metadata and validity checklist

Every recorded run must have a run ID, UTC capture timestamp, protocol/schema version, repository commit and dirty-tree state, host OS/architecture, JDK and Gradle versions, exact commands/tools, and only measurements actually produced. Device results additionally require the physical device/build/API and fixed-condition values; model results require model/runtime/input identifiers and measured-stage definition. Hash the APK/model/input artifacts where applicable. Never record a phone serial, IMEI, user speech, camera capture, or other personal data in this benchmark record.

A run is comparable only when the measured scope and relevant artifacts/conditions match. Missing measurements mean “not collected,” not zero. The machine-readable record contract is `benchmarks/benchmark-schema.json`.
