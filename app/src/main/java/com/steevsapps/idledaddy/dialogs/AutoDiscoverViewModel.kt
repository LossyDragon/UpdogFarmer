package com.steevsapps.idledaddy.dialogs

import android.annotation.SuppressLint
import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.utils.Utils
import java.util.ArrayDeque

@Suppress("DEPRECATION")
class AutoDiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private lateinit var webHandler: SteamWebHandler
    private val discoveryQueue = ArrayDeque<String>()

    var isFinished: Boolean = true
        private set

    val statusMessage: LiveData<String>
        field = MutableLiveData<String>()

    fun init(webHandler: SteamWebHandler) {
        this.webHandler = webHandler
    }

    @SuppressLint("StaticFieldLeak")
    fun autodiscover() {
        isFinished = false
        val app = getApplication<Application>()
        object : AsyncTask<Void, String, Boolean>() {
            @Deprecated("Deprecated in Java")
            override fun doInBackground(vararg voids: Void?): Boolean {
                try {
                    if (discoveryQueue.isEmpty()) {
                        // Generate new discovery queue
                        publishProgress(app.getString(R.string.generating_discovery))

                        Utils.runWithRetries(3) {
                            val newQueue = webHandler.generateNewDiscoveryQueue()
                            for (i in 0 until newQueue.length()) {
                                discoveryQueue.add(newQueue.getString(i))
                            }
                        }
                    }

                    val count = discoveryQueue.size
                    for (i in 0 until count) {
                        val appId = discoveryQueue.first
                        publishProgress(app.getString(R.string.discovering, appId, i + 1, count))
                        Utils.runWithRetries(3) {
                            webHandler.clearFromQueue(appId)
                            discoveryQueue.pop()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    return false
                }
                return true
            }

            @Deprecated("Deprecated in Java")
            override fun onProgressUpdate(vararg values: String?) {
                statusMessage.value = values[0]
            }

            @Deprecated("Deprecated in Java")
            override fun onPostExecute(result: Boolean) {
                isFinished = true
                val result = if (result) R.string.discovery_finished else R.string.discovery_error
                statusMessage.value = app.getString(result)
            }
        }.execute()
    }
}
