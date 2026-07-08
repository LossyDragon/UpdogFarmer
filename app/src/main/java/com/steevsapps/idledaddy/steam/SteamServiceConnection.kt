package com.steevsapps.idledaddy.steam

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * App-wide bind to [SteamService], shared by every ViewModel instead of each
 * holding its own [ServiceConnection]. Bound with the application context for
 * the lifetime of the process.
 */
class SteamServiceConnection(private val context: Context) : ServiceConnection {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val service: StateFlow<SteamService?>
        field = MutableStateFlow(null)

    /**
     * State of the currently bound service, or a logged-out default while unbound
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<SteamServiceState> = service
        .flatMapLatest { it?.state ?: flowOf(SteamServiceState()) }
        .stateIn(scope, SharingStarted.Eagerly, SteamServiceState())

    private var bound = false

    fun bind() {
        if (bound) return
        val intent = SteamService.createIntent(context)
        ContextCompat.startForegroundService(context, intent)
        context.bindService(intent, this, Context.BIND_AUTO_CREATE)
        bound = true
    }

    fun unbind() {
        if (!bound) return
        context.unbindService(this)
        service.value = null
        bound = false
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder) {
        service.value = (binder as SteamService.LocalBinder).service
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        service.value = null
    }
}