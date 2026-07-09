package com.steevsapps.idledaddy.steam.model

import com.steevsapps.idledaddy.steam.converter.GamesOwnedResponseSerializer
import kotlinx.serialization.Serializable

@Serializable(with = GamesOwnedResponseSerializer::class)
data class GamesOwnedResponse(
    val count: Int = 0,
    val games: List<Game> = listOf(),
)