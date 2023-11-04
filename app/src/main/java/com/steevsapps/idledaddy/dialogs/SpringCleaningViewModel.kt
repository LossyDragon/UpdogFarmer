package com.steevsapps.idledaddy.dialogs

import android.annotation.SuppressLint
import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.SteamWebHandler

class SpringCleaningViewModel(application: Application) : AndroidViewModel(application) {

    private val statusText = MutableLiveData<String>()
    private var service: SteamService? = null
    private var webHandler: SteamWebHandler? = null

    val status: LiveData<String>
        get() = statusText

    var isFinished = true
        private set

    fun init(webHandler: SteamWebHandler?, service: SteamService) {
        this.webHandler = webHandler
        if (this.service == null) {
            this.service = service
        }
    }

    @SuppressLint("StaticFieldLeak")
    fun completeTasks() {
        isFinished = false
        statusText.value = ""

        object : AsyncTask<Void, String, Void>() {
            override fun doInBackground(vararg voids: Void): Void? {
                val taskApps = webHandler!!.taskAppIds

                taskApps.forEach { app ->
                    publishProgress(app)
                    service!!.registerAndIdle(app)
                }

                return null
            }

            override fun onProgressUpdate(vararg values: String) {
                statusText.value = getApplication<Application>()
                    .getString(R.string.now_playing2, values[0])
            }

            override fun onPostExecute(aVoid: Void) {
                isFinished = true
                statusText.value = getApplication<Application>()
                    .getString(R.string.daily_tasks_completed)
            }
        }.execute()
    }
}
