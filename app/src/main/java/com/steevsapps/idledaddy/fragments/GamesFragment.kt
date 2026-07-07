package com.steevsapps.idledaddy.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.steevsapps.idledaddy.MainActivity
import com.steevsapps.idledaddy.dialogs.RedeemDialog
import com.steevsapps.idledaddy.preferences.PrefsManager.writeLastSession
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.screen.games.GamesScreen
import com.steevsapps.idledaddy.ui.screen.games.GamesViewModel

class GamesFragment : Fragment() {
    private lateinit var viewModel: GamesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[GamesViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            GamesScreen(
                viewModel = viewModel,
                onMenuClick = { (activity as? MainActivity)?.openDrawer() },
                onRedeem = { RedeemDialog.newInstance().show(parentFragmentManager, "redeem") },
            )
        }
    }

    override fun onPause() {
        val selected = viewModel.uiState.selected
        if (selected.isNotEmpty()) {
            // Save idling session
            writeLastSession(selected.toMutableList())
        }
        super.onPause()
    }

    /**
     * Called by MainActivity when the currently idling games change
     */
    fun update(games: ArrayList<Game>) {
        viewModel.setSelected(games)
    }

    companion object {
        // FAB menu tabs
        const val TAB_GAMES: Int = 0
        const val TAB_LAST: Int = 1
        const val TAB_BLACKLIST: Int = 2

        fun newInstance(): GamesFragment = GamesFragment()
    }
}