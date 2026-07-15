package com.steevsapps.idledaddy.ui.screen.games

import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.SteamServiceConnection
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.screen.games.GamesViewModel.Companion.MAX_GAMES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val showIcons: Boolean = false,
    val optionsGame: Game? = null,
    val optionsBlacklisted: Boolean = false,
    val fabMenuExpanded: Boolean = false,
    val redeemDialogVisible: Boolean = false,
    val iconOverrides: Map<Int, String> = emptyMap(),
)

class GamesViewModel(
    private val connection: SteamServiceConnection,
    private val webHandler: SteamWebHandler,
) : ViewModel() {

    val uiState: StateFlow<GamesScreenState>
        field = MutableStateFlow(GamesScreenState())

    private var sortId: Int = PrefsManager.getSortValue()
    private var steamId: Long = 0

    // Everything Steam returned; uiState.games holds the tab/query-filtered subset shown on screen
    private var allGames: List<Game> = emptyList()

    // appIds we've already tried to resolve via the store appdetails API, so a
    // repeatedly-failing icon doesn't fire a new request every time the item recomposes
    private val iconRetries = mutableSetOf<Int>()

    private val service: SteamService?
        get() = connection.service.value

    init {
        viewModelScope.launch {
            connection.state
                .distinctUntilChangedBy { it.steamId }
                .collect { state ->
                    steamId = state.steamId
                    uiState.update {
                        it.copy(selected = state.currentGames)
                    }
                    refresh()
                }
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

    /**
     * Save the currently idling games as the last session, called when the screen is paused
     */
    fun saveLastSession() {
        val selected = uiState.value.selected
        if (selected.isNotEmpty()) {
            PrefsManager.writeLastSession(selected)
        }
    }

    fun showOptions(game: Game) {
        uiState.update { it.copy(optionsGame = game, optionsBlacklisted = isBlacklisted(game)) }
    }

    fun dismissOptions() {
        uiState.update { it.copy(optionsGame = null) }
    }

    fun isBlacklisted(game: Game): Boolean =
        PrefsManager.getBlacklist().contains(game.appId.toString())

    /**
     * Add/remove the game from the blacklist and close the options dialog
     */
    fun toggleBlacklist(game: Game) {
        val blacklist = PrefsManager.getBlacklist()
        val id = game.appId.toString()
        if (blacklist.contains(id)) {
            blacklist.remove(id)
        } else {
            blacklist.add(0, id)
        }
        PrefsManager.writeBlacklist(blacklist)
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
        service?.addGames(games)
    }

    fun refresh() {
        if (service == null || !service!!.state.value.loggedIn) {
            return
        }

        if (uiState.value.tab == TAB_LAST) {
            // Load last idling session
            val games = uiState.value.selected.ifEmpty {
                PrefsManager.getLastSession()
            }
            setGames(games)
        } else {
            uiState.update { it.copy(refreshing = true) }
            fetchGames()
        }
    }

    fun sort(sortId: Int) {
        if (this.sortId == sortId) {
            return
        }
        this.sortId = sortId
        PrefsManager.writeSortValue(sortId)
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
            val blacklist = PrefsManager.getBlacklist()
            games = games.filter { blacklist.contains(it.appId.toString()) }
        }
        if (state.query.isNotEmpty()) {
            games = games.filter { it.name.contains(state.query, ignoreCase = true) }
        }
        uiState.update {
            it.copy(
                games = games,
                showPlayAll = it.tab == TAB_LAST && games.isNotEmpty(),
                showIcons = !PrefsManager.minimizeData(),
            )
        }
    }

    private fun fetchGames() {
        Log.i(TAG, "Fetching games...")
        viewModelScope.launch {
            val ownedGames = service!!.getOwnedGames()
            Log.i(TAG, "Got ${ownedGames.first} games!")
            setGames(ownedGames.second)
        }
    }

    fun onImageError(game: Game) {
        if (!iconRetries.add(game.appId)) {
            return
        }
        Log.i(TAG, "Retrying for app image for ${game.appId}")
        viewModelScope.launch {
            val url = runCatching { webHandler.getHeaderImage(game.appId) }
                .onFailure { Log.i(TAG, "Failed to fetch app details for ${game.appId}", it) }
                .getOrNull() ?: return@launch
            uiState.update { it.copy(iconOverrides = it.iconOverrides + (game.appId to url)) }
        }
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