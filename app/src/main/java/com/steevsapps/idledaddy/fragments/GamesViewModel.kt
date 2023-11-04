package com.steevsapps.idledaddy.fragments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.steevsapps.idledaddy.preferences.PrefsManager.sortValue
import com.steevsapps.idledaddy.preferences.PrefsManager.writeSortValue
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

/**
 * Reminder: Must be public or else you will get a runtime exception
 */
class GamesViewModel : ViewModel() {

    private var games: MutableLiveData<List<Game>>? = null
    private var sortId = SORT_ALPHABETICALLY
    private var steamId: Long = 0
    private var webHandler: SteamWebHandler? = null

    fun init(webHandler: SteamWebHandler?, steamId: Long) {
        this.webHandler = webHandler
        this.steamId = steamId
        sortId = sortValue
    }

    fun getGames(): LiveData<List<Game>> {
        if (games == null) {
            games = MutableLiveData()
        }

        return games!!
    }

    fun setGames(games: List<Game>) {
        when (sortId) {
            SORT_ALPHABETICALLY -> games.sortedBy { it.name.lowercase(Locale.getDefault()) }
            SORT_HOURS_PLAYED -> games.sortedByDescending { it.hoursPlayed }
            SORT_HOURS_PLAYED_REVERSED -> games.sorted()
        }

        this.games!!.value = games
    }

    fun sort(sortId: Int) {
        if (this.sortId == sortId) {
            return
        }

        val games = games!!.getValue()
        if (!games.isNullOrEmpty()) {
            this.sortId = sortId
            setGames(games)
        }

        writeSortValue(sortId)
    }

    fun fetchGames() {
        Log.i(TAG, "Fetching games...")

        webHandler!!.getGamesOwned(steamId).enqueue(
            object : Callback<GamesOwnedResponse> {
                override fun onResponse(
                    call: Call<GamesOwnedResponse>,
                    response: Response<GamesOwnedResponse>
                ) {
                    if (response.isSuccessful) {
                        Log.i(TAG, "Success!")
                        setGames(response.body()!!.games.orEmpty())
                    } else {
                        Log.i(TAG, "Got error code: " + response.code())
                        setGames(ArrayList())
                    }
                }

                override fun onFailure(call: Call<GamesOwnedResponse>, t: Throwable) {
                    Log.i(TAG, "Got error", t)
                    setGames(ArrayList())
                }
            }
        )
    }

    companion object {
        private val TAG = GamesViewModel::class.java.getSimpleName()

        const val SORT_ALPHABETICALLY = 0
        const val SORT_HOURS_PLAYED = 1
        const val SORT_HOURS_PLAYED_REVERSED = 2
    }
}
