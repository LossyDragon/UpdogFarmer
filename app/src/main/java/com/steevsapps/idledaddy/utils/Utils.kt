package com.steevsapps.idledaddy.utils

object Utils {
    private const val AVATAR_URL =
        "https://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/"

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
     * Build the full avatar URL from an avatar hash
     */
    fun avatar(avatarHash: String): String {
        val avatar = if(avatarHash.isBlank()) {
            "$AVATAR_URL/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb_full.jpg"
        } else {
            "$AVATAR_URL${avatarHash.substring(0, 2)}/${avatarHash}_full.jpg"
        }
        return avatar
    }
}
