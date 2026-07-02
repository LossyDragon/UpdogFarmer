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

class GamesAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val dataSet: MutableList<Game> = mutableListOf()
    private val dataSetCopy: MutableList<Game> = mutableListOf()
    private val gamePickedListener: GamePickedListener = context as? GamePickedListener
        ?: throw ClassCastException("$context must implement GamePickedListener.")
    private var currentGames: ArrayList<Game> = ArrayList()
    private var headerEnabled = false
    private val pendingUpdates: Deque<MutableList<Game>> = ArrayDeque()
    private var updateListener: GamesListUpdateListener? = null

    fun setListener(listener: GamesListUpdateListener?) {
        this.updateListener = listener
    }

    fun setData(games: MutableList<Game>) {
        dataSet.clear()
        dataSetCopy.clear()
        dataSetCopy.addAll(games)
        updateData(games)
    }

    fun updateData(games: MutableList<Game>) {
        pendingUpdates.push(games)
        if (pendingUpdates.size > 1) {
            return
        }
        updateDataInternal(games)
    }

    private fun updateDataInternal(newGames: MutableList<Game>) {
        val oldGames: MutableList<Game> = ArrayList(dataSet)
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val diffResult = DiffUtil.calculateDiff(GamesDiffCallback(newGames, oldGames))
            handler.post { applyDiffResult(newGames, diffResult) }
        }.start()
    }

    private fun applyDiffResult(games: MutableList<Game>, diffResult: DiffResult) {
        pendingUpdates.remove(games)
        dispatchUpdates(games, diffResult)
        if (pendingUpdates.isNotEmpty()) {
            val latest = pendingUpdates.pop()
            pendingUpdates.clear()
            updateDataInternal(latest)
        }
    }

    private fun dispatchUpdates(games: MutableList<Game>, diffResult: DiffResult) {
        diffResult.dispatchUpdatesTo(GamesListUpdateCallback(this, headerEnabled))
        dataSet.clear()
        dataSet.addAll(games)
        updateListener?.onGamesListUpdated()
    }

    fun filter(text: String) {
        if (text.isEmpty()) {
            updateData(dataSetCopy.toMutableList())
        } else {
            val query = text.lowercase(Locale.getDefault())
            val newGames = dataSetCopy.filterTo(mutableListOf()) {
                it.name.lowercase(Locale.getDefault()).contains(query)
            }
            updateData(newGames)
        }
    }

    fun setCurrentGames(games: ArrayList<Game>) {
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
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.games_header_item, parent, false)
            return VHHeader(view)
        } else if (viewType == ITEM_NORMAL) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.games_item, parent, false)
            return VHItem(view)
        }
        throw IllegalArgumentException("Unknown view type: $viewType")
    }

    override fun getItemViewType(position: Int): Int {
        if (headerEnabled && position == 0) {
            return ITEM_HEADER
        }
        return ITEM_NORMAL
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is VHItem) {
            val game = dataSet[if (headerEnabled) position - 1 else position]
            holder.name.text = game.name
            val quantity =
                if (game.hoursPlayed < 1) 0 else ceil(game.hoursPlayed.toDouble()).toInt()
            holder.hours.text = context.resources
                .getQuantityString(R.plurals.hours_on_record, quantity, game.hoursPlayed)
            if (!minimizeData()) {
                Glide.with(context)
                    .load(game.iconUrl)
                    .into(holder.logo)
            } else {
                holder.logo.setImageResource(R.drawable.ic_image_white_48dp)
            }
            holder.itemView.isActivated = currentGames.contains(game)
        }
    }

    override fun getItemCount(): Int = if (headerEnabled) dataSet.size + 1 else dataSet.size

    private inner class VHHeader(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

        init {
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            gamePickedListener.onGamesPicked(dataSet)
            currentGames.clear()
            currentGames.addAll(dataSet)
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

        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return
            val game = dataSet[if (headerEnabled) position - 1 else position]
            if (!currentGames.contains(game) && currentGames.size < 32) {
                currentGames.add(game)
                itemView.isActivated = true
                gamePickedListener.onGamePicked(game)
            } else {
                currentGames.remove(game)
                itemView.isActivated = false
                gamePickedListener.onGameRemoved(game)
            }
        }

        override fun onLongClick(v: View?): Boolean {
            val position = bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) return false
            val game = dataSet[if (headerEnabled) position - 1 else position]
            gamePickedListener.onGameLongPressed(game)
            return true
        }
    }

    companion object {
        const val ITEM_HEADER: Int = 1
        const val ITEM_NORMAL: Int = 2
    }
}