package com.steevsapps.idledaddy.steam;

import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse;
import com.steevsapps.idledaddy.steam.model.TimeQuery;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;

public interface SteamAPI {
    @GET("IPlayerService/GetOwnedGames/v0001/?include_appinfo=1&format=json")
    Call<GamesOwnedResponse> getGamesOwned(@QueryMap Map<String,String> args);

    @FormUrlEncoded
    @POST("ITwoFactorService/QueryTime/v0001")
    Call<TimeQuery> queryServerTime(@Field("steamid") String steamId);
}
