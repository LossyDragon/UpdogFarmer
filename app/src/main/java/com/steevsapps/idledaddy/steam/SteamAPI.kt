package com.steevsapps.idledaddy.steam

import com.steevsapps.idledaddy.steam.model.AppDetailsResponse
import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface SteamAPI {
    @GET("IPlayerService/GetOwnedGames/v0001/?include_appinfo=1&format=json")
    fun getGamesOwned(@QueryMap args: MutableMap<String, String>): Call<GamesOwnedResponse>

    @GET("https://store.steampowered.com/api/appdetails")
    suspend fun getAppDetails(@Query("appids") appId: Int): Map<String, AppDetailsResponse>
}
