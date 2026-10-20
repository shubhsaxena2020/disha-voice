# Competitor Landscape — Disha Voice

**Date:** 2026-10-20. All URLs accessed 2026-10-20.
**Confidence convention:** every row states what was verified from a primary source and what was
not. **UNVERIFIED** means the claim was not confirmed from an official source in this session.

> **The single most important finding is at the top.** Read it before the table.

---

## 1. The finding that changes the project

**Google Lookout already ships Hindi. It also already does offline detection, object
identification, and — in Find mode — direction and distance guidance.**

From Google's own support documentation
(<https://support.google.com/accessibility/android/answer/9031274>), Lookout's supported
languages include **Hindi, Bengali, Gujarati, Kannada, Marathi, Tamil, Telugu** and 30+
others, and its **Find** mode "can also tell you the direction and distance to the object being
looked for, depending on device capabilities." From the Play listing
(<https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.reveal>):
"Lookout offers seven modes … **Explore**: Identify objects, people, and text in surroundings …
**Find**: Scan surroundings to find objects like doors, bathrooms, cups, vehicles, and more …
**Images**: Capture, describe, and ask questions about an image."

Google also announced in 2022 that "Lookout now also works offline without the need for Wi-Fi
or data service" (<https://blog.google/products-and-platforms/platforms/android/new-android-features-june-2022/>).

### What this does to the project

| Assumption the project started with | Reality |
|---|---|
| "Hindi-first" is a differentiator | **No.** Google already ships Hindi, along with six other Indian languages. |
| "Offline detection" is a differentiator | **No.** Lookout has worked offline since 2022. |
| "Directional information" is a differentiator | **No.** Lookout's Find mode gives direction *and distance* — the very thing this project says it will not do. |
| "Object identification from a camera" is a differentiator | **No.** Lookout's Explore mode does this, and is **still labelled beta and "less accurate than other modes"**. |

**The honest conclusion: the feature-level differentiation of this project is close to zero.**
Google — a company with world-class on-device vision — already covers the feature space, in
Hindi, offline, on the same platform, for free.

### What is left

Three things, and only three:

1. **Restraint and honesty.** Lookout's own community reviews complain that it "is continually
   announcing 'No text in view'" and that announcements "cannot be turned off"
   (<https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.reveal>,
   user reviews, March 2026 and February 2026). The repetition problem this project treats as
   its core hypothesis is a **documented complaint against the market leader**. That is the
   most interesting finding in this entire research effort.
2. **A published measurement trail.** Competitors publish almost no comparable on-device
   numbers. This project's `benchmarks/` harness and honest status dashboard are the artefact.
3. **A specific accessibility interaction** that is demonstrably better — *if* the field
   evaluation shows it is.

**Implication for the project:** the differentiation must be restated from "a Hindi-first
offline object detector" to "**a study, in the open, of what makes an assistive camera
assistant usable rather than annoying**". The product follows from the study, not the other way
round. That is a smaller and more defensible ambition — and it is the honest one.

**This finding also invalidates the framing in `RESEARCH/differentiation-hypothesis.md`**, which
proposed H4 ("Hindi-first, offline, hands-free") as a hypothesis on the grounds that no
competitor offered it. That premise was wrong. H4 is downgraded from a differentiator to a
**table stakes** requirement: the app must have those properties to be usable in India at all,
but having them is not an achievement.

---

## 2. Comparison matrix

Evidence confidence: **high** = verified from an official source in this session;
**medium** = verified from a secondary source; **low / UNVERIFIED** = not confirmed.

| Product | Platform | Languages | Object detection | Offline | Distance / direction | Privacy posture | Evidence |
|---|---|---|---|---|---|---|---|
| **Google Lookout** | Android 6.0+ | 30+ incl. **Hindi, Bengali, Gujarati, Kannada, Marathi, Tamil, Telugu** | Yes — Explore mode; Find mode over 7 object categories | **Yes** (announced 2022) | **Yes** — direction *and distance* in Find mode | Cloud-assisted for Images mode; Find/Explore described as on-device | **high** |
| **Microsoft Seeing AI** | iOS, **Android** (since Dec 2023) | 18 at Android launch, "plan to increase to 36" — **Hindi support UNVERIFIED** | Yes — scene description, image description, document Q&A | UNVERIFIED | UNVERIFIED | UNVERIFIED | **medium** |
| **Be My Eyes / Be My AI** | iOS, Android | Multiple; **Hindi UNVERIFIED** | Be My AI uses a generative model for descriptions | UNVERIFIED | UNVERIFIED | Community/volunteer model — data handling differs materially | **low** |
| **Envision** | iOS, Android | Multiple; **Hindi UNVERIFIED** | Yes — object/scene/text recognition | UNVERIFIED | UNVERIFIED | UNVERIFIED | **low** |
| **Sullivan+** | iOS | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | UNVERIFIED | **low** |
| **Supersense** | iOS, Android | UNVERIFIED | Yes | UNVERIFIED | UNVERIFIED | UNVERIFIED | **low** |
| **Lazarillo** | iOS, Android | UNVERIFIED | Description-focused | UNVERIFIED | UNVERIFIED | UNVERIFIED | **low** |
| **SightBuddy** | Open source (Android) | UNVERIFIED | Yes | UNVERIFIED | UNVERIFIED | Open source — licence UNVERIFIED (issue #4) | **low** |
| **SeeForMe** | Open source (web) | UNVERIFIED | Yes | UNVERIFIED | UNVERIFIED | Open source — licence UNVERIFIED (issue #4) | **low** |
| **VISION (We All Can SEE)** | Open source | UNVERIFIED | Yes | UNVERIFIED | UNVERIFIED | Open source — licence UNVERIFIED (issue #4) | **low** |

**Why the UNVERIFIED cells matter:** the open-source projects are the ones that could be
*prior art* for the specific interaction design, and their licences have not been read. The
commercial products' Hindi support beyond Lookout has not been confirmed from official sources.
Both gaps are open work (issue #4), and neither is papered over here.

---

## 3. What the market leader does that this project deliberately does not

| Lookout | Disha Voice | Why |
|---|---|---|
| Find mode gives **direction and distance** | Gives **image-relative position only** | Distance estimation from a monocular camera is unreliable. Google ships it because they can use a richer signal stack and a broader model; copying the *claim* without that capability would be a safety problem, not a feature. |
| Images mode uses **Gemini**, a cloud model | **No network at all**, no `INTERNET` permission | A cloud description of a user's surroundings is a bystander-privacy event. Disha Voice accepts the capability loss for the privacy and offline guarantees. |
| Seven modes, a broad feature surface | **One** mode: name a common object and where it is in the frame | Breadth is the enemy's strength; a narrow, well-tuned loop is the only thing a small team can make genuinely good. |
| Broad object vocabulary | **COCO-2017, 90 classes** | A closed, documented, testable class set with known limits. |

**This is the real positioning:** not "more features than Lookout" (it would lose), but
"**fewer claims, fully on-device, and honest about uncertainty**".

---

## 4. Evidence from the Lookout user reviews

The Play listing's reviews are the most valuable competitive intelligence in this document,
because they are users describing the failure mode this project believes it can fix.

| Review | Date | Complaint | Relevance |
|---|---|---|---|
| "For the voice announcements, it would appear that Lookout uses Android's **accessibility audio stream** … the only workaround is to enable another accessibility service that *does* reveal the controls for the speech volume, such as TalkBack" | 25 Mar 2026 | The app's speech and the screen reader use the **same stream**, and the volume controls are not exposed | **Directly relevant** to the TalkBack-collision risk (issue #8). It also suggests Lookout's *stream choice* is contested by users, not just its volume. |
| "it is continually announcing 'No text in view' … **You cannot turn these features off** so useless" | 17 Feb 2026 | Repeated negative announcements, and no way to disable them | **This is hypothesis H1, observed in the market leader's reviews.** A competitor's user complaint independently supports the project's central hypothesis. |
| "There has been a very serious bug" | 25 Mar 2026 | — | — |

**The strongest evidence in this entire project** is that a user of the market leader is
complaining about exactly the failure mode the project is designed to avoid. That is worth more
than any feature list.

**The cautionary half:** the same reviews show that a *large, well-funded* product with the
world's best on-device vision still gets these things wrong. It is evidence that the problem is
real, and evidence that solving it is genuinely hard.

---

## 5. Prior-art collision — the honest answer

**Could a Hindi-first, offline, assistive object-detection app already exist?**

- As a **feature of a large product**: **yes** — Google Lookout.
- As a **standalone open-source project**: **not established**. The three open-source projects
  identified (SightBuddy, SeeForMe, VISION) were not confirmed to be Hindi-first, and their
  licences are unverified. **This remains the largest unverified risk** and is issue #4.
- As a **2024–2026 Indian hackathon project**: **not established.** No such project was
  confirmed in this session. The automated research passes that were meant to search for this
  never produced output files, so this negative result is **weak** — it is "not found", not
  "verified absent".

**Honest statement of confidence: LOW.** The search for prior art was not completed. The
project should not claim novelty, and this document does not.

---

## 6. What this changes in the project's plan

1. **Drop "Hindi-first" as a differentiator.** It is table stakes. It is still worth *having* —
   a regional app that ignores Hindi is useless to its users — but it is not an achievement.
2. **Make the competitive question "why is Lookout annoying, and can we show it is better?"**
   That is a researchable question with an available baseline: Lookout is the baseline, and its
   user reviews are the starting complaint.
3. **Do not compete on direction and distance.** Do not ship a distance claim this project
   cannot validate; a wrong distance claim is a safety failure, and Lookout's richer sensor
   stack is not reproducible here.
4. **Position the on-device, no-cloud, narrow-and-honest design as a deliberate trade**, not as
   a deficiency. Some users will *prefer* an app that never uploads their surroundings.
5. **Treat the "no negative announcement" and "no unwanted speech" design decisions as the
   product**, and validate them against Lookout rather than against an abstract standard.

---

## 7. Sources

| # | Source | Type | Accessed | Used for |
|---|---|---|---|---|
| S1 | <https://support.google.com/accessibility/android/answer/9031274> | primary (Google) | 2026-10-20 | Lookout modes, supported languages incl. Hindi, Find mode direction/distance |
| S2 | <https://play.google.com/store/apps/details?id=com.google.android.apps.accessibility.reveal> | primary (Google / Play listing) | 2026-10-20 | Mode descriptions, Android 6.0+, user reviews |
| S3 | <https://blog.google/products-and-platforms/platforms/android/new-android-features-june-2022/> | primary (Google) | 2026-10-20 | Lookout offline capability |
| S4 | <https://blog.google/company-news/outreach-and-initiatives/accessibility/ai-accessibility-update-gaad-2024/> | primary (Google) | 2026-10-20 | Lookout Find mode beta, 7 object categories |
| S5 | <https://blog.google/products-and-platforms/platforms/android/new-android-features-september-2023/> | primary (Google) | 2026-10-20 | Lookout language count growth |
| S6 | <https://www.thurrott.com/mobile/294072/microsoft-launches-an-improved-seeing-ai-app-on-android-too> | secondary | 2026-10-20 | Seeing AI Android availability, 18 languages |

**Not verified in this session** (each is an open gap, not a negative finding): Seeing AI's
current language list and privacy terms; Be My Eyes' and Envision's Hindi support; the
licences, maintenance state, and feature sets of SightBuddy, SeeForMe, and VISION; and any
standalone Hindi-first assistive app or 2024–2026 Indian hackathon project.
