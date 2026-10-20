# Disha Voice v0.1.2-prototype — **debug build, pre-release**

**This is an early prototype. It is not a release, and it has not been tested on a physical Android device.**

## What this artifact is

- **Build type:** debug (signed with the standard Android debug key)
- **Version:** `0.1.2-prototype` (versionCode 3)
- **APK:** `disha-voice-v0.1.2-prototype-debug.apk`
- **Size:** 30,671,369 bytes
- **SHA-256:** `6b55a59f2a76b94fad397a22b34a021d26adf1a125f1e809b6528ade057f2790`
- **Minimum Android version:** 7.0 (API 24)
- **Target Android version:** 16 (API 36)
- **Permissions requested:** `CAMERA` only. The app has **no** internet permission, requests **no**
  microphone, and contains **no** analytics or crash-reporting SDK.
- **Signing status:** debug-signed. It is **not** a production-signed release. Do not distribute
  it as one.

Verify the checksum after download:

```bash
sha256sum disha-voice-v0.1.2-prototype-debug.apk
# 6b55a59f2a76b94fad397a22b34a021d26adf1a125f1e809b6528ade057f2790
```

## What changed from v0.1.0-prototype

**No user-visible change.** This is the same prototype, republished because the source
changed in a way that matters for trust rather than for behaviour:

- The input-tensor construction was extracted into a testable unit (`InputTensorBuilder`),
  and **7 new unit tests** now pin the model's input convention: raw unsigned 0-255 channel
  values in R, G, B order, with no float scaling, no mean/std subtraction and no
  requantisation.

  That convention was previously only *assumed*. It has now been **measured**: the real model
  was run on the real test images under three candidate conventions, and raw uint8 is the
  correct one (ImageNet mean/std normalisation is measurably worse — bus 0.801 -> 0.668,
  dog 0.840 -> 0.801). See `RESEARCH/model-and-data-audit.md` section 3.

  The reason this needed pinning at all: a silent change to the pre-processing would degrade
  detection without breaking anything, and the desktop benchmark shares the same convention,
  so it would not have caught it either.

- The test count is now **28 distinct tests**, 0 failures (was 21).

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
adb install -r disha-voice-v0.1.2-prototype-debug.apk
```

Or copy the APK to the device and open it (you will need to allow installation from your
file manager).

## Full details

- Project status and the full release gate: `PROJECT_STATUS.md` in the repository
- Development setup: `docs/DEVELOPMENT_SETUP.md`
- Privacy design: `docs/privacy/privacy-policy.md`
- Safety statement: `docs/safety/SAFETY.md`
- Model and licence audit: `RESEARCH/model-and-data-audit.md`
