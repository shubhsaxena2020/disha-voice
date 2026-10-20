# Disha Voice Privacy Policy

**Status:** Draft for the planned Android prototype; not a verified description of a released app. **Last reviewed:** 2026-10-20.

> **Important:** The project is not release-ready and has no verified Android build. The safeguards below are design requirements, not claims that the current source code has been audited or behaves this way. See [Project status](../../PROJECT_STATUS.md).

## In brief

Disha Voice is intended to be a Hindi-first visual-information assistant for blind and low-vision people. Its P0 design calls for local, on-device visual detection using camera access, with no microphone use, no app network/backend use, and no retention of camera frames. **These are intended requirements; they have not yet been verified against source, a built app, or a device.**

## Intended data handling — design requirements, not verified behavior

- **Camera images:** The app is intended to request camera permission only for the camera feature and process live frames locally for supported-object detection. Frames may be held temporarily in working memory while the camera and detector run. The design requirement is not to save or retain frames, thumbnails, or derived image files. Actual memory, cache, log, backup, and crash-report behavior has not been audited.
- **Detection results:** The app may derive object labels and image-relative positions from a frame. The design requirement is to keep this processing on the device and not send results to a server. This has not been verified.
- **Microphone:** P0 is intended not to request microphone permission or record speech. Voice input is not part of the stated P0 scope. This has not been verified against the app manifest or runtime behavior.
- **Network and accounts:** P0 is intended to work without an app backend or app network connection, and no account is planned. This has not been verified by inspecting dependencies, permissions, traffic, or a running build. Do not interpret it as a verified “no data leaves the device” guarantee.
- **Spoken output:** Hindi text-to-speech (TTS) availability and offline capability depend on the Android device and installed speech engine. The app has not been verified with a Hindi voice. A system or third-party TTS engine may have its own data practices or network needs; this project has not audited those engines. The design requirement is to disclose unavailable Hindi speech and not silently switch to an unverified network voice.

## Privacy realities and your choices

Camera permission allows camera access while the app uses it; it does not by itself prove how frames are processed, stored, or transmitted. Android, the device manufacturer, installed TTS engines, backups, and other software have policies and behavior outside this project’s control. Temporarily processed frames are still sensitive while present in device memory. Avoid pointing the camera at private documents, screens, or people who have not agreed to be included.

You can deny or revoke camera permission in Android settings. The app should explain why it needs camera access, check permission when needed, and degrade gracefully if permission is denied; these are design requirements pending verification. Android’s permission guidance recommends explaining sensitive access and handling denial gracefully.

## Retention, sharing, and security

The design requirement is that Disha Voice not persist camera frames or send them to a project backend. Because neither behavior has been source-verified, this policy cannot yet promise that no copies, logs, backups, diagnostics, or third-party processing occur. The project has not completed a dependency, telemetry, backup, or security audit and cannot yet provide a verified list of service providers or retention periods. Do not use a prototype with sensitive imagery on the assumption that it is confidential.

## Questions and updates

A monitored privacy contact channel has not yet been established. A public release must add one before distribution. This policy must be updated after source/manifest and dependency review, network and storage testing, and device verification; update it again if those behaviors change. This document is a transparency draft, not legal advice or a statement of legal compliance.

## References

- [Android Developers: Request runtime permissions](https://developer.android.com/training/permissions/requesting)
- [MeitY: Digital Personal Data Protection Act, 2023](https://meity.gov.in/content/digital-personal-data-protection-act-2023)
