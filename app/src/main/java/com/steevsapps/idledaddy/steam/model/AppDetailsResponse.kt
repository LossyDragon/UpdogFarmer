package com.steevsapps.idledaddy.steam.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppDetailsResponse(
    val success: Boolean = false,
    val data: AppDetails? = null,
)

@Serializable
data class AppDetails(
    @SerialName("header_image")
    val headerImage: String = "",
)