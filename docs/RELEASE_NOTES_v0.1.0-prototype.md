# Disha Voice v0.1.0-prototype — **debug build, pre-release**

**This is an early prototype. It is not a release, and it has not been tested on a physical Android device.**

## What this artifact is

- **Build type:** debug (signed with the standard Android debug key)
- **Version:** `0.1.0-prototype` (versionCode 1)
- **APK:** `disha-voice-v0.1.0-prototype-debug.apk`
- **Size:** 30,671,369 bytes
- **SHA-256:** `96bab1f33062922add352ea4709bac080bd3f2266f19d582e53928346f3dc7b0`
- **Minimum Android version:** 7.0 (API 24)
- **Target Android version:** 16 (API 36)
- **Permissions requested:** `CAMERA` only. The app has **no** internet permission, requests **no**
  microphone, and contains **no** analytics or crash-reporting SDK.
- **Signing status:** debug-signed. It is **not** a production-signed release. Do not distribute
  it as one.

Verify the checksum after download:

```bash
sha256sum disha-voice-v0.1.0-prototype-debug.apk
# 96bab1f33062922add352ea4709bac080bd3f2266f19d582e53928346f3dc7b0
```

## What it does

Point the camera at something; the app looks for a small set of common objects with an
on-device model and may speak a short sentence naming the object and where it sits **in the
image** (left / centre / right).

- Detection is **fully on-device**. The model is bundled in the APK, so it works with no
  network access. There is no server, no account, and no analytics.
- The app speaks **Hindi** when the device has a usable Hindi voice installed, and otherwise
  says so plainly and falls back to English.
- Repeated announcements of the same object in the same image region are suppressed, and the
  pause between announcements is configurable in the source.

## What it does NOT do — read this

- It does **not** measure distance, estimate how far away something is, or tell you the size of
  anything.
- It does **not** detect hazards, obstacles, stairs, drops, traffic, or anything else about
  whether a path is safe.
- It does **not** clear a path, and it does **not** guide you. "Left / centre / right" refers
  only to the position of an object **within the camera image** — it says nothing about where
  the object is in the room or in the world.
- It **can** miss objects, and it **can** misidentify them.
- **Silence does not mean that nothing is there.** If the app says it did not recognise
  anything, that describes the model's output, not the safety of your surroundings.

> **Do not rely on this app for independent mobility, crossing roads, navigating stairs,
> collision avoidance, or any safety-critical decision.** It is not a replacement for a white
> cane, a guide dog, mobility training, or a trusted guide. Keep using your established
> mobility aids and practices.

## Known limitations of this build

- **Never tested on a physical handset.** Camera behaviour, speech output, install and launch,
  TalkBack, performance, and battery impact are all unverified.
- The confidence thresholds (0.70 / 0.45) are prototype defaults, **not** values calibrated
  against real-world data.
- Latency was measured only on a development computer (25.9–35.9 ms median, 7 repeats per
  image). No phone measurement exists.
- A Hindi voice may not be installed on your device. The app will tell you when it falls back
  to English.

## Installing

```bash
adb install -r disha-voice-v0.1.0-prototype-debug.apk
```

Or copy the APK to the device and open it (you will need to allow installation from your
file manager).

## Full details

- Project status and the full release gate: `PROJECT_STATUS.md` in the repository
- Development setup: `docs/DEVELOPMENT_SETUP.md`
- Privacy design: `docs/privacy/privacy-policy.md`
- Safety statement: `docs/safety/SAFETY.md`
- Model and licence audit: `RESEARCH/model-and-data-audit.md`
