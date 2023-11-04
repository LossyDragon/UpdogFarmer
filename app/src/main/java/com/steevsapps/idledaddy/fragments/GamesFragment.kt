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
import androidx.fragment.app.Fragment
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
import com.steevsapps.idledaddy.preferences.PrefsManager.blacklist
import com.steevsapps.idledaddy.preferences.PrefsManager.lastSession
import com.steevsapps.idledaddy.preferences.PrefsManager.writeLastSession
import com.steevsapps.idledaddy.steam.SteamWebHandler
import com.steevsapps.idledaddy.steam.model.Game

class GamesFragment :
    Fragment(),
    SearchView.OnQueryTextListener,
    OnRefreshListener,
    GamesListUpdateListener {

    private lateinit var emptyView: TextView
    private lateinit var fab: FloatingActionButton
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var refreshLayout: SwipeRefreshLayout
    private lateinit var searchView: SearchView
    private var adapter: GamesAdapter? = null
    private var currentGames: MutableList<Game>? = null
    private var currentTab = TAB_GAMES
    private var steamId: Long = 0
    private var viewModel: GamesViewModel? = null

    fun update(games: MutableList<Game>) {
        currentGames = games
        adapter!!.setCurrentGames(currentGames!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        steamId = requireArguments().getLong(STEAM_ID)

        viewModel = ViewModelProvider(this)[GamesViewModel::class.java]
        viewModel!!.init(SteamWebHandler.instance, steamId)

        if (savedInstanceState != null) {
            currentGames = savedInstanceState.getParcelableArrayList(CURRENT_GAMES)
            currentTab = savedInstanceState.getInt(CURRENT_TAB)
        } else {
            currentGames = requireArguments().getParcelableArrayList(CURRENT_GAMES)
            currentTab = requireArguments().getInt(CURRENT_TAB)

            if (steamId == 0L) {
                Toast.makeText(activity, R.string.error_not_logged_in, Toast.LENGTH_LONG).show()
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onPause() {
        if (currentGames!!.isNotEmpty()) {
            // Save idling session
            writeLastSession(currentGames!!)
        }

        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(CURRENT_GAMES, ArrayList(currentGames!!))
        outState.putInt(CURRENT_TAB, currentTab)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.games_fragment, container, false)

        refreshLayout = view.findViewById(R.id.refresh_layout)
        refreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimaryDark)
        refreshLayout.setOnRefreshListener(this)
        refreshLayout.isRefreshing = true

        recyclerView = view.findViewById(R.id.games_list)

        adapter = GamesAdapter(recyclerView.context)
        adapter!!.setListener(this)
        adapter!!.setCurrentGames(currentGames!!)
        adapter!!.setHeaderEnabled(currentTab == TAB_LAST)

        layoutManager = GridLayoutManagerWrapper(
            recyclerView.context,
            resources.getInteger(R.integer.game_columns)
        )

        layoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter!!.getItemViewType(position)) {
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

        viewModel!!.getGames().observe(getViewLifecycleOwner()) { games ->
            setGames(games)
        }

        loadData()

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_games, menu)

        val searchItem = menu.findItem(R.id.search)
        searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId

        when (itemId) {
            R.id.refresh -> {
                fetchGames()
                return true
            }
            R.id.sort_alphabetically -> {
                viewModel!!.sort(GamesViewModel.SORT_ALPHABETICALLY)
                return true
            }
            R.id.sort_hours_played -> {
                viewModel!!.sort(GamesViewModel.SORT_HOURS_PLAYED)
                return true
            }
            R.id.sort_hours_played_reversed -> {
                viewModel!!.sort(GamesViewModel.SORT_HOURS_PLAYED_REVERSED)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        adapter!!.filter(query)
        searchView.clearFocus()

        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        adapter!!.filter(newText)
        return true
    }

    private fun loadData() {
        if (viewModel!!.getGames().getValue() == null) {
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
            val games = if (currentGames!!.isNotEmpty()) currentGames else lastSession
            viewModel!!.setGames(games.orEmpty())
        } else {
            // Fetch games from Steam
            refreshLayout.isRefreshing = true
            viewModel!!.fetchGames()
        }
    }

    /**
     * Update games list
     * @param games the list of games
     */
    private fun setGames(games: List<Game>) {
        if (currentTab == TAB_BLACKLIST) {
            // Only list blacklisted games
            val blacklist = blacklist
            val blacklistGames: MutableList<Game> = ArrayList()

            games.forEach { game ->
                if (blacklist.contains(game.appId.toString())) {
                    blacklistGames.add(game)
                }
            }

            adapter!!.setHeaderEnabled(false)
            adapter!!.setData(blacklistGames)
            emptyView.visibility = if (blacklistGames.isEmpty()) View.VISIBLE else View.GONE
        } else {
            adapter!!.setHeaderEnabled(games.isNotEmpty() && currentTab == TAB_LAST)
            adapter!!.setData(games)
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
        private val TAG = GamesFragment::class.java.getSimpleName()

        private const val STEAM_ID = "STEAM_ID"
        private const val CURRENT_GAMES = "CURRENT_GAMES"
        private const val CURRENT_TAB = "CURRENT_TAB"

        // Spinner nav items
        const val TAB_GAMES = 0
        const val TAB_LAST = 1
        const val TAB_BLACKLIST = 2

        fun newInstance(
            steamId: Long,
            currentGames: ArrayList<Game>,
            position: Int
        ): GamesFragment = GamesFragment().apply {
            val args = Bundle().apply {
                putLong(STEAM_ID, steamId)
                putParcelableArrayList(CURRENT_GAMES, currentGames)
                putInt(CURRENT_TAB, position)
            }
            setArguments(args)
        }
    }
}
