package com.steevsapps.idledaddy.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.InvalidAlgorithmParameterException
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.UnrecoverableEntryException
import java.security.cert.CertificateException
import java.util.Calendar
import java.util.GregorianCalendar
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.security.auth.x500.X500Principal

object CryptHelper {
    private val TAG: String = CryptHelper::class.java.getSimpleName()

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "IdleDaddy"
    private const val TYPE_RSA = "RSA"
    private const val CYPHER = "RSA/ECB/PKCS1Padding"

    fun encryptString(toEncrypt: String?): String? {
        if (toEncrypt.isNullOrEmpty()) return ""
        try {
            val privateKeyEntry = getPrivateKey()
            if (privateKeyEntry != null) {
                val publicKey = privateKeyEntry.certificate.publicKey

                // Encrypt the text
                val input = Cipher.getInstance(CYPHER)
                input.init(Cipher.ENCRYPT_MODE, publicKey)

                val outputStream = ByteArrayOutputStream()
                val cipherOutputStream = CipherOutputStream(outputStream, input)
                cipherOutputStream.write(toEncrypt.toByteArray(Charsets.UTF_8))
                cipherOutputStream.close()

                return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt string", e)
        }
        return ""
    }

    fun decryptString(encrypted: String?): String {
        if (encrypted.isNullOrEmpty()) return ""
        try {
            val privateKeyEntry = getPrivateKey()
            if (privateKeyEntry != null) {
                val privateKey = privateKeyEntry.privateKey

                val output = Cipher.getInstance(CYPHER)
                output.init(Cipher.DECRYPT_MODE, privateKey)

                val cipherInputStream = CipherInputStream(
                    ByteArrayInputStream(Base64.decode(encrypted, Base64.DEFAULT)), output
                )

                return cipherInputStream.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt string", e)
        }
        return ""
    }

    @Throws(
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        CertificateException::class,
        IOException::class,
        UnrecoverableEntryException::class
    )
    private fun getPrivateKey(): KeyStore.PrivateKeyEntry? {
        var ks = KeyStore.getInstance(KEYSTORE)

        // Weird artifact of Java API.  If you don't have an InputStream to load, you still need
        // to call "load", or it'll crash.
        ks.load(null)

        // Load the keypair from the Android Key Store
        var entry = ks.getEntry(ALIAS, null)

        if (entry == null) {
            Log.w(TAG, "No keys found under alias: $ALIAS")
            Log.w(TAG, "Generating new key...")
            try {
                createKeys()

                // Reload key store
                ks = KeyStore.getInstance(KEYSTORE)
                ks.load(null)

                entry = ks.getEntry(ALIAS, null)
                if (entry == null) {
                    Log.e(TAG, "Generating new key failed")
                    return null
                }
            } catch (e: NoSuchProviderException) {
                Log.e(TAG, "Generating new key failed", e)
                return null
            } catch (e: InvalidAlgorithmParameterException) {
                Log.e(TAG, "Generating new key failed", e)
                return null
            }
        }

        /* If entry is not a KeyStore.PrivateKeyEntry, it might have gotten stored in a previous
         * iteration of your application that was using some other mechanism, or been overwritten
         * by something else using the same keystore with the same alias.
         * You can determine the type using entry.getClass() and debug from there.
         */
        if (entry !is KeyStore.PrivateKeyEntry) {
            Log.w(TAG, "Not an instance of a PrivateKeyEntry")
            Log.w(TAG, "Exiting signData()...")
            return null
        }

        return entry
    }

    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidAlgorithmParameterException::class
    )
    private fun createKeys() {
        // Create a start and end time,
        // for the validity range of the key pair that's about to be generated.
        val start: Calendar = GregorianCalendar()
        val end: Calendar = GregorianCalendar()
        end.add(Calendar.YEAR, 25)

        val spec = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setCertificateSubject(X500Principal("CN=$ALIAS"))
            .setCertificateSerialNumber(BigInteger.valueOf(1337))
            .setCertificateNotBefore(start.time)
            .setCertificateNotAfter(end.time)
            .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            .build()

        // Initialize a KeyPair generator using the intended algorithm, in this example, RSA
        // and the KeyStore.  This example uses the AndroidKeyStore.
        val generator = KeyPairGenerator.getInstance(TYPE_RSA, KEYSTORE)
        generator.initialize(spec)

        val kp = generator.generateKeyPair()
        Log.i(TAG, "Public key is ${kp.public}")
    }
}
