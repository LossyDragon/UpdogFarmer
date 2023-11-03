package com.steevsapps.idledaddy.utils

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.math.BigInteger
import java.nio.charset.Charset
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
    private val TAG = CryptHelper::class.java.getSimpleName()

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "IdleDaddy"
    private const val TYPE_RSA = "RSA"
    private const val CYPHER = "RSA/ECB/PKCS1Padding"
    private const val ENCODING = "UTF-8"

    @JvmStatic
    fun encryptString(context: Context, toEncrypt: String): String {
        if (TextUtils.isEmpty(toEncrypt)) {
            return ""
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val privateKeyEntry = getPrivateKey(context)

                if (privateKeyEntry != null) {
                    val publicKey = privateKeyEntry.certificate.publicKey

                    // Encrypt the text
                    val input = Cipher.getInstance(CYPHER)
                    input.init(Cipher.ENCRYPT_MODE, publicKey)

                    val outputStream = ByteArrayOutputStream()
                    val cipherOutputStream = CipherOutputStream(outputStream, input)
                    cipherOutputStream.write(toEncrypt.toByteArray(Charset.forName(ENCODING)))
                    cipherOutputStream.close()

                    return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                }
            } else {
                return Base64.encodeToString(
                    toEncrypt.toByteArray(Charset.forName(ENCODING)),
                    Base64.DEFAULT
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt string", e)
        }

        return ""
    }

    @JvmStatic
    fun decryptString(context: Context, encrypted: String?): String {
        if (TextUtils.isEmpty(encrypted)) {
            return ""
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val privateKeyEntry = getPrivateKey(context)

                if (privateKeyEntry != null) {
                    val privateKey = privateKeyEntry.privateKey

                    val output = Cipher.getInstance(CYPHER)
                    output.init(Cipher.DECRYPT_MODE, privateKey)

                    val cipherInputStream = CipherInputStream(
                        ByteArrayInputStream(Base64.decode(encrypted, Base64.DEFAULT)), output
                    )

                    val values: MutableList<Byte> = ArrayList()
                    var nextByte: Int
                    while (cipherInputStream.read().also { nextByte = it } != -1) {
                        values.add(nextByte.toByte())
                    }

                    val bytes = ByteArray(values.size)
                    for (i in bytes.indices) {
                        bytes[i] = values[i]
                    }

                    return String(bytes, Charset.forName(ENCODING))
                }
            } else {
                return String(Base64.decode(encrypted, Base64.DEFAULT), Charset.forName(ENCODING))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt string", e)
        }

        return ""
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Throws(
        KeyStoreException::class,
        NoSuchAlgorithmException::class,
        CertificateException::class,
        IOException::class,
        UnrecoverableEntryException::class
    )
    private fun getPrivateKey(context: Context): KeyStore.PrivateKeyEntry? {
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
                createKeys(context)

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

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Throws(
        NoSuchAlgorithmException::class,
        NoSuchProviderException::class,
        InvalidAlgorithmParameterException::class
    )
    private fun createKeys(context: Context) {
        // Create a start and end time, for the validity range of the key pair that's about to be
        // generated.
        val start: Calendar = GregorianCalendar()
        val end: Calendar = GregorianCalendar()
        end.add(Calendar.YEAR, 25)

        val spec = KeyPairGeneratorSpec.Builder(context)
            .setAlias(ALIAS)
            .setSubject(X500Principal("CN=$ALIAS"))
            .setSerialNumber(BigInteger.valueOf(1337))
            .setStartDate(start.time)
            .setEndDate(end.time)
            .build()

        // Initialize a KeyPair generator using the the intended algorithm (in this example, RSA
        // and the KeyStore.  This example uses the AndroidKeyStore.
        val generator = KeyPairGenerator.getInstance(TYPE_RSA, KEYSTORE)
        generator.initialize(spec)

        val kp = generator.generateKeyPair()
        Log.i(TAG, "Public key is " + kp.public.toString())
    }
}
