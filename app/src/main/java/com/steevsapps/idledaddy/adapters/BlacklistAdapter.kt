package com.steevsapps.idledaddy.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.steevsapps.idledaddy.R

class BlacklistAdapter(data: String) : RecyclerView.Adapter<BlacklistAdapter.ViewHolder>() {
    private val dataSet: MutableList<String> = data.trim().split(",")
        .filterTo(mutableListOf()) { it.isNotEmpty() }

    val value: String
        get() = dataSet.joinToString(",")

    fun addItem(item: String) {
        if (item !in dataSet) {
            dataSet.add(0, item)
            notifyItemInserted(0)
        }
    }

    private fun removeItem(position: Int) {
        dataSet.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.blacklist_dialog_item, parent, false)
        return ViewHolder(view).also { vh ->
            vh.removeButton.setOnClickListener { removeItem(vh.bindingAdapterPosition) }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.appId.text = dataSet[position]
    }

    override fun getItemCount(): Int = dataSet.size

    class ViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        val appId: TextView = itemView.findViewById(R.id.appid)
        val removeButton: ImageView = itemView.findViewById(R.id.remove_button)
    }
}
