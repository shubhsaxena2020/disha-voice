package org.dishavoice.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Deterministic synthetic property checks; this suite needs no Android SDK or device. */
class AnnouncementPolicyPropertyTest {
    @Test
    fun `seeded valid boxes always map to the zone implied by their normalized center`() {
        val random = Random(FIXED_SEED)

        repeat(10_000) { sample ->
            val left = random.nextFloat()
            val top = random.nextFloat()
            val right = left + random.nextFloat() * (1f - left)
            val bottom = top + random.nextFloat() * (1f - top)
            val box = BoundingBox(left, top, right, bottom)
            val centerX = (left + right) / 2f
            val expected = when {
                centerX < 0.38f -> ImageZone.LEFT
                centerX > 0.62f -> ImageZone.RIGHT
                else -> ImageZone.CENTER
            }

            assertTrue(box.isValid, "seed=0xD15A0 sample=$sample box=$box")
            assertEquals(expected, zoneFor(box), "seed=0xD15A0 sample=$sample box=$box")
            assertEquals(zoneFor(box), zoneFor(box), "classification must be repeatable")
        }
    }

    @Test
    fun `confidence classification is monotonic over the full normalized confidence grid`() {
        var previousRank = rank(ConfidenceBand.IGNORE)

        for (step in 0..10_000) {
            val confidence = step / 10_000f
            val currentRank = rank(confidenceBand(confidence))
            assertTrue(
                currentRank >= previousRank,
                "classification regressed at confidence=$confidence",
            )
            previousRank = currentRank
        }
    }

    @Test
    fun `label-zone cooldown behavior is stable for synthetic independent keys`() {
        val policy = AnnouncementPolicy(cooldownMillis = 4_000L)
        val zones = listOf(
            ImageZone.LEFT to BoundingBox(0.1f, 0.1f, 0.3f, 0.9f),
            ImageZone.CENTER to BoundingBox(0.4f, 0.1f, 0.6f, 0.9f),
            ImageZone.RIGHT to BoundingBox(0.7f, 0.1f, 0.9f, 0.9f),
        )
        var keyIndex = 0

        for (labelIndex in 0 until 12) {
            for ((expectedZone, bounds) in zones) {
                val baseTime = 1_000L + keyIndex * 10_000L
                val canonicalLabel = "synthetic-$labelIndex"
                val first = Detection(" $canonicalLabel ", 0.9f, bounds)
                val caseVariant = Detection(canonicalLabel.uppercase(), 0.9f, bounds)

                val firstDecision = policy.decide(first, nowMillis = baseTime)
                assertTrue(firstDecision is Decision.Announce)
                assertEquals(expectedZone, (firstDecision as Decision.Announce).zone)
                assertEquals(canonicalLabel, firstDecision.label)
                assertEquals(
                    Decision.SuppressedDuplicate,
                    policy.decide(caseVariant, nowMillis = baseTime + 1L),
                )
                assertTrue(
                    policy.decide(first, nowMillis = baseTime + 4_000L) is Decision.Announce,
                    "cooldown boundary must permit a repeat for key=$canonicalLabel/$expectedZone",
                )
                keyIndex++
            }
        }
    }

    @Test
    fun `invalid and low confidence synthetic detections do not start a cooldown`() {
        val policy = AnnouncementPolicy(cooldownMillis = 4_000L)
        val valid = Detection("cup", 0.9f, BoundingBox(0.3f, 0.1f, 0.7f, 0.9f))
        val invalid = Detection(" ", 0.9f, valid.bounds)
        val weak = valid.copy(confidence = 0.1f)

        assertEquals(Decision.SuppressedInvalidDetection, policy.decide(invalid, nowMillis = 1_000L))
        assertEquals(Decision.Announce("cup", ImageZone.CENTER, ConfidenceBand.CONFIDENT), policy.decide(valid, nowMillis = 1_001L))
        assertEquals(Decision.SuppressedLowConfidence, policy.decide(weak, nowMillis = 2_000L))
        assertTrue(policy.decide(valid, nowMillis = 5_001L) is Decision.Announce)
    }

    private fun rank(band: ConfidenceBand): Int = when (band) {
        ConfidenceBand.IGNORE -> 0
        ConfidenceBand.TENTATIVE -> 1
        ConfidenceBand.CONFIDENT -> 2
    }

    private companion object {
        const val FIXED_SEED = 0xD15A0L
    }
}
