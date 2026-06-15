package com.jarvis.security

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

/**
 * Secure credential storage using EncryptedSharedPreferences backed by Android KeyStore.
 * All tokens, API keys, and sensitive credentials are encrypted at rest using AES-256-SIV.
 *
 * Includes robust recovery from corrupted Android Keystore entries (AEADBadTagException),
 * which is a known Android platform issue. If encryption is completely unrecoverable,
 * falls back to regular SharedPreferences so the app never crashes on startup.
 */
object VaultManager {

    private const val TAG = "VaultManager"
    private const val VAULT_FILE_NAME = "jarvis_secure_vault"
    private const val FALLBACK_PREFS_NAME = "jarvis_vault_fallback"
    private const val MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    @Volatile
    private var sharedPreferences: android.content.SharedPreferences? = null

    /**
     * Returns the singleton SharedPreferences instance (encrypted if possible).
     * Lazily initializes the master key and encrypted file on first access.
     *
     * Recovery strategy on AEADBadTagException / corrupted keystore:
     *   1. Delete the corrupted master key alias from the Android KeyStore.
     *   2. Delete the corrupted EncryptedSharedPreferences XML file.
     *   3. Create a fresh MasterKey + EncryptedSharedPreferences.
     *   4. If that still fails, fall back to regular (unencrypted) SharedPreferences
     *      so the app can at least start.
     */
    private fun getVault(context: Context): android.content.SharedPreferences {
        return sharedPreferences ?: synchronized(this) {
            sharedPreferences ?: run {
                val appContext = context.applicationContext
                val prefs = try {
                    createEncryptedPrefs(appContext)
                } catch (e: Exception) {
                    Log.w(TAG, "EncryptedSharedPreferences failed, attempting recovery…", e)
                    try {
                        nukeCorruptedVault(appContext)
                        createEncryptedPrefs(appContext)
                    } catch (e2: Exception) {
                        Log.e(TAG, "Recovery failed. Falling back to unencrypted prefs.", e2)
                        appContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
                    }
                }
                sharedPreferences = prefs
                prefs
            }
        }
    }

    /**
     * Creates a new MasterKey and EncryptedSharedPreferences.
     */
    private fun createEncryptedPrefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            VAULT_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Deletes the corrupted master key from the Android KeyStore
     * AND removes the EncryptedSharedPreferences XML file from disk.
     */
    private fun nukeCorruptedVault(context: Context) {
        // 1. Remove the master key alias from the Android KeyStore
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                keyStore.deleteEntry(MASTER_KEY_ALIAS)
                Log.i(TAG, "Deleted corrupted master key alias from KeyStore.")
            }
        } catch (ksException: Exception) {
            Log.e(TAG, "Could not delete master key alias from KeyStore.", ksException)
        }

        // 2. Delete the encrypted prefs XML file
        try {
            val prefsDir = java.io.File(context.applicationInfo.dataDir, "shared_prefs")
            val prefsFile = java.io.File(prefsDir, "$VAULT_FILE_NAME.xml")
            if (prefsFile.exists()) {
                prefsFile.delete()
                Log.i(TAG, "Deleted corrupted vault file: ${prefsFile.absolutePath}")
            }
        } catch (ioException: Exception) {
            Log.e(TAG, "Could not delete vault file.", ioException)
        }
    }

    /**
     * Saves an encrypted credential. Commonly used for API keys, tokens, etc.
     */
    fun saveCredential(context: Context, key: String, value: String) {
        getVault(context).edit().putString(key, value).apply()
    }

    /**
     * Retrieves a decrypted credential by key. Returns null if not found.
     */
    fun getCredential(context: Context, key: String): String? {
        return getVault(context).getString(key, null)
    }

    /**
     * Removes a stored credential by key.
     */
    fun deleteCredential(context: Context, key: String) {
        getVault(context).edit().remove(key).apply()
    }

    /**
     * Checks whether a credential exists without decrypting its value.
     */
    fun hasCredential(context: Context, key: String): Boolean {
        return getVault(context).contains(key)
    }

    /**
     * Returns all credential keys (names only, not values) stored in the vault.
     */
    fun listCredentialKeys(context: Context): Set<String> {
        return getVault(context).all.keys
    }

    /**
     * Wipes all stored credentials. Use with extreme caution.
     */
    fun clearVault(context: Context) {
        getVault(context).edit().clear().apply()
    }
}
