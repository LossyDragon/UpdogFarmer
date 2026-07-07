package com.steevsapps.idledaddy.ui.screen.games

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.steevsapps.idledaddy.fragments.GamesFragment
import com.steevsapps.idledaddy.preferences.PrefsManager.getBlacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.getLastSession
import com.steevsapps.idledaddy.preferences.PrefsManager.getSortValue
import com.steevsapps.idledaddy.preferences.PrefsManager.writeBlacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.writeSortValue
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

data class GamesUiState(
    val games: List<Game> = emptyList(),
    val refreshing: Boolean = false,
    val query: String = "",
    val tab: Int = GamesFragment.TAB_GAMES,
    val selected: List<Game> = emptyList(),
    val steamId: Long = 0,
    // The game whose long-press options dialog is showing, if any
    val optionsGame: Game? = null,
)

class GamesViewModel(application: Application) : AndroidViewModel(application) {

    var uiState by mutableStateOf(GamesUiState())
        private set

    private val webHandler: SteamWebHandler = SteamWebHandler.instance
    private var sortId: Int = getSortValue()

    @SuppressLint("StaticFieldLeak") // I know...
    private var service: SteamService? = null
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder) {
            service = (iBinder as SteamService.LocalBinder).service.also {
                uiState = uiState.copy(
                    steamId = it.steamId,
                    selected = it.currentGames.toList(),
                )
                refresh()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            service = null
        }
    }

    init {
        val app = getApplication<Application>()
        val serviceIntent = SteamService.createIntent(app)
        ContextCompat.startForegroundService(app, serviceIntent)
        app.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true
    }

    override fun onCleared() {
        if (serviceBound) {
            getApplication<Application>().unbindService(connection)
            serviceBound = false
        }
    }

    /**
     * The games shown for the current tab and search query
     */
    fun visibleGames(): List<Game> {
        var games = uiState.games
        if (uiState.tab == GamesFragment.TAB_BLACKLIST) {
            val blacklist = getBlacklist()
            games = games.filter { blacklist.contains(it.appId.toString()) }
        }
        if (uiState.query.isNotEmpty()) {
            games = games.filter { it.name.contains(uiState.query, ignoreCase = true) }
        }
        return games
    }

    fun setQuery(query: String) {
        uiState = uiState.copy(query = query)
    }

    fun switchTab(tab: Int) {
        uiState = uiState.copy(tab = tab)
        refresh()
    }

    fun setSelected(games: List<Game>) {
        uiState = uiState.copy(selected = games)
    }

    fun showOptions(game: Game) {
        uiState = uiState.copy(optionsGame = game)
    }

    fun dismissOptions() {
        uiState = uiState.copy(optionsGame = null)
    }

    fun isBlacklisted(game: Game): Boolean =
        getBlacklist().contains(game.appId.toString())

    /**
     * Add/remove the game from the blacklist and close the options dialog
     */
    fun toggleBlacklist(game: Game) {
        val blacklist = getBlacklist()
        val id = game.appId.toString()
        if (blacklist.contains(id)) {
            blacklist.remove(id)
        } else {
            blacklist.add(0, id)
        }
        writeBlacklist(blacklist)
        // New games reference forces visibleGames() to re-filter so the blacklist tab updates
        uiState = uiState.copy(games = uiState.games.toList(), optionsGame = null)
    }

    /**
     * Idle or un-idle a game, capped at [MAX_GAMES] at once
     */
    fun toggleGame(game: Game) {
        val selected = uiState.selected
        when {
            selected.contains(game) -> {
                uiState = uiState.copy(selected = selected - game)
                service?.removeGame(game)
            }

            selected.size < MAX_GAMES -> {
                uiState = uiState.copy(selected = selected + game)
                service?.addGame(game)
            }
        }
    }

    /**
     * Idle everything currently shown
     */
    fun playAll() {
        val games = visibleGames()
        uiState = uiState.copy(selected = games)
        service?.addGames(games.toMutableList())
    }

    fun refresh() {
        if (uiState.tab == GamesFragment.TAB_LAST) {
            // Load last idling session
            val games = uiState.selected.ifEmpty {
                getLastSession().filterNotNull()
            }
            setGames(games)
        } else {
            uiState = uiState.copy(refreshing = true)
            fetchGames()
        }
    }

    fun sort(sortId: Int) {
        if (this.sortId == sortId) {
            return
        }
        this.sortId = sortId
        writeSortValue(sortId)
        if (uiState.games.isNotEmpty()) {
            setGames(uiState.games)
        }
    }

    private fun setGames(games: List<Game>) {
        val sorted = games.toMutableList()
        when (sortId) {
            SORT_ALPHABETICALLY -> sorted.sortBy { it.name.lowercase(Locale.getDefault()) }
            SORT_HOURS_PLAYED -> sorted.sortDescending()
            SORT_HOURS_PLAYED_REVERSED -> sorted.sort()
        }
        uiState = uiState.copy(games = sorted, refreshing = false)
    }

    private fun fetchGames() {
        Log.i(TAG, "Fetching games...")
        webHandler.getGamesOwned(uiState.steamId).enqueue(object : Callback<GamesOwnedResponse> {
            override fun onResponse(
                call: Call<GamesOwnedResponse>,
                response: Response<GamesOwnedResponse>
            ) {
                if (response.isSuccessful) {
                    Log.i(TAG, "Success!")
                    setGames(response.body()!!.games)
                } else {
                    Log.i(TAG, "Got error code: ${response.code()}")
                    setGames(emptyList())
                }
            }

            override fun onFailure(call: Call<GamesOwnedResponse>, t: Throwable) {
                Log.i(TAG, "Got error", t)
                setGames(emptyList())
            }
        })
    }

    companion object {
        private val TAG: String = GamesViewModel::class.java.simpleName

        // Steam only allows idling up to 32 games at once
        private const val MAX_GAMES = 32

        const val SORT_ALPHABETICALLY: Int = 0
        const val SORT_HOURS_PLAYED: Int = 1
        const val SORT_HOURS_PLAYED_REVERSED: Int = 2
    }
}