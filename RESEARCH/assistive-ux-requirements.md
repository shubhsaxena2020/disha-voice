# Assistive UX Requirements — Disha Voice

**Date:** 2026-10-20
**Scope:** the P0 Android prototype, whose entire purpose is to be usable by a blind or
low-vision person. This is a requirements document: nothing here has been validated with users,
because no participant study has been run. Statements marked **EVIDENCE** cite a source; the
rest are design decisions.

---

## 1. Who this is for, and what that changes

The primary user is a person who cannot see the camera's field of view, may be navigating
indoors, may be in poor light, may be holding something else, and is using a screen reader or
voice control as their primary interface.

Four consequences that a sighted-person design would get wrong:

1. **The screen is not the interface.** For many users, the app's value is entirely in the
   speech. The visual layer is a fallback, not the product.
2. **The device may be held in one hand, at an angle, while moving.** Touch targets must be
   large; the primary control must be reachable without precise aim.
3. **Silence is information.** A pause, a missing announcement, or an unchanged screen can
   mean "nothing happened" or "the app is broken". The app must have an explicit, discoverable
   way to ask what it last said.
4. **The user cannot verify the world.** If the app says "there is a cup in the middle of the
   image", the user cannot check. If it says nothing, the user cannot know whether nothing is
   there or the model failed. This is the deepest problem in the design and is treated as a
   safety issue, not a UX nicety.

---

## 2. The repetition problem — the central research concern

### Why repeated speech is a real harm, not an annoyance

A continuously running detector reports the same object across many consecutive frames. Speaking
each one produces a near-continuous stream of short utterances.

**EVIDENCE** — the mechanisms at work are well established:

- **Alert fatigue** is a documented failure mode of alarm systems: as the number of alerts
  rises, the probability that a user notices and responds to a *true* alert falls, and
  false-alarm rates rise. This is the standard framing in the clinical alarm literature.
- **Auditory masking and the "cocktail party" effect**: a competing speech stream degrades the
  intelligibility of a target speech stream, and a screen-reader user's auditory environment
  is already carrying the screen reader's own speech.
- **Signal-detection framing**: a stream of mostly-redundant alerts trains a user to treat
  alerts as noise, which is precisely the failure that matters when an alert is real.
- **WCAG 2.2 SC 1.4.2 (Audio Control)** and the general principle of giving the user control
  over automatically-playing audio exist because uncontrollable audio is an accessibility
  barrier: <https://www.w3.org/TR/WCAG22/>

**Consequence for the design:** suppression is not a performance optimisation, it is the
product's core hypothesis. The domain policy suppresses a repeat of the same
`(label, image-zone)` pair for 4 seconds, and a tentative detection is spoken with different
wording from a confident one.

### The counter-risk

Suppression can also *remove* information the user needed — specifically, a user who is
deliberately looking for one specific object needs it re-announced when the camera moves, and a
time-based cooldown does not know about camera movement. This is why the cooldown is a
**configurable** value and why the "repeat last message" affordance exists.

---

## 3. Speech output design

### Principles

| Principle | Rule in the implementation |
|---|---|
| Say what is known, not what is hoped | Never assert safety, clearance, or distance |
| Distinguish certainty in the words, not in the tone | "दिख रहा है" (is visible) vs "लगता है" (it looks like) — a lexical difference that survives a fast listen |
| Make the negative state about the model, not the world | "मुझे … नहीं दिखी" (I did not see), never "कुछ नहीं है" (there is nothing) |
| Always qualify position as image-relative | "तस्वीर के बाईं ओर" (on the left **of the image**) |
| Prefer the everyday word | The user must act on the word, not study it |
| Keep utterances short | One clause; a short utterance is more intelligible and easier to interrupt |
| Every spoken message is also on screen | The user who mutes the app's voice still gets the information |

### The phrasebook

Implemented in `Phrasebook.kt`; the full table with the wording rationale is in
`RESEARCH/speech-hindi-audit.md`.

### The negative state deserves special care

"I did not recognise anything in this image" is factually accurate and still potentially
misheard as reassurance. Two mitigations are in place:

1. The wording is explicitly about the model's perception, not the room.
2. The app never follows a negative announcement with anything resembling "so the path is
   clear".

An **open** question, tracked as issue #7: whether the negative state should be spoken at all,
given that repeated "nothing found" announcements are exactly the fatigue the design is trying
to avoid. It may be better to show it on screen and stay silent.

---

## 4. Non-visual feedback and the screen reader

### The collision problem

A TalkBack user is already listening to the screen reader. The app also speaks. Two speech
streams at once is a serious defect for exactly the audience this project exists to serve.

### Current design decisions

- `QUEUE_FLUSH` for state changes, so a stale announcement never plays after the state changed.
- Every spoken message is **also** rendered as text in a polite live region, so the information
  is never speech-only.
- The app's own control (start/stop) is a single large button, so a screen-reader user has one
  obvious action rather than a mode to understand.

### What is still needed (issue #8)

- Audio-focus handling so the app's speech and the screen reader do not talk over each other.
- A settings toggle to disable in-app speech entirely.
- Detection of an active screen reader, and a decision on default behaviour.
- A manual TalkBack pass on a real device.

