package org.dishavoice.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.dishavoice.core.Detection
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.InterpreterApi
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

    /**
     * The output layout is resolved once, at construction, and a mismatch is
     * surfaced rather than swallowed. Resolving it per frame meant a layout
     * problem could only fail inside the per-frame error handler, where it
     * became an empty detection list: the camera would run and the app would
     * announce nothing, with no error anywhere. Failing here makes the
     * detector report itself as not ready, and the UI then says the model is
     * unavailable.
     */
    private val outputIndices: OutputIndices? = interpreter?.let { tf ->
        runCatching { resolveOutputIndices(tf) }
            .onFailure { Log.e(TAG, "Unexpected model output layout", it) }
            .getOrNull()
    }

    override val isReady: Boolean get() = interpreter != null && outputIndices != null

    override val inputResolution: Int get() = inputSize

    override fun detect(bitmap: Bitmap): List<Detection> {
        val tf = interpreter ?: return emptyList()
        val indices = outputIndices ?: return emptyList()
        return runCatching {
            val input = buildInputBuffer(bitmap, inputSize)

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
     * Output order for this model is boxes, classes, scores, count.
     *
     * The resolution is delegated to [OutputTensorResolver], which uses shape
     * and position only. An earlier version of this file separated the
     * class-id and score tensors by inspecting their *values*, which cannot
     * work: on a freshly allocated interpreter every output tensor reads as
     * zero, so the class-id tensor also looked like a score tensor, the
     * resolution failed, and the detector returned an empty list on every
     * frame - silently, with the camera running and nothing ever announced.
     * See `RESEARCH/platform-api-audit.md` section 2.
     */
    private fun resolveOutputIndices(tf: InterpreterApi): OutputIndices {
        val shapes = (0 until tf.outputTensorCount).map { tf.getOutputTensor(it).shape() }
        val resolved = OutputTensorResolver.resolve(shapes)
        return OutputIndices(
            boxes = resolved.boxes,
            classes = resolved.classes,
            scores = resolved.scores,
            count = resolved.count,
        )
    }

    /** Builds the uint8 RGB input tensor expected by this model. */
    private fun buildInputBuffer(bitmap: Bitmap, size: Int): ByteBuffer =
        InputTensorBuilder.buildFromBitmap(bitmap, size)

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
