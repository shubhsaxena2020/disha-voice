package org.dishavoice.app.detection

import org.dishavoice.core.ConfidenceBand
import org.dishavoice.core.ImageZone
import org.dishavoice.core.confidenceBand
import org.dishavoice.core.zoneFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionDecoderTest {

    @Test
    fun `maps model class index through the verified offset`() {
        // person -> model index 0, bus -> 5, dog -> 17, tie -> 31 (measured anchors).
        assertEquals("person", CocoLabels.labelForModelIndex(0))
        assertEquals("bus", CocoLabels.labelForModelIndex(5))
        assertEquals("dog", CocoLabels.labelForModelIndex(17))
        assertEquals("tie", CocoLabels.labelForModelIndex(31))
    }

    @Test
    fun `unmapped model index yields null instead of a wrong name`() {
        // COCO id 12 has no category; model index 11 maps to it.
        assertNull(CocoLabels.labelForModelIndex(11))
    }

    @Test
    fun `decodes a detection with normalized bounds and score`() {
        // box = [ymin, xmin, ymax, xmax] = [0.05, 0.10, 0.60, 0.35]
        val boxes = floatArrayOf(0.05f, 0.10f, 0.60f, 0.35f)
        val detections = DetectionDecoder.decode(
            boxes = boxes,
            classes = floatArrayOf(17f), // dog
            scores = floatArrayOf(0.84f),
            numDetections = 1,
        )

        assertEquals(1, detections.size)
        val d = detections.first()
        assertEquals("dog", d.label)
        assertEquals(0.84f, d.confidence, 1e-4f)
        assertEquals(0.10f, d.bounds.left, 1e-4f)
        assertEquals(0.35f, d.bounds.right, 1e-4f)
        assertEquals(0.05f, d.bounds.top, 1e-4f)
        assertEquals(0.60f, d.bounds.bottom, 1e-4f)
    }

    @Test
    fun `drops detections below the score floor`() {
        val boxes = FloatArray(8)
        val detections = DetectionDecoder.decode(
            boxes = boxes,
            classes = floatArrayOf(17f, 0f),
            scores = floatArrayOf(0.10f, 0.90f),
            numDetections = 2,
        )
        assertEquals(1, detections.size)
        assertEquals("person", detections.first().label)
    }

    @Test
    fun `respects the reported detection count`() {
        val boxes = FloatArray(8)
        val detections = DetectionDecoder.decode(
            boxes = boxes,
            classes = floatArrayOf(0f, 0f),
            scores = floatArrayOf(0.9f, 0.9f),
            numDetections = 1,
        )
        assertEquals(1, detections.size)
    }

    @Test
    fun `skips detections whose class has no label mapping`() {
        val boxes = FloatArray(4)
        val detections = DetectionDecoder.decode(
            boxes = boxes,
            classes = floatArrayOf(11f), // no COCO category
            scores = floatArrayOf(0.99f),
            numDetections = 1,
        )
        assertTrue(detections.isEmpty())
    }

    @Test
    fun `decoded detection maps to the expected image zone`() {
        // Left third of the image.
        val boxes = floatArrayOf(0.10f, 0.05f, 0.80f, 0.35f)
        val detection = DetectionDecoder.decode(boxes, floatArrayOf(0f), floatArrayOf(0.9f), 1).first()
        assertEquals(ImageZone.LEFT, zoneFor(detection.bounds))
        assertEquals(ConfidenceBand.CONFIDENT, confidenceBand(detection.confidence))
    }

    @Test
    fun `clamps out of range box coordinates instead of producing invalid bounds`() {
        val boxes = floatArrayOf(-0.2f, -0.1f, 1.4f, 1.2f)
        val detection = DetectionDecoder.decode(boxes, floatArrayOf(0f), floatArrayOf(0.9f), 1).first()
        assertTrue(detection.bounds.isValid)
        assertEquals(0f, detection.bounds.left, 1e-4f)
        assertEquals(1f, detection.bounds.right, 1e-4f)
    }

    @Test
    fun `ignores non finite scores`() {
        val boxes = FloatArray(4)
        val detections = DetectionDecoder.decode(
            boxes = boxes,
            classes = floatArrayOf(0f),
            scores = floatArrayOf(Float.NaN),
            numDetections = 1,
        )
        assertTrue(detections.isEmpty())
    }
}
