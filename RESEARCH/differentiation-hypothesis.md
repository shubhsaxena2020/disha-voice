# Differentiation Hypothesis — Disha Voice

**Date:** 2026-10-20
**Status:** a primary hypothesis is proposed but **not yet validated**. The validation plan is a
design, not a result.

Every claim is labelled **VERIFIED** (with a source), **OBSERVATION**, **HYPOTHESIS**, or
**UNKNOWN**.

---

## 1. What is *not* novel

Stated plainly, because a novelty claim would not survive scrutiny:

- **Object detection on a phone camera is not novel.** EfficientDet, MobileNet-SSD, and YOLO
  variants have been on-device for years.
- **Android accessibility apps that detect objects are not novel.** Lookout, Seeing AI, Be My
  Eyes, Envision, SightBuddy, SeeForMe, and others already do some form of this. **VERIFIED** as
  existing products; the exact feature-by-feature comparison is still being compiled and the
  licence audit for the open-source ones is outstanding (issue #4).
- **Text-to-speech in an Indian language is not novel.** Android TTS has shipped `hi-IN` voices
  for years.
- **A "Hindi-first" wrapper is a localisation layer, not a research contribution** on its own.
- **CORRECTED 2026-10-20 after the competitor research:** *"Hindi-first" and "offline" are not
  even differentiators, because the market leader already has both.* Google Lookout supports
  **Hindi** (along with Bengali, Gujarati, Kannada, Marathi, Tamil, Telugu and 30+ others),
  has worked **offline** since 2022, and its Find mode provides **direction and distance**.
  See `RESEARCH/competitor-landscape.md`. **H4 below is therefore downgraded from a
  differentiator to a table-stakes requirement**: the app must have these properties to be
  usable in India at all, but having them is not an achievement, and claiming otherwise would
  be refuted by a Google support page.

So the honest framing of this project is: *a small, careful, offline-first, accessibility-first
implementation of an existing idea, with the research effort spent on the parts that are
genuinely under-specified.*

---

## 2. Candidate hypotheses

### H1 — Lower repetitive-alert burden (**primary candidate**)

> **Statement:** For a blind user running a camera continuously, the dominant failure mode of
> assistive vision apps is not detection accuracy but *announcement fatigue*; a system that
> suppresses repeats and paces its output will be used longer and rated more favourably than an
> equally accurate system that speaks every frame.

- **Mechanism (why it might be true):** a continuously running detector produces a detection on
  most frames; speaking each one produces a stream of near-identical utterances that competes
  with the user's other audio cues and with the screen reader. The design of this project makes
  suppression a first-class feature (dedup on (label, zone) with a 4 s cooldown) rather than an
  afterthought.
- **Falsifier:** if users report that suppression removes information they needed, or that
  continued operation is curtailed by *missing* announcements rather than by excess, H1 fails.
- **Metric:** repeat-announcement rate per minute in continuous use; self-rated annoyance;
  proportion of a session in which the user leaves the app running.
- **Baseline:** the app's own behaviour with suppression disabled, plus user-reported experience
  with existing apps (which is observational, not a controlled comparison).
- **Design:** within-subject, two conditions (suppression on / off), stationary, 5 minutes per
  condition, counterbalanced order, at least 8 participants to start; treat the first 8 as a
  pilot that estimates effect size, then size the main study from that. Stopping rule: if the
  pilot shows no measurable difference in either direction, abandon H1 rather than enlarge the
  study.
- **Segment:** users who run the app for more than a minute at a time.
- **Risk:** the effect may be smaller than the enthusiasm for it; suppression could also be
  actively harmful if the user is *looking for a specific object* and needs re-announcement.

### H2 — Honest uncertainty is a feature, not a disclaimer

> **Statement:** For this audience, a system that says "it looks like a cup" when it is unsure
> and stays silent when it is not, is more useful and more trusted than one that states every
> detection confidently.

- **Mechanism:** a wrong confident statement is worse than no statement, because the user may
  act on it. Two-threshold gating (0.70 confident / 0.45 tentative / below silent) with
  tentative wording is the mechanism.
- **Falsifier:** if users ignore or distrust tentative statements to the point of ignoring
  confident ones, or if the phrasing makes the app seem unreliable overall.
- **Metric:** false-confident rate (a confident statement about an object that is not present)
  under a controlled scene; expected calibration error of the confidence gate.
- **Baseline:** the always-confident variant, within-subject.
- **Design:** a scene grid with known contents, 30 trials per condition; the dependent variable
  is the user's action and their stated confidence, not just a rating.
- **Segment:** all users; highest value where the model is weakest.
- **Risk:** the thresholds are currently **uncalibrated prototype defaults** (issue #2) — this
  hypothesis cannot be evaluated until they are derived from data.

### H3 — Image-relative position is useful

> **Statement:** Telling a user that an object is on the left, centre, or right **of the
> image** helps them act on it.

- **This is the most contested hypothesis in the project, and it may well be false.** Image
  position is not world position; a blind user pointing a camera does not know which way they
  are facing relative to the object.
- **Falsifier:** if users cannot use the information to locate anything, or if egocentric
  wording ("to your left") performs no better, the feature is decoration and should be removed
  rather than defended.
- **Metric:** time-to-locate and correctness of the user's reach, under image-relative vs.
  egocentric vs. no-spatial wording.
- **Design:** a stationary reaching task, never a walking task, with safe reachable targets;
  at least 12 participants for a pilot; the test is designed to be able to *reject* the feature.
- **Segment:** users who are confident about their own orientation.
- **Risk:** this may be the point where the project should conclude that the honest product is
  *object identity without position*, and say so.
- Tracked as issue #3.

### H4 — Hindi-first, offline, hands-free (**downgraded to table stakes**)

> **Statement:** For users whose first language is Hindi and whose data allowance or connectivity
> is limited, an offline app that speaks naturally in Hindi is materially better than an
> English-only or cloud-dependent one.

- **Downgraded 2026-10-20:** Google Lookout already ships Hindi, already works offline, and
  already provides direction and distance. This is therefore **not a differentiator** — it is a
  precondition for being usable in India at all. The hypothesis below survives only as a
  statement about *which users are better served*, not as a claim of novelty.

- **Mechanism:** no network dependency, no data cost, no upload of the user's surroundings, and
  speech in the user's own language.
- **Falsifier:** if participants prefer English, or if the device's Hindi voice is missing or
  poor in practice, the Hindi-first premise collapses.
- **Metric:** task-completion rate and preference in Hindi vs. English; offline success rate.
- **Design:** within-subject language preference with both phrases spoken; record which the
  participant keeps.
- **Segment:** Hindi-first users, especially where connectivity is unreliable.
- **Risk:** **the Hindi voice is a platform dependency, not ours.** Whether a usable `hi-IN` voice
  exists on a given handset is unknown, and several Indian OEM skins ship minimal TTS data. The
  app must degrade honestly to English, and that degradation may make the "Hindi-first" claim
  hollow on many devices. This is issue #1.

### H5 — Transparent local evaluation

> **Statement:** Publishing reproducible, on-device measurements — including the failures — is a
> contribution in itself, because the assistive-vision category publishes almost no comparable
> numbers.

- **Mechanism:** the release notes, the benchmark harness in `benchmarks/`, and the honest
  status dashboard let a user or a reviewer check the claims instead of trusting them.
- **Falsifier:** if it turns out that competitors *do* publish equivalent measurements, this
  is not a differentiator.
- **Metric:** presence and reproducibility of the artefacts.
- **Status:** **partially realised.** `benchmarks/scripts/run_benchmark.py` is reproducible; the
  on-device half of the claim is blocked by the absence of a device.

---

## 3. Chosen positioning

> **Positioning statement:** For blind and low-vision users in India who want information
> without noise, Disha Voice is an accessibility-first Android prototype that names what the
> camera can see and keeps quiet otherwise — running fully on-device, with no account, no
> tracking, and no upload of the user's surroundings.
>
> **It is deliberately narrower than Google Lookout.** Lookout already speaks Hindi, works
> offline, and even gives direction and distance. Disha Voice competes on a different axis:
> **it says less, says it honestly, and never sends a frame off the phone** — and it publishes
> the measurements that show whether that is actually better.

**Elevator pitch (one paragraph):**

> Google Lookout already speaks Hindi and works offline. Disha Voice takes a different bet:
> that the problem with assistive camera apps is not that they fail to speak, but that they
> never stop. Lookout users complain that it "is continually announcing 'No text in view'" and
> that the announcements "cannot be turned off". So Disha Voice is built to say less — one
> short, honest phrase about one object, repeated only when it has changed — and to run
> entirely on your phone, with no account, no tracking, and no upload of your surroundings. It
> is an early prototype, not yet tested on a phone, and explicitly not a navigation aid; it
> publishes exactly what has and has not been verified.

**Three-line store description:**

> Point your camera. Disha Voice names what it sees, in Hindi, and keeps quiet otherwise.
> Runs fully on your phone — no account, no tracking, no uploads.
> An accessibility prototype, and not a navigation aid.

**Tagline options (Hindi + English):**

1. "जो दिखे, वही बोलें — बस ज़रूरत पर।" / "Say what you see. Only when it matters."
2. "शांत आवाज़, साफ़ जानकारी।" / "A quieter voice. Clearer information."
3. "कैमरा आपका, जानकारी आपकी।" / "The camera is yours. So is the information."
4. "बोलकर जानें, चुप रहकर सोचें।" / "Speak to know. Be quiet to think."
5. "एक नज़र, एक सही बात।" / "One look, one true thing."

---

## 4. The honest summary

**There is no strong novel claim available for this project at present.** The strongest
defensible position is narrower and should be stated as such:

> A carefully-made, offline-first, Hindi-first prototype whose *contribution is the restraint
> and the honesty* — with a published measurement trail — rather than any new detection
> capability.

If H1, H2, and H3 are validated on real devices with real users, that becomes a credible
research contribution. If H3 fails, the honest outcome is to publish the negative result and
simplify the product. Both outcomes are worth having; neither is worth overclaiming.
