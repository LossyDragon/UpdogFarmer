package com.steevsapps.idledaddy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewStub
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.steevsapps.idledaddy.LoginActivity.Companion.createIntent
import com.steevsapps.idledaddy.dialogs.AboutDialog
import com.steevsapps.idledaddy.dialogs.AutoDiscoverDialog
import com.steevsapps.idledaddy.dialogs.CustomAppDialog
import com.steevsapps.idledaddy.dialogs.GameOptionsDialog
import com.steevsapps.idledaddy.dialogs.RedeemDialog
import com.steevsapps.idledaddy.dialogs.SharedSecretDialog
import com.steevsapps.idledaddy.fragments.GamesFragment
import com.steevsapps.idledaddy.fragments.HomeFragment
import com.steevsapps.idledaddy.fragments.SettingsFragment
import com.steevsapps.idledaddy.listeners.DialogListener
import com.steevsapps.idledaddy.listeners.GamePickedListener
import com.steevsapps.idledaddy.listeners.SpinnerInteractionListener
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.utils.Utils
import `in`.dragonbra.javasteam.enums.EPersonaState
import java.io.File
import java.io.IOException
import java.util.Locale

class MainActivity :
    BaseActivity(),
    DialogListener,
    GamePickedListener,
    OnSharedPreferenceChangeListener {

    private var farming = false
    private var loggedIn = false
    private var title = ""

    // Views
    private lateinit var adInflater: ViewStub
    private lateinit var avatarView: ImageView
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var drawerView: NavigationView
    private lateinit var logoutToggle: ImageView
    private lateinit var mainContainer: LinearLayout
    private lateinit var searchView: SearchView
    private lateinit var spinnerNav: Spinner
    private lateinit var usernameView: TextView
    private var drawerLayout: DrawerLayout? = null

    private var logoutExpanded = false

    private var drawerItemId = 0

    private var prefs: SharedPreferences = PrefsManager.prefs

    private var steamService: SteamService? = null

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            loggedIn = steamService!!.isLoggedIn
            farming = steamService!!.isFarming
            when (intent.action) {
                SteamService.LOGIN_EVENT, SteamService.DISCONNECT_EVENT, SteamService.STOP_EVENT -> updateStatus()
                SteamService.FARM_EVENT -> showDropInfo(intent)
                SteamService.PERSONA_EVENT -> updateDrawerHeader(intent)
                SteamService.NOW_PLAYING_EVENT -> showNowPlaying()
            }
        }
    }

    private val currentFragment: Fragment?
        get() = supportFragmentManager.findFragmentById(R.id.content_frame)

    private fun doLogout() {
        steamService!!.logoff()

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
            PrefsManager.writePersonaName(personaName ?: "")
            PrefsManager.writeAvatarHash(avatarHash ?: "")
        } else {
            personaName = PrefsManager.personaName
            avatarHash = PrefsManager.avatarHash
        }

        if (personaName!!.isNotEmpty()) {
            usernameView.text = personaName
        }

        if (!PrefsManager.minimizeData() &&
            avatarHash!!.isNotEmpty() &&
            avatarHash != "0000000000000000000000000000000000000000"
        ) {
            val avatar = String.format(
                Locale.US,
                "http://cdn.akamai.steamstatic.com/steamcommunity/public/images/avatars/%s/%s_full.jpg",
                avatarHash.substring(0, 2),
                avatarHash
            )
            Glide.with(this).load(avatar).into(avatarView)
        }
    }

    override fun onServiceConnected() {
        Log.i(TAG, "Service connected")

        steamService = service

        loggedIn = steamService!!.isLoggedIn
        farming = steamService!!.isFarming

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

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mainContainer = findViewById(R.id.main_container)

        // Setup Billing Manager & Consent Manager
        // billingManager = new BillingManager(this);
        // consentManager = new ConsentManager(this);

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.spinner_nav_options,
            R.layout.simple_spinner_title
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        val listener = SpinnerInteractionListener(supportFragmentManager)

        // Setup the navigation spinner (Games fragment only)
        spinnerNav = findViewById(R.id.spinner_nav)
        spinnerNav.setAdapter(adapter)
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

            drawerLayout?.addDrawerListener(drawerToggle)

            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }

        drawerView = findViewById(R.id.left_drawer)

        // Disable shadow
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawerView.elevation = 0f
        }

        drawerView.setNavigationItemSelectedListener { item ->
            val itemId = item.itemId
            when (itemId) {
                R.id.logout -> {
                    // No page for this
                    doLogout()
                }
                R.id.about -> {
                    // Same here
                    AboutDialog.newInstance().show(supportFragmentManager, AboutDialog.TAG)
                    closeDrawer()
                }
                R.id.remove_ads -> {
                    // billingManager.launchPurchaseFlow();
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
            loggedIn = steamService!!.isLoggedIn
            farming = steamService!!.isFarming
            updateStatus()
        }

        // Ads
        adInflater = findViewById(R.id.ad_inflater)
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
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putInt(DRAWER_ITEM, drawerItemId)
            putString(TITLE, title)
            putBoolean(LOGOUT_EXPANDED, logoutExpanded)
        }
    }

    /**
     * Activate a Steam key sent from another app
     */
    private fun handleKeyIntent(intent: Intent) {
        val key = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (PrefsManager.loginKey.isNotEmpty() && key != null) {
            steamService!!.redeemKey(key.trim())
        } else {
            Toast.makeText(
                applicationContext,
                R.string.error_not_logged_in,
                Toast.LENGTH_LONG
            ).show()
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
            R.id.games -> {
                GamesFragment.newInstance(
                    steamService!!.steamId,
                    ArrayList(steamService!!.currentGames),
                    spinnerNav.selectedItemPosition
                )
            }
            R.id.settings -> SettingsFragment.newInstance()
            else -> Fragment()
        }

        supportFragmentManager.beginTransaction().apply {
            replace(R.id.content_frame, fragment)
            if (addToBackStack) {
                addToBackStack(null)
            }
            commit()
        }

        closeDrawer()
    }

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

        val filter = IntentFilter().apply {
            addAction(SteamService.LOGIN_EVENT)
            addAction(SteamService.DISCONNECT_EVENT)
            addAction(SteamService.STOP_EVENT)
            addAction(SteamService.FARM_EVENT)
            addAction(SteamService.PERSONA_EVENT)
            addAction(SteamService.NOW_PLAYING_EVENT)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)

        // Listen for preference changes
        prefs.registerOnSharedPreferenceChangeListener(this)

        // billingManager.queryPurchases();
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    @Suppress("RedundantOverride")
    override fun onDestroy() {
        // billingManager.destroy();
        super.onDestroy()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val loggedIn = steamService != null && steamService!!.isLoggedIn

        drawerView.getHeaderView(0).isClickable = loggedIn

        menu.findItem(R.id.auto_discovery).setVisible(loggedIn)
        menu.findItem(R.id.custom_app).setVisible(loggedIn)
        menu.findItem(R.id.import_shared_secret).setVisible(loggedIn)
        // menu.findItem(R.id.auto_vote).setVisible(loggedIn);
        // menu.findItem(R.id.spring_cleaning_event).setVisible(loggedIn);
        menu.findItem(R.id.search).setVisible(drawerItemId == R.id.games)

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        searchView = menu.findItem(R.id.search).actionView as SearchView

        return true
    }

    override fun onBackPressed() {
        if (!searchView.isIconified) {
            // Dismiss the SearchView
            searchView.setQuery("", false)
            searchView.isIconified = true
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        val itemId = item.itemId
        when (itemId) {
            R.id.logcat -> {
                sendLogcat()
                return true
            }
            R.id.auto_discovery -> {
                AutoDiscoverDialog.newInstance()
                    .show(supportFragmentManager, AutoDiscoverDialog.TAG)
                return true
            }
            R.id.custom_app -> {
                CustomAppDialog.newInstance().show(supportFragmentManager, CustomAppDialog.TAG)
                return true
            }
            R.id.import_shared_secret -> {
                SharedSecretDialog.newInstance(steamService!!.steamId)
                    .show(supportFragmentManager, SharedSecretDialog.TAG)
                return true
            } // else if (itemId == R.id.spring_cleaning_event) {
            //     SpringCleaningDialog.newInstance().show(getSupportFragmentManager(), SpringCleaningDialog.TAG);
            //     return true;
            // } else if (itemId == R.id.auto_vote) {
            //     steamService.autoVote();
            //     return true;
            // }
            else -> return false
        }
    }

    /**
     * Send Logcat output via email
     */
    private fun sendLogcat() {
        val cacheDir = externalCacheDir
        if (cacheDir == null) {
            Log.i(TAG, "Unable to save Logcat. Shared storage is unavailable!")
            return
        }

        val file = File(cacheDir, "idledaddy-logcat.txt")
        try {
            Utils.saveLogcat(file)
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }

        Intent(Intent.ACTION_SEND).apply {
            val fileProvider = FileProvider.getUriForFile(
                this@MainActivity,
                applicationContext.packageName + ".provider",
                file
            )
            setType("*/*")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("steevsapps@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Idle Daddy Logcat")
            putExtra(Intent.EXTRA_STREAM, fileProvider)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }.also(::startActivity)
    }

    fun clickHandler(v: View) {
        val id = v.id
        when (id) {
            R.id.start_idling -> {
                v.setEnabled(false)
                steamService!!.startFarming()
            }
            R.id.stop_idling -> stopSteam()
            R.id.status -> startActivity(createIntent(this))
            R.id.redeem -> RedeemDialog.newInstance().show(supportFragmentManager, "redeem")
            R.id.stop_button -> steamService!!.stopGame()
            R.id.pause_resume_button -> {
                if (steamService!!.isPaused) {
                    steamService!!.resumeGame()
                } else {
                    steamService!!.pauseGame()
                }
            }
            R.id.next_button -> steamService!!.skipGame()
        }
    }

    /**
     * Update the fragments
     */
    private fun updateStatus() {
        invalidateOptionsMenu()

        when (val fragment = currentFragment) {
            is HomeFragment -> {
                drawerItemId = R.id.home
                setTitle(R.string.app_name)
                hideSpinnerNav()
                drawerView.menu.findItem(R.id.home).setChecked(true)
                fragment.update(loggedIn, farming)
                showDropInfo(null)
                showNowPlaying()
            }
            is GamesFragment -> {
                drawerItemId = R.id.games
                setTitle("")
                showSpinnerNav()
                drawerView.menu.findItem(R.id.games).setChecked(true)
                fragment.update(steamService!!.currentGames)
            }
            is SettingsFragment -> {
                drawerItemId = R.id.settings
                setTitle(R.string.settings)
                hideSpinnerNav()
                drawerView.menu.findItem(R.id.settings).setChecked(true)
            }
        }
    }

    /**
     * Show/hide card drop info
     */
    private fun showDropInfo(intent: Intent?) {
        val fragment = currentFragment
        if (fragment is HomeFragment) {
            if (intent != null) {
                // Called by FARM_EVENT, always show drop info
                val gameCount = intent.getIntExtra(SteamService.GAME_COUNT, 0)
                val cardCount = intent.getIntExtra(SteamService.CARD_COUNT, 0)
                fragment.showDropInfo(gameCount, cardCount)
            } else if (farming) {
                // Called by updateStatus(), only show drop info if we're farming
                fragment.showDropInfo(steamService!!.gameCount, steamService!!.cardCount)
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
        val fragment = currentFragment
        if (fragment is HomeFragment) {
            fragment.showNowPlaying(
                steamService!!.currentGames,
                steamService!!.isFarming,
                steamService!!.isPaused
            )
        }
    }

    override fun onYesPicked(text: String) {
        val key = text.uppercase(Locale.getDefault()).trim()
        if (key.isNotEmpty()) {
            steamService!!.redeemKey(key)
        }
    }

    override fun onGamePicked(game: Game) {
        steamService!!.addGame(game)
    }

    override fun onGamesPicked(games: List<Game>) {
        steamService!!.addGames(games)
    }

    override fun onGameRemoved(game: Game) {
        steamService!!.removeGame(game)
    }

    override fun onGameLongPressed(game: Game) {
        // Show game options
        GameOptionsDialog.newInstance(game).show(supportFragmentManager, GameOptionsDialog.TAG)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        when (key) {
            "stay_awake" -> {
                if (PrefsManager.stayAwake()) {
                    // Keep device awake
                    steamService!!.acquireWakeLock()
                } else {
                    // Allow device to sleep
                    steamService!!.releaseWakeLock()
                }
            }
            "offline" -> {
                // Change status
                val status = if (PrefsManager.offline) {
                    EPersonaState.Offline
                } else {
                    EPersonaState.Online
                }

                steamService!!.changeStatus(status)
            }
            "language" -> {
                Toast.makeText(this, R.string.language_changed, Toast.LENGTH_LONG).show()
            }
        }
    }

    // @Override
    // public void onBillingClientSetupFinished() {
    //     if (billingManager.shouldDisplayAds()) {
    //         consentManager.requestConsentInfo();
    //         drawerView.getMenu().findItem(R.id.remove_ads).setVisible(true);
    //     }
    // }

    // @Override
    // public void onPurchasesUpdated(List<Purchase> purchases) {
    //     if (!billingManager.shouldDisplayAds()) {
    //         removeAds();
    //         drawerView.getMenu().findItem(R.id.remove_ads).setVisible(false);
    //     }
    // }

    // @Override
    // public void onPurchaseCanceled() {
    //     if (billingManager.shouldDisplayAds()) {
    //         consentManager.requestConsentInfo();
    //     }
    // }

    // @Override
    // public void onConsentInfoUpdated(ConsentStatus consentStatus, boolean userPrefersAdFree) {
    //     if (userPrefersAdFree) {
    //         billingManager.launchPurchaseFlow();
    //     } else {
    //         final Bundle args = new Bundle();
    //         if (consentStatus == ConsentStatus.NON_PERSONALIZED) {
    //             args.putString("npa", "1");
    //         }
    //         loadAds(args);
    //     }
    // }

    // @Override
    // public void onConsentRevoked() {
    //     if (billingManager.shouldDisplayAds()) {
    //         consentManager.revokeConsent();
    //     } else {
    //         // Consent not needed. No ads are shown in Idle Daddy Premium
    //         Toast.makeText(this, R.string.gdpr_consent_not_needed, Toast.LENGTH_LONG).show();
    //     }
    // }

    // /**
    //  * Inflate adView and load the ad request
    //  */
    // private void loadAds(Bundle args) {
    //     if (adView == null) {
    //         adView = (AdView) adInflater.inflate();
    //     }
    //     MobileAds.initialize(this, BuildConfig.AdmobAppId);
    //     final AdRequest adRequest = new AdRequest.Builder()
    //             .addNetworkExtrasBundle(AdMobAdapter.class, args)
    //             .build();
    //     adView.loadAd(adRequest);
    // }

    // /**
    //  * Remove the adView
    //  */
    // private void removeAds() {
    //     mainContainer.removeView(adView);
    // }

    companion object {
        private val TAG = MainActivity::class.java.getSimpleName()
        private const val DRAWER_ITEM = "DRAWER_ITEM"
        private const val TITLE = "TITLE"
        private const val LOGOUT_EXPANDED = "LOGOUT_EXPANDED"
    }
}
