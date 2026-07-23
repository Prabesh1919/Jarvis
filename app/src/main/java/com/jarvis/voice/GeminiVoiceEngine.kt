package com.jarvis.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Base64
import android.util.Log
import com.jarvis.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini Native Audio Voice Engine for JARVISH Android.
 * Uses the same Gemini Native Audio API as the desktop Mark-XXXIX-OR
 * with the "Charon" deep male voice preset to generate ultra-realistic AI speech.
 *
 * Fallback: If Gemini Audio fails (offline/no key), falls back to Android TTS.
 */
object GeminiVoiceEngine {

    private const val TAG = "GeminiVoiceEngine"
    private const val AUDIO_MODEL = "gemini-2.5-flash-preview-tts"
    private const val VOICE_NAME = "Charon"    // Same as desktop JARVIS
    private const val SAMPLE_RATE = 24000

    private val _voiceState = MutableStateFlow<VoiceEngineState>(VoiceEngineState.Idle)
    val voiceState: StateFlow<VoiceEngineState> = _voiceState.asStateFlow()

    private var audioTrack: AudioTrack? = null

    sealed class VoiceEngineState {
        object Idle : VoiceEngineState()
        object Generating : VoiceEngineState()
        object Speaking : VoiceEngineState()
        data class Error(val message: String) : VoiceEngineState()
    }

    /**
     * Synthesizes speech from text using Gemini's native audio generation API.
     * Returns true if audio was played successfully, false if fallback TTS should be used.
     */
    suspend fun speak(context: Context, text: String): Boolean = withContext(Dispatchers.IO) {
        val apiKey = AppConfig.getGeminiApiKey(context)
        if (apiKey.isNullOrBlank()) {
            Log.w(TAG, "No API key configured, falling back to TTS")
            return@withContext false
        }

        _voiceState.value = VoiceEngineState.Generating

        try {
            val audioBytes = requestGeminiAudio(apiKey, text)
            if (audioBytes != null && audioBytes.isNotEmpty()) {
                _voiceState.value = VoiceEngineState.Speaking
                playPcmAudio(audioBytes)
                _voiceState.value = VoiceEngineState.Idle
                return@withContext true
            } else {
                _voiceState.value = VoiceEngineState.Idle
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini Audio failed: ${e.message}")
            _voiceState.value = VoiceEngineState.Error(e.message ?: "Unknown error")
            return@withContext false
        }
    }

    /**
     * Calls Gemini generateContent API with responseModalities=["AUDIO"] and Charon voice.
     * Returns raw PCM audio bytes decoded from base64 response.
     */
    private fun requestGeminiAudio(apiKey: String, text: String): ByteArray? {
        val baseUrl = "https://generativelanguage.googleapis.com/v1beta"
        val spec = "$baseUrl/models/$AUDIO_MODEL:generateContent?key=$apiKey"
        val url = URL(spec)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 30000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            // Build request with audio output modality and Charon voice
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", text)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().apply { put("AUDIO") })
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", VOICE_NAME)
                            })
                        })
                    })
                })
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    val responseJson = JSONObject(response.toString())
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            val inlineData = parts.getJSONObject(0).optJSONObject("inlineData")
                            if (inlineData != null) {
                                val base64Audio = inlineData.getString("data")
                                Log.i(TAG, "✅ Received Gemini audio (${base64Audio.length} chars base64)")
                                return Base64.decode(base64Audio, Base64.DEFAULT)
                            }
                        }
                    }
                }
            } else {
                val errorStream = connection.errorStream
                val errorMsg = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream, "UTF-8")).use { it.readText() }
                } else "HTTP $responseCode"
                Log.w(TAG, "Gemini Audio API returned: $errorMsg")
            }
        } finally {
            connection.disconnect()
        }

        return null
    }

    /**
     * Plays raw PCM 16-bit mono audio at 24kHz through AudioTrack (same as desktop JARVIS).
     */
    private fun playPcmAudio(pcmData: ByteArray) {
        stopPlayback()

        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        val bufferSize = maxOf(minBufferSize, pcmData.size)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.apply {
            write(pcmData, 0, pcmData.size)
            play()

            // Block until playback finishes
            val durationMs = (pcmData.size.toLong() * 1000L) / (SAMPLE_RATE * 2L) // 16-bit = 2 bytes per sample
            Thread.sleep(durationMs + 200)

            stop()
            release()
        }
        audioTrack = null
    }

    fun stopPlayback() {
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) { }
        audioTrack = null
        _voiceState.value = VoiceEngineState.Idle
    }
}
