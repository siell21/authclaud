package siell.claud.authenticator.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256
    private const val SALT = "CLOUD_AUTH_STATIC_SALT_2026"

    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val LOCAL_KEY_ALIAS = "CloudAuthLocalKey"

    private fun getLocalKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        keyStore.getKey(LOCAL_KEY_ALIAS, null)?.let {
            return it as SecretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            LOCAL_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build()
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    fun encryptLocal(plainText: String): String {
        try {
            val secretKey = getLocalKey()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + cipherText
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return plainText
        }
    }

    fun decryptLocal(encryptedText: String): String {
        try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12) // GCM IV is typically 12 bytes
            val cipherText = combined.copyOfRange(12, combined.size)
            
            val secretKey = getLocalKey()
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainText = cipher.doFinal(cipherText)
            return String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return encryptedText
        }
    }

    private fun generateKey(userId: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec = PBEKeySpec(userId.toCharArray(), SALT.toByteArray(), ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encrypt(plainText: String, userId: String): String {
        try {
            val secretKey = generateKey(userId)
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = iv + cipherText
            return Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            return plainText
        }
    }

    fun decrypt(encryptedText: String, userId: String): String {
        try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, 12)
            val cipherText = combined.copyOfRange(12, combined.size)
            
            val secretKey = generateKey(userId)
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val plainText = cipher.doFinal(cipherText)
            return String(plainText, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return encryptedText
        }
    }
}
