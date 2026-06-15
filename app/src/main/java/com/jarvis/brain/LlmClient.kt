package com.jarvis.brain

import android.content.Context
import com.jarvis.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Lightweight native client to communicate with the Gemini Developer API.
 * Uses only standard HttpURLConnection and org.json parsing to avoid bulky external dependencies.
 */
object LlmClient {

    /**
     * Sends a text prompt to the configured Gemini model.
     * Returns the generated text response, or throws an exception on failure.
     */
    suspend fun generateContent(context: Context, prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = AppConfig.getGeminiApiKey(context)
            ?: throw IllegalStateException("Gemini API Key is not configured. Please set it first.")
        
        val baseUrl = AppConfig.getLlmBaseUrl(context)
        val model = AppConfig.getLlmModel(context)
        
        // Build Endpoint URL: https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=YOUR_KEY
        val spec = "$baseUrl/models/$model:generateContent?key=$apiKey"
        val url = URL(spec)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            // Construct JSON request body:
            // {
            //   "contents": [{
            //     "parts": [{"text": prompt}]
            //   }]
            // }
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            // Write output stream
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(requestJson.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read response stream
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }

                    // Parse JSON response: response.candidates[0].content.parts[0].text
                    val responseJson = JSONObject(response.toString())
                    val candidates = responseJson.getJSONArray("candidates")
                    if (candidates.length() > 0) {
                        val firstCandidate = candidates.getJSONObject(0)
                        val contentObj = firstCandidate.getJSONObject("content")
                        val parts = contentObj.getJSONArray("parts")
                        if (parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).getString("text")
                        }
                    }
                    throw IllegalStateException("Received empty content representation from Gemini.")
                }
            } else {
                // Read error stream
                val errorStream = connection.errorStream
                val errorResponse = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { reader ->
                        val response = java.lang.StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        response.toString()
                    }
                } else "No error description returned."
                throw RuntimeException("HTTP Request failed with response code $responseCode. Details: $errorResponse")
            }
        } finally {
            connection.disconnect()
        }
    }
}
