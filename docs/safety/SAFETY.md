# Disha Voice Safety

**Status:** Safety position for a prototype in progress; behavior and mitigations are not verified until tested in the app. See [Project status](../../PROJECT_STATUS.md).

## Intended use

Disha Voice is intended to provide limited visual information about supported objects in a camera view, including image-relative position. It is an experimental aid, not an authority. It may miss objects, misidentify them, report a stale or misleading position, or fail silently. Lighting, camera angle, motion, obstruction, model limits, and device performance can affect results.

## Not for mobility or hazard avoidance

**Do not use Disha Voice to walk, cross streets, navigate, avoid obstacles, judge whether a path is clear, detect stairs or drop-offs, or make any other mobility or safety-critical decision.** Do not use its output instead of a cane, guide dog, trusted human assistance, established mobility skills, traffic signals, or other appropriate aids. A camera view cannot reliably establish the full scene, distance, depth, motion, or safety of a route. Keep attention on the environment and follow your usual mobility practices.

## Safe-use boundaries

- Use only while stationary in a safe place, and only as a supplemental description of visible objects.
- Do not rely on detections to identify people, read sensitive material, diagnose conditions, or make decisions about emergencies, vehicles, machinery, medication, or other high-consequence matters.
- Treat every label and position as uncertain. Confirm important information independently.
- Stop if the camera, display, spoken output, or app state is confusing, delayed, or unavailable.
- Respect consent and privacy when directing the camera toward other people or private spaces.

## Camera and spoken-output limits

Camera use and local processing without saving frames are P0 design requirements, not source-verified guarantees. The project has not yet verified capture, temporary memory, storage, logging, network traffic, or third-party SDK behavior. See the [Privacy Policy](../privacy/privacy-policy.md). Do not assume camera frames cannot be recovered or exposed.

Hindi text-to-speech availability is device-dependent and has not been verified on a target device. If Hindi speech is unavailable, the app/demo must say clearly that spoken Hindi output is unavailable, avoid implying that a detection was spoken, and offer a safe stop/exit or visual alternative if available. Do not silently substitute an unverified voice or depend on audio for a safety instruction.

## Prototype and testing status

The prototype is not release-ready. No Android device test has verified camera access, on-device detection, Hindi TTS, accessibility, or fail-safe behavior. Design requirements for a future build include clear permission rationale, graceful camera-denial handling, visible/audio status that does not overstate success, and fail-closed behavior when the camera or detector is unavailable. These are requirements, not verified features. Do not use the prototype as a mobility or safety product.

## Reference

- [Android Developers: Request runtime permissions](https://developer.android.com/training/permissions/requesting) — explains permission rationale and graceful handling of denial.
