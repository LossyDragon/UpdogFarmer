package com.steevsapps.idledaddy

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.steevsapps.idledaddy.LoginActivity.Companion.createIntent
import com.steevsapps.idledaddy.dialogs.AboutDialog
import com.steevsapps.idledaddy.dialogs.AutoDiscoverDialog
import com.steevsapps.idledaddy.dialogs.CustomAppDialog
import com.steevsapps.idledaddy.dialogs.RedeemDialog
import com.steevsapps.idledaddy.fragments.GamesFragment
import com.steevsapps.idledaddy.fragments.HomeFragment
import com.steevsapps.idledaddy.fragments.SettingsFragment
import com.steevsapps.idledaddy.listeners.DialogListener
import com.steevsapps.idledaddy.listeners.SpinnerInteractionListener
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.SteamService
import `in`.dragonbra.javasteam.enums.EPersonaState
import java.util.Locale

class MainActivity : BaseActivity(), DialogListener, OnSharedPreferenceChangeListener {
    private var title = ""
    private var loggedIn = false
    private var farming = false

    // Views
    private var mainContainer: LinearLayout? = null
    private lateinit var avatarView: ImageView
    private lateinit var usernameView: TextView
    private var drawerLayout: DrawerLayout? = null
    private lateinit var drawerView: NavigationView
    private var drawerToggle: ActionBarDrawerToggle? = null
    private lateinit var logoutToggle: ImageView
    private lateinit var spinnerNav: Spinner
    private var searchView: SearchView? = null

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (searchView?.isIconified == false) {
                // Dismiss the SearchView
                searchView!!.setQuery("", false)
                searchView!!.isIconified = true
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private var logoutExpanded = false
    private var drawerItemId = 0

    private lateinit var prefs: SharedPreferences

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            loggedIn = service!!.isLoggedIn
            farming = service!!.isFarming
            when (intent.action) {
                SteamService.LOGIN_EVENT, SteamService.DISCONNECT_EVENT, SteamService.STOP_EVENT -> updateStatus()
                SteamService.FARM_EVENT -> showDropInfo(intent)
                SteamService.PERSONA_EVENT -> updateDrawerHeader(intent)
                SteamService.NOW_PLAYING_EVENT -> showNowPlaying()
            }
        }
    }

    private fun doLogout() {
        service!!.logoff()
        closeDrawer()
        avatarView.setImageResource(R.color.transparent)
        usernameView.text = ""
        logoutExpanded = false
        logoutToggle.rotation = 0f
        drawerView.menu.setGroupVisible(R.id.logout_group, false)
        loggedIn = false
        farming = false
        updateStatus()
    }

    /**
     * Update drawer header with avatar and username
     */
    private fun updateDrawerHeader(intent: Intent?) {
        val personaName: String?
        val avatarHash: String?

        if (intent != null) {
            personaName = intent.getStringExtra(SteamService.PERSONA_NAME)
            avatarHash = intent.getStringExtra(SteamService.AVATAR_HASH)
            PrefsManager.writePersonaName(personaName)
            PrefsManager.writeAvatarHash(avatarHash)
        } else {
            personaName = PrefsManager.getPersonaName()
            avatarHash = PrefsManager.getAvatarHash()
        }

        if (!personaName.isNullOrEmpty()) {
            usernameView.text = personaName
        }

        if (!PrefsManager.minimizeData() && !avatarHash.isNullOrEmpty() && (avatarHash != "0000000000000000000000000000000000000000")) {
            val avatar = "http://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/" +
                    "${avatarHash.substring(0, 2)}/${avatarHash}_full.jpg"
            Glide.with(this).load(avatar).into(avatarView)
        }
    }

    override fun onServiceConnected() {
        Log.i(TAG, "Service connected")
        loggedIn = service!!.isLoggedIn
        farming = service!!.isFarming
        updateStatus()
        updateDrawerHeader(null)

        // Check if a Steam key was sent to us from another app
        if (Intent.ACTION_SEND == intent.action) {
            handleKeyIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mainContainer = findViewById(R.id.main_container)

        // Setup the navigation spinner (Games fragment only)
        spinnerNav = findViewById(R.id.spinner_nav)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.spinner_nav_options, R.layout.simple_spinner_title
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNav.setAdapter(adapter)
        val listener = SpinnerInteractionListener(supportFragmentManager)
        spinnerNav.onItemSelectedListener = listener
        spinnerNav.setOnTouchListener(listener)

        drawerLayout = findViewById(R.id.drawer_layout)
        // On tablets we use the DrawerView but not the DrawerLayout
        if (drawerLayout != null) {
            drawerToggle = object : ActionBarDrawerToggle(
                this,
                drawerLayout,
                R.string.open_drawer,
                R.string.close_drawer
            ) {
                override fun onDrawerClosed(drawerView: View) {
                    super.onDrawerClosed(drawerView)
                    invalidateOptionsMenu()
                }

                override fun onDrawerOpened(drawerView: View) {
                    super.onDrawerOpened(drawerView)
                    invalidateOptionsMenu()
                }
            }
            drawerLayout!!.addDrawerListener(drawerToggle!!)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }
        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
        val insetRoot: View = drawerLayout ?: mainContainer!!
        ViewCompat.setOnApplyWindowInsetsListener(insetRoot) { _, insetsCompat ->
            val bars = insetsCompat.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            toolbar.setPaddingRelative(
                toolbar.paddingStart, bars.top,
                toolbar.paddingEnd, toolbar.paddingBottom
            )
            contentFrame.setPaddingRelative(
                contentFrame.paddingStart, contentFrame.paddingTop,
                contentFrame.paddingEnd, bars.bottom
            )
            insetsCompat
        }

        drawerView = findViewById(R.id.left_drawer)
        // Disable shadow
        drawerView.elevation = 0f
        drawerView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.logout -> {
                    // No page for this
                    doLogout()
                }

                R.id.about -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content_frame, AboutDialog.newInstance())
                        .addToBackStack(null)
                        .commit()
                    closeDrawer()
                }

                else -> {
                    // Go to page
                    selectItem(item.itemId, true)
                }
            }
            true
        }

        // Get avatar and username views from drawer header
        val headerView = drawerView.getHeaderView(0)
        avatarView = headerView.findViewById(R.id.avatar)
        usernameView = headerView.findViewById(R.id.username)
        logoutToggle = headerView.findViewById(R.id.logout_toggle)
        headerView.setOnClickListener {
            logoutExpanded = !logoutExpanded
            val rotation = if (logoutExpanded) 180 else 0
            logoutToggle.animate().rotation(rotation.toFloat()).setDuration(250).start()
            drawerView.menu.setGroupVisible(R.id.logout_group, logoutExpanded)
        }

        // Update the navigation drawer and title on backstack changes
        supportFragmentManager.addOnBackStackChangedListener {
            loggedIn = service!!.isLoggedIn
            farming = service!!.isFarming
            updateStatus()
        }

        if (savedInstanceState != null) {
            drawerItemId = savedInstanceState.getInt(DRAWER_ITEM)
            logoutExpanded = savedInstanceState.getBoolean(LOGOUT_EXPANDED)
            setTitle(savedInstanceState.getString(TITLE)!!)
            drawerView.menu.setGroupVisible(R.id.logout_group, logoutExpanded)
            logoutToggle.rotation = (if (logoutExpanded) 180 else 0).toFloat()
        } else {
            logoutExpanded = false
            selectItem(R.id.home, false)
        }
    }

    public override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle?.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(DRAWER_ITEM, drawerItemId)
        outState.putString(TITLE, title)
        outState.putBoolean(LOGOUT_EXPANDED, logoutExpanded)
    }

    /**
     * Activate a Steam key sent from another app
     */
    private fun handleKeyIntent(intent: Intent) {
        val key = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (PrefsManager.getRefreshToken().isNotEmpty() && key != null) {
            service!!.redeemKey(key.trim())
        } else {
            Toast.makeText(applicationContext, R.string.error_not_logged_in, Toast.LENGTH_LONG)
                .show()
        }
        finish()
    }

    private fun selectItem(id: Int, addToBackStack: Boolean) {
        if (drawerItemId == id) {
            // Already selected
            closeDrawer()
            return
        }

        val fragment: Fragment = when (id) {
            R.id.home -> HomeFragment.newInstance(loggedIn, farming)
            R.id.games -> GamesFragment.newInstance(
                service!!.steamId,
                ArrayList(service!!.currentGames),
                spinnerNav.selectedItemPosition
            )

            R.id.settings -> SettingsFragment.newInstance()
            else -> Fragment()
        }
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.content_frame, fragment)
        if (addToBackStack) {
            ft.addToBackStack(null)
        }
        ft.commit()
        closeDrawer()
    }

    private val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.content_frame)

    private fun closeDrawer() {
        drawerLayout?.closeDrawer(drawerView)
    }

    /**
     * Show the navigation spinner (Games fragment only)
     */
    private fun showSpinnerNav() {
        spinnerNav.visibility = View.VISIBLE
    }

    /**
     * Hide it
     */
    private fun hideSpinnerNav() {
        spinnerNav.visibility = View.GONE
    }

    override fun setTitle(titleId: Int) {
        title = getString(titleId)
        super.setTitle(titleId)
    }

    override fun setTitle(title: CharSequence) {
        this.title = title.toString()
        super.setTitle(title)
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()
        filter.addAction(SteamService.LOGIN_EVENT)
        filter.addAction(SteamService.DISCONNECT_EVENT)
        filter.addAction(SteamService.STOP_EVENT)
        filter.addAction(SteamService.FARM_EVENT)
        filter.addAction(SteamService.PERSONA_EVENT)
        filter.addAction(SteamService.NOW_PLAYING_EVENT)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
        // Listen for preference changes
        prefs = PrefsManager.getPrefs()
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val loggedIn = service != null && service!!.isLoggedIn
        val showOverflow = drawerItemId != R.id.about
        drawerView.getHeaderView(0).isClickable = loggedIn
        menu.findItem(R.id.auto_discovery).isVisible = loggedIn && showOverflow
        menu.findItem(R.id.custom_app).isVisible = loggedIn && showOverflow
        menu.findItem(R.id.import_shared_secret).isVisible = loggedIn && showOverflow
        menu.findItem(R.id.logcat).isVisible = showOverflow
        menu.findItem(R.id.search).isVisible = drawerItemId == R.id.games
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        searchView = menu.findItem(R.id.search).actionView as SearchView?
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle?.onOptionsItemSelected(item) == true) {
            return true
        }
        return when (item.itemId) {
            R.id.auto_discovery -> {
                AutoDiscoverDialog.newInstance()
                    .show(supportFragmentManager, AutoDiscoverDialog.TAG)
                true
            }

            R.id.custom_app -> {
                CustomAppDialog.newInstance().show(supportFragmentManager, CustomAppDialog.TAG)
                true
            }

            else -> false
        }
    }

    fun clickHandler(v: View) {
        when (v.id) {
            R.id.start_idling -> {
                v.isEnabled = false
                service!!.startFarming()
            }

            R.id.stop_idling -> stopSteam()
            R.id.status -> startActivity(createIntent(this))
            // R.id.redeem -> RedeemDialog.newInstance().show(supportFragmentManager, "redeem")
            R.id.stop_button -> service!!.stopGame()
            R.id.pause_resume_button -> {
                if (service!!.isPaused) {
                    service!!.resumeGame()
                } else {
                    service!!.pauseGame()
                }
            }

            R.id.next_button -> service!!.skipGame()
        }
    }

    /**
     * Update the fragments
     */
    private fun updateStatus() {
        invalidateOptionsMenu()
        when (val fragment = this.currentFragment) {
            is HomeFragment -> {
                drawerItemId = R.id.home
                setTitle(R.string.app_name)
                hideSpinnerNav()
                drawerView.menu.findItem(R.id.home).isChecked = true
                fragment.update(loggedIn, farming)
                showDropInfo(null)
                showNowPlaying()
            }

            is GamesFragment -> {
                drawerItemId = R.id.games
                setTitle("")
                showSpinnerNav()
                drawerView.menu.findItem(R.id.games).isChecked = true
                fragment.update(ArrayList(service!!.currentGames))
            }

            is SettingsFragment -> {
                drawerItemId = R.id.settings
                setTitle(R.string.settings)
                hideSpinnerNav()
                drawerView.menu.findItem(R.id.settings).isChecked = true
            }

            is AboutDialog -> {
                drawerItemId = R.id.about
                setTitle(R.string.about)
                hideSpinnerNav()
                drawerView.menu.findItem(R.id.about).isChecked = true
            }
        }
    }

    /**
     * Show/hide card drop info
     */
    private fun showDropInfo(intent: Intent?) {
        val fragment = this.currentFragment
        if (fragment is HomeFragment) {
            if (intent != null) {
                // Called by FARM_EVENT, always show drop info
                val gameCount = intent.getIntExtra(SteamService.GAME_COUNT, 0)
                val cardCount = intent.getIntExtra(SteamService.CARD_COUNT, 0)
                fragment.showDropInfo(gameCount, cardCount)
            } else if (farming) {
                // Called by updateStatus(), only show drop info if we're farming
                fragment.showDropInfo(
                    service!!.gameCount,
                    service!!.cardCount
                )
            } else {
                // Hide drop info
                fragment.hideDropInfo()
            }
        }
    }

    /**
     * Show now playing if we're idling any games
     */
    private fun showNowPlaying() {
        val fragment = this.currentFragment
        if (fragment is HomeFragment) {
            fragment.showNowPlaying(
                service!!.currentGames,
                service!!.isFarming,
                service!!.isPaused
            )
        }
    }

    override fun onYesPicked(text: String) {
        val key = text.uppercase(Locale.getDefault()).trim()
        if (key.isNotEmpty()) {
            service!!.redeemKey(key)
        }
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "stay_awake" -> {
                if (PrefsManager.stayAwake()) {
                    // Keep device awake
                    service!!.acquireWakeLock()
                } else {
                    // Allow device to sleep
                    service!!.releaseWakeLock()
                }
            }

            "offline" -> {
                // Change status
                service!!.changeStatus(if (PrefsManager.getOffline()) EPersonaState.Offline else EPersonaState.Online)
            }

            "language" -> Toast.makeText(this, R.string.language_changed, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
        private const val DRAWER_ITEM = "DRAWER_ITEM"
        private const val TITLE = "TITLE"
        private const val LOGOUT_EXPANDED = "LOGOUT_EXPANDED"
    }
}
