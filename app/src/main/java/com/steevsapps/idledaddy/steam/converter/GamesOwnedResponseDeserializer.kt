package com.steevsapps.idledaddy.steam.converter

import com.google.gson.Gson
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse
import java.lang.reflect.Type
import java.util.Locale

class GamesOwnedResponseDeserializer : JsonDeserializer<GamesOwnedResponse> {
    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): GamesOwnedResponse {
        val element = json.getAsJsonObject()["response"]
        val response = Gson().fromJson(element, GamesOwnedResponse::class.java)

        response.games.forEach { game ->
            game.iconUrl = String.format(Locale.US, IMG_URL, game.appId, game.iconUrl)
            game.hoursPlayed /= 60f
            game.dropsRemaining = 0
        }

        return response
    }

    companion object {
        private const val IMG_URL =
            "http://media.steampowered.com/steamcommunity/public/images/apps/%d/%s.jpg"
    }
}
