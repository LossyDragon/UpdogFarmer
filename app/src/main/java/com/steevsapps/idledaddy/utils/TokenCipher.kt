package com.steevsapps.idledaddy.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the Steam refresh token before it is written to disk. The AES key is generated in
 * the Android Keystore and never leaves it, so the stored value is useless without this
 * device: a copy of the preferences file does not sign in.
 *
 * Stored form: `v1:` followed by Base64 of the 12-byte GCM nonce and the ciphertext.
 */
object TokenCipher {
    private const val TAG = "TokenCipher"

    private const val KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "idledaddy_refresh_token"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREFIX = "v1:"
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128

    /** True if [stored] is in the encrypted form rather than a token saved by an older build. */
    fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)

    /** The encrypted form of [token], or null if the Keystore refused (rare, broken devices). */
    fun encrypt(token: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val sealed = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(sealed, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Refresh token could not be encrypted", e)
            null
        }
    }

    /**
     * The token behind [stored]. A value without the prefix was saved before tokens were
     * encrypted and is returned as it is. Null if it cannot be decrypted, which happens when
     * the key is gone (app data moved to another device, say); the user then simply has to
     * sign in again.
     */
    fun decrypt(stored: String): String? {
        if (!isEncrypted(stored)) {
            return stored
        }
        return try {
            val sealed = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, sealed, 0, NONCE_BYTES))
            val plain = cipher.doFinal(sealed, NONCE_BYTES, sealed.size - NONCE_BYTES)
            String(plain, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "Stored refresh token could not be decrypted", e)
            null
        }
    }

    /** The AES key from the Keystore, created on first use. */
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }
}
