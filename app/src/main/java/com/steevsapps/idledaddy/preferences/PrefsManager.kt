package com.steevsapps.idledaddy.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.utils.CryptHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.zhanghai.compose.preference.Preferences
import me.zhanghai.compose.preference.createPreferenceFlow
import kotlin.math.roundToInt

/**
 * SharedPreferences manager
 */
object PrefsManager {
    private const val CURRENT_VERSION = 4

    private const val USERNAME = "username"
    private const val PASSWORD = "password"
    private const val REFRESH_TOKEN = "login_key"
    private const val GUARD_DATA = "sentry_hash"
    private const val OFFLINE = "offline"
    private const val STAY_AWAKE = "stay_awake"
    private const val MINIMIZE_DATA = "minimize_data"
    private const val PARENTAL_PIN = "parental_pin"
    private const val BLACKLIST = "blacklist"
    private const val LAST_SESSION = "last_session"
    private const val HOURS_UNTIL_DROPS = "hours_until_drops"
    private const val INCLUDE_FREE_GAMES = "include_free_games"
    private const val USE_CUSTOM_LOGINID = "use_custom_loginid"
    private const val PERSONA_NAME = "persona_name"
    private const val AVATAR_HASH = "avatar_hash"
    private const val API_KEY = "api_key"
    private const val LANGUAGE = "language"
    private const val VERSION = "version"
    private const val SORT_VALUE = "sort_value"
    private const val CELL_ID = "cell_id"

    private lateinit var prefs: SharedPreferences
    private val json = Json { ignoreUnknownKeys = true }

    fun init(c: Context) {
        if (!::prefs.isInitialized) {
            prefs = PreferenceManager.getDefaultSharedPreferences(c)
        }

        if (getVersion() != CURRENT_VERSION) {
            onUpgrade(getVersion())
        }
    }

    private fun writePref(key: String?, value: String?) {
        prefs.edit { putString(key, value) }
    }

    private fun writePref(key: String?, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    private fun onUpgrade(oldVersion: Int) {
        if (oldVersion < 2) {
            // Serialized names have changed
            writeLastSession(emptyList())
        }
        if (oldVersion < 3) {
            writeRefreshToken("")
            writeGuardData("")
        }
        if (oldVersion < 4) {
            // hours_until_drops is now stored as Float (Compose slider); convert old Int values
            (prefs.all[HOURS_UNTIL_DROPS] as? Int)?.let {
                prefs.edit { putFloat(HOURS_UNTIL_DROPS, it.toFloat()) }
            }
        }
        writeVersion(CURRENT_VERSION)
    }

    val preferenceFlow: MutableStateFlow<Preferences> by lazy { createPreferenceFlow(prefs) }

    /**
     * Clear all preferences related to user
     */
    fun clearUser() {
        prefs.edit {
            putString(USERNAME, "")
            putString(PASSWORD, "")
            putString(REFRESH_TOKEN, "")
            putString(GUARD_DATA, "")
            putString(BLACKLIST, "")
            putString(LAST_SESSION, "")
            putString(PARENTAL_PIN, "")
            putString(PERSONA_NAME, "")
            putString(AVATAR_HASH, "")
            putString(API_KEY, "")
        }
    }

    fun getUsername(): String = prefs.getString(USERNAME, "")!!
    fun writeUsername(username: String?) {
        writePref(USERNAME, username)
    }

    fun getPassword(): String =
        CryptHelper.decryptString(prefs.getString(PASSWORD, ""))

    fun writePassword(password: String) {
        writePref(PASSWORD, CryptHelper.encryptString(password))
    }

    fun getRefreshToken(): String = prefs.getString(REFRESH_TOKEN, "")!!
    fun writeRefreshToken(refreshToken: String?) {
        writePref(REFRESH_TOKEN, refreshToken)
    }

    fun getGuardData(): String = prefs.getString(GUARD_DATA, "")!!
    fun writeGuardData(guardData: String?) {
        writePref(GUARD_DATA, guardData)
    }

    @JvmStatic
    fun getBlacklist(): MutableList<String?> {
        val blacklist: Array<String?> =
            prefs.getString(BLACKLIST, "")!!.split(",".toRegex())
                .dropLastWhile { it.isEmpty() }.toTypedArray()
        return ArrayList(listOf(*blacklist))
    }

    fun writeBlacklist(blacklist: MutableList<String?>) {
        writePref(BLACKLIST, blacklist.joinToString(","))
    }

    fun getLastSession(): List<Game> {
        val raw = prefs.getString(LAST_SESSION, "")!!
        if (raw.isEmpty()) {
            return emptyList()
        }
        return runCatching { json.decodeFromString<List<Game>>(raw) }.getOrDefault(emptyList())
    }

    fun writeLastSession(games: List<Game>) {
        writePref(LAST_SESSION, json.encodeToString(games))
    }

    fun getPersonaName(): String = prefs.getString(PERSONA_NAME, "")!!
    fun writePersonaName(personaName: String?) {
        writePref(PERSONA_NAME, personaName)
    }

    fun getAvatarHash(): String = prefs.getString(AVATAR_HASH, "")!!
    fun writeAvatarHash(avatarHash: String?) {
        writePref(AVATAR_HASH, avatarHash)
    }

    @JvmStatic
    fun getApiKey(): String = prefs.getString(API_KEY, "")!!
    @JvmStatic
    fun writeApiKey(apiKey: String?) {
        writePref(API_KEY, apiKey)
    }

    fun getLanguage(): String = prefs.getString(LANGUAGE, "")!!
    fun writeLanguage(language: String?) {
        writePref(LANGUAGE, language)
    }

    fun getVersion(): Int = prefs.getInt(VERSION, 1)
    fun writeVersion(version: Int) {
        writePref(VERSION, version)
    }

    fun getSortValue(): Int = prefs.getInt(SORT_VALUE, 0)
    fun writeSortValue(sortValue: Int) {
        writePref(SORT_VALUE, sortValue)
    }

    fun getCellId(): Int = prefs.getInt(CELL_ID, -1)
    fun writeCellId(cellId: Int) {
        writePref(CELL_ID, cellId)
    }

    fun getOffline(): Boolean = prefs.getBoolean(OFFLINE, false)

    fun stayAwake(): Boolean = prefs.getBoolean(STAY_AWAKE, false)

    fun minimizeData(): Boolean = prefs.getBoolean(MINIMIZE_DATA, false)

    @JvmStatic
    fun getParentalPin(): String = prefs.getString(PARENTAL_PIN, "")!!

    fun getHoursUntilDrops(): Int = prefs.getFloat(HOURS_UNTIL_DROPS, 3f).roundToInt()

    @JvmStatic
    fun includeFreeGames(): Boolean = prefs.getBoolean(INCLUDE_FREE_GAMES, false)

    fun useCustomLoginId(): Boolean = prefs.getBoolean(USE_CUSTOM_LOGINID, false)
}
