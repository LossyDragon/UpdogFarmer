package com.steevsapps.idledaddy.listeners

import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.AdapterView
import androidx.fragment.app.FragmentManager
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.fragments.GamesFragment

/**
 * Many events can trigger the onItemSelected call, and it is difficult to keep track of all of them.
 * This solution allows you to only respond to user-initiated changes using an OnTouchListener.
 */
class SpinnerInteractionListener(
    private val fm: FragmentManager
) : AdapterView.OnItemSelectedListener, OnTouchListener {

    private var userSelect = false

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        userSelect = true
        return false
    }

    override fun onItemSelected(adapterView: AdapterView<*>?, view: View, position: Int, id: Long) {
        if (userSelect) {
            handleSelection(position)
        }
    }

    override fun onNothingSelected(adapterView: AdapterView<*>?) {}

    private fun handleSelection(position: Int) {
        val fragment = fm.findFragmentById(R.id.content_frame)
        if (fragment is GamesFragment) {
            when (position) {
                GamesFragment.TAB_GAMES -> fragment.switchToGames() // Library
                GamesFragment.TAB_LAST -> fragment.switchToLastSession() // Last idling session
                GamesFragment.TAB_BLACKLIST -> fragment.switchToBlacklist() // Blacklisted games
            }
        }
    }
}
