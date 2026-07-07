package com.steevsapps.idledaddy.utils

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
}
