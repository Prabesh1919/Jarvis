package com.jarvis.brain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * OpenRouter Kotlin Client for Android JARVISH.
 * Enables routing heavy text, reasoning, and vision tasks through OpenRouter's free-tier model pool
 * with dynamic fallback and rate-limit cooldown management.
 */
object OpenRouterClient {

    private const val API_URL = "https://openrouter.ai/api/v1/chat/completions"
    private const val RATE_LIMIT_COOLDOWN_MS = 60_000L

    private val rateLimitedModels = ConcurrentHashMap<String, Long>()

    // Verified working OpenRouter free model pool
    private val DEFAULT_FREE_MODELS = mutableListOf(
        "meta-llama/llama-3.3-70b-instruct:free",
        "google/gemini-2.0-flash-exp:free",
        "google/gemma-2-9b-it:free",
        "qwen/qwen-2.5-72b-instruct:free",
        "deepseek/deepseek-r1:free",
        "meta-llama/llama-3.2-3b-instruct:free"
    )

    private fun isRateLimited(model: String): Boolean {
        val timestamp = rateLimitedModels[model] ?: return false
        if (System.currentTimeMillis() - timestamp > RATE_LIMIT_COOLDOWN_MS) {
            rateLimitedModels.remove(model)
            return false
        }
        return true
    }

    private fun markRateLimited(model: String) {
        rateLimitedModels[model] = System.currentTimeMillis()
    }

    suspend fun chat(
        apiKey: String,
        prompt: String,
        systemPrompt: String = "You are JARVISH, an elite AI assistant.",
        maxTokens: Int = 2048,
        temperature: Double = 0.7
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("OpenRouter API key is empty.")
        }

        var lastException: Exception? = null

        for (model in DEFAULT_FREE_MODELS) {
            if (isRateLimited(model)) continue

            try {
                val url = URL(API_URL)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("HTTP-Referer", "https://github.com/Prabesh1919/Jarvis")
                    setRequestProperty("X-Title", "JARVISH Android")
                }

                val payload = JSONObject().apply {
                    put("model", model)
                    put("max_tokens", maxTokens)
                    put("temperature", temperature)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    })
                }

                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(payload.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == 429) {
                    markRateLimited(model)
                    continue
                }

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream, "UTF-8")).use { reader ->
                        val sb = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line)
                        }
                        val jsonResp = JSONObject(sb.toString())
                        val choices = jsonResp.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val content = choices.getJSONObject(0)
                                .optJSONObject("message")
                                ?.optString("content", "") ?: ""
                            if (content.isNotBlank()) {
                                return@withContext content.trim()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
            }
        }

        throw IllegalStateException("All OpenRouter models failed or rate-limited. Last error: ${lastException?.message}")
    }
}
