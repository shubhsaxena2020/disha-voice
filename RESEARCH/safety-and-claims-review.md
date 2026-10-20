# Safety, Ethics and Claims Review — Disha Voice

**Date:** 2026-10-20
**Purpose:** define which words the project may and may not use, so that marketing, the app, the
website, and the release notes all stay inside what the evidence supports.

This is a **self-imposed publishing standard**, not legal advice. Nothing here is a legal
opinion, and no claim of regulatory compliance is made.

---

## 1. The claims matrix

Each claim is judged against what has actually been verified. **Allowed** means the project may
say it. **Conditional** means it may say it only with the stated qualifier. **Forbidden** means
the project must not say it, because it is not true or not established.

| Claim | Verdict | Why / what is required |
|---|---|---|
| "Runs entirely on your phone" | **Allowed** | The app has no `INTERNET` permission, verified with `aapt2` on the built APK. The detection model is bundled in the APK. |
| "Works offline" | **Allowed, scoped** | Say it about the **app's detection**. Do not imply the whole experience is offline — the Hindi voice may be a downloadable system asset. |
| "No account, no tracking, no analytics" | **Allowed** | No account, no analytics SDK, no crash SDK, no ads in the dependency graph. |
| "Requests only the camera" | **Allowed** | Verified: `CAMERA` is the only declared permission besides the AndroidX-generated internal one. |
| "Never saves your images" | **Allowed** | No code path writes frames. State it as an implementation property, not a promise about the future. |
| "Hindi-first" | **Conditional** | Only with the honest qualifier: "speaks Hindi **when the device has a Hindi voice installed**". The app says so aloud when it cannot. |
| "Describes objects in the camera view" | **Allowed** | This is exactly what it does, and the phrase says "in the camera view". |
| "Real-time" | **Forbidden** | No on-device latency measurement exists. A desktop-CPU number (25.9–35.9 ms) is not a phone claim. |
| "Accurate" / "X% accuracy" | **Forbidden** | Zero labelled observations have been collected. The field protocol is a plan, not a result. |
| "Detects objects" (unqualified) | **Conditional** | Say "recognises some common objects" and name the class set (COCO-2017, 90 classes) rather than implying general object detection. |
| "Private" / "100% private" | **Forbidden** | A privacy-policy-shaped promise is not an audit. Say the specific, checkable facts instead: no network permission, no persistence, no analytics. |
| "Lightweight" / "battery-efficient" | **Forbidden** | Memory, battery and thermal behaviour are entirely unmeasured (issue #10). |
| "Accessible" | **Conditional** | Say "designed for accessibility" or list the specific features. Do not claim a conformance level that has not been audited — no WCAG conformance claim is made for the app, and the website was checked by automated audit plus manual review, which is **not** a full WCAG conformance statement. |
| "Safe for navigation" / "helps you walk safely" | **Forbidden** | Absolutely not. This is the central non-claim of the project. |
| "Tells you where things are" | **Forbidden** | Ambiguous enough to be read as a world-space claim. Say "where the object is **in the image**". |
| "Knows what is in the room" | **Forbidden** | It sees a 320×320 crop of a frame, not a room. |
| "Not a mobility aid" | **Allowed — required** | It must appear in the app, the README, the website, and the release notes. |
| "Never misses anything" / "finds every object" | **Forbidden** | It misses objects, and the field evaluation has not measured how often. |

---

## 2. Mandatory safety statements

These must appear wherever the project is presented. They are present in all four required
places today (app UI, README, website, release notes).

> Disha Voice describes what the camera can see. It does **not** measure distance, detect
> hazards, or tell you where it is safe to walk. It may miss objects or misidentify them.
> Silence does not mean that nothing is there. Do not rely on it for independent mobility,
> street crossing, stairs, collision avoidance, or any safety-critical decision. Continue to use
> your established mobility aids, training, and practices.

---

## 3. Why these restrictions exist

The failure mode being defended against is not a legal one. It is a **user harm**: a blind user
who believes a calm, fluent voice is describing their surroundings accurately, and acts on
it.

| Restriction | Harm prevented |
|---|---|
| No distance, direction-of-travel, or hazard language | The user infers a safe path from an object description |
| Position is always qualified as "of the image" | The user mistakes an image coordinate for a bearing |
| The negative state is phrased as the model's limitation | The user reads "nothing found" as "nothing there" |
| Tentative wording for low confidence | The user treats a guess as a fact |
| No accuracy percentage | The user calibrates trust on a number that was never measured |
| No "real-time" | The user expects a responsiveness that was never measured on their device |

---

## 4. App-store and platform requirements (verified sources only)

| Requirement | Status | Source |
|---|---|---|
| Target API level for Play submission | **Not met.** The app targets API 36, which is current, but the app is not submitted. | Google Play target API requirement policy — <https://developer.android.com/google/play/requirements/target-sdk> |
| Privacy policy required for a Play listing | **Met in substance** — `docs/privacy/privacy-policy.md` is published and honest about being a draft. | Google Play requires a privacy policy URL for apps that collect personal or sensitive data. <https://developer.android.com/google/play/requirements/privacy-policy> |
| Data safety form | **Not yet done** — the truthful answer is "no data collected, no data shared", which is simple to declare, but the form has not been submitted. | Play Console data safety section |
| Health / medical claims | **Not applicable, and must stay not applicable.** The app must not be positioned as a health or safety device. | — |
| 16 KB page-size support | **Unknown.** Android 15+ requires 16 KB page-size support for new apps on some devices. The bundled native libraries (LiteRT) were not tested for this. Worth checking before any Play submission. | <https://developer.android.com/guide/practices/page-sizes> |
| `PLAY_RECOGNITION` / `FOREGROUND_SERVICE` | **Not used.** The app runs only while visible; no service is declared. | — |

**None of the above has been submitted anywhere.** No store listing exists.

---

## 5. Ethical commitments the project should make explicit

These are not enforced by the platform; they are the project's own commitments:

1. **No user images or audio are collected for model training, ever, without an explicit,
   separate, opt-in consent flow** — and a bystander-consent story for any such flow.
2. **No data collection in the MVP**, and any future analytics must be opt-in, aggregate, and
   documented.
3. **No user testing that asks a participant to walk, cross roads, or use stairs on the
   strength of the prototype's output.**
4. **No claims about the project being "for" blind people while excluding them from
   evaluation.** Sighted testing validates plumbing, not experience.
5. **Negative results get published**, not quietly dropped.

---

## 6. A note on the red-team review

The independent skeptical review (`RESEARCH/skeptical-review-research.md`) is a companion to
this document and should be read alongside it. It argues that the strongest likely failure of
this project is not a safety violation but the **TalkBack audio collision** (issue #8) — the
app being unusable for exactly the users it is built for.

The two documents agree on the rules. They differ on emphasis: this one is about what may be
*said*; the red-team one is about what may *work*. Both point at the same next step — a device.
