package com.steevsapps.idledaddy.steam

import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.steevsapps.idledaddy.preferences.PrefsManager.sharedSecret
import com.steevsapps.idledaddy.utils.Utils.calculateRFC2104HMAC
import java.nio.charset.Charset
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException

object SteamGuard {

    private val TAG = SteamGuard::class.java.getSimpleName()

    private val steamGuardCodeTranslations = byteArrayOf(
        50, 51, 52, 53, 54, 55,
        56, 57, 66, 67, 68, 70,
        71, 72, 74, 75, 77, 78,
        80, 81, 82, 84, 86, 87,
        88, 89
    )

    fun generateSteamGuardCodeForTime(time: Long): String {
        var guardTime = time
        val sharedSecret = sharedSecret

        if (TextUtils.isEmpty(sharedSecret)) {
            Log.w(TAG, "shared_secret is empty!")
            return ""
        }

        val sharedSecretArray: ByteArray = try {
            Base64.decode(sharedSecret, Base64.DEFAULT)
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Invalid base64", e)
            return ""
        }

        val timeArray = ByteArray(8)

        guardTime /= 30L

        for (i in 8 downTo 1) {
            timeArray[i - 1] = guardTime.toByte()
            guardTime = guardTime shr 8
        }

        val codeArray = ByteArray(5)

        val hashedData: ByteArray = try {
            calculateRFC2104HMAC(timeArray, sharedSecretArray)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Failed to compute HMAC SHA1!", e)
            return ""
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Failed to compute HMAC SHA1!", e)
            return ""
        }

        return try {
            val b = (hashedData[19].toInt() and 0xF).toByte()
            var codePoint =
                hashedData[b.toInt()].toInt() and 0x7F shl 24 or
                    (hashedData[b + 1].toInt() and 0xFF shl 16) or
                    (hashedData[b + 2].toInt() and 0xFF shl 8) or
                    (hashedData[b + 3].toInt() and 0xFF)

            for (i in 0..4) {
                codeArray[i] =
                    steamGuardCodeTranslations[codePoint % steamGuardCodeTranslations.size]
                codePoint /= steamGuardCodeTranslations.size
            }

            String(codeArray, Charset.forName("UTF-8"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate SteamGuard code!", e)

            ""
        }
    }
}
