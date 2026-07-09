package com.steevsapps.idledaddy.steam.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Game(
    @SerialName("appid")
    val appId: Int,

    @SerialName("name")
    val name: String = "",

    @SerialName("img_logo_url")
    val iconUrl: String = "",

    @SerialName("playtime_forever")
    val hoursPlayed: Float = 0f,

    @SerialName("drops_remaining")
    val dropsRemaining: Int = 0,
) : Comparable<Game> {

    constructor(appId: Int, name: String, hoursPlayed: Float, dropsRemaining: Int) : this(
        appId = appId,
        name = name,
        iconUrl = "https://cdn.akamai.steamstatic.com/steam/apps/$appId/header_292x136.jpg",
        hoursPlayed = hoursPlayed,
        dropsRemaining = dropsRemaining,
    )

    override fun compareTo(other: Game): Int {
        if (hoursPlayed == other.hoursPlayed) {
            return 0
        }
        return if (hoursPlayed < other.hoursPlayed) -1 else 1
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is Game) {
            return false
        }
        return other.appId == appId
    }

    override fun hashCode(): Int {
        // Start with a non-zero constant. Prime is preferred
        var result = 17
        // Include a hash for each field
        result = 31 * result + appId
        return result
    }
}