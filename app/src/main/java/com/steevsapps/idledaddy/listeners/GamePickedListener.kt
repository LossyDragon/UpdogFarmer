package com.steevsapps.idledaddy.listeners

import com.steevsapps.idledaddy.steam.model.Game

interface GamePickedListener {
    fun onGamePicked(game: Game)
    fun onGamesPicked(games: List<Game>)
    fun onGameRemoved(game: Game)
    fun onGameLongPressed(game: Game)
}
