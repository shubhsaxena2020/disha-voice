# Disha Voice Demo Guide

**For a controlled prototype demonstration only.** Disha Voice is not release-ready. No Android device test has yet verified camera, detector, Hindi speech, or accessibility behavior (see [Project status](../PROJECT_STATUS.md)). This guide separates planned behavior from verified behavior.

## Safety first

Use a stationary phone in a quiet, controlled location. A presenter familiar with the build should manage setup and explain what is happening; do not ask an attendee to move around based on the app. Do not demonstrate near roads, stairs, edges, vehicles, machinery, or other hazards. Do not present the app as a mobility, navigation, obstacle-avoidance, or path-clearance aid. Follow the [Safety guidance](safety/SAFETY.md).

Ask permission before aiming the camera at people. Frame only a few ordinary, non-sensitive objects on a clear table. Avoid faces, screens, documents, addresses, and personal belongings. Tell attendees when camera access is active and stop the camera when the demonstration ends.

## Before the session

1. Use only a build and detector/model whose source, provenance, and distribution license have been reviewed. Never substitute a mock, sample image, or scripted label and imply it came from live detection.
2. Test in advance on the actual Android device and build. Verify camera permission behavior, supported-object detection, Hindi text-to-speech, spoken/visual error states, TalkBack/accessibility, and app recovery. If a behavior is unverified, describe it as planned and do not demonstrate it as working.
3. Confirm the device can run the model locally without an app backend. P0 intends local detection, no microphone, no app network/backend, and no retained frames. These are design requirements, not source-verified guarantees: do not promise “no data leaves the device” or “nothing is stored” until manifest, dependencies, storage, logging, backup, and network behavior have been audited and tested.
4. Check that the Android device has a Hindi TTS voice available and test actual playback. Android TTS language support depends on the installed engine and voice data; Hindi speech has not been verified for this project.
5. Charge the device; use a stable stand or a firm stationary grip. Ensure the camera lens is clear and the demo area is well lit. Have a non-app explanation ready.

## Suggested short demo

1. Explain: “This is an experimental visual-information prototype. It may miss or misidentify objects. Do not use it to move around or judge whether a route is safe.”
2. Explain the privacy status accurately: “The planned P0 design processes camera frames locally, does not use the microphone or an app backend, and does not retain frames. We have not yet verified all those behaviors in source and on a device.”
3. Open the app and explain the camera permission request before granting it. If permission is denied, stop; do not pressure anyone or bypass the choice.
4. With the phone stationary, point it only at a simple table arrangement of supported, non-sensitive objects. Describe any result as an uncertain image-relative observation, not a guarantee about the real scene.
5. If Hindi speech works on this tested device, play one brief output at a comfortable volume. Otherwise say Hindi TTS is unavailable on this device and stop the audio portion; do not silently switch languages or imply speech worked.
6. Stop camera use, close or leave the demo screen, and invite questions. Do not retain or share recordings of the demo unless everyone recorded has explicitly agreed and the storage/sharing implications are explained.

## If something fails

- **Camera permission denied, unavailable, or camera feed blank:** Stop the live demo. Explain that camera access is required for the planned visual feature and that permission is optional. Do not repeatedly prompt or claim detection is operating.
- **Hindi TTS missing, silent, or in the wrong language:** State clearly that spoken Hindi is unavailable or failed on this device. Do not treat silent output as success, use an unverified online voice, or depend on audio to convey a safety-critical message. Continue with a spoken explanation by the presenter only if useful, or end the demonstration.
- **Detector/model unavailable, slow, or gives no result:** Say detection is unavailable or uncertain and stop. Do not fabricate or manually narrate a result as if the app detected it. Never interpret no result as “nothing is there.”
- **Incorrect, confusing, stale, or unsafe-sounding result:** Stop camera use and explain the limitation. Do not retry in a risky setting or rely on the result. Record the issue without recording sensitive camera content, if practical.
- **App crash, overheating, or unexpected network activity:** End the demo, close the app if safe, and do not resume until investigated. Unexpected network activity means the intended offline/no-backend property is not established; report it for review.
- **Attendee asks to use it while walking or near a hazard:** Decline. Explain that it is not a mobility or hazard-avoidance tool.

## What is—and is not—verified

The project’s intended P0 scope is local visual detection, camera permission, no microphone, no app backend/network, and no retained frames. The repository’s current status says the Android build and on-device camera, detection, Hindi TTS, and TalkBack checks are not complete. Consequently, this guide is a safe demo procedure, not evidence that the planned safeguards or features work. Revisit this guide after implementation, privacy/security review, and device testing.

## References

- [Android Developers: Request runtime permissions](https://developer.android.com/training/permissions/requesting)
- [Android Developers: TextToSpeech API](https://developer.android.com/reference/android/speech/tts/TextToSpeech) — documents language-availability checks and missing/unsupported language data.
- [Disha Voice Privacy Policy](privacy/privacy-policy.md)
- [Disha Voice Safety](safety/SAFETY.md)
