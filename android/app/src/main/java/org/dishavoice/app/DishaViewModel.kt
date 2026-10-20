package org.dishavoice.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.dishavoice.app.camera.YuvFrameConverter
import org.dishavoice.app.detection.Detector
import org.dishavoice.app.detection.EfficientDetDetector
import org.dishavoice.app.speech.Phrasebook
import org.dishavoice.app.speech.Speaker
import org.dishavoice.core.AnnouncementPolicy
import org.dishavoice.core.Decision
import org.dishavoice.core.Detection

/** User-visible session state. */
enum class SessionState { IDLE, STARTING, RUNNING, PAUSED, ERROR }

data class UiState(
    val sessionState: SessionState = SessionState.IDLE,
    val statusText: String = "",
    val lastAnnouncement: String = "",
    val isHindiAvailable: Boolean = true,
    val permissionRequired: Boolean = false,
    val modelReady: Boolean = false,
)

/**
 * Owns the camera session, the detector, and the announcement policy.
 *
 * The domain policy (:core) is the single place that decides what may be
 * announced; this class only supplies it with detections and speaks the
 * resulting phrase.
 */
class DishaViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var detector: Detector? = null
    private var speaker: Speaker? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var analysis: ImageAnalysis? = null

    private val policy = AnnouncementPolicy()
    private var phrasebook = Phrasebook(hindi = true)
    private var lastSpoken = ""

    /** Detector is created lazily so model loading never blocks app start. */
    private fun ensureDetector(): Detector {
        detector?.let { return it }
        val created = EfficientDetDetector(getApplication())
        detector = created
        _uiState.value = _uiState.value.copy(modelReady = created.isReady)
        return created
    }

    private fun ensureSpeaker() {
        if (speaker != null) return
        val created = Speaker(getApplication())
        speaker = created
        created.whenReady { state ->
            val hindi = state == Speaker.State.READY && created.isHindiAvailable
            phrasebook = Phrasebook(hindi = hindi)
            _uiState.value = _uiState.value.copy(isHindiAvailable = hindi)
            if (state == Speaker.State.READY && !hindi) {
                speak(created, phrasebook.speechUnavailable())
            }
        }
    }

    private fun speak(target: Speaker, text: String) {
        lastSpoken = text
        _uiState.value = _uiState.value.copy(lastAnnouncement = text)
        target.speak(text)
    }

    /** Binds the camera to a lifecycle owner. Safe to call repeatedly. */
    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewSurfaceProvider: androidx.camera.core.Preview.SurfaceProvider,
    ) {
        val context = getApplication<Application>()
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                cameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewSurfaceProvider
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    // Bounded backpressure: only the newest frame is analysed, so
                    // slow inference cannot build an unbounded backlog.
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                analysis = imageAnalysis

                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    sessionState = SessionState.ERROR,
                    statusText = "Camera could not start",
                )
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /** Starts detection and announcements. */
    fun start() {
        val app = getApplication<Application>()
        _uiState.value = _uiState.value.copy(sessionState = SessionState.STARTING, statusText = "Starting")
        ensureSpeaker()
        val det = ensureDetector()
        if (!det.isReady) {
            _uiState.value = _uiState.value.copy(
                sessionState = SessionState.ERROR,
                statusText = "Model unavailable",
            )
            speaker?.let { speak(it, phrasebook.modelUnavailable()) }
            return
        }
        _uiState.value = _uiState.value.copy(sessionState = SessionState.RUNNING, statusText = "Listening")
        speaker?.let { speak(it, phrasebook.started()) }
        startAnalysis(det)
    }

    private fun startAnalysis(det: Detector) {
        val imageAnalysis = analysis ?: return
        imageAnalysis.clearAnalyzer()
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(getApplication()),
        ) { proxy -> analyze(det, proxy) }
    }

    private fun analyze(det: Detector, proxy: androidx.camera.core.ImageProxy) {
        try {
            val bitmap = YuvFrameConverter.toBitmap(proxy)
            if (bitmap != null) {
                viewModelScope.launch(Dispatchers.Default) {
                    val detections = withContext(Dispatchers.Default) { det.detect(bitmap) }
                    bitmap.recycle()
                    handleDetections(detections)
                }
            }
        } finally {
            // Always release the frame: this is what keeps memory bounded.
            proxy.close()
        }
    }

    private fun handleDetections(detections: List<Detection>) {
        if (_uiState.value.sessionState != SessionState.RUNNING) return
        val now = android.os.SystemClock.elapsedRealtime()
        val decisions = detections.map { policy.decide(it, now) }
        val announce = decisions.filterIsInstance<Decision.Announce>()

        val chosen = announce.maxByOrNull { it.confidenceBand.ordinal * 1000 + it.zone.ordinal }
        if (chosen != null) {
            speaker?.let { speak(it, phrasebook.objectFound(chosen.label, chosen.zone, chosen.confidenceBand)) }
            return
        }
        if (decisions.isEmpty() || decisions.all { it is Decision.SuppressedLowConfidence }) {
            // Announce "not recognised" occasionally rather than on every frame.
            val lastEmpty = _uiState.value.lastAnnouncement
            if (lastEmpty != phrasebook.nothingDetected()) {
                speaker?.let { speak(it, phrasebook.nothingDetected()) }
            }
        }
    }

    /** Stops detection and silences any in-flight speech. */
    fun stop() {
        analysis?.clearAnalyzer()
        _uiState.value = _uiState.value.copy(sessionState = SessionState.IDLE, statusText = "Stopped")
        speaker?.let {
            it.stop()
            speak(it, phrasebook.stopped())
        }
    }

    /** Repeats the most recent spoken message. */
    fun repeatLast() {
        val target = speaker ?: return
        if (lastSpoken.isNotEmpty()) {
            target.speak(phrasebook.repeat())
            target.speak(lastSpoken)
        } else {
            target.speak(phrasebook.help())
        }
    }

    /** Speaks the short help message. */
    fun speakHelp() {
        speaker?.let { speak(it, phrasebook.help()) }
    }

    fun onPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            sessionState = SessionState.ERROR,
            statusText = "Camera permission needed",
            permissionRequired = true,
        )
        speaker?.let { speak(it, phrasebook.permissionNeeded()) }
    }

    override fun onCleared() {
        analysis?.clearAnalyzer()
        cameraProvider?.unbindAll()
        detector?.close()
        speaker?.close()
        super.onCleared()
    }
}
