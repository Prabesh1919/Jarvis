package com.jarvis

import android.app.Application
import android.util.Log
import com.jarvis.config.AppConfig
import com.jarvis.context.ContextEngine
import com.jarvis.memory.MemoryManager
import com.jarvis.reliability.MaintenanceWorker

/**
 * Main Application class for initializing databases, context monitors,
 * and loading compile-time configuration parameters.
 */
class JarvisApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize databases and memory systems
        MemoryManager.initialize(this)

        // 2. Start tracking network, battery, and location context
        ContextEngine.startMonitoring(this)

        // 3. Load compile-time BuildConfig environment variables into secure vault
        try {
            syncEnvironmentKeys()
        } catch (e: Exception) {
            Log.e("JarvisApplication", "Failed to sync environment keys to vault. " +
                    "API key will be read from BuildConfig at runtime.", e)
        }

        // 4. Schedule periodic database maintenance worker
        MaintenanceWorker.schedule(this)
    }

    /**
     * Checks for compile-time configurations defined in .env and synchronizes them
     * with the secure KeyStore-backed VaultManager if no key is saved yet.
     */
    private fun syncEnvironmentKeys() {
        // Only set from BuildConfig if key is not already configured in Vault
        if (!AppConfig.isApiKeyConfigured(this)) {
            if (BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "your_gemini_api_key_here") {
                AppConfig.setGeminiApiKey(this, BuildConfig.GEMINI_API_KEY)
            }
        }

        if (BuildConfig.LLM_BASE_URL.isNotBlank()) {
            AppConfig.setLlmBaseUrl(this, BuildConfig.LLM_BASE_URL)
        }
        if (BuildConfig.LLM_MODEL.isNotBlank()) {
            AppConfig.setLlmModel(this, BuildConfig.LLM_MODEL)
        }
    }
}
