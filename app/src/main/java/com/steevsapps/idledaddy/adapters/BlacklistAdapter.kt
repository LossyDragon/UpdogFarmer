package com.steevsapps.idledaddy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.utils.Utils.arrayToString
import java.util.Arrays

class BlacklistAdapter(
    private var data: String?
) : RecyclerView.Adapter<BlacklistAdapter.ViewHolder>() {

    private val dataSet: MutableList<String> = ArrayList()

    val value: String
        get() = arrayToString(dataSet)

    init {
        data = data?.trim()

        data?.let { d ->
            if (d.isNotEmpty()) {
                dataSet.addAll(
                    listOf(
                        *d.split(",".toRegex())
                            .dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    )
                )
            }
        }
    }

    fun addItem(item: String) {
        if (!dataSet.contains(item)) {
            dataSet.add(0, item)
            notifyItemInserted(0)
        }
    }

    private fun removeItem(position: Int) {
        dataSet.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.blacklist_dialog_item, parent, false)

        return ViewHolder(view).apply {
            removeButton.setOnClickListener { removeItem(getAdapterPosition()) }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appId = dataSet[position]
        holder.appId.text = appId
    }

    override fun getItemCount(): Int = dataSet.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appId: TextView = itemView.findViewById(R.id.appid)
        val removeButton: ImageView = itemView.findViewById(R.id.remove_button)
    }
}
