package org.dishavoice.app.detection

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Builds the uint8 RGB input tensor for the bundled EfficientDet-Lite0 model.
 *
 * Kept free of TensorFlow types, and split so that the part that decides the
 * *convention* - [toRgbBytes] - is pure and unit testable on the JVM.
 *
 * The convention is not a guess. The model's input tensor was measured against
 * the shipped asset: `[1, 320, 320, 3]`, **uint8**, quantisation
 * `(scale=0.0078125, zero_point=127)`. Running the real model on the real test
 * images under three candidate conventions showed that raw uint8 pixels are
 * correct, and that ImageNet mean/std normalisation is measurably worse
 * (bus 0.801 -> 0.668, dog 0.840 -> 0.801). See
 * `benchmarks/scripts/verify_preprocessing.py` and
 * `RESEARCH/model-and-data-audit.md` section 3.
 *
 * This matters because a silent change here would degrade detection without
 * breaking anything: the desktop benchmark shares the same convention, so it
 * would not notice either. Hence the unit test.
 */
object InputTensorBuilder {

    /**
     * Packs ARGB pixels into the model's uint8 RGB layout.
     *
     * Bytes are the **raw 0-255 channel values**, in **R, G, B** order. There is
     * no float scaling, no mean/std subtraction, and no requantisation: the
     * model takes uint8, so it gets uint8.
     *
     * @param pixels `size * size` ARGB ints, as returned by `Bitmap.getPixels`.
     * @param size the model's input edge length.
     */
    fun toRgbBytes(pixels: IntArray, size: Int): ByteArray {
        require(size > 0) { "size must be positive, was $size" }
        require(pixels.size >= size * size) {
            "expected at least ${size * size} pixels, got ${pixels.size}"
        }
        val out = ByteArray(size * size * 3)
        var i = 0
        for (pixel in pixels) {
            out[i++] = (pixel shr 16 and 0xFF).toByte() // R
            out[i++] = (pixel shr 8 and 0xFF).toByte()  // G
            out[i++] = (pixel and 0xFF).toByte()       // B
        }
        return out
    }

    /**
     * Scales [bitmap] to the model input size and packs it into a direct
     * byte buffer ready for `Interpreter.runForMultipleInputsOutputs`.
     */
    fun buildFromBitmap(bitmap: Bitmap, size: Int): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        val bytes = toRgbBytes(pixels, size)
        if (scaled !== bitmap) scaled.recycle()
        val buffer = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
        buffer.put(bytes)
        buffer.rewind()
        return buffer
    }
}
