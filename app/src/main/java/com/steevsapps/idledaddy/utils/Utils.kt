package com.steevsapps.idledaddy.utils

import com.steevsapps.idledaddy.ThrowingTask
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.security.InvalidKeyException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object Utils {

    private const val SHA1_ALGORITHM = "SHA-1"
    private const val HMAC_SHA1_ALGORITHM = "HmacSHA1"

    /**
     * Get the current unix time
     */
    val currentUnixTime: Long
        get() = System.currentTimeMillis() / 1000L

    /**
     * Convert byte array to hex string
     * https://stackoverflow.com/a/9855338
     */
    @JvmStatic
    fun bytesToHex(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(it) }

    /**
     * Convert ArrayList to comma-separated String
     */
    @JvmStatic
    fun arrayToString(list: List<String>): String = list.joinToString(separator = ",") { it }

    /**
     * Convert array to comma-separated String
     */
    fun arrayToString(array: Array<String>): String = arrayToString(listOf(*array))

    /**
     * Save Logcat to file
     */
    @Throws(IOException::class)
    fun saveLogcat(file: File?) {
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            process.inputStream.bufferedReader().use { reader ->
                file?.bufferedWriter().use { writer ->
                    reader.forEachLine { line ->
                        writer?.write(line)
                        writer?.newLine()
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Strips non-ASCII characters from String
     */
    fun removeSpecialChars(s: String): String = s.replace("[^\\u0000-\\u007F]".toRegex(), "")


    /**
     * Check if API key is valid
     */
    @JvmStatic
    fun isValidKey(key: String): Boolean = key.matches("^[0-9A-Fa-f]+$".toRegex())

    /**
     * Calculate the SHA-1 hash of a file
     */
    @JvmStatic
    @Throws(IOException::class, NoSuchAlgorithmException::class)
    fun calculateSHA1(file: File?): ByteArray {
        if(file == null)
            return byteArrayOf()

        FileInputStream(file).use { fis ->
            val md = MessageDigest.getInstance(SHA1_ALGORITHM)
            val buffer = ByteArray(8192)
            var n: Int

            while (fis.read(buffer).also { n = it } != -1) {
                md.update(buffer, 0, n)
            }

            return md.digest()
        }
    }

    /**
     * Calculate HMAC SHA1
     */
    @JvmStatic
    @Throws(NoSuchAlgorithmException::class, InvalidKeyException::class)
    fun calculateRFC2104HMAC(data: ByteArray?, key: ByteArray?): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA1_ALGORITHM).apply {
            val secretKey = SecretKeySpec(key, HMAC_SHA1_ALGORITHM)
            init(secretKey)
        }

        return mac.doFinal(data)
    }

    /**
     * Run a block of code a maximum of maxTries
     */
    @JvmStatic
    @Throws(Exception::class)
    fun runWithRetries(maxTries: Int, task: ThrowingTask) {
        var count = 0

        while (count < maxTries) {
            try {
                task.run()

                return
            } catch (e: Exception) {
                if (++count >= maxTries) {
                    throw e
                }

                Thread.sleep(1000)
            }
        }
    }
}
