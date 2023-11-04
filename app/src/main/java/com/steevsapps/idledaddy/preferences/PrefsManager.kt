package com.steevsapps.idledaddy.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.utils.CryptHelper.decryptString
import com.steevsapps.idledaddy.utils.CryptHelper.encryptString
import com.steevsapps.idledaddy.utils.Utils.arrayToString

/**
 * SharedPreferences manager
 */
@Suppress("MemberVisibilityCanBePrivate")
object PrefsManager {

    private const val CURRENT_VERSION = 2

    private const val API_KEY = "api_key"
    private const val AVATAR_HASH = "avatar_hash"
    private const val BLACKLIST = "blacklist"
    private const val HOURS_UNTIL_DROPS = "hours_until_drops"
    private const val INCLUDE_FREE_GAMES = "include_free_games"
    private const val LANGUAGE = "language"
    private const val LAST_SESSION = "last_session"
    private const val LOGIN_KEY = "login_key"
    private const val MINIMIZE_DATA = "minimize_data"
    private const val OFFLINE = "offline"
    private const val PARENTAL_PIN = "parental_pin"
    private const val PASSWORD = "password"
    private const val PERSONA_NAME = "persona_name"
    private const val SENTRY_HASH = "sentry_hash"
    private const val SHARED_SECRET = "shared_secret"
    private const val SORT_VALUE = "sort_value"
    private const val STAY_AWAKE = "stay_awake"
    private const val USERNAME = "username"
    private const val USE_CUSTOM_LOGINID = "use_custom_loginid"
    private const val VERSION = "version"

    lateinit var prefs: SharedPreferences
        private set

    @JvmStatic
    val apiKey: String
        get() = prefs.getString(API_KEY, "")!!

    val language: String
        get() = prefs.getString(LANGUAGE, "")!!

    val version: Int
        get() = prefs.getInt(VERSION, 1)

    @JvmStatic
    val sortValue: Int
        get() = prefs.getInt(SORT_VALUE, 0)

    val personaName: String
        get() = prefs.getString(PERSONA_NAME, "")!!

    val avatarHash: String
        get() = prefs.getString(AVATAR_HASH, "")!!

    @JvmStatic
    val hoursUntilDrops: Int
        get() = prefs.getInt(HOURS_UNTIL_DROPS, 3)

    @JvmStatic
    val username: String
        get() = prefs.getString(USERNAME, "")!!

    @JvmStatic
    val loginKey: String
        get() = prefs.getString(LOGIN_KEY, "")!!

    @JvmStatic
    val sentryHash: String
        get() = prefs.getString(SENTRY_HASH, "")!!

    @JvmStatic
    val sharedSecret: String
        get() = prefs.getString(SHARED_SECRET, "")!!

    @JvmStatic
    val offline: Boolean
        get() = prefs.getBoolean(OFFLINE, false)

    @JvmStatic
    val parentalPin: String
        get() = prefs.getString(PARENTAL_PIN, "")!!

    @JvmStatic
    val blacklist: List<String>
        get() {
            val blacklist = prefs.getString(BLACKLIST, "")!!
                .split(",".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()

            return listOf(*blacklist)
        }

    @JvmStatic
    val lastSession: List<Game>
        get() {
            val json = prefs.getString(LAST_SESSION, "")
            val type = object : TypeToken<List<Game>>() {}.type

            return Gson().fromJson(json, type) ?: listOf()
        }

    fun init(c: Context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(c)

        if (version != CURRENT_VERSION) {
            onUpgrade(version)
        }
    }

    private fun onUpgrade(oldVersion: Int) {
        if (oldVersion < 2) {
            // Serialized names have changed
            writeLastSession(ArrayList())
        }

        writeVersion(CURRENT_VERSION)
    }

    /**
     * Clear all preferences related to user
     */
    @JvmStatic
    fun clearUser() {
        prefs.edit()
            .putString(USERNAME, "")
            .putString(PASSWORD, "")
            .putString(LOGIN_KEY, "")
            .putString(SENTRY_HASH, "")
            .putString(BLACKLIST, "")
            .putString(LAST_SESSION, "")
            .putString(PARENTAL_PIN, "")
            .putString(PERSONA_NAME, "")
            .putString(AVATAR_HASH, "")
            .putString(API_KEY, "")
            .apply()
    }

    fun writeUsername(username: String) {
        writePref(USERNAME, username)
    }

    fun writePassword(password: String) {
        writePref(PASSWORD, encryptString(password))
    }

    @JvmStatic
    fun writeLoginKey(loginKey: String) {
        writePref(LOGIN_KEY, loginKey)
    }

    @JvmStatic
    fun writeSentryHash(sentryHash: String) {
        writePref(SENTRY_HASH, sentryHash)
    }

    @JvmStatic
    fun writeSharedSecret(sharedSecret: String) {
        writePref(SHARED_SECRET, sharedSecret)
    }

    @JvmStatic
    fun writeBlacklist(blacklist: List<String>) {
        writePref(BLACKLIST, arrayToString(blacklist))
    }

    @JvmStatic
    fun writeLastSession(games: List<Game>) {
        val json = Gson().toJson(games)
        writePref(LAST_SESSION, json)
    }

    fun writePersonaName(personaName: String) {
        writePref(PERSONA_NAME, personaName)
    }

    fun writeAvatarHash(avatarHash: String) {
        writePref(AVATAR_HASH, avatarHash)
    }

    @JvmStatic
    fun writeApiKey(apiKey: String) {
        writePref(API_KEY, apiKey)
    }

    fun writeLanguage(language: String) {
        writePref(LANGUAGE, language)
    }

    fun writeVersion(version: Int) {
        writePref(VERSION, version)
    }

    @JvmStatic
    fun writeSortValue(sortValue: Int) {
        writePref(SORT_VALUE, sortValue)
    }

    fun getPassword(): String =
        decryptString(prefs.getString(PASSWORD, ""))

    @JvmStatic
    fun stayAwake(): Boolean = prefs.getBoolean(STAY_AWAKE, false)

    @JvmStatic
    fun minimizeData(): Boolean = prefs.getBoolean(MINIMIZE_DATA, false)

    @JvmStatic
    fun includeFreeGames(): Boolean = prefs.getBoolean(INCLUDE_FREE_GAMES, false)

    @JvmStatic
    fun useCustomLoginId(): Boolean = prefs.getBoolean(USE_CUSTOM_LOGINID, false)

    /* ----- */

    private fun writePref(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    private fun writePref(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }
}
