package com.steevsapps.idledaddy.adapters

import androidx.recyclerview.widget.DiffUtil
import com.steevsapps.idledaddy.steam.model.Game

internal class GamesDiffCallback(
    private val newGames: List<Game>,
    private val oldGames: List<Game>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldGames.size

    override fun getNewListSize(): Int = newGames.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
        oldGames[oldItemPosition].appId == newGames[newItemPosition].appId

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldGame = oldGames[oldItemPosition]
        val newGame = newGames[newItemPosition]
        return oldGame.appId == newGame.appId && oldGame.hoursPlayed == newGame.hoursPlayed
    }
}
