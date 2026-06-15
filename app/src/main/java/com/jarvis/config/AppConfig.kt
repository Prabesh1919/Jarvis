package com.jarvis.config

import android.content.Context
import android.util.Log
import com.jarvis.security.VaultManager

/**
 * Central configuration manager for the Jarvis assistant.
 * Reads API keys from the secure VaultManager and provides
 * a simple interface for all modules to access configuration.
 *
 * Usage:
 *   1. On first launch, call AppConfig.initializeKeys(context, geminiApiKey)
 *      to store the API key securely in the encrypted vault.
 *   2. After that, call AppConfig.getGeminiApiKey(context) from anywhere.
 */
object AppConfig {

    private const val TAG = "AppConfig"
    private const val KEY_GEMINI_API = "gemini_api_key"
    private const val KEY_LLM_BASE_URL = "llm_base_url"
    private const val KEY_LLM_MODEL = "llm_model"

    // --- API Key Management ---

    /**
     * Stores the Gemini API key securely in the encrypted vault.
     * Call this once during initial setup or settings screen.
     */
    fun setGeminiApiKey(context: Context, apiKey: String) {
        VaultManager.saveCredential(context, KEY_GEMINI_API, apiKey)
    }

    /**
     * Retrieves the stored Gemini API key from the encrypted vault.
     * Falls back to BuildConfig if no key has been manually configured
     * or if the vault is inaccessible.
     */
    fun getGeminiApiKey(context: Context): String? {
        try {
            val vaultKey = VaultManager.getCredential(context, KEY_GEMINI_API)
            if (!vaultKey.isNullOrBlank()) return vaultKey
        } catch (e: Exception) {
            Log.w(TAG, "Could not read API key from vault, falling back to BuildConfig.", e)
        }
        
        val buildKey = com.jarvis.BuildConfig.GEMINI_API_KEY
        return if (buildKey.isNotBlank()) buildKey else null
    }

    /**
     * Checks whether the Gemini API key has been configured in either Vault or BuildConfig.
     */
    fun isApiKeyConfigured(context: Context): Boolean {
        return !getGeminiApiKey(context).isNullOrBlank()
    }

    // --- Optional LLM Endpoint Config ---

    fun setLlmBaseUrl(context: Context, url: String) {
        VaultManager.saveCredential(context, KEY_LLM_BASE_URL, url)
    }

    fun getLlmBaseUrl(context: Context): String {
        return try {
            VaultManager.getCredential(context, KEY_LLM_BASE_URL)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read LLM base URL from vault.", e)
            null
        } ?: "https://generativelanguage.googleapis.com/v1beta"
    }

    fun setLlmModel(context: Context, model: String) {
        VaultManager.saveCredential(context, KEY_LLM_MODEL, model)
    }

    fun getLlmModel(context: Context): String {
        return try {
            VaultManager.getCredential(context, KEY_LLM_MODEL)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read LLM model from vault.", e)
            null
        } ?: "gemini-2.5-flash"
    }
}
