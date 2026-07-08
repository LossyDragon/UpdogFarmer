package com.steevsapps.idledaddy.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented because CryptHelper relies on the real AndroidKeyStore provider,
 * which isn't available in a plain JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
class CryptHelperTest {
    @Test
    fun encryptString_emptyInput_returnsEmptyString() {
        assertEquals("", CryptHelper.encryptString(""))
    }

    @Test
    fun encryptString_nullInput_returnsEmptyString() {
        assertEquals("", CryptHelper.encryptString(null))
    }

    @Test
    fun decryptString_emptyInput_returnsEmptyString() {
        assertEquals("", CryptHelper.decryptString(""))
    }

    @Test
    fun encryptString_thenDecryptString_returnsOriginalText() {
        val original = "correct horse battery staple"
        val encrypted = CryptHelper.encryptString(original)
        assertEquals(original, CryptHelper.decryptString(encrypted))
    }
}
