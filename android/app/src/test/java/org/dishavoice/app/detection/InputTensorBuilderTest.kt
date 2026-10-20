package org.dishavoice.app.detection

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the input convention the app uses for the bundled model.
 *
 * This exists because a silent change to the pre-processing would degrade
 * detection **without breaking anything**, and the desktop benchmark would not
 * catch it either, because it shares the same convention. The measured
 * evidence is in `RESEARCH/model-and-data-audit.md` section 3 and is
 * reproducible with `benchmarks/scripts/verify_preprocessing.py`:
 *
 *  - raw uint8 (what the app does)  -> bus 0.801, dog 0.840, dog 0.723
 *  - normalise to [-1,1], requantise  -> near-identical, because this model's
 *    input quantisation (scale 0.0078125, zero_point 127) maps the uint8 range
 *    onto [-1,1]
 *  - ImageNet mean/std normalisation  -> clearly worse: bus 0.801 -> 0.668,
 *    dog 0.840 -> 0.801
 */
class InputTensorBuilderTest {

    private fun argb(r: Int, g: Int, b: Int, a: Int = 0xFF): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun `produces three bytes per pixel`() {
        val bytes = InputTensorBuilder.toRgbBytes(IntArray(4 * 4) { argb(1, 2, 3) }, 4)
        assertEquals(4 * 4 * 3, bytes.size)
    }

    @Test
    fun `writes raw channel values without normalisation`() {
        val bytes = InputTensorBuilder.toRgbBytes(intArrayOf(argb(200, 100, 50)), 1)
        // Raw uint8 in R, G, B order. A normalised value would be a small
        // signed number around 0 or 127, not the full 0-255 range.
        assertArrayEquals(byteArrayOf(200.toByte(), 100.toByte(), 50.toByte()), bytes)
    }

    @Test
    fun `keeps the full 0-255 range intact`() {
        // 2x2 = 4 pixels
        val extremes = intArrayOf(
            argb(0, 0, 0),
            argb(255, 255, 255),
            argb(255, 0, 0),
            argb(0, 255, 0),
        )
        val bytes = InputTensorBuilder.toRgbBytes(extremes, 2)
        assertArrayEquals(
            byteArrayOf(
                0, 0, 0,
                255.toByte(), 255.toByte(), 255.toByte(),
                255.toByte(), 0, 0,
                0, 255.toByte(), 0,
            ),
            bytes,
        )
        // Every byte must be an unsigned channel value, never a negative
        // signed byte from a [-1,1] float that was never requantised.
        assertTrue(bytes.all { it.toInt() and 0xFF in 0..255 })
    }

    @Test
    fun `writes channels in red green blue order`() {
        val bytes = InputTensorBuilder.toRgbBytes(intArrayOf(argb(10, 20, 30)), 1)
        assertEquals(10, bytes[0].toInt() and 0xFF)
        assertEquals(20, bytes[1].toInt() and 0xFF)
        assertEquals(30, bytes[2].toInt() and 0xFF)
    }

    @Test
    fun `ignores the alpha channel`() {
        val opaque = InputTensorBuilder.toRgbBytes(intArrayOf(argb(10, 20, 30, 0xFF)), 1)
        val transparent = InputTensorBuilder.toRgbBytes(intArrayOf(argb(10, 20, 30, 0x00)), 1)
        assertArrayEquals(opaque, transparent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a size with too few pixels`() {
        InputTensorBuilder.toRgbBytes(IntArray(3), 2)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects a non positive size`() {
        InputTensorBuilder.toRgbBytes(IntArray(0), 0)
    }
}