---

## 5. Accessibility acceptance criteria

Testable checklist for the app. Items marked ☐ are **not yet done** — the app has not been
run with a screen reader.

### Structure and semantics

- ☐ Every interactive element has a meaningful `contentDescription` or text label.
- ☐ The primary control exposes its state (`stateDescription`: "Running" / "Stopped").
- ☐ The latest message is in a polite live region so it is announced when it changes.
- ☐ The camera preview is labelled as decorative, because the information is delivered by voice.
- ☐ Focus order follows the visual order: title → status → primary control → secondary controls.
- ☐ Decorative icons are `aria-hidden` and not focusable.

### Touch and pointer

- ☑ The primary start/stop control is 160 dp — far above the 48 dp minimum.
- ☑ The secondary controls are 64 dp tall.
- ☐ Verified on a real screen at the user's font scale.

### Visual

- ☑ High-contrast text: the body text is 13.94:1, the primary accent 5.82:1, and the button
  label 6.43:1 — all pass WCAG AA.
- ☐ Dynamic type honoured end to end at the largest system font scale.
- ☐ Dark theme provided and checked for contrast.
- ☑ No meaning conveyed by colour alone: state uses an icon, a text label, and a spoken phrase.
- ☐ Reduced-motion respected in the app UI (the website already honours it).

### Speech and timing

- ☐ Speech can be interrupted immediately by pressing stop.
- ☐ No announcement outlives the state that produced it.
- ☐ The app does not speak while the user is interacting with a screen-reader control.
- ☐ "Repeat last message" retrieves the last phrase verbatim.

### Failure states

- ☐ Camera permission denied → a clear spoken and visible explanation, and a way to retry.
- ☐ Permission revoked while running → the session stops and says so.
- ☐ No camera hardware → a clear message rather than a crash.
- ☐ Model fails to load → a clear message rather than silence.
- ☐ No Hindi voice → says so, in English, and continues in English.
- ☐ The app is left running with the screen off → no unbounded resource use.

---

## 6. User-testing ethics

**EVIDENCE** — the principles below follow standard research-ethics practice and the
accessibility-specific guidance referenced in `TESTING/field-evaluation-protocol.md`.

### Non-negotiable rules

1. **Never ask a participant to walk, cross a road, use stairs, or rely on prototype output.**
   The prototype is unverified and may be wrong. Asking someone to navigate on the strength of
   an unvalidated detector is a foreseeable harm.
2. **Informed consent** that plainly states what is being tested, that the prototype is
   experimental, and that the participant may stop at any moment without giving a reason.
3. **The test must itself be accessible**: materials in a readable format, a verbal
   description of anything visual, no reliance on a GUI the participant cannot see, and
   reasonable time.
4. **Do not collect bystander imagery.** Camera frames stay on the device; the study records
   annotated observations, not photographs of participants or their homes.
5. **Anonymise** any recorded material, and delete raw recordings after transcription, with the
   participant told this in advance.
6. **Compensation** should be offered, and should not create pressure to over-report success.
7. **Anonymity** in anything published, including the number of participants if it is small.

### How the field protocol reflects this

The protocol in `TESTING/field-evaluation-protocol.md` is a **stationary** protocol: the
participant and the device stay in one place, objects are placed in a controlled arrangement,
and the participant is never asked to move through space on the strength of the app's output.
Stop rules and a "do no harm" override are specified.

---

## 7. Candidate differentiators, with falsifiable claims

These are the hypotheses in `RESEARCH/differentiation-hypothesis.md`, restated as UX
requirements with the test that would settle each.

| # | Differentiator | Falsifiable claim | Test |
|---|---|---|---|
| H1 | Lower repetitive-alert burden | Suppression increases how long a user keeps the app running, without reducing task success | Within-subject, suppression on vs. off, stationary, ≥8 participants pilot |
| H2 | Honest uncertainty | A "looks like" phrasing prevents a confident error from being acted on | Controlled scene grid, 30 trials, measure the user's action and stated confidence |
| H3 | Image-relative position | Users can act on "left/centre/right of the image" to locate an object | Stationary reaching task vs. egocentric wording vs. no position. **May fail** |
| H4 | Hindi-first, offline | Hindi + offline is preferred and more successful for the target segment | Within-subject language preference, offline success rate |
| H5 | Transparent local evaluation | Published measurements are rare enough to be a contribution | Presence and reproducibility of `benchmarks/` |

---

## 8. What would make this product fail for its users

Listed deliberately, because a requirements document that only lists successes is not useful:

- The phone has **no Hindi voice**, making the "Hindi-first" claim hollow on that device.
- The app talks **over TalkBack**, making it unusable with a screen reader.
- The detector is **wrong often enough to be distrusted**, so the user stops paying attention.
- The app is **too chatty**, so the user turns it off.
- The app is **too quiet**, so the user believes there is nothing around them.
- The camera **drains the battery** within minutes, so the app cannot be used continuously.
- The user cannot tell **whether the app is running**, so they cannot trust it.
