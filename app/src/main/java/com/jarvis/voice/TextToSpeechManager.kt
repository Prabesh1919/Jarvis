package com.jarvis.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

sealed class TtsState {
    object Idle : TtsState()
    object Initializing : TtsState()
    object Ready : TtsState()
    object Speaking : TtsState()
    data class Error(val errorMsg: String) : TtsState()
}

/**
 * Manages native Text-to-Speech playback. 
 * Requests Audio Focus dynamically prior to output, ducking background media transiently.
 */
class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Initializing)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private var tts: TextToSpeech? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var onCompleteCallback: (() -> Unit)? = null

    // Audio focus listener. Instantly halts speaking if focus is permanently lost.
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            stopSpeaking()
        }
    }

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.UK) ?: tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                _ttsState.value = TtsState.Error("Default Language is not supported on this TTS engine.")
            } else {
                configureJarvisMaleVoice()
                _ttsState.value = TtsState.Ready
                setupProgressListener()
            }
        } else {
            _ttsState.value = TtsState.Error("Failed to initialize TextToSpeech engine.")
        }
    }

    /**
     * Configures pitch, speech rate, and selects a deep British/US Male voice matching JARVIS on Desktop.
     */
    private fun configureJarvisMaleVoice() {
        val ttsEngine = tts ?: return

        // Lower pitch (0.88f) for deep JARVIS resonance
        ttsEngine.setPitch(0.88f)
        // Crisp assistant speech speed (1.05f)
        ttsEngine.setSpeechRate(1.05f)

        try {
            val availableVoices = ttsEngine.voices
            if (!availableVoices.isNullOrEmpty()) {
                val maleVoice = availableVoices.firstOrNull { voice ->
                    val name = voice.name.lowercase(Locale.ROOT)
                    val lang = voice.locale.language.lowercase(Locale.ROOT)
                    val country = voice.locale.country.lowercase(Locale.ROOT)

                    (name.contains("male") || name.contains("rsk") || name.contains("gbb") || name.contains("iom")) &&
                    (lang == "en" && (country == "gb" || country == "us" || country == ""))
                } ?: availableVoices.firstOrNull { voice ->
                    voice.locale.language == "en" && voice.locale.country == "GB"
                } ?: availableVoices.firstOrNull { voice ->
                    voice.name.contains("male", ignoreCase = true)
                }

                if (maleVoice != null) {
                    ttsEngine.voice = maleVoice
                }
            }
        } catch (e: Exception) {
            // Fallback to pitch tuning
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _ttsState.value = TtsState.Speaking
            }

            override fun onDone(utteranceId: String?) {
                _ttsState.value = TtsState.Ready
                abandonAudioFocus()
                onCompleteCallback?.invoke()
            }

            override fun onError(utteranceId: String?) {
                _ttsState.value = TtsState.Error("Playback error occurred during TTS speech.")
                abandonAudioFocus()
            }
        })
    }

    /**
     * Synthesizes and plays the provided text. Requests system audio focus.
     */
    fun speak(text: String, onComplete: () -> Unit = {}) {
        if (_ttsState.value is TtsState.Error || tts == null) {
            onComplete()
            return
        }

        onCompleteCallback = onComplete
        
        val focusGranted = requestAudioFocus()
        if (focusGranted) {
            val utteranceId = UUID.randomUUID().toString()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            onComplete()
        }
    }

    /**
     * Instantly stops audio playback and releases audio focus.
     */
    fun stopSpeaking() {
        tts?.stop()
        abandonAudioFocus()
        _ttsState.value = TtsState.Ready
    }

    /**
     * Cleans up engine bindings and releases audio focus.
     */
    fun destroy() {
        tts?.shutdown()
        tts = null
        abandonAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                audioFocusRequest = null
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }
}
