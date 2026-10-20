package org.dishavoice.app.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the bundled EfficientDet-Lite0 model's output-tensor contract and the
 * rules used to resolve it.
 *
 * The contract below was **measured** from the shipped asset
 * (`android/app/src/main/assets/efficientdet_lite0_detection.tflite`) with
 * LiteRT, not read from documentation. See `RESEARCH/platform-api-audit.md`
 * section 2.
 *
 * The regression this test exists for: an earlier version of the detector
 * separated the class-id tensor from the score tensor by inspecting their
 * values. On a freshly allocated interpreter every output tensor reads as
 * zero, so the class-id tensor also satisfied the "0..1 means scores" test,
 * the resolution failed, and the detector returned an empty list on every
 * frame. The app would show the camera and never announce anything.
 */
class OutputTensorResolverTest {

    /** The measured output layout of the shipped model, in interpreter ordinal order. */
    private val measuredShapes = listOf(
        intArrayOf(1, 25, 4), // ordinal 0 - StatefulPartitionedCall:3 - boxes
        intArrayOf(1, 25),    // ordinal 1 - StatefulPartitionedCall:2 - class ids
        intArrayOf(1, 25),    // ordinal 2 - StatefulPartitionedCall:1 - scores
        intArrayOf(1),        // ordinal 3 - StatefulPartitionedCall:0 - count
    )

    @Test
    fun `resolves the measured model layout`() {
        val resolved = OutputTensorResolver.resolve(measuredShapes)
        assertEquals(0, resolved.boxes)
        assertEquals(1, resolved.classes)
        assertEquals(2, resolved.scores)
        assertEquals(3, resolved.count)
    }

    @Test
    fun `the class-id and score tensors really do share a shape`() {
        // This is the fact that makes value-free resolution necessary.
        assertTrue(measuredShapes[1].contentEquals(measuredShapes[2]))
    }

    @Test
    fun `resolution does not depend on the tensor values`() {
        // On a fresh interpreter every output tensor is all zeros, and zero is
        // inside 0..1, so a value-range test cannot tell the class ids from
        // the scores. The resolver must still produce the right answer from
        // shape and position alone.
        val freshInterpreterValues = FloatArray(25) { 0f }
        assertTrue(freshInterpreterValues.all { it in 0f..1f })

        val resolved = OutputTensorResolver.resolve(measuredShapes)
        assertEquals(1, resolved.classes)
        assertEquals(2, resolved.scores)
    }

    @Test
    fun `rejects a layout with the wrong number of outputs`() {
        val failure = runCatching {
            OutputTensorResolver.resolve(measuredShapes.take(3))
        }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("output tensors"))
    }

    @Test
    fun `rejects a layout with no box tensor`() {
        val broken = listOf(
            intArrayOf(1, 25),
            intArrayOf(1, 25),
            intArrayOf(1, 25),
            intArrayOf(1),
        )
        val failure = runCatching { OutputTensorResolver.resolve(broken) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `rejects a layout with only one two dimensional output`() {
        val broken = listOf(
            intArrayOf(1, 25, 4),
            intArrayOf(1, 25),
            intArrayOf(1, 25, 3),
            intArrayOf(1),
        )
        val failure = runCatching { OutputTensorResolver.resolve(broken) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `rejects a layout with no count tensor`() {
        val broken = listOf(
            intArrayOf(1, 25, 4),
            intArrayOf(1, 25),
            intArrayOf(1, 25),
            intArrayOf(1, 25),
        )
        val failure = runCatching { OutputTensorResolver.resolve(broken) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }

    @Test
    fun `rejects a box tensor that is not a batch of one`() {
        val broken = listOf(
            intArrayOf(2, 25, 4),
            intArrayOf(1, 25),
            intArrayOf(1, 25),
            intArrayOf(1),
        )
        val failure = runCatching { OutputTensorResolver.resolve(broken) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
    }
}
