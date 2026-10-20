# Competitor Landscape — Disha Voice

**Date:** 2026-10-20. All URLs accessed 2026-10-20.
**Confidence convention:** every row states what was verified from a primary source and what was
not. **UNVERIFIED** means the claim was not confirmed from an official source in this session.

> **The findings that matter are at the top.** Read them before the table.
>
> **Second pass, 2026-10-20.** The first version of this document was written before a
> more thorough search was run. That search found **Google Guided Vision in Gemini Live** —
> a closer competitor than Lookout — and **VisionAId**, an offline-first Android assistant
> for people with visual impairment. Both are now covered below, and the conclusions have
> been changed accordingly. The first version's claim of "close to zero feature-level
> differentiation" was **too generous**.

---

## 1. Finding A — Google Guided Vision in Gemini Live (launched 2026-10-01)

**This is the closest thing to a direct competitor, and it is nineteen days old.**

From Google's own announcement, *"Guided Vision in Gemini Live: built for accessibility"*
(<https://blog.google/innovation-and-ai/products/gemini-app/guided-vision-gemini-live>,
published **Oct 01, 2026**, by Isha Sheth, Senior PM Gemini Live, and Scott Adams, Senior PM
Google Accessibility Engineering):

> "Built alongside the blind and low-vision community, Guided Vision in Gemini Live offers
> conversational, real-time visual assistance. Share your camera to receive dynamic audio
> descriptions and natural verbal reframing cues to explore your environment."

What it does, in Google's words:

- **Share your camera** during a Gemini Live session to "hear clear descriptions of your
  surroundings, ask detailed follow-up questions, and receive proactive, spoken cues to
  center whatever you want to explore."
- **Reframing cues.** "If your camera is pointed too high, too close, or slightly off to the
  side, Gemini provides natural verbal prompts to help you reframe — asking you to pan
  slowly to the right, tilt downward, or step back so it can get the clear visual context
  needed to answer your questions."
- **Trained with Aira** on "tens of thousands of hours" of visual interpretation, with "more
  than 1,000 members of Aira's Trusted Tester network" stress-testing it, and Aira
  specialists helping "establish and evaluate our safety guardrails."
- **Tested with blind and low-vision communities in India, Brazil, Singapore, Indonesia and
  Japan.**
- **Available on Android 9+** "in regions and languages where Gemini Live is supported."
- **Three launch routes:** the Gemini app (profile settings → "Use Guided Vision in Live"),
  an **Android Accessibility shortcut** (floating accessibility button, two-finger swipe, or
  both volume keys), and — critically — **the Google TalkBack menu**.

### Why this is worse for the project than Lookout

| | Google Lookout | Google Guided Vision |
|---|---|---|
| Interaction | Single mode, no conversation | **Conversational** — describe, then ask follow-ups |
| Output | Discrete announcements | **Dynamic descriptions plus proactive spoken cues** |
| Reframing | Not offered | **"Pan right / tilt down / step back"** |
| Training | — | **Tens of thousands of hours with Aira; 1,000+ Trusted Testers** |
| Language | Hindi supported | Gemini Live is 40+ languages; Gemini supports Hindi and 12 other Indian languages |
| Screen-reader coexistence | Documented complaint (see §4) | **Built into the TalkBack menu** |
| Offline | Yes | **No — requires a network connection** |
| Model | On-device | Cloud (Gemini) |

**An earlier version of this project assumed Lookout was the ceiling. That assumption was
wrong, and the gap to close is much larger than it was a day ago.**

### What it does *not* do — and this is the only remaining ground

1. **It needs the network.** It is a feature of Gemini Live, a cloud conversational
   assistant; the launch guidance for it explicitly lists "an internet connection" as a
   requirement. (High confidence — the architecture is a cloud LLM, and the requirement is
   stated in the launch coverage.)
2. **It is conversational**, so it is verbose by nature. A conversational model asked to
   describe a scene will keep describing. **The problem this project is built around —
   repetitive, exhausting announcements — is therefore not solved by Guided Vision, and may
   be made worse by it.** This is the single most important observation in this document.
3. **It is closed and unmeasurable.** No published latency, no published accuracy, no
   reproducible benchmark, no way to check whether an announcement was correct.

### What Guided Vision proves, and what it costs the project

**It proves the thesis.** A search for "assistive camera apps that talk too much" found the
market leader's own users complaining about it (§4). Now Google's newest, best-funded
accessibility feature has been built as a *conversational* assistant — the same shape that
produces the fatigue. The problem is real enough that Google is attacking it with a
completely different approach, and this project's approach remains worth testing.

