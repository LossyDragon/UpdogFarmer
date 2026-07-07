package com.steevsapps.idledaddy.ui.screen.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.model.Game
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Immutable
data class HomeUiState(
    val loggedIn: Boolean = false,
    val farming: Boolean = false,
    val paused: Boolean = false,
    val parentalStatus: Boolean = false,
    val currentGames: List<Game> = emptyList(),
    val gameCount: Int = 0,
    val cardCount: Int = 0,
    val showDropInfo: Boolean = false,
    val customAppDialogVisible: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    val uiState: StateFlow<HomeUiState>
        field = MutableStateFlow(HomeUiState())

    @SuppressLint("StaticFieldLeak") // I know...
    private var service: SteamService? = null
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder) {
            service = (iBinder as SteamService.LocalBinder).service.also(::syncFromService)
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            service = null
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val service = service ?: return
            when (intent.action) {
                SteamService.LOGIN_EVENT,
                SteamService.DISCONNECT_EVENT,
                SteamService.STOP_EVENT -> syncFromService(service)

                SteamService.FARM_EVENT -> {
                    uiState.update {
                        it.copy(
                            showDropInfo = true,
                            gameCount = intent.getIntExtra(SteamService.GAME_COUNT, 0),
                            cardCount = intent.getIntExtra(SteamService.CARD_COUNT, 0),
                        )
                    }
                }

                SteamService.NOW_PLAYING_EVENT -> {
                    uiState.update {
                        it.copy(
                            farming = service.isFarming,
                            paused = service.isPaused,
                            currentGames = service.currentGames.toList(),
                        )
                    }
                }

                SteamService.PARENTAL_STATUS -> {
                    uiState.update {
                        it.copy(parentalStatus = intent.getBooleanExtra("status", false))
                    }
                }
            }
        }
    }

    init {
        val app = getApplication<Application>()
        val serviceIntent = SteamService.createIntent(app)
        ContextCompat.startForegroundService(app, serviceIntent)
        app.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true

        val filter = IntentFilter().apply {
            addAction(SteamService.LOGIN_EVENT)
            addAction(SteamService.DISCONNECT_EVENT)
            addAction(SteamService.STOP_EVENT)
            addAction(SteamService.FARM_EVENT)
            addAction(SteamService.NOW_PLAYING_EVENT)
            addAction(SteamService.PARENTAL_STATUS)
        }
        LocalBroadcastManager.getInstance(app).registerReceiver(receiver, filter)
    }

    override fun onCleared() {
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(receiver)
        if (serviceBound) {
            getApplication<Application>().unbindService(connection)
            serviceBound = false
        }
    }

    private fun syncFromService(service: SteamService) {
        uiState.update {
            it.copy(
                loggedIn = service.isLoggedIn,
                farming = service.isFarming,
                paused = service.isPaused,
                parentalStatus = service.parentalStatus,
                currentGames = service.currentGames.toList(),
                gameCount = service.gameCount,
                cardCount = service.cardCount,
                showDropInfo = service.isFarming,
            )
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
        if (service.isPaused) service.resumeGame() else service.pauseGame()
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
}