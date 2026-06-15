package com.jarvis.reliability

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted rolling logger using Android KeyStore AES-GCM encryption.
 * Saves system logs securely inside the app's internal filesystem sandbox.
 */
object EncryptedLogger {

    private const val KEY_ALIAS = "JarvisLoggerKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    init {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                 .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                 .build()

                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts a string log entry using the KeyStore AES key.
     * Returns a formatted Base64 payload containing: [IV] + ":" + [EncryptedBytes].
     */
    fun encrypt(text: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))

            val ivString = Base64.encodeToString(iv, Base64.NO_WRAP)
            val encryptedString = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            "$ivString:$encryptedString"
        } catch (e: Exception) {
            "ENCRYPTION_ERROR:${e.localizedMessage}"
        }
    }

    /**
     * Decrypts an encrypted Base64 payload.
     */
    fun decrypt(encryptedPayload: String): String {
        return try {
            val parts = encryptedPayload.split(":")
            if (parts.size != 2) return "DECRYPTION_ERROR:Invalid payload structure"

            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "DECRYPTION_ERROR:${e.localizedMessage}"
        }
    }

    /**
     * Appends an encrypted log message to a private file.
     */
    fun log(context: Context, level: String, message: String) {
        try {
            val logFile = File(context.filesDir, "jarvis_encrypted_logs.txt")
            val logEntry = "[${System.currentTimeMillis()}] [$level] $message"
            val encrypted = encrypt(logEntry)
            logFile.appendText("$encrypted\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reads and decrypts all logged lines from storage.
     */
    fun readLogs(context: Context): List<String> {
        val logFile = File(context.filesDir, "jarvis_encrypted_logs.txt")
        if (!logFile.exists()) return emptyList()

        return logFile.readLines().map { line ->
            if (line.startsWith("ENCRYPTION_ERROR")) line else decrypt(line)
        }
    }

    /**
     * Deletes log files exceeding 1MB.
     */
    fun rotateLogs(context: Context) {
        val logFile = File(context.filesDir, "jarvis_encrypted_logs.txt")
        if (logFile.exists() && logFile.length() > 1024 * 1024) { // 1MB limit
            logFile.delete()
        }
    }
}