**It costs the project the "first mover" and "most capable" claims entirely.** Any
submission, README, or demo that implies the project does something Google's newest
accessibility feature does not will be refuted immediately.

**The honest position is now narrow and specific:** *a small, fully on-device, offline,
non-conversational alternative that says less — with the measurements to show whether
saying less is better.* That is a smaller ambition than the project started with. It is
also the only one left that is both true and defensible.

---

## 2. Finding B — VisionAId: offline-first Android assistant for people with visual impairment

**arXiv:2607.02371**, submitted **2 Jul 2026**, by Cristian-Gabriel Florea and Stelian Spînu,
*"VisionAId: An Offline-First Multimodal Android Assistant for People with Visual Impairment,
Featuring Personalized Object Retrieval"* (<https://arxiv.org/abs/2607.02371>).

From the abstract:

> "We present VisionAId, an Android application that turns a commodity smartphone into a
> real-time visual assistant. The system integrates **six on-device deep learning models**
> (metric monocular depth estimation, instance segmentation, visual and facial embeddings,
> face detection, and a custom banknote detector) running entirely through **ONNX Runtime**,
> with an **optional cloud large language model** (Google Gemini Flash) used only for
> narrative scene description and automatic object labeling."

> "A distinctive contribution is a few-shot pipeline for personal objects: the user
> photographs an object from several angles, and the system later locates that specific
> instance in the environment, guiding the user toward it with **augmented-reality markers,
> spatial audio, and distance-proportional haptics**."

> "All feedback is multimodal (**Romanian speech synthesis**, voice commands, vibration).
> On a reference device (Samsung Galaxy S21 Ultra), INT8 quantization reduces depth latency
> from ~1200 ms to ~491 ms, the custom banknote detector reaches an mAP@50 of 0.986, and
> metric depth is calibrated to below 1 cm of error within 3 m."

### What this means for the project

