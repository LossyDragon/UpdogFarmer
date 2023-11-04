package com.steevsapps.idledaddy.adapters

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.DiffResult
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.listeners.GamePickedListener
import com.steevsapps.idledaddy.listeners.GamesListUpdateListener
import com.steevsapps.idledaddy.preferences.PrefsManager.minimizeData
import com.steevsapps.idledaddy.steam.model.Game
import java.util.ArrayDeque
import java.util.Deque
import java.util.Locale
import kotlin.math.ceil

class GamesAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val dataSet: MutableList<Game> = ArrayList()
    private val dataSetCopy: MutableList<Game> = ArrayList()
    private val pendingUpdates: Deque<List<Game>> = ArrayDeque()
    private var currentGames: MutableList<Game>? = null
    private var gamePickedListener: GamePickedListener? = null
    private var headerEnabled = false
    private var updateListener: GamesListUpdateListener? = null

    init {
        gamePickedListener = try {
            context as GamePickedListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement GamePickedListener.")
        }
    }

    fun setListener(listener: GamesListUpdateListener?) {
        updateListener = listener
    }

    fun setData(games: List<Game>) {
        dataSet.clear()
        dataSetCopy.clear()
        dataSetCopy.addAll(games)
        updateData(games)
    }

    fun updateData(games: List<Game>) {
        pendingUpdates.push(games)

        if (pendingUpdates.size > 1) {
            return
        }

        updateDataInternal(games)
    }

    private fun updateDataInternal(newGames: List<Game>) {
        val oldGames: List<Game> = ArrayList(dataSet)
        val handler = Handler(Looper.getMainLooper())

        Thread {
            val diffResult = DiffUtil.calculateDiff(GamesDiffCallback(newGames, oldGames))
            handler.post { applyDiffResult(newGames, diffResult) }
        }.start()
    }

    private fun applyDiffResult(games: List<Game>, diffResult: DiffResult) {
        pendingUpdates.remove(games)
        dispatchUpdates(games, diffResult)

        if (pendingUpdates.size > 0) {
            val latest = pendingUpdates.pop()
            pendingUpdates.clear()
            updateDataInternal(latest)
        }
    }

    private fun dispatchUpdates(games: List<Game>, diffResult: DiffResult) {
        diffResult.dispatchUpdatesTo(GamesListUpdateCallback(this, headerEnabled))
        dataSet.clear()
        dataSet.addAll(games)
        updateListener?.onGamesListUpdated()
    }

    fun filter(text: String) {
        val newGames: MutableList<Game> = ArrayList()
        if (text.isEmpty()) {
            newGames.addAll(dataSetCopy)
            updateData(newGames)
        } else {
            for (game in dataSetCopy) {
                if (game.name
                    .lowercase(Locale.getDefault())
                    .contains(text.lowercase(Locale.getDefault()))
                ) {
                    newGames.add(game)
                }
            }

            updateData(newGames)
        }
    }

    fun setCurrentGames(games: MutableList<Game>) {
        currentGames = games
        notifyDataSetChanged()
    }

    fun setHeaderEnabled(b: Boolean) {
        if (headerEnabled != b) {
            headerEnabled = b

            if (headerEnabled) {
                notifyItemInserted(0)
            } else {
                notifyItemRemoved(0)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == ITEM_HEADER) {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.games_header_item, parent, false)

            return VHHeader(view)
        } else if (viewType == ITEM_NORMAL) {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.games_item, parent, false)

            return VHItem(view)
        }

        throw IllegalArgumentException("Unknown view type: $viewType")
    }

    override fun getItemViewType(position: Int): Int =
        if (headerEnabled && position == 0) ITEM_HEADER else ITEM_NORMAL

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == ITEM_HEADER) {
            holder as VHHeader
        } else if (holder.itemViewType == ITEM_NORMAL) {
            val item = holder as VHItem
            val game = dataSet[if (headerEnabled) position - 1 else position]

            item.name.text = game.name

            val quantity =
                if (game.hoursPlayed < 1) 0 else ceil(game.hoursPlayed.toDouble()).toInt()

            item.hours.text = context.resources
                .getQuantityString(R.plurals.hours_on_record, quantity, game.hoursPlayed)

            if (!minimizeData()) {
                Glide.with(context)
                    .load(game.iconUrl)
                    .into(item.logo)
            } else {
                item.logo.setImageResource(R.drawable.ic_image_white_48dp)
            }

            item.itemView.isActivated = currentGames!!.contains(game)
        }
    }

    override fun getItemCount(): Int = if (headerEnabled) dataSet.size + 1 else dataSet.size

    private inner class VHHeader(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            gamePickedListener!!.onGamesPicked(dataSet)
            currentGames!!.clear()
            currentGames!!.addAll(dataSet)
            notifyDataSetChanged()
        }
    }

    private inner class VHItem(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener, OnLongClickListener {

        val name: TextView = itemView.findViewById(R.id.name)
        val logo: ImageView = itemView.findViewById(R.id.logo)
        val hours: TextView = itemView.findViewById(R.id.hours)

        init {
            itemView.setOnClickListener(this)
            itemView.setOnLongClickListener(this)
        }

        override fun onClick(v: View) {
            val position = getAdapterPosition()

            if (position == RecyclerView.NO_POSITION) {
                return
            }

            val game = dataSet[if (headerEnabled) position - 1 else position]

            if (!currentGames!!.contains(game) && currentGames!!.size < 32) {
                currentGames!!.add(game)
                itemView.isActivated = true
                gamePickedListener!!.onGamePicked(game)
            } else {
                currentGames!!.remove(game)
                itemView.isActivated = false
                gamePickedListener!!.onGameRemoved(game)
            }
        }

        override fun onLongClick(v: View): Boolean {
            val position = getAdapterPosition()

            if (position == RecyclerView.NO_POSITION) {
                return false
            }

            val game = dataSet[if (headerEnabled) position - 1 else position]
            gamePickedListener!!.onGameLongPressed(game)

            return true
        }
    }

    companion object {
        const val ITEM_HEADER = 1
        const val ITEM_NORMAL = 2
    }
}
