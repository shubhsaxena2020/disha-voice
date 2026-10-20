package org.dishavoice.app.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Converts a CameraX YUV frame into a Bitmap the detector can consume.
 *
 * The frame is converted and released immediately; nothing is written to disk
 * and no image is retained beyond the call.
 */
internal object YuvFrameConverter {

    private const val TAG = "YuvFrameConverter"

    /** Target size for the model input; the detector scales to its own size. */
    const val TARGET_WIDTH = 480
    const val TARGET_HEIGHT = 480

    fun toBitmap(image: ImageProxy): Bitmap? {
        return try {
            val planes = image.planes
            if (planes.size < 3) {
                Log.w(TAG, "Unexpected plane count: ${planes.size}")
                return null
            }
            val yBuffer = planes[0].buffer
            val uBuffer = planes[1].buffer
            val vBuffer = planes[2].buffer

            val yWidth = image.width
            val yHeight = image.height
            val ySize = yWidth * yHeight

            val yuv = ByteArray(ySize + 2 * (yWidth / 2) * (yHeight / 2))
            yBuffer.get(yuv, 0, ySize)
            val chromaWidth = yWidth / 2
            val chromaHeight = yHeight / 2
            val chromaSize = chromaWidth * chromaHeight
            uBuffer.get(yuv, ySize, chromaSize)
            vBuffer.get(yuv, ySize + chromaSize, chromaSize)

            val bitmap = Bitmap.createBitmap(yWidth, yHeight, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(ySize)
            for (j in 0 until yHeight) {
                for (i in 0 until yWidth) {
                    val yIndex = j * yWidth + i
                    val y = yuv[yIndex].toInt() and 0xFF
                    val uvIndex = ySize + (j / 2) * chromaWidth + (i / 2)
                    val u = yuv[uvIndex].toInt() and 0xFF
                    val v = yuv[uvIndex + chromaSize].toInt() and 0xFF
                    pixels[yIndex] = yuvToArgb(y, u, v)
                }
            }
            bitmap.setPixels(pixels, 0, yWidth, 0, 0, yWidth, yHeight)

            val scaled = Bitmap.createScaledBitmap(bitmap, TARGET_WIDTH, TARGET_HEIGHT, true)
            if (scaled !== bitmap) bitmap.recycle()
            scaled
        } catch (t: Throwable) {
            Log.e(TAG, "Frame conversion failed", t)
            null
        }
    }

    private fun yuvToArgb(y: Int, u: Int, v: Int): Int {
        val c = y - 16
        val d = u - 128
        val e = v - 128
        val r = (298 * c + 409 * e + 128) shr 8
        val g = (298 * c - 100 * d - 208 * e + 128) shr 8
        val b = (298 * c + 516 * d + 128) shr 8
        val rr = r.coerceIn(0, 255)
        val gg = g.coerceIn(0, 255)
        val bb = b.coerceIn(0, 255)
        return (0xFF shl 24) or (rr shl 16) or (gg shl 8) or bb
    }
}
