package com.steevsapps.idledaddy.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.os.BundleCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.adapters.GamesAdapter
import com.steevsapps.idledaddy.listeners.GamesListUpdateListener
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.preferences.PrefsManager.getBlacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.writeLastSession
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.steam.model.Game

class GamesFragment : Fragment(), SearchView.OnQueryTextListener, OnRefreshListener,
    GamesListUpdateListener {
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GamesAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var searchView: SearchView
    private lateinit var emptyView: TextView
    private lateinit var viewModel: GamesViewModel
    private lateinit var fab: FloatingActionButton

    private var steamId: Long = 0
    private var currentGames: ArrayList<Game> = ArrayList()

    private var currentTab: Int = TAB_GAMES

    fun update(games: ArrayList<Game>) {
        currentGames = games
        adapter.setCurrentGames(currentGames)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        steamId = requireArguments().getLong(STEAM_ID)
        viewModel = ViewModelProvider(this)[GamesViewModel::class.java]
        viewModel.init(SteamWebHandler.instance, steamId)
        if (savedInstanceState != null) {
            currentGames = BundleCompat.getParcelableArrayList(
                savedInstanceState, CURRENT_GAMES, Game::class.java
            ) ?: ArrayList()
            currentTab = savedInstanceState.getInt(CURRENT_TAB)
        } else {
            currentGames = BundleCompat.getParcelableArrayList(
                requireArguments(), CURRENT_GAMES, Game::class.java
            ) ?: ArrayList()
            currentTab = requireArguments().getInt(CURRENT_TAB)
            if (steamId == 0L) {
                Toast.makeText(activity, R.string.error_not_logged_in, Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    override fun onPause() {
        if (currentGames.isNotEmpty()) {
            // Save idling session
            writeLastSession(currentGames)
        }
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(CURRENT_GAMES, currentGames)
        outState.putInt(CURRENT_TAB, currentTab)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.games_fragment, container, false)
        refreshLayout = view.findViewById(R.id.refresh_layout)
        refreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryDark)
        refreshLayout.setOnRefreshListener(this)
        refreshLayout.isRefreshing = true

        recyclerView = view.findViewById(R.id.games_list)
        adapter = GamesAdapter(recyclerView.context)
        adapter.setListener(this)
        adapter.setCurrentGames(currentGames)
        adapter.setHeaderEnabled(currentTab == TAB_LAST)
        layoutManager = GridLayoutManagerWrapper(
            recyclerView.context,
            resources.getInteger(R.integer.game_columns)
        )
        layoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    GamesAdapter.ITEM_HEADER -> layoutManager.spanCount
                    GamesAdapter.ITEM_NORMAL -> 1
                    else -> -1
                }
            }
        }

        recyclerView.setLayoutManager(layoutManager)
        recyclerView.setHasFixedSize(true)
        recyclerView.setAdapter(adapter)

        emptyView = view.findViewById(R.id.empty_view)
        fab = view.findViewById(R.id.redeem)
        // Show redeem button if user is logged in
        if (steamId > 0) {
            fab.show()
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_games, menu)
                val searchItem = menu.findItem(R.id.search)
                searchView = searchItem.actionView as SearchView
                searchView.setOnQueryTextListener(this@GamesFragment)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.refresh -> {
                        fetchGames()
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

        viewModel.getGames().observe(
            viewLifecycleOwner,
            Observer<MutableList<Game>> { games -> setGames(games) })
        loadData()

        return view
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        adapter.filter(query)
        searchView.clearFocus()
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        adapter.filter(newText)
        return true
    }

    private fun loadData() {
        if (viewModel.getGames().value == null) {
            fetchGames()
        }
    }

    /**
     * Switch to the 'Games' tab
     */
    fun switchToGames() {
        currentTab = TAB_GAMES
        fetchGames()
    }

    /**
     * Switch to the 'Last Session' tab
     */
    fun switchToLastSession() {
        currentTab = TAB_LAST
        fetchGames()
    }

    /**
     * Switch to the 'Blacklist' tab
     */
    fun switchToBlacklist() {
        currentTab = TAB_BLACKLIST
        fetchGames()
    }

    private fun fetchGames() {
        if (currentTab == TAB_LAST) {
            // Load last idling session
            val games = if (currentGames.isNotEmpty()) {
                currentGames
            } else {
                PrefsManager.getLastSession().filterNotNull().toMutableList()
            }
            viewModel.setGames(games)
        } else {
            // Fetch games from Steam
            refreshLayout.isRefreshing = true
            viewModel.fetchGames()
        }
    }

    /**
     * Update games list
     * @param games the list of games
     */
    private fun setGames(games: MutableList<Game>) {
        if (currentTab == TAB_BLACKLIST) {
            // Only list blacklisted games
            val blacklist = getBlacklist()
            val blacklistGames: MutableList<Game> = ArrayList()
            for (game in games) {
                if (blacklist.contains(game.appId.toString())) {
                    blacklistGames.add(game)
                }
            }
            adapter.setHeaderEnabled(false)
            adapter.setData(blacklistGames)
            emptyView.visibility = if (blacklistGames.isEmpty()) View.VISIBLE else View.GONE
        } else {
            adapter.setHeaderEnabled(!games.isEmpty() && currentTab == TAB_LAST)
            adapter.setData(games)
            emptyView.visibility = if (games.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onRefresh() {
        fetchGames()
    }

    override fun onGamesListUpdated() {
        // Scroll to top
        recyclerView.scrollToPosition(0)
        refreshLayout.isRefreshing = false
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