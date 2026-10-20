package org.dishavoice.app.ui

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Camera preview. The view is decorative for screen-reader users: the
 * information is delivered as speech, so it is labelled as such and hidden
 * from the accessibility tree's focus order.
 */
@Composable
fun PreviewSurface(previewView: PreviewView, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.semantics {
            contentDescription = "Camera preview. Detected objects are described by voice."
        },
        factory = { previewView },
    )
}