**The "offline-first on-device Android assistant for people with visual impairment" thesis
is not novel.** That was the largest unverified risk in the project (issue #4), and it is
now **confirmed prior art**.

**But it is not a Hindi collision, and it is not the same design.** The differences are
decisive rather than cosmetic:

| | VisionAId | Disha Voice |
|---|---|---|
| Language | **Romanian** speech synthesis | **Hindi (hi-IN)**, with an English fallback |
| Scope | Depth + instance segmentation + face recognition + **distance-proportional haptics** + AR markers | One thing: name a common object and say where it is in the frame |
| Cloud | Optional Gemini Flash for narrative | **No cloud at all** — no `INTERNET` permission |
| Output | Multimodal: speech + voice commands + haptics + AR | Speech only, plus a visible text mirror |
| Distribution | arXiv preprint, July 2026 | Public repo, published pre-release |
| Claims | Publishes measured numbers (mAP@50 0.986, depth error <1 cm) | Publishes measurements — currently of *the absence* of measurements |

**The honest reading:** VisionAId is a stronger system by almost every technical axis, in a
different country, for a different language, published four months ago. It is not a
competitive threat in the Indian market. It is a **citation** — proof that the technical
approach works, and a template for the kind of measurement this project should eventually
publish.

**It also demonstrates the one thing this project has explicitly refused to do**: calibrated
metric depth, accurate to under 1 cm within 3 m, on a mid-range phone. The project has
written "we do not estimate distance" as a safety principle. VisionAId shows that distance
*can* be done well. That does not make the project's refusal wrong — a research prototype
that ships an unvalidated distance claim would be — but it does mean the refusal should be
stated as a **present limitation**, not as a permanent ethical position. The project
currently says it will not do distance **at all**; that framing is now questionable.

---

## 3. Finding C — Andha-Dhun: Hindi audio description is its own research area

**arXiv:2607.06457**, submitted **7 Jul 2026**, *"Andha-Dhun: A First Look at Audio
Descriptions in Hindi"* (<https://arxiv.org/abs/2607.06457>).

> "Audio Descriptions (ADs) narrate visual content for Blind and Low Vision (BLV) audiences
> during gaps in audiovisual media. […] there is no work that generates ADs for any Indian
> language. […] We introduce **Andha-Dhun, the first dataset of human-authored Hindi ADs**
> collected from 8 full-length movies. […] we find that **naive translation introduces
> artifacts and narrows diversity** compared to original Hindi ADs. Direct machine
> translation fails to adapt cultural references, while human-translated ADs do better but
> still fall short. Our findings emphasize that the purpose of Hindi ADs is accessibility
> for Indian BLV audiences, and that this **requires adapting content for the audience more
> than strict fidelity to the source**."

**Why this is cited.** It independently supports a design decision the project already made:
the Hindi phrases in `Phrasebook.kt` are **written in Hindi, not translated from English**,
because machine-translated phrasing is measurably worse and less diverse than
purpose-written Hindi. That decision was made on intuition; this paper is the citation for
it.

It also establishes that **Hindi assistive description is an acknowledged research area with
prior art** — further evidence against any novelty claim in the language dimension.

---

## 4. Google Lookout — still relevant, now the second-place competitor

**Google Lookout** already ships Hindi, already works offline, and its Find mode already
gives direction *and distance* — verified from Google's own support documentation and Play
listing (see `RESEARCH/source-register.md` S1-S5). Its supported languages include **Hindi,
Bengali, Gujarati, Kannada, Marathi, Tamil, Telugu** and 30+ others, and its **Find** mode
"can also tell you the direction and distance to the object being looked for, depending on
device capabilities."

Lookout's **Explore** mode is still labelled beta and "less accurate than other modes".

### The market leader's own users describe the project's exact thesis

The Lookout Play listing's reviews are the most valuable competitive intelligence in this
document, because they are users describing the failure mode this project believes it can
fix:

| Review | Date | Complaint | Relevance |
|---|---|---|---|
| "For the voice announcements, it would appear that Lookout uses Android's **accessibility audio stream** … the only workaround is to enable another accessibility service that *does* reveal the controls for the speech volume, such as TalkBack" | 25 Mar 2026 | The app's speech and the screen reader use the **same stream**, and the volume controls are not exposed | **Directly relevant** to the TalkBack-collision risk (issue #8). Note that Google's *newer* product, Guided Vision, is integrated into the TalkBack menu — Google appears to have learned this lesson. |
| "it is continually announcing 'No text in view' … **You cannot turn these features off** so useless" | 17 Feb 2026 | Repeated negative announcements, and no way to disable them | **This is hypothesis H1, observed in the market leader's reviews.** A competitor's user complaint independently supports the project's central hypothesis. |
| "There has been a very serious bug" | 25 Mar 2026 | — | — |

**The strongest evidence in this entire project** is that a user of the market leader is
complaining about exactly the failure mode the project is designed to avoid. That is worth
more than any feature list.

**The cautionary half:** the same reviews show that a *large, well-funded* product with the
world's best on-device vision still gets these things wrong. It is evidence that the problem
is real, and evidence that solving it is genuinely hard.

---

## 5. Comparison matrix

Evidence confidence: **high** = verified from an official source in this session;
**medium** = verified from secondary reporting; **low / UNVERIFIED** = not confirmed.

| Product | Platform | Languages | Object / scene description | Offline | Distance / direction | Screen-reader integration | Privacy posture | Evidence |
|---|---|---|---|---|---|---|---|---|
| **Google Guided Vision in Gemini Live** | Android 9+ | Gemini Live 40+; Gemini supports Hindi + 12 other Indian languages | **Yes — conversational, with follow-up questions** | **No — network required** | Reframing cues ("pan right, tilt down, step back") | **Built into the TalkBack menu**; also an Accessibility shortcut | Cloud (Gemini); Aira used for training | **high** |
| **Google Lookout** | Android 6.0+ | 30+ incl. **Hindi, Bengali, Gujarati, Kannada, Marathi, Tamil, Telugu** | Yes — Explore mode; Find over 7 object categories | **Yes** (since 2022) | **Yes** — direction *and distance* | Not documented; users report an audio-stream clash | Cloud-assisted for Images mode | **high** |
| **VisionAId** (arXiv 2607.02371) | Android | **Romanian** speech | Yes — six on-device ONNX models; depth, instance seg, faces | **Yes (offline-first)**, optional cloud LLM | **Yes** — metric depth <1 cm error within 3 m; distance-proportional haptics | Voice commands | Optional cloud LLM for narrative | **high** |
| **Microsoft Seeing AI** | iOS, **Android** (since Dec 2023) | 18 at Android launch, "plan to increase to 36" — **Hindi UNVERIFIED** | Yes — scene description, image description, document Q&A | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | **medium** |
| **Be My Eyes / Be My AI** | iOS, Android | Multiple; **Hindi UNVERIFIED** | Be My AI uses a generative model for descriptions | UNVERIFIED | UNVERIFIED | UNVERIFIED | Community model — data handling differs materially | **low** |
| **Envision** | iOS, Android | Multiple; **Hindi UNVERIFIED** | Yes — object/scene/text recognition | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | **low** |
| **Sullivan+** | iOS | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | **low** |
| **Supersense** | iOS, Android | UNVERIFIED | Yes | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | **low** |
| **Lazarillo** | iOS, Android | UNVERIFIED | Description-focused | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | **low** |
| **SightBuddy** | Open source (Android) | UNVERIFIED | Yes | UNVERIFIED | UNVERIFIED | UNVERIFIED | Open source — licence UNVERIFIED (issue #4) | **low** |
| **SeeForMe** | Open source (web) | UNVERIFIED | Yes | UNVERIFIED | UNVERIFIED | UNVERIFIED | Open source — licence UNVERIFIED (issue #4) | **low** |
| **VISION (We All Can SEE)** | Open source | UNVERIFIED | Yes | UNVERIFIED | UNVERIFIED | UNVERIFIED | Open source — licence UNVERIFIED (issue #4) | **low** |
| **Andha-Dhun** (arXiv 2607.06457) | Research | **Hindi** AD dataset | Research, not a product | n/a | n/a | n/a | n/a | **high** |
| **Disha Voice (this project)** | Android 7.0+ (API 24) | Devanagari (hi-IN) when the device has a Hindi voice; English fallback | COCO-2017 90 classes via bundled EfficientDet-Lite0 | **Yes — no INTERNET permission** | Image-relative position only; **no distance** | Semantics and live region written; **no manual TalkBack review** | CAMERA only; no analytics; no image persistence | **high** (self-reported; device untested) |

**Why the UNVERIFIED cells matter:** the open-source projects are the ones that could be
prior art for the specific interaction design, and their licences have not been read. The
commercial products' Hindi support beyond Gemini and Lookout has not been confirmed. Neither
gap is papered over here.

---

## 6. What is left to differentiate on — the honest, narrow list

| # | Claim | Why it survives | Evidence it needs |
|---|---|---|---|
| 1 | **Fully on-device, no network** | Guided Vision needs a connection; Lookout's Images mode uses the cloud. For a user in a basement, a lift, or on a data plan, offline is the difference between the app working and not. | A device test with the network off, and a published measurement of what works without it |
| 2 | **Says less** | Every competitor is conversational or repetitive, and the market leader's users complain about it. | The field evaluation, comparing suppression on vs. off (H1) |
| 3 | **Does not estimate distance** | A wrong distance claim is a safety failure. VisionAId shows depth is possible, but it did so with a published calibration this project has not done. | Either a validated depth model, or a permanent, clearly-scoped refusal |
| 4 | **Publishes what it has and has not verified** | No competitor publishes reproducible on-device numbers. VisionAId published *its* numbers in a paper — which is a citation, not a competitor. | Continuity: keep the benchmark harness and the honest status dashboard current |
| 5 | **Purpose-written Hindi, not translated** | Andha-Dhun (arXiv 2607.06457) shows naive translation of assistive descriptions is measurably worse. | The phrasebook audit; native-speaker review (not yet done) |

**What is no longer claimable:** being first, being Hindi-first as a differentiator, being
offline as a differentiator, being conversational, being more capable, or being more
accessible than the competition.

---

## 7. Prior-art collision — the honest answer, now with a confirmed hit

**Could a Hindi-first, offline, assistive object-detection app already exist?**

- As a **feature of a large product**: **YES, and it is worse than the first version of this
  document realised.** **Guided Vision in Gemini Live** (Google, 2026-10-01) is
  conversational camera description for blind and low-vision users, in Gemini's 40+
  languages including Hindi, in India, on Android, integrated with TalkBack.
- As a **standalone research system**: **YES.** **VisionAId** (arXiv 2607.02371) is an
  offline-first on-device Android assistant for people with visual impairment — the
  technical thesis is confirmed prior art, in Romanian.
- As a **standalone open-source project**: **not established.** SightBuddy, SeeForMe and
  VISION were not confirmed to be Hindi-first, and their licences are unverified (issue #4).
- As a **Hindi-specific offline product**: **not established.** Andha-Dhun is the closest
  Hindi-specific work found, and it is a dataset and study of audio descriptions, not an app.
- As a **2024–2026 Indian hackathon project**: **not established.** No such project was
  confirmed in this session.

**Honest statement of confidence: the "no prior art" position is dead.** Two confirmed
collisions exist, one of them from Google and nineteen days old. What remains unclaimed is
narrow: an **offline, non-conversational, Hindi-first** assistive camera app that publishes
its measurements. That is a real gap, but it is a small one, and the project should say so.

---

## 8. What this changes in the project's plan

1. **Rewrite the positioning again, and this time name the competitor.** The site must say
   that Google shipped Guided Vision in Gemini Live on 1 October 2026, that it is more
   capable, and that this project is narrower on purpose.
2. **Stop describing "Hindi-first" and "offline" as differentiators.** They are table
   stakes against Guided Vision (which is neither offline nor Hindi-specific in its
   framing) and against Lookout (which is both).
3. **Adopt Google's reframing cue as prior art for issue #3.** "Pan slowly to the right,
   tilt downward, or step back" is *actionable* in a way that "left of the image" is not.
   The project's standing decision to delete image-relative position if it does not help
   should be widened: consider adopting reframing cues instead.
4. **Adopt Google's TalkBack-menu integration as prior art for issue #8.** A feature
   launched *inside* the TalkBack menu is a design that coexists with the screen reader
   rather than talking over it. That is a concrete answer to the audio collision.
5. **Re-examine the permanent refusal to estimate distance.** VisionAId's calibrated depth
   shows the capability exists in a research context. The project's refusal should be
   framed as "not yet validated here", not "never".
6. **Cite Andha-Dhun** for the decision to write original Hindi rather than translate.
7. **Treat VisionAId as the model to emulate**, not a competitor: it is what "publishing
   measurements" looks like when there are measurements to publish.

---

## 9. Third-pass findings — what Indian users actually report, and what they actually ask for

Two peer-reviewed studies were retrieved and read in a later pass. Neither is a
competitor product, and neither changes the technical position. Both change the
**framing**, and one of them bears directly on the project's own open question.

### 9.1 The OR apps in India were designed for a different social context (S32)

A seven-day diary study followed by one-on-one interviews with **seven object-recognition
app users in India** (CHI 2025) found that previous research had examined the *functional*
accessibility of OR apps and given almost no attention to their **social** accessibility.
The paper's finding is that a camera-based assistive technology "designed for independent,
western contexts" does not sit well in a multicultural, interdependent Indian society,
where people navigate public and private spheres *with* others. It proposes designing for
interdependence, community-centered practice, cultural alignment, and local adaptability.

**What this does to the project.** It is the strongest published support for the project's
differentiator, and it is support for a reason the project had not articulated: the gap
is not "Hindi", it is **context**. Two things follow.

First, the claim "Hindi-first" is not sufficient on its own, and the project should not
lean on it as though it were. A Hindi-language app designed on Western assumptions is
still designed on Western assumptions.

Second, and less comfortably: the paper's subject is OR app **users** — people who already
use these apps and describe the social friction of doing so. That is evidence about
adoption, and the project's own target users are people who do *not* use such apps yet.
It sharpens the field evaluation protocol (`TESTING/field-evaluation-protocol.md`): the
stationary, do-no-harm protocol should ask about the social context, not only whether the
object was named correctly.

### 9.2 What blind users say they want described — and how position factors in (S33)

**"Beyond the Cane"** (ACM TOAC, 2022) ran two urban walking experiments with **13
cane-using participants**, five of them early blind, to work out which objects in a street
scene are worth describing and how they should be described. Two findings are directly
relevant to issue #3, which asks whether the project's image-relative left/centre/right is
actually useful.

- **Late-blind participants requested information more frequently and prioritised
  information about objects' locations.** Relative position was one of the factors that
  affected how useful a description was.
- **The detail level, the relative position and the type of information** all changed how
  useful participants found a description to be — and participants frequently could not
  explain how they had used the information, even when they had used it.

**What this does to the project.** It is published evidence that **relative position
matters** to blind users, which is the strongest available support for the project's
"left / centre / right" design and for the decision not to guess at distance. It also
warns against reading too much into it: the study found early- and late-blind
participants wanted different things, and users often could not articulate the benefit
even when it was there. That is an argument for the project's own confidence-aware
phrasing, and for issue #2's threshold calibration, rather than for confident claims
about the feature's value. Issue #3 should cite this paper.

It is also a caution about the *other* obvious response. "Beyond the Cane" studied
**urban walking for mobility**; the project is explicitly not a mobility aid, and the
paper's participants were actively navigating streets. Its findings transfer to the
project's object-naming case only partially, and the project should say so rather than
borrow the authority wholesale.

---

## 10. Sources

| # | Source | Type | Accessed | Used for |
|---|---|---|---|---|
| S1 | <https://blog.google/innovation-and-ai/products/gemini-app/guided-vision-gemini-live> | primary (Google) | 2026-10-20 | Guided Vision: launch date, camera sharing, reframing cues, Aira training, TalkBack-menu integration, Android 9+ |
| S2 | <https://support.google.com/accessibility/android/answer/9031274> | primary (Google) | 2026-10-20 | Lookout modes, supported languages incl. Hindi, Find mode direction/distance |
| S3 | <https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.reveal> | primary (Google / Play) | 2026-10-20 | Lookout mode descriptions, Android 6.0+, user reviews |
| S4 | <https://blog.google/products-and-platforms/platforms/android/new-android-features-june-2022/> | primary (Google) | 2026-10-20 | Lookout offline capability |
| S5 | <https://blog.google/company-news/outreach-and-initiatives/accessibility/ai-accessibility-update-gaad-2024/> | primary (Google) | 2026-10-20 | Lookout Find mode beta, 7 object categories |
| S6 | <https://arxiv.org/abs/2607.02371> | primary (arXiv) | 2026-10-20 | VisionAId: offline-first, six on-device ONNX models, Romanian TTS, depth/haptics, measured results |
| S7 | <https://arxiv.org/abs/2607.06457> | primary (arXiv) | 2026-10-20 | Andha-Dhun: first Hindi audio-description dataset; translation artifacts and diversity loss |
| S8 | <https://support.google.com/gemini/answer/13575153> | primary (Google) | 2026-10-20 | Gemini supported languages (incl. Hindi, Bengali, Gujarati, Kannada, Malayalam, Marathi, Odia, Punjabi, Tamil, Telugu, Urdu) and countries |
| S9 | <https://blog.google/products-and-platforms/products/gemini/gemini-live-extensions-language-expansion/> | primary (Google) | 2026-10-20 | Gemini Live expanded to 40+ languages |
| S10 | <https://blog.google/products-and-platforms/platforms/android/new-android-features-september-2023/> | primary (Google) | 2026-10-20 | Lookout language count growth |
| S11 | <https://www.thurrott.com/mobile/294072/microsoft-launches-an-improved-seeing-ai-app-on-android-too> | secondary | 2026-10-20 | Seeing AI Android availability, 18 languages |
| S12 | <https://gcn.com/google-gemini-live-guided-vision-android/22187> | secondary | 2026-10-20 | Guided Vision launch routes and availability (corroborates S1) |
| S13 | <https://timesofindia.indiatimes.com/technology/tech-news/google-launches-guided-vision-for-gemini-live-what-is-it-and-how-it-works/amp_articleshow/134639412.cms> | secondary | 2026-10-20 | Guided Vision feedback from BLV communities incl. India (corroborates S1) |
| S14 | <https://dl.acm.org/doi/10.1145/3706598.3713107> | primary (ACM CHI) | 2026-10-21 | Seven-day diary study and interviews with seven OR-app users in India; social accessibility, interdependence, Western design context (section 9.1) |
| S15 | <https://pubmed.ncbi.nlm.nih.gov/36148267/> | primary (ACM TOAC) | 2026-10-21 | 13 cane-using participants, urban walking; late-blind participants prioritised object locations; relative position affects usefulness (section 9.2) |

**Not verified in this session** (open gaps, not negative findings): Guided Vision's
availability *specifically for Hindi camera guidance* (secondary reporting says Indic
languages "may lag English"; Google's own page says "regions and languages where Gemini Live
is supported" without naming which); Guided Vision's data-retention terms; whether
Guided Vision can be run without a network; Seeing AI's current language list and privacy
terms; Be My Eyes' and Envision's Hindi support; the licences, maintenance state and
feature sets of SightBuddy, SeeForMe and VISION; and any 2024–2026 Indian hackathon project
with a comparable design; the licences of the 2024-2026 Indian hackathon projects found
by the third pass (the Fosshack 2026 project list could not be retrieved - the host
resolved to a private address, and the web-search backend was failing in this session, so
a project named "DrishtiMap" - real-time audio navigation for visually impaired Indians,
on-device object detection with spoken descriptions in Bengali and Hindi - is recorded as
an **unverified lead, not a fact**, and must not be cited until its own source is read).
