#!/usr/bin/env bash
# Create the Disha Voice issue tracker. Idempotent: skips a title that already exists.
set -uo pipefail
R=shubhsaxena2020/disha-voice

issue() {
  local title="$1" body="$2" labels="$3" milestone="$4"
  if gh issue list --repo "$R" --state all --search "$title in:title" --json title --jq '.[].title' 2>/dev/null | grep -qF "$title"; then
    echo "SKIP  $title"; return 0
  fi
  gh issue create --repo "$R" --title "$title" --body "$body" --label "$labels" --milestone "$milestone" >/dev/null 2>&1 \
    && echo "CREATED  $title" || echo "FAILED  $title"
}

issue "Verify the app end-to-end on a physical Android handset" \
"No Android device was available while this prototype was built, so the core loop is unverified in the real world.

- [ ] Install \`app-debug.apk\` and confirm it launches
- [ ] Grant camera permission; confirm the permission flow and the denial path
- [ ] Confirm detection works against real objects (not only the benchmark images)
- [ ] Confirm the **Hindi** voice is used when the device has one, and that the spoken fallback notice appears when it does not
- [ ] Run a manual TalkBack pass: focus order, labels, live-region announcements, repeat-last-message
- [ ] Record device make/model, Android version, API level, and the model/runtime version
- [ ] Measure on-device inference latency (median and p90 over at least 3 repeats) and note thermal/battery behaviour
- [ ] Capture screenshots and a screen recording of the **real** app for the website — no mockups presented as live

**Why:** the release gate requires real-device verification of camera, detection, and speech. Until this is done, the prototype must not be described as tested.

**Blocked for:** every user-facing performance and accuracy claim." \
"P0-critical,device-test" "P0 - Device verification"

issue "Calibrate the confidence thresholds (0.70 / 0.45) against real data" \
"The current thresholds are prototype defaults. They were never derived from measured accuracy, and the detector's score is a class posterior, not a probability that the label is correct.

- [ ] Collect labelled observations using the protocol in \`TESTING/field-evaluation-protocol.md\`
- [ ] Build a reliability curve (score bucket vs. actual correctness)
- [ ] Choose thresholds from that curve, not by feel
- [ ] Re-check the tentative phrasing threshold (the \"it looks like\" wording)
- [ ] Record the method and the resulting numbers in \`RESEARCH/\`

**Why:** announcing a wrong object confidently is the main safety risk of this design. The current thresholds are the only thing standing between a 0.5-score guess and a stated fact.

**Related:** the false-confidence failure mode in \`RESEARCH/safety-and-claims-review.md\`." \
"P0-critical,research" "P0 - Release readiness"

issue "Decide whether image-relative left/centre/right is actually useful" \
"This is the most contested design claim in the project. \"Left / centre / right in the image\" is not the same as the object's position relative to the user, and there is a real possibility that it does not help a blind user act on it.

- [ ] Search the literature for evidence on image-relative vs. egocentric spatial reference for blind users
- [ ] Design a comparison that could settle it (image-relative wording vs. egocentric bearing vs. no spatial information)
- [ ] Run it as a stationary, non-navigational study — no walking, no stairs, no roads
- [ ] If the evidence is negative, change the wording rather than the claim

**Why:** shipping a feature that sounds useful but does not help is worse than not shipping it. The app currently avoids implying safety, but the usefulness of the core output is still unestablished.

**Related:** the H4 hypothesis in \`RESEARCH/differentiation-hypothesis.md\`." \
"research,accessibility" "P1 - Usability and automation"

issue "Complete the licence audit for the prior-art projects" \
"\`RESEARCH/open-source-license-audit.md\` records the prior-art projects (SightBuddy, SeeForMe, VISION, Be My Eyes components) with their licence marked **UNKNOWN**, because the automated passes did not re-read each \`LICENSE\` file.

- [ ] Read the actual \`LICENSE\` file for each repository and record the exact SPDX identifier and copyright line
- [ ] Record the **model** licence separately from the code licence for each project
- [ ] Replace the UNKNOWN rows in the audit table
- [ ] Decide whether to replace the two Ultralytics-sourced test images with self-taken photographs

**Why:** a licence mistake discovered after publication is expensive; a licence badge is not evidence." \
"P1-high,documentation" "P0 - Release readiness"

issue "Set up a release signing key and produce a signed release APK" \
"Only a debug-signed APK exists. A release requires an operator-managed keystore.

- [ ] Generate a release keystore **outside** the repository
- [ ] Store the keystore and its passwords in a password manager or OS keystore
- [ ] Configure \`signingConfigs\` in \`android/app/build.gradle.kts\` to read the values from Gradle properties, so no credential is committed
- [ ] Build \`assembleRelease\`, verify with \`apksigner verify --print-certs\`
- [ ] Publish the SHA-256 alongside the artifact

**Never** commit the keystore, its passwords, or a token. See \`docs/RELEASE.md\`." \
"P1-high" "P0 - Release readiness"

issue "Add CI artefacts: APK upload and unit-test reports" \
"CI currently runs the core tests and the website link check. It does not build or publish the Android app.

- [ ] Add an \`:android:app:assembleDebug\` job with the SDK installed
- [ ] Upload the APK as a workflow artefact (retention-limited)
- [ ] Publish the JUnit XML test reports
- [ ] Keep permissions least-privilege and pin every action to a commit SHA
- [ ] Do not upload an APK that has not been built and tested in that same run

**Why:** the release gate expects a build from a clean checkout with recorded results." \
"P1-high" "P1 - Usability and automation"

issue "Add a spoken 'no object recognised' state that never implies a clear path" \
"The phrasebook already avoids implying safety, but the negative case deserves its own review: the current English wording is \"I did not recognise anything in this image\", and the Hindi equivalent says the same.

- [ ] Have a native Hindi speaker review the Devanagari phrasing and its naturalness
- [ ] Confirm the wording cannot be misheard as reassurance about the surroundings
- [ ] Consider whether the negative case should be spoken at all, or only shown visually — repeated \"nothing found\" announcements are exactly the alert fatigue this project is trying to avoid
- [ ] Record the decision and the reasoning

**Related:** the repetition-burden hypothesis in \`RESEARCH/differentiation-hypothesis.md\`." \
"accessibility,research" "P1 - Usability and automation"

issue "Prevent the app's speech from colliding with TalkBack" \
"The app speaks through \`TextToSpeech\` while TalkBack may also be speaking. The current implementation uses \`QUEUE_FLUSH\` for announcements but does not yet handle audio focus explicitly.

- [ ] Determine the correct audio-focus behaviour when a screen reader is active
- [ ] Add a setting to disable in-app speech when TalkBack is running, if that is the better behaviour
- [ ] Verify that stopping the camera actually silences queued speech
- [ ] Test with a screen reader enabled on a real device

**Why:** a screen-reader user hearing the app talk over TalkBack is a serious accessibility defect, and this project exists for exactly that audience." \
"P0-critical,accessibility,device-test" "P0 - Device verification"

issue "Publish the results of the field evaluation" \
"\`TESTING/field-evaluation-protocol.md\` defines a protocol for at least 100 labelled observations. **No results have been collected yet** — the protocol is a plan, not an outcome.

- [ ] Recruit participants with informed consent and accessible test design
- [ ] Never ask a participant to walk, cross roads, navigate stairs, or rely on prototype output
- [ ] Do not collect bystander imagery without consent; anonymise where feasible
- [ ] Report precision/recall per class, zone agreement, latency median and p90, and any crashes or freezes
- [ ] Document sampling bias and limitations

**Why:** a single unexplained \"accuracy\" number is not acceptable; the breakdown and its limits are the point." \
"research,device-test" "P1 - Usability and automation"

issue "Document the memory, battery and thermal profile of continuous use" \
"Nothing is known about sustained behaviour. The app runs a camera and an on-device model continuously, which is exactly the pattern that causes thermal throttling on mid-range phones.

- [ ] Measure memory over a sustained session
- [ ] Record CPU throttling and frame drop over time
- [ ] Measure battery drain over a defined session length
- [ ] Document the frame throttling strategy and whether inference rate should be reduced under thermal pressure

**Why:** \"lightweight\" and \"battery-efficient\" cannot be claimed without this, and the project currently claims neither — it should not start to." \
"performance,device-test" "P0 - Device verification"

echo "=== issues ==="
gh issue list --repo "$R" --state all --limit 30 --json number,title,milestone --jq '.[] | "\(.number)\t\(.title)\t[\(.milestone.title // "no milestone")]"' | sort -n
