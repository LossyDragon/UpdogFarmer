package com.steevsapps.idledaddy.steam

import androidx.annotation.IntDef
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.preferences.PrefsManager.getApiKey
import com.steevsapps.idledaddy.preferences.PrefsManager.getBlacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.writeApiKey
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.utils.Utils.isValidKey
import `in`.dragonbra.javasteam.util.Strings.toHex
import `in`.dragonbra.javasteam.util.crypto.CryptoHelper
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import retrofit2.Retrofit
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Scrapes card drop info from Steam website
 */
class SteamWebHandler private constructor() {
    private var authenticated = false
    private var steamId: Long = 0
    private var sessionId: String? = null
    private var accessToken: String? = null
    private var steamParental: String? = null
    private var apiKey = BuildConfig.STEAM_API_KEY

    private val api: SteamAPI

    init {
        val json = Json { ignoreUnknownKeys = true }

        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECS.toLong(), TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(STEAM_API)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .client(client)
            .build()

        api = retrofit.create(SteamAPI::class.java)
    }

    /**
     * Authenticate on the Steam website.
     *
     *
     * Steam now issues the website session directly from the JWT access token handed out by
     * `SteamAuthentication`/`beginAuthSessionViaCredentials`, instead of the old
     * RSA-encrypted-nonce dance against `ISteamUserAuth/AuthenticateUser` (that endpoint
     * no longer works). The `steamLoginSecure` cookie is simply `<steamid>||<accessToken>`.
     *
     * @param steamId the logged on user's SteamID
     * @param accessToken a short-lived JWT access token, see SteamAuthentication#generateAccessTokenForApp
     * @param parentalToken family view unlock token, used as the steamparental cookie
     * @return true if authenticated
     */
    fun authenticate(steamId: Long, accessToken: String?, parentalToken: String? = null): Boolean {
        this.steamId = steamId
        this.accessToken = accessToken
        this.sessionId = toHex(CryptoHelper.generateRandomBlock(4))
        this.authenticated = true
        this.steamParental = parentalToken

        return true
    }

    /**
     * Generate Steam web cookies
     * @return Map of the cookies
     */
    private fun generateWebCookies(): MutableMap<String, String> {
        if (!authenticated) {
            return mutableMapOf()
        }

        val cookies: MutableMap<String, String> = mutableMapOf()
        cookies["sessionid"] = sessionId!!
        cookies["steamLoginSecure"] = "$steamId||$accessToken"
        val parental = steamParental
        if (parental != null) {
            cookies["steamparental"] = parental
        }

        return cookies
    }

    /**
     * Get a list of games with card drops remaining
     * @return list of games with remaining drops
     */
    val remainingGames: MutableList<Game>?
        get() {
            val url = STEAM_COMMUNITY + "my/badges?l=english"
            val badgeList: MutableList<Game> = mutableListOf()
            val doc = try {
                Jsoup.connect(url)
                    .followRedirects(true)
                    .cookies(generateWebCookies())
                    .get()
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

            if (doc.select("a.user_avatar").first() == null) {
                // Invalid cookie data
                return null
            }

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

            val blacklist = getBlacklist()
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

                val appId = m.group(1)!!.toInt()

                // Get remaining card drops
                val progressInfo = b.select("span.progress_info_bold").first() ?: continue
                m = dropPattern.matcher(progressInfo.text())
                if (!m.find()) {
                    continue
                }
                val drops = m.group(1)!!.toInt()

                // Get app name
                val badgeTitle = b.select("div.badge_title").first() ?: continue
                val name = badgeTitle.ownText().trim()

                // Get play time
                val playTime = b.select("div.badge_title_stats_playtime").first() ?: continue
                val playTimeText = playTime.text().trim()
                m = timePattern.matcher(playTimeText)
                var time = 0f
                if (m.find()) {
                    time = m.group(1)!!.toFloat()
                }

                badgeList.add(Game(appId, name, time, drops))
            }

            return badgeList
        }

    /**
     * Get the store header image url for an app, used as a fallback when the icon fails to load
     */
    suspend fun getHeaderImage(appId: Int): String? = api.getAppDetails(appId)[appId.toString()]
        ?.takeIf { it.success }
        ?.data
        ?.headerImage
        ?.ifEmpty { null }

    /**
     * Check if user is currently NOT in-game, so we can resume farming.
     */
    fun checkIfNotInGame(): Boolean? {
        val url = STEAM_COMMUNITY + "my/profile?l=english"
        val doc = try {
            Jsoup.connect(url)
                .followRedirects(true)
                .cookies(generateWebCookies())
                .get()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        if (doc.select("a.user_avatar").first() == null) {
            // Invalid cookie data
            return null
        }

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
                .data("sessionid", sessionId!!)
                .data("subid", subId.toString())
                .data("action", "add_to_cart")
                .post()
            return doc.select("div.add_free_content_success_area").first() != null
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    @ApiKeyState
    fun updateApiKey(): Int {
        if (isValidKey(getApiKey())) {
            // Use saved API key
            apiKey = getApiKey()
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
                apiKey = BuildConfig.STEAM_API_KEY
                writeApiKey(apiKey)
                return ApiKeyState.ACCESS_DENIED
            }
            val bodyContentsEx = doc.select("div#bodyContents_ex p").first() ?: return ApiKeyState.ERROR
            val text = bodyContentsEx.text().trim()
            if (text.lowercase(Locale.getDefault()).contains("registering for a steam web api key")
                && registerApiKey()
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
                .data("sessionid", sessionId!!)
                .data("Submit", "Register")
                .post()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return false
    }

    @IntDef(ApiKeyState.REGISTERED, ApiKeyState.UNREGISTERED, ApiKeyState.ACCESS_DENIED, ApiKeyState.ERROR)
    @Retention(AnnotationRetention.SOURCE)
    annotation class ApiKeyState {
        companion object {
            // Account has registered an API key
            const val REGISTERED: Int = 1

            // Account has not registered an API key yet
            const val UNREGISTERED: Int = 2

            // Account is limited and can't register an API key
            const val ACCESS_DENIED: Int = -1

            // Some other error occurred
            const val ERROR: Int = -2
        }
    }

    companion object {
        private const val TIMEOUT_SECS = 30

        private const val STEAM_STORE = "https://store.steampowered.com/"
        private const val STEAM_COMMUNITY = "https://steamcommunity.com/"
        private const val STEAM_API = "https://api.steampowered.com/"

        // Pattern to match app ID
        private val playPattern: Pattern = Pattern.compile("^steam://run/(\\d+)$")

        // Pattern to match card drops remaining
        private val dropPattern: Pattern = Pattern.compile("^(\\d+) card drops? remaining$")

        // Pattern to match play time
        private val timePattern: Pattern = Pattern.compile("([0-9.]+) hrs on record")

        val instance: SteamWebHandler = SteamWebHandler()
    }
}
