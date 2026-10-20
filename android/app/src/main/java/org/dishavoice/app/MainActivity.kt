package org.dishavoice.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import org.dishavoice.app.ui.DishaScreen
import org.dishavoice.app.ui.LicencesScreen

class MainActivity : ComponentActivity() {

    private val viewModel: DishaViewModel by viewModels()

    /**
     * The preview view is created here (not inside Compose) so the camera can be
     * bound to it as soon as the activity is ready, without a timing race.
     */
    private val previewView: PreviewView by lazy {
        PreviewView(this).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                bindCamera()
            } else {
                viewModel.onPermissionDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showLicences by rememberSaveable { mutableStateOf(false) }
            if (showLicences) {
                LicencesScreen(onBack = { showLicences = false })
            } else {
                DishaScreen(
                    viewModel = viewModel,
                    previewView = previewView,
                    onShowLicences = { showLicences = true },
                )
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            bindCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun bindCamera() {
        viewModel.bindCamera(this, previewView.surfaceProvider)
    }
}
