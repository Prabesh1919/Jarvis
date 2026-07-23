package com.jarvis.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
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
 * Manages native Text-to-Speech playback with ultra-high quality Neural/HD British & US Male Voice Profiles.
 * Automatically selects non-robotic, high-bitrate voices and handles audio focus ducking.
 */
class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TextToSpeechManager"
    }

    private val _ttsState = MutableStateFlow<TtsState>(TtsState.Initializing)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private var tts: TextToSpeech? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var onCompleteCallback: (() -> Unit)? = null

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
                _ttsState.value = TtsState.Error("English Language is not supported on this TTS engine.")
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
     * Selects the highest quality Neural/HD Male Voice available on the system.
     * Prefers British English (en_GB) HD male voices for JARVIS style, then US English (en_US) HD male.
     */
    private fun configureJarvisMaleVoice() {
        val ttsEngine = tts ?: return

        // Set natural pitch (0.96f) and rate (1.0f) to avoid robotic distortion
        ttsEngine.setPitch(0.96f)
        ttsEngine.setSpeechRate(1.0f)

        try {
            val availableVoices = ttsEngine.voices
            if (!availableVoices.isNullOrEmpty()) {
                // Filter out uninstalled or low-quality voices
                val highQualityVoices = availableVoices.filter { voice ->
                    !voice.isNetworkConnectionRequired &&
                    voice.quality >= Voice.QUALITY_HIGH &&
                    voice.locale.language.equals("en", ignoreCase = true)
                }.ifEmpty { availableVoices.filter { it.locale.language.equals("en", ignoreCase = true) } }

                // Priority 1: High-Quality British (en-GB) Male Voices
                var selectedVoice = highQualityVoices.firstOrNull { v ->
                    val name = v.name.lowercase(Locale.ROOT)
                    v.locale.country.equals("GB", ignoreCase = true) &&
                    (name.contains("male") || name.contains("rsk") || name.contains("gbb") || name.contains("fis") || name.contains("network") || name.contains("local"))
                }

                // Priority 2: High-Quality US (en-US) Male Voices
                if (selectedVoice == null) {
                    selectedVoice = highQualityVoices.firstOrNull { v ->
                        val name = v.name.lowercase(Locale.ROOT)
                        v.locale.country.equals("US", ignoreCase = true) &&
                        (name.contains("male") || name.contains("iom") || name.contains("sfg") || name.contains("tpf") || name.contains("network") || name.contains("local"))
                    }
                }

                // Priority 3: Any English Male Voice
                if (selectedVoice == null) {
                    selectedVoice = availableVoices.firstOrNull { v ->
                        v.name.contains("male", ignoreCase = true) && v.locale.language.equals("en", ignoreCase = true)
                    }
                }

                if (selectedVoice != null) {
                    ttsEngine.voice = selectedVoice
                    Log.i(TAG, "🔊 Selected HD JARVISH Voice: ${selectedVoice.name} (Quality: ${selectedVoice.quality})")
                } else {
                    Log.w(TAG, "No HD male voice found; using system default English voice.")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error configuring voice profile: ${e.message}")
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

    fun speak(text: String, onComplete: () -> Unit = {}): Boolean {
        if (_ttsState.value is TtsState.Error || tts == null) {
            onComplete()
            return false
        }

        onCompleteCallback = onComplete

        val focusGranted = requestAudioFocus()
        if (focusGranted) {
            val utteranceId = UUID.randomUUID().toString()
            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            return result == TextToSpeech.SUCCESS
        } else {
            onComplete()
            return false
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        abandonAudioFocus()
        _ttsState.value = TtsState.Ready
    }

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
