package com.steevsapps.idledaddy.steam.converter

import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.steam.model.GamesOwnedResponse
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import java.util.Locale

/**
 * The API wraps the payload in a `{"response": {...}}` envelope, so this unwraps it before
 * decoding, then rewrites each [Game]'s icon URL and normalizes playtime/drops like the site
 * scraper does elsewhere.
 */
object GamesOwnedResponseSerializer : KSerializer<GamesOwnedResponse> {
    @Serializable
    private class Envelope(val response: Body)

    @Serializable
    private class Body(
        @SerialName("game_count") val count: Int = 0,
        @SerialName("games") val games: List<Game> = listOf(),
    )

    override val descriptor = Envelope.serializer().descriptor

    override fun deserialize(decoder: Decoder): GamesOwnedResponse {
        val jsonDecoder = decoder as JsonDecoder
        val envelope = jsonDecoder.json.decodeFromJsonElement(
            Envelope.serializer(),
            jsonDecoder.decodeJsonElement(),
        )
        val games = envelope.response.games.map { game ->
            game.copy(
                iconUrl = IMG_URL.format(Locale.US, game.appId),
                hoursPlayed = game.hoursPlayed / 60f,
                dropsRemaining = 0,
            )
        }
        return GamesOwnedResponse(count = envelope.response.count, games = games)
    }

    override fun serialize(encoder: Encoder, value: GamesOwnedResponse) {
        throw UnsupportedOperationException("GamesOwnedResponse is deserialize-only")
    }

    private const val IMG_URL =
        "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/%d/header.jpg"
}