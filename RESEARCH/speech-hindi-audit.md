# Hindi Speech and TTS Audit — Disha Voice

**Date:** 2026-10-20
**Status:** design-level audit. **No Hindi voice has been heard on a real handset yet** — no device
was available. Every claim about runtime behaviour is a requirement of the implementation, not a
verified observation.

---

## 1. Android TextToSpeech with `hi-IN`

### What the platform actually provides

`TextToSpeech` is a thin client over the device's installed TTS engine. The engine is a
separate app (commonly `com.google.android.tts` on stock Android and on most OEM skins), and
the **Hindi voice is data installed by that engine**, not something the calling app ships.

Consequences that matter for this project:

| Fact | Consequence for Disha Voice |
|---|---|
| The engine may not be installed | `TextToSpeech` initialisation fails → the app must speak nothing, and must say so |
| The engine may be installed **without** Hindi voice data | `isLanguageAvailable(hi-IN)` returns `LANG_MISSING_DATA` or `LANG_NOT_SUPPORTED` → English fallback |
| Some OEM engines ship a reduced language set | The "Hindi-first" promise is a **platform dependency**, not something we control |
| The engine can be updated, removed, or disabled by the user | The app must check at runtime, not assume |
| Voice quality varies by engine and by installed data | A voice that exists may still sound poor; a "Hindi voice present" check is not a "Hindi voice is good" check |

### The availability check that must be used

`TextToSpeech.isLanguageAvailable(Locale)` returns one of:

| Constant | Meaning | Correct behaviour |
|---|---|---|
| `LANG_AVAILABLE` | Fully available | Use it |
| `LANG_COUNTRY_AVAILABLE` | Available, but the country variant is not | Use it |
| `LANG_COUNTRY_VAR_AVAILABLE` | Available, with a country/variant difference | Use it |
| `LANG_MISSING_DATA` | The language is known but its voice data is not installed | Fall back to English; tell the user |
| `LANG_NOT_SUPPORTED` | The language is not supported at all | Fall back to English; tell the user |
| `LANG_MISSING_DATA` / `-1` on older platforms | Unavailable | Fall back |

`setLanguage()` returns the same family of codes, and it can fail **after** a successful
`isLanguageAvailable()` check. So the implementation must check **both**: the return value of
`isLanguageAvailable`, **and** the return value of `setLanguage`. `Speaker.kt` does this — it
treats a `setLanguage` failure as "Hindi unavailable" and falls back to `Locale.US`.

### The honesty requirement

The app must **never** imply a Hindi voice exists when it does not. Concretely:

- If Hindi is unavailable, the app speaks the **English** phrase *"A Hindi voice is not available
  on this phone, so I will speak English."* — and it says this **in English**, because it
  cannot speak Hindi. It must not attempt to pronounce a Devanagari sentence with an English
  voice.
- The user-facing UI shows the same fact in text, so a screen-reader user is not told one thing
  and hears another.
- This is a first-class state, not an error: the app is still fully functional in English.

---

## 2. Devanagari pronunciation and normalisation

### Why naive text fails

Hindi TTS engines vary widely in how well they handle Devanagari, but some failures are
universal:

| Issue | Example | What goes wrong |
|---|---|---|
| **Schwa deletion** | `कमल` (lotus) is pronounced *kamal*, not *kamala* | The written schwa is silent; a naive transliteration reads it as a full vowel |
| **Implicit schwa after a conjunct** | `हिन्दी` (*hindi*) vs. a naive *hindee* | A cluster's inherent vowel is dropped, but a spelling-to-sound engine may add it |
| **Matras change the base vowel** | `क` + `ि` → `कि` (*ki*) vs. `क` (*ka*) | Vowel signs replace the inherent vowel entirely |
| **Nasalisation** | `हाँ` (*haan*) vs. `हां` | `ँ` (candrabindu) and `ं` (anusvara) are different phonemes |
| **Aspiration** | `क` (*ka*) vs. `ख` (*kha*) vs. `ग` (*ga*) | The aspirated series is a separate set of letters |
| **Loanwords** | `स्कूल` (*school*) | Perso-Arabic loanwords keep English pronunciation while being written in Devanagari |
| **Word boundaries** | `नहीं` (*nahin*) | Written with a virama; a naive reader may say *n-h-in* |

### What the app should do

**Do not hand-roll a transliteration layer.** The system TTS engine has its own Devanagari
grapheme-to-phoneme rules, and a hand-rolled layer would fight it and produce worse results
for most inputs.

The app's own responsibilities are narrow and achievable:

1. **Use native Devanagari text**, not a Latin transliteration, as the input to the engine.
2. **Keep sentences short.** The phrasebook's longest entries are one clause. Short utterances
   reduce the chance of a prosody error and make interruption easier.
