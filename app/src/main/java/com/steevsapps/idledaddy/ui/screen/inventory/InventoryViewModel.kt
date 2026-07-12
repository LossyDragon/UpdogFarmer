package com.steevsapps.idledaddy.ui.screen.inventory

import androidx.lifecycle.ViewModel
import com.steevsapps.idledaddy.steam.SteamServiceConnection
import com.steevsapps.idledaddy.steam.SteamWebHandler

class InventoryViewModel(
    private val connection: SteamServiceConnection,
    webHandler: SteamWebHandler,
) : ViewModel() {

    val url = "${SteamWebHandler.STEAM_COMMUNITY}profiles/${connection.state.value.steamId}/inventory/"
    val cookies: Map<String, String> = webHandler.generateWebCookies()

    override fun onCleared() {
        connection.service.value?.clearItemAnnouncements()
    }
}