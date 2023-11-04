package com.steevsapps.idledaddy.steam

import android.util.Log
import androidx.annotation.IntDef
import com.google.gson.GsonBuilder
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.preferences.PrefsManager.blacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.includeFreeGames
import com.steevsapps.idledaddy.preferences.PrefsManager.parentalPin
import com.steevsapps.idledaddy.preferences.PrefsManager.sentryHash
import com.steevsapps.idledaddy.preferences.PrefsManager.writeApiKey
import com.steevsapps.idledaddy.steam.converter.GamesOwnedResponseDeserializer
import com.steevsapps.idledaddy.steam.converter.VdfConverterFactory.Companion.create
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse
import com.steevsapps.idledaddy.steam.model.TimeQuery
import com.steevsapps.idledaddy.utils.Utils.bytesToHex
import com.steevsapps.idledaddy.utils.Utils.isValidKey
import com.steevsapps.idledaddy.utils.WebHelpers.urlEncode
import `in`.dragonbra.javasteam.steam.steamclient.SteamClient
import `in`.dragonbra.javasteam.types.KeyValue
import `in`.dragonbra.javasteam.types.SteamID
import `in`.dragonbra.javasteam.util.KeyDictionary
import `in`.dragonbra.javasteam.util.crypto.CryptoException
import `in`.dragonbra.javasteam.util.crypto.CryptoHelper
import `in`.dragonbra.javasteam.util.crypto.RSACrypto
import okhttp3.OkHttpClient
import okhttp3.OkHttpClient.Builder
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Scrapes card drop info from Steam website
 */
class SteamWebHandler private constructor() {

    private lateinit var sessionId: String

    private val api: SteamAPI

    private var apiKey = BuildConfig.SteamApiKey
    private var authenticated = false
    private var steamId: Long = 0
    private var steamParental: String? = null
    private var token: String? = null
    private var tokenSecure: String? = null

