package org.dishavoice.core

/** Horizontal region of the camera image, never a world-space direction or distance. */
enum class ImageZone { LEFT, CENTER, RIGHT }

/** Image-relative rectangle with coordinates normalized to [0, 1]. */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val isValid: Boolean
        get() = left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite() &&
            left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f &&
            left <= right && top <= bottom

    val centerX: Float? get() = if (isValid) (left + right) / 2f else null
}

/** A detector result in image coordinates. Label is the detector's stable class identifier. */
data class Detection(
    val label: String,
    val confidence: Float,
    val bounds: BoundingBox,
)

enum class ConfidenceBand { CONFIDENT, TENTATIVE, IGNORE }

/** Pure image-relative classification. Boundaries are deterministic and intentionally broad. */
const val LEFT_ZONE_END = 0.38f
const val RIGHT_ZONE_START = 0.62f
const val DEFAULT_CONFIDENT_THRESHOLD = 0.70f
const val DEFAULT_TENTATIVE_THRESHOLD = 0.45f
const val DEFAULT_COOLDOWN_MILLIS = 4_000L

fun zoneFor(bounds: BoundingBox): ImageZone? {
    val centerX = bounds.centerX ?: return null
    return when {
        centerX < LEFT_ZONE_END -> ImageZone.LEFT
        centerX > RIGHT_ZONE_START -> ImageZone.RIGHT
        else -> ImageZone.CENTER
    }
}

fun confidenceBand(
    confidence: Float,
    confidentThreshold: Float = DEFAULT_CONFIDENT_THRESHOLD,
    tentativeThreshold: Float = DEFAULT_TENTATIVE_THRESHOLD,
): ConfidenceBand {
    require(confidentThreshold in 0f..1f && tentativeThreshold in 0f..1f)
    require(tentativeThreshold <= confidentThreshold)
    if (!confidence.isFinite() || confidence !in 0f..1f) return ConfidenceBand.IGNORE
    return when {
        confidence >= confidentThreshold -> ConfidenceBand.CONFIDENT
        confidence >= tentativeThreshold -> ConfidenceBand.TENTATIVE
        else -> ConfidenceBand.IGNORE
    }
}

sealed interface Decision {
    data class Announce(val label: String, val zone: ImageZone, val confidenceBand: ConfidenceBand) : Decision
    data object SuppressedLowConfidence : Decision
    data object SuppressedInvalidDetection : Decision
    data object SuppressedDuplicate : Decision
}

/**
 * Stateful repeat suppression. Construct one per camera session and call with monotonic elapsed
 * time (e.g. Android SystemClock.elapsedRealtime()); it stores labels/zones/timestamps only.
 */
class AnnouncementPolicy(
    private val cooldownMillis: Long = DEFAULT_COOLDOWN_MILLIS,
    private val confidentThreshold: Float = DEFAULT_CONFIDENT_THRESHOLD,
    private val tentativeThreshold: Float = DEFAULT_TENTATIVE_THRESHOLD,
) {
    private val lastAnnouncedAt = mutableMapOf<Pair<String, ImageZone>, Long>()

    init {
        require(cooldownMillis >= 0L)
        require(confidentThreshold in 0f..1f && tentativeThreshold in 0f..1f)
        require(tentativeThreshold <= confidentThreshold)
    }

    fun decide(detection: Detection, nowMillis: Long): Decision {
        val label = detection.label.trim()
        val zone = zoneFor(detection.bounds)
        if (label.isEmpty() || zone == null) return Decision.SuppressedInvalidDetection

        val band = confidenceBand(detection.confidence, confidentThreshold, tentativeThreshold)
        if (band == ConfidenceBand.IGNORE) return Decision.SuppressedLowConfidence

        val key = label.lowercase() to zone
        val lastTime = lastAnnouncedAt[key]
        if (lastTime != null && nowMillis >= lastTime && nowMillis - lastTime < cooldownMillis) {
            return Decision.SuppressedDuplicate
        }

        lastAnnouncedAt[key] = nowMillis
        return Decision.Announce(label, zone, band)
    }

    companion object {
        const val LEFT_ZONE_END = 0.38f
        const val RIGHT_ZONE_START = 0.62f
        const val DEFAULT_CONFIDENT_THRESHOLD = 0.70f
        const val DEFAULT_TENTATIVE_THRESHOLD = 0.45f
        const val DEFAULT_COOLDOWN_MILLIS = 4_000L
    }
}
