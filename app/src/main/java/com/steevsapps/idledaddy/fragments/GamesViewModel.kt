package com.steevsapps.idledaddy.fragments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.steevsapps.idledaddy.preferences.PrefsManager.getSortValue
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
    private lateinit var webHandler: SteamWebHandler
    private var steamId: Long = 0
    private val games: MutableLiveData<MutableList<Game>> = MutableLiveData()
    private var sortId: Int = SORT_ALPHABETICALLY

    fun init(webHandler: SteamWebHandler, steamId: Long) {
        this.webHandler = webHandler
        this.steamId = steamId
        this.sortId = getSortValue()
    }

    fun getGames(): LiveData<MutableList<Game>> = games

    fun setGames(games: List<Game>) {
        val games = games.toMutableList()
        when (sortId) {
            SORT_ALPHABETICALLY -> games.sortBy { it.name.lowercase(Locale.getDefault()) }
            SORT_HOURS_PLAYED -> games.sortDescending()
            SORT_HOURS_PLAYED_REVERSED -> games.sort()
        }
        this.games.value = games
    }

    fun sort(sortId: Int) {
        if (this.sortId == sortId) {
            return
        }

        val games = this.games.value
        if (!games.isNullOrEmpty()) {
            this.sortId = sortId
            setGames(games)
        }

        writeSortValue(sortId)
    }

    fun fetchGames() {
        Log.i(TAG, "Fetching games...")
        webHandler.getGamesOwned(steamId).enqueue(object : Callback<GamesOwnedResponse> {
            override fun onResponse(
                call: Call<GamesOwnedResponse>,
                response: Response<GamesOwnedResponse>
            ) {
                if (response.isSuccessful) {
                    Log.i(TAG, "Success!")
                    setGames(response.body()!!.games)
                } else {
                    Log.i(TAG, "Got error code: ${response.code()}")
                    setGames(ArrayList())
                }
            }

            override fun onFailure(call: Call<GamesOwnedResponse>, t: Throwable) {
                Log.i(TAG, "Got error", t)
                setGames(ArrayList())
            }
        })
    }

    companion object {
        private val TAG: String = GamesViewModel::class.java.simpleName

        const val SORT_ALPHABETICALLY: Int = 0
        const val SORT_HOURS_PLAYED: Int = 1
        const val SORT_HOURS_PLAYED_REVERSED: Int = 2
    }
}