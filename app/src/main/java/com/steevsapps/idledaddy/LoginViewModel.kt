package com.steevsapps.idledaddy

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {
    private val timeoutHandler = Handler(Looper.getMainLooper())

    val timeout: SingleLiveEvent<Void?> = SingleLiveEvent()
    private val timeoutRunnable: Runnable = Runnable {
        // Trigger event to show a timeout error
        timeout.call()
    }

    fun startTimeout() {
        Log.i(TAG, "Starting login timeout")
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MILLIS.toLong())
    }

    fun stopTimeout() {
        Log.i(TAG, "Stopping login timeout")
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }

    companion object {
        private val TAG: String = LoginViewModel::class.java.getSimpleName()
        private const val TIMEOUT_MILLIS = 30000
    }
}
