package com.steevsapps.idledaddy.steam.model

import com.google.gson.annotations.SerializedName

class GamesOwnedResponse {
    @SerializedName("game_count")
    val count = 0

    @SerializedName("games")
    val games: List<Game>? = null
}
