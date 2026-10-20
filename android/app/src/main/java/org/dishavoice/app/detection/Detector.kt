package org.dishavoice.app.detection

import android.graphics.Bitmap
import org.dishavoice.core.Detection

/** On-device object detection source. Implementations must not persist frames. */
interface Detector : AutoCloseable {
    /** True when the model is loaded and detection can run. */
    val isReady: Boolean

    /** Square input resolution the model expects, in pixels. */
    val inputResolution: Int

    /** Detects objects in the given frame. The frame is not retained. */
    fun detect(bitmap: Bitmap): List<Detection>
}
