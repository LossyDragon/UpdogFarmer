package com.steevsapps.idledaddy.ui.screen.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.SteamServiceConnection
import com.steevsapps.idledaddy.steam.SteamServiceState
import com.steevsapps.idledaddy.steam.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class HomeUiState(
    val loggedIn: Boolean = false,
    val farming: Boolean = false,
    val paused: Boolean = false,
    val blocked: Boolean = false,
    val nextRetryAtMillis: Long = 0L,
    val parentalStatus: Boolean = false,
    val currentGames: List<Game> = emptyList(),
    val gameCount: Int = 0,
    val cardCount: Int = 0,
    val showDropInfo: Boolean = false,
    val customAppDialogVisible: Boolean = false,
    val itemAnnouncements: Int = 0,
)

class HomeViewModel(private val connection: SteamServiceConnection) : ViewModel() {

    val uiState: StateFlow<HomeUiState>
        field = MutableStateFlow(HomeUiState())

    private val service: SteamService?
        get() = connection.service.value

    private val serviceState: SteamServiceState
        get() = connection.state.value

    init {
        viewModelScope.launch {
            connection.state.collect { state ->
                uiState.update {
                    it.copy(
                        loggedIn = state.loggedIn,
                        farming = state.farming,
                        paused = state.paused,
                        blocked = state.blockedIdle,
                        nextRetryAtMillis = state.nextRetryAtMillis,
                        parentalStatus = state.parentalStatus,
                        currentGames = state.currentGames,
                        gameCount = state.gameCount,
                        cardCount = state.cardCount,
                        showDropInfo = state.farming,
                        itemAnnouncements = state.itemAnnouncements,
                    )
                }
            }
        }
    }

    fun startFarming() {
        service?.startFarming()
    }

    fun stopGame() {
        service?.stopGame()
    }

    fun pauseOrResume() {
        val service = service ?: return
        if (serviceState.paused) service.resumeGame() else service.pauseGame()
    }

    fun skipGame() {
        service?.skipGame()
    }

    /**
     * Idle a hidden/non-Steam game
     */
    fun idleGame(game: Game) {
        service?.addGame(game)
    }

    /**
     * Idle a list of hidden games by app ID
     */
    fun idleGames(games: List<Game>) {
        service?.addGames(games)
    }
}