package com.steevsapps.idledaddy.steam

import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse
import com.steevsapps.idledaddy.steam.model.TimeQuery
import `in`.dragonbra.javasteam.types.KeyValue
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.QueryMap

interface SteamAPI {
    @GET("IPlayerService/GetOwnedGames/v0001/?include_appinfo=1&format=json")
    fun getGamesOwned(@QueryMap args: Map<String?, String>): Call<GamesOwnedResponse>

    @FormUrlEncoded
    @POST("ISteamUserAuth/AuthenticateUser/v0001/")
    fun authenticateUser(@FieldMap(encoded = true) args: Map<String, String>): Call<KeyValue>

    @FormUrlEncoded
    @POST("ITwoFactorService/QueryTime/v0001")
    fun queryServerTime(@Field("steamid") steamId: String): Call<TimeQuery>
}
