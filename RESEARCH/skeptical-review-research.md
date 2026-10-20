# Independent Skeptical Review — Disha Voice

**Date:** 2026-10-20
**Role:** adversarial review. The purpose of this document is to **attack** the project, not to
support it. A finding that only confirms what the project already believes is a wasted finding.

Each finding carries: **SEVERITY**, **CONFIDENCE**, **BLOCKS_MVP**, the evidence, and the
specific test that would settle it.

**Method note and an honest limitation:** this review was produced by the same agent that built
the prototype, working adversarially against its own work. That is a weaker form of
independence than a reviewer with no stake in the code, and it is stated here rather than
implied away. The automated research passes that were dispatched to produce competing
independent reviews did not produce output files. The strongest findings below are the ones
that survived the most hostile reading of my own work.

---

## 1. The core attack: is "Hindi-first + offline" a moat at all?

**SEVERITY: high | CONFIDENCE: high | BLOCKS_MVP: no**

**The argument:** localisation and offline-ness are *configuration*, not research. Every
competitor can add a Hindi voice. Offline is a packaging decision — bundle the model instead
of downloading it. Neither is defensible on its own, and the project's own positioning document
concedes this. If the moat is reduced to "we did the Hindi part carefully", the work is
**engineering polish, not a contribution**, and the evaluation criteria may well reward a
project that ships something more distinctive.

**Counter-argument, and I think it is the stronger one:** the *deliverable* is not the feature,
it is the **evidence**. A small, offline, accessible app that publishes reproducible on-device
measurements, says what it cannot do, and shows its failures is a different *kind* of artifact
from a feature demo. That is a defensible contribution — but it is a contribution about
**discipline**, not about technology, and the project should not pretend otherwise.

**The test that settles it:** run the field evaluation. If, after real users, the suppression
and uncertainty design measurably changes behaviour, the "restraint" thesis is real. If it does
not, the honest conclusion is that this is a well-made regional clone and should be presented as
such.

---

## 2. The most damaging technical finding: is image-relative position useless?

**SEVERITY: high | CONFIDENCE: medium | BLOCKS_MVP: no (but it should block *claims*)**

**The attack:** "left / centre / right **of the image**" is not a spatial fact about the world.
A blind user pointing a camera does not know which way their body is facing relative to the
object. A statement that is true about the image can be useless for acting, and — worse — a
user who finds it useless may nonetheless act on it, because it is the only spatial information
offered.

