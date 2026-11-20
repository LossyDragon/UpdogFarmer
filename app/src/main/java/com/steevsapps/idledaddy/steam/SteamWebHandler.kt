package com.steevsapps.idledaddy.steam

import android.os.SystemClock
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

    // Badge page scan state, see getRemainingGames()
    @Volatile
    private var scanStart = 2

    @Volatile
    private var lastFullScan: FullScan? = null

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
        if (steamId != this.steamId) {
            // Different account, different badge pages
            scanStart = 2
            lastFullScan = null
        }
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
     * Games with card drops remaining, as found by [getRemainingGames].
     * @param complete false if the scan stopped early, so later badge pages may hold more games
     */
    class RemainingGames(val games: List<Game>, val complete: Boolean)

    /**
     * Get the games with card drops remaining.
     *
     * Large accounts have hundreds of badge pages, and fetching them all on every check is slow
     * and gets rate limited, so a check reads as few pages as it can:
     * - It stops once [enough] games are found. Games on the remaining pages are picked up
     *   once the earlier ones run out of drops.
     * - It starts at the first page that had drops last time ([scanStart]) and wraps around,
     *   so pages that are already farmed out are only read when nothing else is left.
     * - After a scan that covered every page, only the pages that had drops are re-read until
     *   [FULL_SCAN_INTERVAL_MS] has passed or they run dry.
     * - It stops at the first page that fails to load (e.g. rate limited) and continues from
     *   that page on the next check.
     *
     * The first page is always read: it tells us whether the cookies are valid and how many
     * pages there are.
     * @param enough stop scanning once this many games are found
     * @return the games found, or null if the first page failed or the cookies are invalid
     */
    fun getRemainingGames(enough: Int): RemainingGames? {
        val url = STEAM_COMMUNITY + "my/badges?l=english"
        val games: MutableList<Game> = mutableListOf()
        val blacklist = getBlacklist()

        // Each page is reduced to its games before the next one is fetched, so only one
        // parsed page is ever held in memory.
        val pageCount = loadBadgePage(url, blacklist, games) ?: return null
        val fetched = hashSetOf(1)
        val pagesWithDrops = sortedSetOf<Int>()
        if (games.isNotEmpty()) {
            pagesWithDrops.add(1)
        }

        // Returns false if the page could not be read
        fun load(page: Int): Boolean {
            val before = games.size
            loadBadgePage("$url&p=$page", blacklist, games) ?: return false
            fetched.add(page)
            if (games.size > before) {
                pagesWithDrops.add(page)
            }
            return true
        }

        // A recent scan covered every page, so only the pages that had drops need checking
        val known = lastFullScan
        if (known != null && known.isUsable(pageCount, blacklist)) {
            for (page in known.pagesWithDrops) {
                if (games.size >= enough) {
                    return RemainingGames(games, complete = false)
                }
                if (page !in fetched && !load(page)) {
                    return RemainingGames(games, complete = false)
                }
            }
            if (games.isNotEmpty()) {
                return RemainingGames(games, complete = true)
            }
            // Nothing left where drops were last seen. Scan everything again to be sure.
        }
        lastFullScan = null

        // Go around pages 2..pageCount once, starting where drops were last found
        var page = scanStart.coerceIn(2, maxOf(2, pageCount))
        var firstPageWithDrops = 0
        var complete = true
        repeat(pageCount - 1) {
            if (games.size >= enough) {
                complete = false
                return@repeat
            }
            if (page !in fetched) {
                val before = games.size
                if (!load(page)) {
                    scanStart = page
                    return RemainingGames(games, complete = false)
                }
                if (firstPageWithDrops == 0 && games.size > before) {
                    firstPageWithDrops = page
                }
            }
            page = if (page >= pageCount) 2 else page + 1
        }

        if (firstPageWithDrops != 0) {
            scanStart = firstPageWithDrops
        } else if (complete) {
            scanStart = 2
        }
        if (complete) {
            lastFullScan = FullScan(
                atMillis = SystemClock.elapsedRealtime(),
                pageCount = pageCount,
                blacklist = blacklist.toList(),
                pagesWithDrops = pagesWithDrops.toList(),
            )
        }
        return RemainingGames(games, complete)
    }

    /** What the last scan of every badge page found, see [getRemainingGames]. */
    private class FullScan(
        val atMillis: Long,
        val pageCount: Int,
        val blacklist: List<String?>,
        val pagesWithDrops: List<Int>,
    ) {
        fun isUsable(pageCount: Int, blacklist: List<String?>): Boolean =
            SystemClock.elapsedRealtime() - atMillis < FULL_SCAN_INTERVAL_MS &&
                pageCount == this.pageCount &&
                blacklist == this.blacklist
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

        // How long a scan of every badge page is trusted before all pages are read again
        private const val FULL_SCAN_INTERVAL_MS = 60 * 60 * 1000L

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
