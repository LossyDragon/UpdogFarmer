package com.steevsapps.idledaddy

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.SteamService.LocalBinder
import com.steevsapps.idledaddy.utils.LocaleManager

/**
 * Base activity that's bound to the Steam Service
 */
abstract class BaseActivity : AppCompatActivity() {

    private val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            service = (iBinder as LocalBinder).service
            this@BaseActivity.onServiceConnected()
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            service = null
            serviceBound = false
        }
    }

    private var serviceBound = false

    /**
     * Get the Steam Service
     */
    var service: SteamService? = null
        private set

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onStop() {
        super.onStop()
        doUnbind()
    }

    override fun onStart() {
        super.onStart()
        doBind()
    }

    /**
     * Executed when the Activity is connected to the Service
     */
    protected open fun onServiceConnected() {}

    /**
     * Bind Activity to the service
     */
    private fun doBind() {
        Log.i(TAG, "Binding service...")

        SteamService.createIntent(this).apply {
            ContextCompat.startForegroundService(this@BaseActivity, this)
            bindService(this, connection, BIND_AUTO_CREATE)
        }

        serviceBound = true
    }

    /**
     * Unbind Activity from the service
     */
    private fun doUnbind() {
        if (serviceBound) {
            Log.i(TAG, "Unbinding service...")
            unbindService(connection)
            serviceBound = false
        }
    }

    /**
     * Stop Steam Service and finish Activity
     */
    protected fun stopSteam() {
        doUnbind()
        stopService(SteamService.createIntent(this))
        finish()
    }

    companion object {
        private val TAG = BaseActivity::class.java.getSimpleName()
    }
}
