package com.steevsapps.idledaddy.dialogs

import android.annotation.SuppressLint
import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.utils.Utils.runWithRetries
import java.util.ArrayDeque

class AutoDiscoverViewModel(application: Application) : AndroidViewModel(application) {

    private val statusText = MutableLiveData<String>()
    private var webHandler: SteamWebHandler? = null
    private val discoveryQueue = ArrayDeque<String>()

    var isFinished = true
        private set

    val status: LiveData<String>
        get() = statusText

    fun init(webHandler: SteamWebHandler?) {
        this.webHandler = webHandler
    }

    @SuppressLint("StaticFieldLeak")
    fun autodiscover() {
        isFinished = false
        object : AsyncTask<Void, String, Boolean>() {
            override fun doInBackground(vararg voids: Void): Boolean {
                try {
                    if (discoveryQueue.isEmpty()) {
                        // Generate new discovery queue
                        publishProgress(getApplication<Application>().getString(R.string.generating_discovery))

                        runWithRetries(3) {
                            val newQueue = webHandler!!.generateNewDiscoveryQueue()
                            var i = 0
                            val count = newQueue.length()

                            while (i < count) {
                                discoveryQueue.add(newQueue.getString(i))
                                i++
                            }
                        }
                    }

                    var i = 0
                    val count = discoveryQueue.size
                    while (i < count) {
                        val appId = discoveryQueue.getFirst()

                        publishProgress(
                            getApplication<Application>().getString(
                                R.string.discovering,
                                appId,
                                i + 1,
                                count
                            )
                        )

                        runWithRetries(3) {
                            webHandler!!.clearFromQueue(appId)
                            discoveryQueue.pop()
                        }

                        i++
                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                    return false
                }

                return true
            }

            override fun onProgressUpdate(vararg values: String) {
                statusText.value = values[0]
            }

            override fun onPostExecute(result: Boolean) {
                isFinished = true

                val string = if (result) R.string.discovery_finished else R.string.discovery_error
                statusText.value = getApplication<Application>().getString(string)
            }
        }.execute()
    }
}
