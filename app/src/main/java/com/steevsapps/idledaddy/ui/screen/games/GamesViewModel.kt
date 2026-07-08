package com.steevsapps.idledaddy.ui.screen.games

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.steevsapps.idledaddy.preferences.PrefsManager.getBlacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.getLastSession
import com.steevsapps.idledaddy.preferences.PrefsManager.getSortValue
import com.steevsapps.idledaddy.preferences.PrefsManager.minimizeData
import com.steevsapps.idledaddy.preferences.PrefsManager.writeBlacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.writeSortValue
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse
import com.steevsapps.idledaddy.ui.screen.games.GamesViewModel.Companion.MAX_GAMES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

const val TAB_GAMES: Int = 0
const val TAB_LAST: Int = 1
const val TAB_BLACKLIST: Int = 2

@Immutable
data class GamesScreenState(
    val games: List<Game> = emptyList(),
    val selected: List<Game> = emptyList(),
    val tab: Int = TAB_GAMES,
    val query: String = "",
    val refreshing: Boolean = false,
    val showPlayAll: Boolean = false,
    val showRedeem: Boolean = false,
    val showIcons: Boolean = false,
    val optionsGame: Game? = null,
    val optionsBlacklisted: Boolean = false,
    val fabMenuExpanded: Boolean = false,
    val redeemDialogVisible: Boolean = false,
)

class GamesViewModel(private val appContext: Context) : ViewModel() {

    val uiState: StateFlow<GamesScreenState>
        field = MutableStateFlow(GamesScreenState())

    private val webHandler: SteamWebHandler = SteamWebHandler.instance
    private var sortId: Int = getSortValue()
    private var steamId: Long = 0

    // Everything Steam returned; uiState.games holds the tab/query-filtered subset shown on screen
    private var allGames: List<Game> = emptyList()

    @SuppressLint("StaticFieldLeak") // I know...
    private var service: SteamService? = null
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder) {
            service = (iBinder as SteamService.LocalBinder).service.also {
                steamId = it.steamId
                uiState.update { state ->
                    state.copy(selected = it.currentGames.toList(), showRedeem = steamId > 0)
                }
                refresh()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            service = null
        }
    }

    init {
        val app = appContext
        val serviceIntent = SteamService.createIntent(app)
        ContextCompat.startForegroundService(app, serviceIntent)
        app.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true
    }

    override fun onCleared() {
        if (serviceBound) {
            appContext.unbindService(connection)
            serviceBound = false
        }
    }

    fun setQuery(query: String) {
        uiState.update { it.copy(query = query) }
        applyFilter()
    }

    fun switchTab(tab: Int) {
        uiState.update { it.copy(tab = tab) }
        refresh()
    }

    fun setSelected(games: List<Game>) {
        uiState.update { it.copy(selected = games) }
    }

    fun showOptions(game: Game) {
        uiState.update { it.copy(optionsGame = game, optionsBlacklisted = isBlacklisted(game)) }
    }

    fun dismissOptions() {
        uiState.update { it.copy(optionsGame = null) }
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
        uiState.update { it.copy(optionsGame = null) }
        applyFilter()
    }

    /**
     * Idle or un-idle a game, capped at [MAX_GAMES] at once
     */
    fun toggleGame(game: Game) {
        val selected = uiState.value.selected
        when {
            selected.contains(game) -> {
                uiState.update { it.copy(selected = selected - game) }
                service?.removeGame(game)
            }

            selected.size < MAX_GAMES -> {
                uiState.update { it.copy(selected = selected + game) }
                service?.addGame(game)
            }
        }
    }

    /**
     * Idle everything currently shown
     */
    fun playAll() {
        val games = uiState.value.games
        uiState.update { it.copy(selected = games) }
        service?.addGames(games.toMutableList())
    }

    fun refresh() {
        if (uiState.value.tab == TAB_LAST) {
            // Load last idling session
            val games = uiState.value.selected.ifEmpty {
                getLastSession().filterNotNull()
            }
            setGames(games)
        } else {
            uiState.update { it.copy(refreshing = true) }
            fetchGames()
        }
    }

    /**
     * Redeem a Steam key or free game ID
     */
    fun redeemKey(text: String) {
        val key = text.uppercase(Locale.getDefault()).trim()
        if (key.isNotEmpty()) {
            service?.redeemKey(key)
        }
    }

    fun sort(sortId: Int) {
        if (this.sortId == sortId) {
            return
        }
        this.sortId = sortId
        writeSortValue(sortId)
        if (allGames.isNotEmpty()) {
            setGames(allGames)
        }
    }

    private fun setGames(games: List<Game>) {
        val sorted = games.toMutableList()
        when (sortId) {
            SORT_ALPHABETICALLY -> sorted.sortBy { it.name.lowercase(Locale.getDefault()) }
            SORT_HOURS_PLAYED -> sorted.sortDescending()
            SORT_HOURS_PLAYED_REVERSED -> sorted.sort()
        }
        allGames = sorted
        applyFilter()
        uiState.update { it.copy(refreshing = false) }
    }

    /**
     * Re-derive the tab/query-filtered games shown on screen from [allGames]
     */
    private fun applyFilter() {
        val state = uiState.value
        var games = allGames
        if (state.tab == TAB_BLACKLIST) {
            val blacklist = getBlacklist()
            games = games.filter { blacklist.contains(it.appId.toString()) }
        }
        if (state.query.isNotEmpty()) {
            games = games.filter { it.name.contains(state.query, ignoreCase = true) }
        }
        uiState.update {
            it.copy(
                games = games,
                showPlayAll = it.tab == TAB_LAST && games.isNotEmpty(),
                showIcons = !minimizeData(),
            )
        }
    }

    private fun fetchGames() {
        Log.i(TAG, "Fetching games...")
        webHandler.getGamesOwned(steamId).enqueue(object : Callback<GamesOwnedResponse> {
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