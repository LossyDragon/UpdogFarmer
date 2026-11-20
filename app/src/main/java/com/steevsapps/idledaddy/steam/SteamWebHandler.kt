package com.steevsapps.idledaddy.steam

import com.steevsapps.idledaddy.preferences.PrefsManager.getBlacklist
import com.steevsapps.idledaddy.steam.model.Game
import `in`.dragonbra.javasteam.util.Strings.toHex
import `in`.dragonbra.javasteam.util.crypto.CryptoHelper
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Scrapes card drop info from Steam website
 */
class SteamWebHandler {
    private var authenticated = false
    private var steamId: Long = 0
    private var sessionId: String? = null
    private var accessToken: String? = null
    private var steamParental: String? = null

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
     * Steam now issues the website session directly from the JWT access token handed out by
     * `SteamAuthentication`/`beginAuthSessionViaCredentials`, instead of the old
     * RSA-encrypted-nonce dance against `ISteamUserAuth/AuthenticateUser` (that endpoint
     * no longer works). The `steamLoginSecure` cookie is simply `<steamid>||<accessToken>`.
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
    fun generateWebCookies(): MutableMap<String, String> {
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
            val blacklist = getBlacklist()

            // Each page is reduced to its games before the next one is fetched, so only one
            // parsed page is ever held in memory.
            val pageCount = loadBadgePage(url, blacklist, badgeList) ?: return null
            for (i in 2..pageCount) {
                loadBadgePage("$url&p=$i", blacklist, badgeList)
            }

            return badgeList
        }

    /**
     * Fetch one badge page and add its games with drops remaining to [out].
     * @return the number of badge pages, or null if the fetch failed or the cookies are invalid
     */
    private fun loadBadgePage(url: String, blacklist: List<String?>, out: MutableList<Game>): Int? {
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

        var m: Matcher
        for (b in doc.select("div.badge_title_row")) {
            // Get app id
            val playGame = b.select("div.badge_title_playgame").first() ?: continue
            val href = playGame.select("a[href]").first()?.attr("href") ?: continue
            m = playPattern.matcher(href)
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

            out.add(Game(appId, name, time, drops))
        }

        return doc.select("a.pagelink").last()?.text()?.toIntOrNull() ?: 1
    }

    /**
     * Get the store header image url for an app, used as a fallback when the icon fails to load
     */
    suspend fun getHeaderImage(appId: Int): String? = api.getAppDetails(appId)[appId.toString()]
        ?.takeIf { it.success }
        ?.data
        ?.headerImage
        ?.ifEmpty { null }

    // TODO protobuf alternative with subid??
    /**
     * Add a free license to your account
     * @param subId subscription id
     * @return true if successful
     */
    fun addFreeLicense(subId: Int): Boolean {
        val sessionId = sessionId ?: return false
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

    companion object {
        private const val TIMEOUT_SECS = 30

        private const val STEAM_STORE = "https://store.steampowered.com/"
        const val STEAM_COMMUNITY = "https://steamcommunity.com/"
        private const val STEAM_API = "https://api.steampowered.com/"

        // Pattern to match app ID
        private val playPattern: Pattern = Pattern.compile("^steam://run/(\\d+)$")

        // Pattern to match card drops remaining
        private val dropPattern: Pattern = Pattern.compile("^(\\d+) card drops? remaining$")

        // Pattern to match play time
        private val timePattern: Pattern = Pattern.compile("([0-9.]+) hrs on record")
    }
}
