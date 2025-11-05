package com.driftdetector.app.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Privacy-first encryption manager using Android Keystore
 */
class EncryptionManager(private val context: Context) {

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "DriftDetectorMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_NAME = "drift_detector_secure_prefs"
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    val encryptedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        ensureKeyExists()
    }

    /**
     * Ensure encryption key exists in Keystore
     */
    private fun ensureKeyExists() {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    /**
     * Generate a new AES key in Android Keystore
     */
    private fun generateKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                KEYSTORE_PROVIDER
            )

            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate encryption key")
            throw SecurityException("Unable to generate encryption key", e)
        }
    }

    /**
     * Retrieve the secret key from Keystore
     */
    private fun getSecretKey(): SecretKey {
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypt data using AES-GCM
     */
    fun encrypt(data: ByteArray): EncryptedData {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())

            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(data)

            EncryptedData(encryptedBytes, iv)
        } catch (e: Exception) {
            Timber.e(e, "Encryption failed")
            throw SecurityException("Encryption failed", e)
        }
    }

    /**
     * Decrypt data using AES-GCM
     */
    fun decrypt(encryptedData: EncryptedData): ByteArray {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            cipher.doFinal(encryptedData.data)
        } catch (e: Exception) {
            Timber.e(e, "Decryption failed")
            throw SecurityException("Decryption failed", e)
        }
    }

    /**
     * Encrypt string data
     */
    fun encryptString(plainText: String): EncryptedData {
        return encrypt(plainText.toByteArray(Charsets.UTF_8))
    }

    /**
     * Decrypt string data
     */
    fun decryptString(encryptedData: EncryptedData): String {
        return decrypt(encryptedData).toString(Charsets.UTF_8)
    }

    /**
     * Securely wipe key from Keystore
     */
    fun deleteKey() {
        try {
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete encryption key")
        }
    }
}

/**
 * Container for encrypted data with IV
 */
data class EncryptedData(
    val data: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedData
        if (!data.contentEquals(other.data)) return false
        if (!iv.contentEquals(other.iv)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }

    fun toBase64(): String {
        return android.util.Base64.encodeToString(
            data + iv,
            android.util.Base64.NO_WRAP
        )
    }

    companion object {
        fun fromBase64(encoded: String): EncryptedData {
            val combined = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
            val ivSize = 12 // GCM IV size
            val data = combined.copyOfRange(0, combined.size - ivSize)
            val iv = combined.copyOfRange(combined.size - ivSize, combined.size)
            return EncryptedData(data, iv)
        }
    }
}
