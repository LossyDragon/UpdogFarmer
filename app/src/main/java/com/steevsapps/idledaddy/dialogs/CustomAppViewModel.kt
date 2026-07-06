package com.steevsapps.idledaddy.dialogs

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.model.Game

/**
 * This is temporary until its UI is ported to kotlin.
 */
class CustomAppViewModel(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak") // I know...
    private var service: SteamService? = null
    private var serviceBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder) {
            service = (iBinder as SteamService.LocalBinder).service
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            service = null
        }
    }

    init {
        val app = getApplication<Application>()
        val serviceIntent = SteamService.createIntent(app)
        app.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        serviceBound = true
    }

    override fun onCleared() {
        if (serviceBound) {
            getApplication<Application>().unbindService(connection)
            serviceBound = false
        }
    }

    fun idleGame(game: Game) {
        service?.addGame(game)
    }
}