**The counter-argument:** for a user who holds the phone and knows which way they are facing
in a room, image-relative position is a genuine, actionable cue ("the object is to the side of
the phone"). The literature on spatial reference for blind users covers both egocentric
("to your left") and viewpoint-relative ("at the edge of the frame") reference, and the
viewpoint-relative form is known to help with some tasks.

**What makes this serious is the project's own uncertainty.** The app's *safety* language
carefully says "in the image" every time, which handles the **misrepresentation** risk. It
does **not** handle the **uselessness** risk. The feature could be strictly correct and still
be noise.

**The test that settles it:** a stationary reaching task comparing three phrasings —
image-relative, egocentric, and no position — with the participant locating a named object.
Stationary only; never a walking task. If image-relative does not beat "no position", **delete
the feature** rather than defending it. Tracked as issue #3.

---

## 3. The accessibility paradox: the app may be unusable with a screen reader

**SEVERITY: critical | CONFIDENCE: medium | BLOCKS_MVP: yes | issue #8**

**The attack:** this is the sharpest contradiction in the project. The app speaks through
`TextToSpeech` while the user is probably running TalkBack. `TextToSpeech` defaults to the
**media** stream; TalkBack uses the **accessibility** stream. The OS will not arbitrate
between them, so the likely default is **two voices at once**. For a blind user, that is not
an annoyance — it destroys the audio channel the app exists to use, and it competes with the
screen reader they depend on for everything else.

**Additional sting:** the app's value proposition is that it describes things the user cannot
see. But the user can *already* see everything the screen reader says. The app's marginal
information is the object name and image position; everything else it says is a *restatement*
of what TalkBack is already announcing. A well-designed screen reader user may find the app
mostly redundant and irritating.

**The counter-argument:** the app's information comes from a camera, which the screen reader
cannot see. It is genuinely new information. And a screen-reader user can simply mute the
screen reader's own speech for the app's screen.

**What I still think is true after arguing both sides:** the two-voice collision is a real,
likely, and completely unverified defect. The mitigation (an explicit speech toggle, correct
audio attributes, screen-reader detection) is not implemented. This is the single most likely
reason the app would fail its actual users, and it is a **complete** failure rather than a
subtle one.

**The test that settles it:** a manual TalkBack pass on a real device, with audio recorded, so
the overlap is heard rather than assumed.

---

## 4. The claim-vs-reality gap on "offline"

**SEVERITY: medium | CONFIDENCE: high | BLOCKS_MVP: no**

**The attack:** the project markets "offline-first", and the app has no `INTERNET` permission —
which is a genuinely strong property. But the *website*, the *documentation*, and the *release
process* all sit on the internet, and the model is fetched at build time. More importantly, a
user's **Hindi voice** is a network-downloadable asset on many devices: a user with no Hindi
voice installed may need to download the TTS language pack over Wi-Fi before the "Hindi-first"
experience exists. The app is offline; the *experience* may not be.

**What is fair:** the app's own behaviour is offline. The claim should be scoped to the app, and
it is.

**The test that settles it:** on a device with no Hindi voice data, measure what the user
actually experiences. This is part of issue #1.

---

## 5. The live-demo failure modes

**SEVERITY: medium (as a project risk) | CONFIDENCE: high | BLOCKS_MVP: no**

A hackathon demo is a hostile environment for an app whose headline feature depends on
platform data the demo machine may not have.

| Failure | Likelihood | Impact | Mitigation |
|---|---|---|---|
| The judge's phone/browser has no Hindi voice | **High** | The demo speaks English, undercutting the whole premise | The app says so honestly, but it looks like a failure. **Demo on a prepared device with the voice installed, and pre-record a fallback clip.** |
| Bad venue lighting | **High** | Detection quality drops visibly | Prepare a bright, high-contrast, close-range demo scene. This is legitimate, not cheating — it is the same class of control as a product photograph. |
| Slow first inference (model load) | Medium | A long pause at the start | The model is only 4.4 MB, but warm-up on a cold device is unmeasured. **Measure it.** |
| TTS queue stall or dropped audio | Medium | Speech cuts off mid-sentence | Untested. Needs a device. |
| The judge's phone blocks the camera with a case | Low | The frame is partly obscured | Trivial to avoid. |
| Wi-Fi captive portal / no network | Low | Nothing — the app needs no network. **This is a genuine advantage to demonstrate.** | None needed, and worth turning into a demonstration: run the demo in airplane mode, so "it worked with no connection" is shown rather than claimed. |
| Someone asks "does it detect stairs / is it safe to walk?" | **High** | The demo derails into a safety conversation | Prepare a one-sentence answer: it describes what is in the picture; it is not a navigation aid. |

The last row is the most likely and the most under-prepared. **The demo script should include
the safety framing, not leave it to improvisation.** `docs/DEMO_GUIDE.md` exists; it should be
rehearsed.

---

## 6. The microphone question, if voice commands are added

**SEVERITY: high (for P1) | CONFIDENCE: high | BLOCKS_MVP: no (P0 is unaffected)**

**The attack:** requesting `RECORD_AUDIO` on the phone of a blind user is a disproportionate ask.
That phone is their primary input device and their gateway to their accounts, it is often
unlocked and unattended in their hand, and a microphone permission request for a *convenience
feature* is hard for a user to evaluate. A malicious or merely curious app with microphone
access can record the room — and for a blind user, they may not know it is happening.

**The counter-argument:** voice input is a major accessibility win, and refusing it on privacy
grounds can exclude the users with the most need for hands-free operation.

**My conclusion:** P0's exclusion of the microphone is right, and it is one of the strongest
decisions in the project. If P1 adds voice commands, it must be **opt-in, default off**, and
the app must make clear that it is not recording. This is also why the app currently has *no*
microphone permission: the safest state is the one you do not have to defend.

---

## 7. Ethical review

**SEVERITY: medium | CONFIDENCE: high | BLOCKS_MVP: no (ongoing)**

**The genuine ethical risks for this project:**

1. **False confidence.** A calm, fluent, wrong voice is the most dangerous thing this app can
   produce. The design mitigates it (two thresholds, tentative wording, a permanent safety
   note), but the mitigations are unvalidated.
2. **Over-reliance.** A user who trusts the app's silence may walk into something. The
   "silence is not reassurance" wording helps, but a *warning* in a UI is weaker than a
   *structural* defence. The structural defence is that the app never claims to assess safety
   — which it honours.
3. **Bystander privacy.** The camera is pointed at other people who never consented. Since
   nothing leaves the device, the exposure is bounded — but the *existence* of a camera pointed
   at bystanders in the user's own home is a real consideration, and the system camera indicator
   is the only visible cue.
4. **Training-data extraction.** If a future version collects images for model improvement,
   every frame contains bystanders' faces. The current design has no such path, and any future
   one must be opt-in, consented, and publicly documented. **This should be a written
   prohibition in the repository, not just an absence.**
5. **Testing on the target users.** An accessibility prototype is often tested first on
   sighted colleagues. That is not sufficient, and it is not a substitute — sighted testing
   can validate the *plumbing*, never the *experience*.

---

## 8. What I could not verify

Stated plainly, because a review that implies complete coverage is not credible:

- **Whether any existing product already does exactly this** (Hindi + offline + object
  detection + speech). The competitor landscape was dispatched repeatedly and never produced a
  file. If a 2024–2026 Indian hackathon winner built this, the differentiation is much weaker
  than assumed. **This is the biggest unverified risk in the project.**
- **The licences of the open-source prior art**, which are marked UNKNOWN rather than guessed.
- **Whether the real-world detection accuracy is anywhere near usable.** Four clean test
  images are not an evaluation, and the field protocol has collected zero observations.
- **Everything about on-device performance**, because no device was available.

---

## 9. The strongest surviving claim

After all of the above — **including the second-pass competitor findings in section 10** — here
is what I think the project can honestly claim, and no more:

> **Disha Voice is a small, offline, accessible Android prototype whose contribution is
> restraint and honesty rather than capability: it names what a bundled open-licensed model
> can see, in Hindi when the device allows it, and it publishes exactly what it has and has
> not verified — including the design decisions that may turn out to be wrong.**

That is a modest claim. It is also one the evidence actually supports, which is worth more than
a bold one it does not.

**The second pass made this claim smaller, and made it more true.** Google's Guided Vision
(section 10) is a better product, launched nineteen days ago, that does more of what this
project does. What remains — offline, non-conversational, Hindi-specific, and *measured* — is
a narrower claim, but a claim that survives a check. A project that knows exactly what it is
not good at is easier to trust than one that has not looked.

**And the most valuable possible outcome of the next phase** is a negative result: if the field
evaluation shows that image-relative position does not help, or that the TalkBack collision
cannot be solved, the right response is to publish that and simplify the product. A project
that can report "this turned out not to work, and here is the evidence" has done better
research work than one that quietly ships a feature nobody validated.

---

## 10. Second-pass finding — the competitor arrived the day before (2026-10-20)

**SEVERITY: critical | CONFIDENCE: high | BLOCKS_MVP: no (but it changes what the MVP is for)**

A more thorough competitor search, run after the first version of this review, found
**Google Guided Vision in Gemini Live**, launched **2026-10-01**
(<https://blog.google/innovation-and-ai/products/gemini-app/guided-vision-gemini-live>).

It is a **conversational, real-time visual assistant** for blind and low-vision people. You
share your camera in a Gemini Live session and it describes what it sees, answers follow-up
questions, and gives **proactive spoken reframing cues** — "asking you to pan slowly to the
right, tilt downward, or step back". It was developed with the blind and low-vision
community, trained with **Aira on tens of thousands of hours**, stress-tested by **more than
1,000 Aira Trusted Testers**, and launched on Android 9+ in Gemini's 40+ languages —
**including Hindi, in India**. It can be launched from the **Google TalkBack menu**.

### Why this is the most damaging finding in the review

The first version of this document argued that the project's differentiation was "close to
zero" but that the remaining value was in *restraint and honesty* — being quieter and more
truthful than the competition. That argument is now much weaker, for three reasons.

1. **The competition got better, and got closer, nineteen days ago.** Guided Vision is not
   a dated product with a stale feature set. It is a launch, backed by tens of thousands of
   hours of expert-labelled data and a thousand-person evaluation network, aimed squarely at
   this project's users. Any claim of being "ahead of the competition" is now simply false.
2. **It solved the problem this project was going to solve — differently.** The project's
   section 2 finding was that image-relative position may be useless, and that the fix would
   have to be discovered. Google's answer is to **not describe position at all**, and instead
   tell the user how to move: "pan right", "tilt down", "step back". That is *actionable*
   where "left of the image" was not. The project should treat this as prior art for its own
   open issue #3, and should be honest that its own proposed answer is the weaker one.
3. **It addressed the TalkBack collision by design.** Google ships Guided Vision **inside the
   TalkBack menu** — a feature that coexists with the screen reader rather than talking over
   it. That is a concrete answer to the project's own section 3 finding, the one flagged as
   "the most likely complete failure". Google appears to have solved it, or at least to have
   designed around it, and the project is still carrying the problem as an open issue.

### What the project should actually do about it

**Not:** panic, and abandon the research. The Google product does not settle the question
this project exists to answer. It is *conversational*, by design and by nature — a model
that answers follow-up questions will keep describing. **The fatigue problem is not solved by
Guided Vision; it is very likely made worse by it.** And it needs a network connection, which
is the difference between "works" and "does not work" for a user in a basement, a lift, or on
a metered connection.

**Instead:**

1. **Say so, prominently and without hedging.** The website and the README should name
   Guided Vision, state that it is more capable, and state what this project is for instead.
   A project that pretends Google's newest accessibility launch does not exist is not
   trustworthy on anything else it says.
2. **Reframe the target.** The interesting question is no longer "can a small team build a
   Hindi object detector?" — obviously not, and it should not try. It is: **"does an
   assistive camera assistant that deliberately says less, works with no network, and states
   its own uncertainty, produce a better experience than one that talks continuously?"**
   That question is still open, and the fact that Google chose the opposite trade is
   evidence that the trade is a real design decision with real costs either way.
3. **Adopt what Google did better.** The reframing cue (section 2) and the TalkBack-menu
   integration (section 3) are both directly usable. This is what prior art is for.
4. **Keep the honesty.** The one thing the project can still do that no competitor does is
   publish the evidence — including the evidence that its own features might not help. That
   is now a larger part of the contribution, not a smaller one, because the competitive
   ground has shrunk to almost exactly that.

### One more, smaller finding: VisionAId (arXiv 2607.02371, 2026-07-02)

An offline-first on-device Android assistant for people with visual impairment, with six
ONNX models including calibrated metric depth (below 1 cm error within 3 m), multimodal
output in **Romanian**, and **published measurements** (mAP@50 0.986; depth latency 491 ms
INT8 on a Galaxy S21 Ultra).

This is **confirmed prior art for the technical thesis** that issue #4 had flagged as the
project's largest unverified risk. It is not a Hindi collision, and it is a research system
rather than a product, so it is not a competitive threat. But it is a demonstration that
**calibrated depth on a mid-range phone is achievable** — which means the project's standing
refusal to estimate distance should be restated as *"not yet validated here"* rather than
*"never"*. A permanent ethical position that the research literature has already overtaken
is not restraint; it is a stale assumption.

It is also the model to emulate. VisionAId is what "publishing measurements" looks like
when there are measurements to publish.
