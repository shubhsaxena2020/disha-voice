package org.dishavoice.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.dishavoice.core.Detection
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.Tensor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.MappedByteBuffer

/**
 * Runs the bundled EfficientDet-Lite0 model on device.
 *
 * Model facts measured against the actual asset file with LiteRT (not assumed
 * from documentation):
 *  - input:  `serving_default_images:0`, `[1, 320, 320, 3]`, **uint8**
 *  - outputs: boxes `[1,25,4]`, classes `[1,25]`, scores `[1,25]`, count `[1]`
 *
 * The model is Apache-2.0, COCO-2017 trained, 4.4 MB, and ships inside the APK
 * so detection works with no network access.
 */
class EfficientDetDetector(
    private val context: Context,
    modelAssetPath: String = DEFAULT_MODEL_ASSET,
) : Detector {

    private val interpreter: Interpreter? = runCatching {
        Interpreter(
            loadModel(modelAssetPath),
            Interpreter.Options().setNumThreads(2).setUseXNNPACK(true),
        )
    }.onFailure { Log.e(TAG, "Failed to load model $modelAssetPath", it) }.getOrNull()

    private val inputSize: Int = interpreter
        ?.let { runCatching { it.getInputTensor(0).shape()[1] }.getOrNull() }
        ?: DEFAULT_INPUT_SIZE

    override val isReady: Boolean get() = interpreter != null

    override val inputResolution: Int get() = inputSize

    override fun detect(bitmap: Bitmap): List<Detection> {
        val tf = interpreter ?: return emptyList()
        return runCatching {
            val input = buildInputBuffer(bitmap, inputSize)
            val indices = resolveOutputIndices(tf)

            val maxDetections = tf.getOutputTensor(indices.scores).shape()[1]
            val outputs = HashMap<Int, Any>()
            outputs[indices.boxes] = FloatArray(maxDetections * 4)
            outputs[indices.classes] = FloatArray(maxDetections)
            outputs[indices.scores] = FloatArray(maxDetections)
            outputs[indices.count] = FloatArray(1)

            tf.runForMultipleInputsOutputs(arrayOf<Any>(input), outputs)

            val boxes = outputs.getValue(indices.boxes) as FloatArray
            val classes = outputs.getValue(indices.classes) as FloatArray
            val scores = outputs.getValue(indices.scores) as FloatArray
            val count = (outputs.getValue(indices.count) as FloatArray)[0].toInt()

            DetectionDecoder.decode(boxes, classes, scores, count)
        }.getOrElse {
            Log.e(TAG, "Inference failed", it)
            emptyList()
        }
    }

    private data class OutputIndices(
        val boxes: Int,
        val classes: Int,
        val scores: Int,
        val count: Int,
    )

    /**
     * Output order for this model is boxes, classes, scores, count. The indices
     * are resolved by tensor shape so the code stays correct if the asset is
     * ever replaced with a different detector head.
     */
    private fun resolveOutputIndices(tf: InterpreterApi): OutputIndices {
        var boxes = -1
        var classes = -1
        var scores = -1
        var count = -1
        for (i in 0 until tf.outputTensorCount) {
            val shape = tf.getOutputTensor(i).shape()
            when {
                shape.size == 3 && shape[2] == 4 -> boxes = i
                shape.size == 1 -> count = i
                shape.size == 2 -> {
                    // The classes tensor holds integral ids; the scores tensor is
                    // bounded to 0..1. Distinguish them by their value ranges.
                    val values = readFloats(tf.getOutputTensor(i))
                    val looksLikeScores = values.all { it.isFinite() && it in 0f..1f }
                    if (looksLikeScores) scores = i else classes = i
                }
            }
        }
        if (boxes < 0 || classes < 0 || scores < 0 || count < 0) {
            error("Unexpected model output layout")
        }
        return OutputIndices(boxes, classes, scores, count)
    }

    /** Reads a float tensor through its read-only buffer. */
    private fun readFloats(tensor: Tensor): FloatArray {
        val buffer = tensor.asReadOnlyBuffer()
        val count = buffer.remaining() / 4
        val values = FloatArray(count)
        for (i in 0 until count) values[i] = buffer.float
        return values
    }

    /** Builds the uint8 RGB input tensor expected by this model. */
    private fun buildInputBuffer(bitmap: Bitmap, size: Int): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val buffer = ByteBuffer.allocateDirect(size * size * 3).order(ByteOrder.nativeOrder())
        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        for (pixel in pixels) {
            buffer.put((pixel shr 16 and 0xFF).toByte()) // R
            buffer.put((pixel shr 8 and 0xFF).toByte())  // G
            buffer.put((pixel and 0xFF).toByte())       // B
        }
        buffer.rewind()
        if (scaled !== bitmap) scaled.recycle()
        return buffer
    }

    override fun close() {
        runCatching { interpreter?.close() }
    }

    private fun loadModel(assetPath: String): MappedByteBuffer {
        val assetFd = context.assets.openFd(assetPath)
        val channel = FileInputStream(assetFd.fileDescriptor).channel
        val buffer = channel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFd.startOffset,
            assetFd.declaredLength,
        )
        channel.close()
        assetFd.close()
        return buffer
    }

    companion object {
        const val TAG = "EfficientDetDetector"
        const val DEFAULT_MODEL_ASSET = "efficientdet_lite0_detection.tflite"
        const val DEFAULT_INPUT_SIZE = 320
    }
}
