package com.steevsapps.idledaddy.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.os.BundleCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.dialogs.RedeemDialog
import com.steevsapps.idledaddy.preferences.PrefsManager.minimizeData
import com.steevsapps.idledaddy.preferences.PrefsManager.writeLastSession
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.screen.games.GamesScreen

class GamesFragment : Fragment() {
    private lateinit var viewModel: GamesViewModel
    private var searchView: SearchView? = null

    private var steamId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        steamId = requireArguments().getLong(STEAM_ID)
        val currentGames = BundleCompat.getParcelableArrayList(
            requireArguments(), CURRENT_GAMES, Game::class.java
        ) ?: ArrayList()

        viewModel = ViewModelProvider(this)[GamesViewModel::class.java]
        viewModel.init(
            webHandler = SteamWebHandler.instance,
            steamId = steamId,
            tab = requireArguments().getInt(CURRENT_TAB),
            selected = currentGames,
        )

        if (savedInstanceState == null && steamId == 0L) {
            Toast.makeText(activity, R.string.error_not_logged_in, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setContent {
            val state = viewModel.uiState
            val visibleGames = remember(state.games, state.query, state.tab) {
                viewModel.visibleGames()
            }
            GamesScreen(
                games = visibleGames,
                selected = state.selected,
                refreshing = state.refreshing,
                showPlayAll = state.tab == TAB_LAST && visibleGames.isNotEmpty(),
                showRedeem = steamId > 0,
                showIcons = !minimizeData(),
                optionsGame = state.optionsGame,
                optionsBlacklisted = state.optionsGame?.let(viewModel::isBlacklisted) ?: false,
                onRefresh = viewModel::refresh,
                onGameClick = viewModel::toggleGame,
                onGameLongClick = viewModel::showOptions,
                onPlayAll = viewModel::playAll,
                onRedeem = { RedeemDialog.newInstance().show(parentFragmentManager, "redeem") },
                onToggleBlacklist = viewModel::toggleBlacklist,
                onDismissOptions = viewModel::dismissOptions,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_games, menu)
                searchView = (menu.findItem(R.id.search).actionView as SearchView).apply {
                    setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                        override fun onQueryTextSubmit(query: String): Boolean {
                            viewModel.setQuery(query)
                            searchView?.clearFocus()
                            return true
                        }

                        override fun onQueryTextChange(newText: String): Boolean {
                            viewModel.setQuery(newText)
                            return true
                        }
                    })
                }
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.refresh -> {
                        viewModel.refresh()
                        true
                    }

                    R.id.sort_alphabetically -> {
                        viewModel.sort(GamesViewModel.SORT_ALPHABETICALLY)
                        true
                    }

                    R.id.sort_hours_played -> {
                        viewModel.sort(GamesViewModel.SORT_HOURS_PLAYED)
                        true
                    }

                    R.id.sort_hours_played_reversed -> {
                        viewModel.sort(GamesViewModel.SORT_HOURS_PLAYED_REVERSED)
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
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

    fun switchToGames() {
        viewModel.switchTab(TAB_GAMES)
    }

    fun switchToLastSession() {
        viewModel.switchTab(TAB_LAST)
    }

    fun switchToBlacklist() {
        viewModel.switchTab(TAB_BLACKLIST)
    }

    companion object {
        private const val STEAM_ID = "STEAM_ID"
        private const val CURRENT_GAMES = "CURRENT_GAMES"
        private const val CURRENT_TAB = "CURRENT_TAB"

        // Spinner nav items
        const val TAB_GAMES: Int = 0
        const val TAB_LAST: Int = 1
        const val TAB_BLACKLIST: Int = 2

        fun newInstance(
            steamId: Long,
            currentGames: ArrayList<Game>,
            position: Int
        ): GamesFragment {
            val fragment = GamesFragment()
            Bundle().apply {
                putLong(STEAM_ID, steamId)
                putParcelableArrayList(CURRENT_GAMES, currentGames)
                putInt(CURRENT_TAB, position)
            }.also(fragment::setArguments)
            return fragment
        }
    }
}