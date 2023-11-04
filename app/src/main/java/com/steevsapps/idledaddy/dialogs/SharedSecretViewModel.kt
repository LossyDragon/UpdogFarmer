package com.steevsapps.idledaddy.dialogs

import android.annotation.SuppressLint
import android.app.Application
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager.writeSharedSecret
import eu.chainfire.libsuperuser.Shell
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

class SharedSecretViewModel(application: Application) : AndroidViewModel(application) {

    private val statusText = MutableLiveData<String>()
    private var steamId: Long = 0
    private var suAvailable = false
    private var suResult: List<String>? = null

    val status: LiveData<String>
        get() = statusText

    fun init(steamId: Long) {
        this.steamId = steamId
    }

    fun setValue(value: String) {
        statusText.value = value
    }

    @get:SuppressLint("staticfieldleak")
    val sharedSecret: Unit
        get() {
            object : AsyncTask<Void, Void, Void>() {
                override fun doInBackground(vararg voids: Void): Void? {
                    // Check if root is available
                    suAvailable = Shell.SU.available()

                    if (!suAvailable) {
                        return null
                    }

                    // Read the SteamGuard file
                    suResult = Shell.SU.run(String.format(Locale.US, STEAMGUARD_CMD, steamId))

                    return null
                }

                override fun onPostExecute(aVoid: Void?) {
                    if (!suAvailable) {
                        Log.e(TAG, "Device is not rooted")

                        statusText.value = getApplication<Application>()
                            .getString(R.string.device_not_rooted)

                        return
                    }

                    val sb = StringBuilder()
                    suResult?.forEach { line ->
                        sb.append(line).append("\n")
                    }

                    try {
                        val sharedSecret = JSONObject(sb.toString())
                            .optString("shared_secret")

                        if (sharedSecret.isNotEmpty()) {
                            Log.i(TAG, "shared_secret import successful")

                            writeSharedSecret(sharedSecret)

                            statusText.value = getApplication<Application>()
                                .getString(R.string.your_shared_secret, sharedSecret)

                            return
                        }
                    } catch (e: JSONException) {
                        Log.e(TAG, "Failed to import shared_secret", e)

                        statusText.value = getApplication<Application>()
                            .getString(R.string.import_shared_secret_failed)

                        return
                    }

                    Log.e(TAG, "Failed to import shared_secret")

                    statusText.value = getApplication<Application>()
                        .getString(R.string.import_shared_secret_failed)
                }
            }.execute()
        }

    companion object {
        private val TAG = SharedSecretViewModel::class.java.getSimpleName()

        // Shell command to read SteamGuard file
        private const val STEAMGUARD_CMD =
            "cat /data/data/com.valvesoftware.android.steam.community/files/Steamguard-%d"
    }
}
