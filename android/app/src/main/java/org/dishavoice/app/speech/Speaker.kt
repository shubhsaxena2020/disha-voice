package org.dishavoice.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Thin wrapper over Android TextToSpeech.
 *
 * Hindi is preferred but never assumed: if the device has no usable Hindi
 * voice the speaker falls back to English and reports that through
 * [isHindiAvailable] so the UI can tell the user plainly.
 */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    /** Result of the asynchronous TTS initialisation. */
    enum class State { UNINITIALISED, READY, FAILED }

    @Volatile
    var state: State = State.UNINITIALISED
        private set

    /** True when a Hindi voice is actually usable on this device. */
    @Volatile
    var isHindiAvailable: Boolean = false
        private set

    private var tts: TextToSpeech? = null
    private var pending: ((State) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        state = if (status == TextToSpeech.SUCCESS) State.READY else State.FAILED
        if (state == State.READY) {
            isHindiAvailable = configureLocale(Locale("hi", "IN"))
        }
        pending?.invoke(state)
        pending = null
    }

    /**
     * Selects the best available voice for the requested locale.
     * @return true when the locale is usable.
     */
    private fun configureLocale(locale: Locale): Boolean {
        val engine = tts ?: return false
        return when (engine.isLanguageAvailable(locale)) {
            TextToSpeech.LANG_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_AVAILABLE,
            TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE,
            -> {
                engine.language = locale
                true
            }
            else -> {
                // Fall back to English so the app still speaks.
                engine.language = Locale.US
                false
            }
        }
    }

    /** Runs [block] once the engine has initialised. */
    fun whenReady(block: (State) -> Unit) {
        if (state != State.UNINITIALISED) {
            block(state)
        } else {
            pending = block
        }
    }

    /**
     * Speaks the text, replacing anything queued. Used for announcements the
     * user asked for, where stale speech is worse than interruption.
     */
    fun speak(text: String, flush: Boolean = true) {
        val engine = tts ?: return
        if (state != State.READY) return
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        engine.speak(text, mode, null, "disha-${System.nanoTime()}")
    }

    /** Stops any speech immediately, e.g. when the user stops the camera. */
    fun stop() {
        runCatching { tts?.stop() }
    }

    fun close() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        state = State.UNINITIALISED
    }

    companion object {
        private const val TAG = "DishaSpeaker"
    }
}