    /**
     * Get a list of task appid for the Spring Cleaning Event
     * @return
     */
    val taskAppIds: List<String>
        get() {
            val url = STEAM_STORE + "springcleaning?l=english"
            val taskAppIds: MutableList<String> = ArrayList()

            try {
                val doc = Jsoup.connect(url)
                    .referrer(STEAM_STORE)
                    .followRedirects(true)
                    .cookies(generateWebCookies())
                    .get()

                val tasks = doc.select("div.spring_cleaning_task_ctn")
                for (task in tasks) {
                    val springGame = task.select("div.spring_game").first()
                    if (springGame == null || !springGame.hasAttr("data-sg-appid")) {
                        Log.d(TAG, "Skipping spring game")
                        continue
                    }

                    taskAppIds.add(springGame.attr("data-sg-appid").trim())
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return taskAppIds
        }

    /**
     * Get a list of games with card drops remaining
     * @return list of games with remaining drops
     */
    val remainingGames: List<Game>?
        get() {
            val url = STEAM_COMMUNITY + "my/badges?l=english"
            val badgeList: MutableList<Game> = ArrayList()
            val doc: Document = try {
                Jsoup.connect(url)
                    .followRedirects(true)
                    .cookies(generateWebCookies())
                    .get()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

            doc.select("a.user_avatar").first() ?: return null // Invalid cookie data

            val badges = doc.select("div.badge_title_row")
            val pages = doc.select("a.pagelink").last()

            if (pages != null) {
                // Multiple pages
                val p = pages.text().toInt()

                // Try to combine all the pages
                for (i in 2..p) {
                    try {
                        val doc2 = Jsoup.connect("$url&p=$i")
                            .followRedirects(true)
                            .cookies(generateWebCookies())
                            .get()
                        val badges2 = doc2.select("div.badge_title_row")
                        badges.addAll(badges2)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            val blacklist = blacklist
            var m: Matcher

            for (b in badges) {
                // Get app id
                val playGame = b.select("div.badge_title_playgame").first() ?: continue

                m = playPattern.matcher(playGame.select("a[href]").first()!!.attr("href"))

                if (!m.find()) {
                    continue
                }

                if (blacklist.contains(m.group(1))) {
                    // Skip appids in the blacklist
                    continue
                }

                val appId = m.group(1).toInt()

                // Get remaining card drops
                val progressInfo = b.select("span.progress_info_bold").first() ?: continue
                m = dropPattern.matcher(progressInfo.text())

                if (!m.find()) {
                    continue
                }

                val drops = m.group(1).toInt()

                // Get app name
                val badgeTitle = b.select("div.badge_title").first() ?: continue
                val name = badgeTitle.ownText().trim()

                // Get play time
                val playTime = b.select("div.badge_title_stats_playtime").first() ?: continue
                val playTimeText = playTime.text().trim()

                m = timePattern.matcher(playTimeText)

                var time = 0f
                if (m.find()) {
                    time = m.group(1).toFloat()
                }

                badgeList.add(Game(appId, name, time, drops))
            }

            return badgeList
        }

    init {
        val gson = GsonBuilder()
            .registerTypeAdapter(GamesOwnedResponse::class.java, GamesOwnedResponseDeserializer())
            .create()

        val client: OkHttpClient = Builder()
            .connectTimeout(TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(STEAM_API)
            .addConverterFactory(create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()

        api = retrofit.create(SteamAPI::class.java)
    }

    /**
     * Authenticate on the Steam website
     *
     * @param client the Steam client
     * @param webApiUserNonce the WebAPI User Nonce returned by LoggedOnCallback
     * @return true if authenticated
     */
    fun authenticate(client: SteamClient, webApiUserNonce: String): Boolean {
        authenticated = false

        val clientSteamId: SteamID = client.steamID ?: return false

        steamId = clientSteamId.convertToUInt64()
        sessionId = bytesToHex(CryptoHelper.generateRandomBlock(4))

        // generate an AES session key
        val sessionKey: ByteArray = CryptoHelper.generateRandomBlock(32)

        // rsa encrypt it with the public key for the universe we're on
        val publicKey: ByteArray = KeyDictionary.getPublicKey(client.universe) ?: return false
        val rsa = RSACrypto(publicKey)
        val cryptedSessionKey: ByteArray = rsa.encrypt(sessionKey)
        val loginKey = ByteArray(20)

        System.arraycopy(webApiUserNonce.toByteArray(), 0, loginKey, 0, webApiUserNonce.length)

        // aes encrypt the loginkey with our session key
        val cryptedLoginKey: ByteArray = try {
            CryptoHelper.symmetricEncrypt(loginKey, sessionKey)
        } catch (e: CryptoException) {
            e.printStackTrace()
            return false
        }

        val authResult: KeyValue?
        val args: MutableMap<String, String> = HashMap()
        args["steamid"] = steamId.toString()
        args["sessionkey"] = urlEncode(cryptedSessionKey)
        args["encrypted_loginkey"] = urlEncode(cryptedLoginKey)
        args["format"] = "vdf"

        authResult = try {
            api.authenticateUser(args).execute().body()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        if (authResult == null) {
            return false
        }

        token = authResult["token"].asString()
        tokenSecure = authResult["tokenSecure"].asString()
        authenticated = true

        val pin = parentalPin.trim()
        if (pin.isNotEmpty()) {
            steamParental = unlockParental(pin) // Unlock family view
        }

        return true
    }

    /**
     * Generate Steam web cookies
     * @return Map of the cookies
     */
    private fun generateWebCookies(): Map<String, String?> {
        if (!authenticated) {
            return HashMap()
        }

        val cookies: MutableMap<String, String?> = HashMap()
        cookies["sessionid"] = sessionId
        cookies["steamLogin"] = token
        cookies["steamLoginSecure"] = tokenSecure

        val sentryHash = sentryHash.trim()
        if (sentryHash.isNotEmpty()) {
            cookies["steamMachineAuth$steamId"] = sentryHash
        }

        if (steamParental != null) {
            cookies["steamparental"] = steamParental
        }

        return cookies
    }

    /**
     * Unlock Steam parental controls with a pin
     */
    private fun unlockParental(pin: String): String? {
        val url = STEAM_STORE + "parental/ajaxunlock"
        try {
            val responseCookies = Jsoup.connect(url)
                .referrer(STEAM_STORE)
                .followRedirects(true)
                .ignoreContentType(true)
                .cookies(generateWebCookies())
                .data("pin", pin)
                .method(Connection.Method.POST)
                .execute()
                .cookies()

            return responseCookies["steamparental"]
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun getGamesOwned(steamId: Long): Call<GamesOwnedResponse> {
        val args: MutableMap<String, String> = HashMap()
        args["key"] = apiKey
        args["steamid"] = steamId.toString()

        if (includeFreeGames()) {
            args["include_played_free_games"] = "1"
        }

        return api.getGamesOwned(args.toMap())
    }

    fun queryServerTime(): Call<TimeQuery> = api.queryServerTime("0")

    /**
     * Check if user is currently NOT in-game, so we can resume farming.
     */
    fun checkIfNotInGame(): Boolean? {
        val url = STEAM_COMMUNITY + "my/profile?l=english"
        val doc: Document = try {
            Jsoup.connect(url)
                .followRedirects(true)
                .cookies(generateWebCookies())
                .get()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        doc.select("a.user_avatar").first() ?: return null // Invalid cookie data

        return doc.select("div.profile_in_game_name").first() == null
    }

    /**
     * Add a free license to your account
     *
     * @param subId subscription id
     * @return true if successful
     */
    fun addFreeLicense(subId: Int): Boolean {
        val url = STEAM_STORE + "checkout/addfreelicense"
        try {
            val doc = Jsoup.connect(url)
                .referrer(STEAM_STORE)
                .followRedirects(true)
                .cookies(generateWebCookies())
                .data("sessionid", sessionId)
                .data("subid", subId.toString())
                .data("action", "add_to_cart")
                .post()
            return doc.select("div.add_free_content_success_area").first() != null
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    @Throws(Exception::class)
    fun generateNewDiscoveryQueue(): JSONArray {
        val url = STEAM_STORE + "explore/generatenewdiscoveryqueue"
        val json = Jsoup.connect(url)
            .ignoreContentType(true)
            .referrer(STEAM_STORE)
            .followRedirects(true)
            .cookies(generateWebCookies())
            .method(Connection.Method.POST)
            .data("sessionid", sessionId)
            .data("queuetype", "0")
            .execute()
            .body()
        return JSONObject(json).getJSONArray("queue")
    }

    @Throws(Exception::class)
    fun clearFromQueue(appId: String) {
        val url = STEAM_STORE + "app/10"
        Jsoup.connect(url)
            .ignoreContentType(true)
            .referrer(STEAM_STORE)
            .followRedirects(true)
            .cookies(generateWebCookies())
            .data("sessionid", sessionId)
            .data("appid_to_clear_from_queue", appId)
            .post()
    }

    @ApiKeyState
    fun updateApiKey(): Int {
        if (isValidKey(PrefsManager.apiKey)) {
            // Use saved API key
            apiKey = PrefsManager.apiKey
            return ApiKeyState.REGISTERED
        }
        // Try to fetch key from web
        val url = STEAM_COMMUNITY + "dev/apikey?l=english"
        try {
            val doc = Jsoup.connect(url)
                .referrer(STEAM_COMMUNITY)
                .followRedirects(true)
                .cookies(generateWebCookies())
                .get()
            val titleNode = doc.select("div#mainContents h2").first() ?: return ApiKeyState.ERROR
            val title = titleNode.text().trim()
            if (title.lowercase(Locale.getDefault()).contains("access denied")) {
                // Limited account, use the built-in API key
                apiKey = BuildConfig.SteamApiKey
                writeApiKey(apiKey)
                return ApiKeyState.ACCESS_DENIED
            }
            val bodyContentsEx = doc.select("div#bodyContents_ex p").first()
                ?: return ApiKeyState.ERROR
            val text = bodyContentsEx.text().trim()
            if (text.lowercase(Locale.getDefault())
                .contains("registering for a steam web api key") &&
                registerApiKey()
            ) {
                // Should actually be registered here, but we have to call this method again to get the key
                return ApiKeyState.UNREGISTERED
            } else if (text.lowercase(Locale.getDefault()).startsWith("key: ")) {
                val key = text.substring(5)
                if (isValidKey(key)) {
                    apiKey = key
                    writeApiKey(apiKey)
                    return ApiKeyState.REGISTERED
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ApiKeyState.ERROR
    }

    private fun registerApiKey(): Boolean {
        val url = STEAM_COMMUNITY + "dev/registerkey"
        try {
            Jsoup.connect(url)
                .ignoreContentType(true)
                .followRedirects(true)
                .referrer(STEAM_COMMUNITY)
                .cookies(generateWebCookies())
                .data("domain", "localhost")
                .data("agreeToTerms", "agreed")
                .data("sessionid", sessionId)
                .data("Submit", "Register")
                .post()

            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    fun openCottageDoor(): Boolean {
        var url = STEAM_STORE + "promotion/cottage_2018/?l=english"

        val doc: Document = try {
            Jsoup.connect(url)
                .followRedirects(true)
                .referrer(STEAM_STORE)
                .cookies(generateWebCookies())
                .get()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open door", e)
            return false
        }

        val door = doc.select("div[data-door-id]").not(".cottage_door_open").first()
        if (door == null) {
            Log.e(TAG, "Didn't find any doors to open")
            return false
        }

        val doorId = door.attr("data-door-id")

        Log.i(TAG, "Opening door $doorId")

        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        val t = sdf.format(Date())

        url = STEAM_STORE + "promotion/opencottagedoorajax"

        try {
            Jsoup.connect(url)
                .ignoreContentType(true)
                .followRedirects(true)
                .referrer(url)
                .cookies(generateWebCookies())
                .data("sessionid", sessionId)
                .data("door_index", doorId)
                .data("t", t)
                .data("open_door", "true")
                .post()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to open door $doorId", e)
            return false
        }

        return true
    }

    @IntDef(
        ApiKeyState.REGISTERED,
        ApiKeyState.UNREGISTERED,
        ApiKeyState.ACCESS_DENIED,
        ApiKeyState.ERROR
    )
    @Retention(AnnotationRetention.SOURCE)
    annotation class ApiKeyState {
        companion object {
            // Account has registered an API key
            const val REGISTERED = 1

            // Account has not registered an API key yet
            const val UNREGISTERED = 2

            // Account is limited and can't register an API key
            const val ACCESS_DENIED = -1

            // Some other error occurred
            const val ERROR = -2
        }
    }

    companion object {
        private val TAG = SteamWebHandler::class.java.getSimpleName()

        private const val TIMEOUT_SECS = 30

        private const val STEAM_STORE = "https://store.steampowered.com/"
        private const val STEAM_COMMUNITY = "https://steamcommunity.com/"
        private const val STEAM_API = "https://api.steampowered.com/"

        // Pattern to match app ID
        private val playPattern = Pattern.compile("^steam://run/(\\d+)$")

        // Pattern to match card drops remaining
        private val dropPattern = Pattern.compile("^(\\d+) card drops? remaining$")

        // Pattern to match play time
        @Suppress("RegExpRedundantEscape")
        private val timePattern = Pattern.compile("([0-9\\.]+) hrs on record")

        @JvmStatic
        val instance = SteamWebHandler()
    }
}
