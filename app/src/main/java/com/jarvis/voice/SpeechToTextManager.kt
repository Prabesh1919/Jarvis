package com.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale

sealed class SpeechState {
    object Idle : SpeechState()
    object Listening : SpeechState()
    data class PartialResults(val text: String) : SpeechState()
    data class Results(val text: String) : SpeechState()
    data class Error(val errorMsg: String) : SpeechState()
}

/**
 * Manages native Speech-to-Text conversion using a walkie-talkie model.
 *
 * Press-and-hold to listen; release to finalize. Guards against the common
 * ERROR_CLIENT crash caused by calling startListening() while the recognizer
 * is still active or being torn down.
 */
class SpeechToTextManager(private val context: Context) {

    companion object {
        private const val TAG = "SpeechToTextManager"
    }

    private val _speechState = MutableStateFlow<SpeechState>(SpeechState.Idle)
    val speechState: StateFlow<SpeechState> = _speechState.asStateFlow()

    /** Tracks the latest partial text so we can emit it as Results when the user releases. */
    private var latestPartialText: String = ""

    private var speechRecognizer: SpeechRecognizer? = null

    /** Guard flag — prevents double-start and start-during-teardown races. */
    @Volatile
    private var isActive = false

    // ── Public API ──────────────────────────────────────────────

    /**
     * Starts the recognizer (walkie-talkie: "button down").
     * Safe to call repeatedly — second calls are no-ops while already listening.
     */
    suspend fun startListening() = withContext(Dispatchers.Main) {
        if (isActive) {
            Log.w(TAG, "startListening() called while already active — ignoring.")
            return@withContext
        }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _speechState.value = SpeechState.Error("Speech recognition is not available on this device.")
            return@withContext
        }

        // Tear down any leftover recognizer from a previous session
        destroyRecognizerSync()
        // Small yield so the OS fully releases the mic & recognizer resources
        delay(150)

        isActive = true
        latestPartialText = ""

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
            speechRecognizer = it
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Keep listening for a longer silence window (walkie-talkie pattern)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000L)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                _speechState.value = SpeechState.Listening
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) { /* used for UI amplitude later */ }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                // Don't go Idle yet — wait for onResults/onError to fire.
            }

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mic permission required"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Try speaking again"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Mic is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Recognition service lost"
                    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests"
                    else -> "Error: $error"
                }
                Log.w(TAG, "onError: $error → $message")

                // For transient errors, just reset to Idle so the user can try again immediately.
                if (error == SpeechRecognizer.ERROR_CLIENT ||
                    error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                    error == SpeechRecognizer.ERROR_NO_MATCH ||
                    error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                    error == SpeechRecognizer.ERROR_SERVER_DISCONNECTED ||
                    error == 13 // Service busy/unavailable
                ) {
                    Log.i(TAG, "Transient error $error - resetting recognizer")
                    isActive = false
                    destroyRecognizerSync() // Force a fresh start for next time
                    _speechState.value = SpeechState.Idle
                } else {
                    isActive = false
                    _speechState.value = SpeechState.Error(message)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull() ?: latestPartialText
                Log.d(TAG, "onResults: \"$text\"")
                isActive = false
                if (text.isNotBlank()) {
                    _speechState.value = SpeechState.Results(text)
                } else {
                    _speechState.value = SpeechState.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    latestPartialText = text
                    // Emit partial as a transient state so the UI can display live text
                    // without triggering the full STT -> LLM -> TTS pipeline.
                    _speechState.value = SpeechState.PartialResults(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startListening threw", e)
            isActive = false
            _speechState.value = SpeechState.Idle
        }
    }

    /**
     * Stops the recognizer (walkie-talkie: "button up").
     * The recognizer will fire onResults or onError after this.
     */
    suspend fun stopListening() = withContext(Dispatchers.Main) {
        if (!isActive) return@withContext
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "stopListening threw", e)
            isActive = false
            _speechState.value = SpeechState.Idle
        }
    }

    /**
     * Cancels active recognition immediately without waiting for results.
     */
    suspend fun cancel() = withContext(Dispatchers.Main) {
        try {
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        isActive = false
        _speechState.value = SpeechState.Idle
    }

    /** Resets state back to Idle (e.g., after processing the Results). */
    fun resetState() {
        _speechState.value = SpeechState.Idle
    }

    /**
     * Full teardown — call from Activity.onDestroy().
     */
    suspend fun destroy() = withContext(Dispatchers.Main) {
        destroyRecognizerSync()
        isActive = false
    }

    // ── Internal ────────────────────────────────────────────────

    private fun destroyRecognizerSync() {
        speechRecognizer?.let {
            try {
                it.cancel()
                it.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "destroyRecognizerSync threw", e)
            }
            speechRecognizer = null
        }
    }
}
