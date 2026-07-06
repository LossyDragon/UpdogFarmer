package com.steevsapps.idledaddy.utils

import com.steevsapps.idledaddy.ThrowingTask

object Utils {
    private val NON_ASCII_REGEX = Regex("[^\\u0000-\\u007F]")
    private val API_KEY_REGEX = Regex("^[0-9A-Fa-f]+$")

    /**
     * Strips non-ASCII characters from String
     */
    fun removeSpecialChars(s: String): String = s.replace(NON_ASCII_REGEX, "")

    /**
     * Check if API key is valid
     */
    fun isValidKey(key: String): Boolean = key.matches(API_KEY_REGEX)

    /**
     * Run a block of code a maximum of maxTries
     */
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