3. **Avoid rare conjuncts and Sanskritised vocabulary** where a simpler word exists.
4. **Prefer the everyday word** over the technically precise one — `कप` (cup) over a formal
   term, because the user has to *act* on the word, not study it.
5. **Avoid abbreviations and numerals** where a word is clearer (`दस` rather than `10` in a
   sentence that will be heard once).

### Reference points for normalisation

- **ISO 15919** — the scholarly transliteration standard for Devanagari. Useful as a reference
  for what the *actual* pronunciation is, and for writing a testable description of a phrase's
  intended sound. It is **not** something to feed to the TTS engine.
- **CLRS** (Computational Linguistics Research Society, Oxford) — a long-standing
  romanisation scheme for South Asian languages with careful attention to the schwa and to
  inherent-vowel behaviour.
- **Unihan / kCanDDI** (`kCanDDI` — the *Kanji and Kanwa/Chinese Character Database for
  Ideographic Description*) — a per-character database of readings and linguistic properties,
  including a "Mandarin/Kannada-style" decomposition of Devanagari syllable structure. It is
  the most rigorous machine-readable source for the *structure* of a Devanagari syllable.

**Recommendation:** no normalisation is applied at runtime in P0. The phrasebook uses only
common, short, correctly-spelled words, and correctness is verified by **native-speaker
review** (issue #7) rather than by an algorithm. A normalisation layer is a P1 item if review
shows the engine's output is unreliable for a specific phrase.

---

## 3. Speech recognition (P1, deliberately not in P0)

### Why continuous speech recognition is not in P0

| Problem | Detail |
|---|---|
| **Reliability** | Android's `SpeechRecognizer` is designed for dictation, not for a small keyword set. It is tuned for long, well-formed utterances and is much weaker on isolated keywords in noise. |
| **Endpointing** | There is no dependable way to know a one-word command has ended. The recogniser's silence timeout is heuristic. |
| **Barge-in** | A recogniser listening continuously will hear the app's own TTS output. Ducking or pausing the recogniser during speech is possible but adds failure modes. |
| **Availability** | `isRecognitionAvailable()` is false on devices without the Google app, and offline support varies by OEM. |
| **Permission** | `RECORD_AUDIO` for a screen-reader user's phone is a significant privacy cost — the microphone is a plausible spyware target, and the request is hard to justify for a feature that does not exist yet. |
| **Language coverage** | Hindi ASR quality on mid-range Indian devices is inconsistent. |

**Decision: P0 has no microphone and no `RECORD_AUDIO` permission.** This is the single most
privacy-respecting choice in the project, and it is enforced by the manifest.

### Options if voice commands are added later

| Option | Licence | Offline | APK size | Notes |
|---|---|---|---|---|
| Android `SpeechRecognizer` | Platform | Depends on engine | 0 | The zero-dependency choice, but weakest for keywords |
| `VoiceInteractionService` / `VoiceInteractionSession` | Platform | Engine-dependent | 0 | The *correct* Android mechanism for an assistant; intended for system-level assistants and requires a `RECORD_AUDIO` permission |
| Vosk | Apache-2.0 | **Yes**, fully offline | ~40 MB (model-dependent) | Good small-vocabulary accuracy; a large APK hit |
| sherpa-onnx | Apache-2.0 | **Yes** | ~10–30 MB | Good streaming KWS; heavy native dependency |
| Porcupine | Commercial (free tier) | Yes | Small | Excellent accuracy; a licence dependency and a vendor relationship |

**Recommendation for P1:** if voice commands ship at all, they should be **opt-in**, default
**off**, and evaluated with the same care as the rest of the P0. A user who never asked for
voice input should not be asked for the microphone.

---

## 4. TTS and TalkBack coexistence

This is the most important accessibility problem in the whole design, and it is **open** — it
cannot be resolved without a screen reader running on a real device (issue #8).

### The conflict

A TalkBack user navigates by listening to the screen reader. The app also wants to speak. If
both speak at once, the user hears two overlapping voices, and the app's message may be
unintelligible. If the app's speech stops the screen reader (or vice versa), navigation breaks.

### Android's actual mechanics

- `TextToSpeech.speak()` goes to the **media stream** by default. TalkBack also uses the
  accessibility stream. They are different streams, so the OS will not automatically stop
  one when the other starts — which means **overlap is the likely default behaviour**, not
  mutual exclusion.
- `TextToSpeech` has `setAudioAttributes()` since API 21, which allows choosing
  `USAGE_ASSISTANCE_ACCESSIBILITY` or `USAGE_ASSISTANT` instead of `USAGE_MEDIA`. The
  `USAGE_ASSISTANT` usage is the semantically correct one for a spoken prompt from an app.
- Audio focus: `TextToSpeech` does not automatically request audio focus for you. Without an
  explicit focus request, the app's speech may be ducked or dropped by another app's focus
  request — including the screen reader's own.

### Design decisions for P0

1. **`QUEUE_FLUSH` for user-triggered speech** (start, stop, repeat) so a stale announcement
   never plays after the user has changed the state. `QUEUE_ADD` is not used for state
   changes.
2. **A settings toggle to disable in-app speech entirely**, for users who prefer the screen
   reader alone. The setting does not exist yet in P0 — it is part of issue #8's scope.
3. **Detect an active screen reader** and, at minimum, avoid the two voices talking over each
   other. The standard detection signal is `AccessibilityManager.getEnabledAccessibilityServiceList`
   filtered for the screen-reader service, or the `accessibility_enabled` system setting.
4. **The screen must always carry the same information as the speech.** Every spoken message is
   also shown in text, in a live region, so a user who mutes the app's voice still gets the
   information. This is the fallback that makes the whole problem survivable.

**Open question, to be answered on a device:** whether a screen-reader user prefers the app to
speak at all, and whether the app should defer to the screen reader by default.

---

## 5. The phrasebook

Implemented in `android/app/src/main/java/org/dishavoice/app/speech/Phrasebook.kt`.
Every state has a Hindi and an English line; the English line is used when a Hindi voice is
unavailable.

| State | Hindi (Devanagari) | English fallback |
|---|---|---|
| Started | कैमरा शुरू। मैं बताऊँगा कि तस्वीर में क्या दिख रहा है। | Camera started. I will say what I can see in the image. |
| Stopped | कैमरा बंद। | Camera stopped. |
| Object found (confident) | {position} {object} दिख रहा है। | There is {object} {position}. |
| Object found (tentative) | {position} लगता है {object} दिख रहा है। | There {looks like} {object} {position}. |
| Nothing recognised | इस तस्वीर में मुझे कोई जानी-पहचानी चीज़ नहीं दिखी। | I did not recognise anything in this image. |
| Permission needed | कैमरे की अनुमति चाहिए। बिना कैमरे के मैं कुछ नहीं देख सकता। | Camera permission is needed. Without the camera I cannot see anything. |
| Camera error | कैमरा शुरू नहीं हो सका। | The camera could not be started. |
| Model unavailable | पहचानने वाला मॉडल लोड नहीं हो सका। | The recognition model could not be loaded. |
| Hindi voice missing | इस फ़ोन पर हिंदी आवाज़ उपलब्ध नहीं है, इसलिए मैं अंग्रेज़ी में बोलूँगा। | A Hindi voice is not available on this phone, so I will speak English. |
| Repeat | आख़िरी बात दोहराता हूँ। | Repeating the last message. |
| Help | मैं कैमरे की तस्वीर में आम चीज़ें पहचानता हूँ और बताता हूँ कि वे तस्वीर में कहाँ हैं। | I recognise common objects in the camera image and say where they are in the image. |

Position wording, which is the safety-critical part:

| Zone | Hindi | English |
|---|---|---|
| LEFT | तस्वीर के बाईं ओर | on the left of the image |
| CENTER | तस्वीर के बीच में | in the middle of the image |
| RIGHT | तस्वीर के दाईं ओर | on the right of the image |

### Wording rules enforced

- **"तस्वीर" (image/photo) is always used** for the position reference, never the room, the
  direction of travel, or the user's surroundings. *"तस्वीर के बाईं ओर"* literally means "on the
  left **of the image**".
- **"दिख रहा है" (is visible/appearing)** is used for a confident detection; **"लगता है" (it
  looks like)** for a tentative one. The distinction is lexical, not just a different word
  order, so it survives a fast or inattentive listen.
- **The negative state is phrased as the model's limitation**, not as a statement about the
  world: *"मुझे ... नहीं दिखी"* ("I did not see") rather than *"कुछ नहीं है"* ("there is
  nothing"). This is the single most important wording decision in the project.
- **No distance, no direction of travel, no hazard, no safety.** There is no phrase in the
  phrasebook that can be read as "it is safe to walk forward".
- Object names use everyday words, with a Hindi term where one is unambiguous
  (`बोतल` bottle, `कप` cup, `कुर्सी` chair, `मोबाइल फ़ोन` cell phone). Unmapped COCO classes
  fall back to the English label, which is honest but mixed-language — a known weakness to
  resolve with native-speaker review.

### Outstanding

1. **Native-speaker review** of every line: naturalness, correct Devanagari, and whether the
   uncertainty distinction survives being heard once. (Issue #7.)
2. **Verification on a device with a real Hindi voice** — the actual audio output has never been
   heard. (Issue #1.)
3. A decision on whether the "nothing recognised" line should be spoken at all, given that
   repeated negative announcements are precisely the alert fatigue the project is trying to
   avoid.
4. Whether to add a `VERBOSE` / `BRIEF` verbosity setting, and what the brief form is.
