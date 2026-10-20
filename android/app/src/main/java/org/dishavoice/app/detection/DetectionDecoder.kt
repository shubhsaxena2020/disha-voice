package org.dishavoice.app.detection

import org.dishavoice.core.BoundingBox
import org.dishavoice.core.Detection

/**
 * Decodes the EfficientDet-Lite0 SSD head output into domain detections.
 *
 * Kept free of TensorFlow types so it can be unit tested on the JVM.
 *
 * The model emits four tensors (verified against the actual `.tflite` file):
 *  - boxes   `[1, N, 4]` float, each `[ymin, xmin, ymax, xmax]` normalized to 0..1
 *  - classes `[1, N]`   float, holding integral class indices
 *  - scores  `[1, N]`   float in 0..1
 *  - count   `[1]`       float, the number of valid detections
 */
object DetectionDecoder {

    /** Detections below this score are dropped before reaching the domain layer. */
    const val DEFAULT_SCORE_FLOOR = 0.25f

    /**
     * @param boxes flat `N * 4` values in `ymin, xmin, ymax, xmax` order.
     * @param classes flat `N` class indices.
     * @param scores flat `N` confidence scores.
     * @param numDetections how many leading entries are valid.
     */
    fun decode(
        boxes: FloatArray,
        classes: FloatArray,
        scores: FloatArray,
        numDetections: Int,
        scoreFloor: Float = DEFAULT_SCORE_FLOOR,
    ): List<Detection> {
        val limit = minOf(numDetections, classes.size, scores.size, boxes.size / 4)
        val result = ArrayList<Detection>(limit)
        for (i in 0 until limit) {
            val score = scores[i]
            if (!score.isFinite() || score < scoreFloor) continue

            val base = i * 4
            val yMin = boxes[base]
            val xMin = boxes[base + 1]
            val yMax = boxes[base + 2]
            val xMax = boxes[base + 3]

            val label = CocoLabels.labelForModelIndex(classes[i].toInt()) ?: continue
            val bounds = BoundingBox(
                left = xMin.coerceIn(0f, 1f),
                top = yMin.coerceIn(0f, 1f),
                right = xMax.coerceIn(0f, 1f),
                bottom = yMax.coerceIn(0f, 1f),
            )
            if (!bounds.isValid) continue

            result += Detection(label = label, confidence = score, bounds = bounds)
        }
        return result
    }
}
