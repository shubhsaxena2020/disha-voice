package org.dishavoice.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AnnouncementPolicyTest {
    @Test
    fun `classifies normalized boxes by horizontal image region`() {
        assertEquals(ImageZone.LEFT, zoneFor(centerX = 0.20f))
        assertEquals(ImageZone.CENTER, zoneFor(centerX = 0.50f))
        assertEquals(ImageZone.RIGHT, zoneFor(centerX = 0.80f))
    }

    @Test
    fun `zone boundaries are deterministic`() {
        assertEquals(ImageZone.LEFT, zoneFor(centerX = 0.0f))
        assertEquals(ImageZone.CENTER, zoneFor(centerX = 0.38f))
        assertEquals(ImageZone.CENTER, zoneFor(centerX = 0.62f))
        assertEquals(ImageZone.RIGHT, zoneFor(centerX = 1.0f))
    }

    @Test
    fun `rejects invalid normalized boxes instead of inventing a direction`() {
        assertNull(zoneFor(centerX = -0.01f))
        assertNull(zoneFor(centerX = 1.01f))
        assertNull(zoneFor(centerX = Float.NaN))
    }

    @Test
    fun `confidence policy distinguishes confident tentative and insufficient detections`() {
        assertEquals(ConfidenceBand.CONFIDENT, confidenceBand(0.70f))
        assertEquals(ConfidenceBand.TENTATIVE, confidenceBand(0.45f))
        assertEquals(ConfidenceBand.IGNORE, confidenceBand(0.449f))
        assertEquals(ConfidenceBand.IGNORE, confidenceBand(Float.NaN))
    }

    @Test
    fun `does not announce the same label and zone during its cooldown`() {
        val policy = AnnouncementPolicy(cooldownMillis = 4_000L)
        val objectAtLeft = Detection("bottle", 0.9f, BoundingBox(0.05f, 0.1f, 0.35f, 0.8f))

        assertTrue(policy.decide(objectAtLeft, nowMillis = 1_000L) is Decision.Announce)
        assertEquals(Decision.SuppressedDuplicate, policy.decide(objectAtLeft, nowMillis = 2_000L))
        assertTrue(policy.decide(objectAtLeft, nowMillis = 5_001L) is Decision.Announce)
    }

    @Test
    fun `does not suppress a same-label detection in a different image zone`() {
        val policy = AnnouncementPolicy(cooldownMillis = 4_000L)
        val left = Detection("bottle", 0.9f, BoundingBox(0.05f, 0.1f, 0.35f, 0.8f))
        val right = Detection("bottle", 0.9f, BoundingBox(0.65f, 0.1f, 0.95f, 0.8f))

        assertTrue(policy.decide(left, nowMillis = 1_000L) is Decision.Announce)
        assertTrue(policy.decide(right, nowMillis = 2_000L) is Decision.Announce)
    }

    @Test
    fun `does not speak detections below the uncertainty floor`() {
        val policy = AnnouncementPolicy()
        val weak = Detection("cup", 0.2f, BoundingBox(0.3f, 0.2f, 0.7f, 0.8f))
        assertEquals(Decision.SuppressedLowConfidence, policy.decide(weak, nowMillis = 1L))
    }

    @Test
    fun `bounds must be ordered and inside the normalized image`() {
        assertFalse(BoundingBox(-0.1f, 0f, 0.5f, 1f).isValid)
        assertFalse(BoundingBox(0.7f, 0f, 0.3f, 1f).isValid)
        assertFalse(BoundingBox(0f, 0f, Float.NaN, 1f).isValid)
        assertTrue(BoundingBox(0f, 0f, 1f, 1f).isValid)
    }

    private fun zoneFor(centerX: Float): ImageZone? =
        zoneFor(BoundingBox(centerX, 0.1f, centerX, 0.9f))
}
