package com.steevsapps.idledaddy.steam

import com.steevsapps.idledaddy.steam.model.AppDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SteamAPI {
    @GET("https://store.steampowered.com/api/appdetails")
    suspend fun getAppDetails(@Query("appids") appId: Int): Map<String, AppDetailsResponse>
}
