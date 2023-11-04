package com.steevsapps.idledaddy

import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.steam.model.TimeQuery
import com.steevsapps.idledaddy.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginViewModel : ViewModel() {

    private val timeDifference = MutableLiveData<Int>()

    private val timeoutHandler = Handler()

    private var timeAligned = false

    private val timeoutRunnable = Runnable {
        // Trigger event to show a timeout error
        timeout.call()
    }

    private var webHandler: SteamWebHandler? = null

    val timeout = SingleLiveEvent<Void>()

    fun init(webHandler: SteamWebHandler?) {
        this.webHandler = webHandler
    }

    fun getTimeDifference(): LiveData<Int> {
        if (!timeAligned) {
            alignTime()
        }
        return timeDifference
    }

    fun startTimeout() {
        Log.i(TAG, "Starting login timeout")
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MILLIS.toLong())
    }

    fun stopTimeout() {
        Log.i(TAG, "Stopping login timeout")
        timeoutHandler.removeCallbacks(timeoutRunnable)
    }

    private fun alignTime() {
        val currentTime = Utils.currentUnixTime
        webHandler!!.queryServerTime().enqueue(object : Callback<TimeQuery> {
            override fun onResponse(call: Call<TimeQuery>, response: Response<TimeQuery>) {
                if (response.isSuccessful) {
                    timeDifference.value =
                        (response.body()!!.response!!.serverTime - currentTime).toInt()
                    timeAligned = true
                }
            }

            override fun onFailure(call: Call<TimeQuery>, t: Throwable) {
                Log.e(TAG, "Failed to get server time", t)
            }
        })
    }

    companion object {
        private val TAG = LoginViewModel::class.java.getSimpleName()
        private const val TIMEOUT_MILLIS = 30000
    }
}
