package com.steevsapps.idledaddy.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented because TokenCipher relies on the real AndroidKeyStore provider,
 * which isn't available in a plain JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
class TokenCipherTest {
    @Test
    fun encrypt_thenDecrypt_returnsOriginalToken() {
        // Longer than the old RSA helper could handle, like a real refresh token
        val original = "eyJ0eXAiOiJKV1QiLCJhbGciOiJFZERTQSJ9.".repeat(20)
        val encrypted = TokenCipher.encrypt(original)!!
        assertTrue(TokenCipher.isEncrypted(encrypted))
        assertNotEquals(original, encrypted)
        assertEquals(original, TokenCipher.decrypt(encrypted))
    }

    @Test
    fun decrypt_plainTokenFromOlderBuild_returnsItUnchanged() {
        assertEquals("plain-token", TokenCipher.decrypt("plain-token"))
    }

    @Test
    fun decrypt_garbage_returnsNull() {
        assertNull(TokenCipher.decrypt("v1:not-valid-base64-ciphertext"))
    }
}
