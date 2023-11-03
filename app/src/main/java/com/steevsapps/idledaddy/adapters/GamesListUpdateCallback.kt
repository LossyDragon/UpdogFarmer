package com.steevsapps.idledaddy.adapters

import androidx.recyclerview.widget.ListUpdateCallback

/**
 * This ListUpdateCallback considers that when the list header is enabled,
 * item positions will be off by one.
 */
internal class GamesListUpdateCallback(
    private val adapter: GamesAdapter,
    headerEnabled: Boolean
) : ListUpdateCallback {

    private val offset: Int = if (headerEnabled) 1 else 0

    override fun onInserted(position: Int, count: Int) {
        adapter.notifyItemRangeInserted(position + offset, count)
    }

    override fun onRemoved(position: Int, count: Int) {
        adapter.notifyItemRangeRemoved(position + offset, count)
    }

    override fun onMoved(fromPosition: Int, toPosition: Int) {
        adapter.notifyItemMoved(fromPosition + offset, toPosition + offset)
    }

    override fun onChanged(position: Int, count: Int, payload: Any?) {
        adapter.notifyItemRangeChanged(position + offset, count, payload)
    }
}
