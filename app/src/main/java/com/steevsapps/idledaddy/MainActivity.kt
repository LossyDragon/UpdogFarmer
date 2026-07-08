package com.steevsapps.idledaddy

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.steevsapps.idledaddy.ui.compat.setEdgeToEdgeConfig
import com.steevsapps.idledaddy.ui.screen.about.AboutScreen
import com.steevsapps.idledaddy.ui.screen.games.GamesScreen
import com.steevsapps.idledaddy.ui.screen.home.HomeScreen
import com.steevsapps.idledaddy.ui.screen.login.LoginScreen
import com.steevsapps.idledaddy.ui.screen.settings.SettingsScreen
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import com.steevsapps.idledaddy.utils.LocaleManager
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private data class DrawerDestination(
    val key: NavKeyRoot,
    @param:StringRes val label: Int,
    val icon: ImageVector,
)

private val drawerDestinations = listOf(
    DrawerDestination(NavKeyRoot.Home, R.string.home, Icons.Default.Home),
    DrawerDestination(NavKeyRoot.Games, R.string.games, Icons.Default.SportsEsports),
    DrawerDestination(NavKeyRoot.Settings, R.string.settings, Icons.Default.Settings),
    DrawerDestination(NavKeyRoot.About, R.string.about, Icons.Default.Info),
)

private sealed class NavKeyRoot : NavKey {
    @Serializable
    data object Home : NavKeyRoot()

    @Serializable
    data object Login : NavKeyRoot()

    @Serializable
    data object Games : NavKeyRoot()

    @Serializable
    data object About : NavKeyRoot()

    @Serializable
    data object Settings : NavKeyRoot()
}

class MainActivity : ComponentActivity() {

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "onCreate")

        setEdgeToEdgeConfig()
        setContent {
            val backStack = rememberNavBackStack(NavKeyRoot.Home)
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            IdleTheme {
                MainScreen(
                    backStack = backStack,
                    drawerState = drawerState,
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    backStack: NavBackStack<NavKey>,
    drawerState: DrawerState,
) {
    val scope = rememberCoroutineScope()
    val currentKey = backStack.lastOrNull()

    val toggleDrawer: () -> Unit = {
        scope.launch {
            if (drawerState.targetValue == DrawerValue.Open) drawerState.close()
            else drawerState.open()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp),
                drawerShape = RectangleShape,
                windowInsets = WindowInsets(0),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.material_header),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Row(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AsyncImage(
                            model = null, // TODO avatar url
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                        )
                        Column(Modifier.padding(start = 16.dp)) {
                            val textShadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(0f, 2f),
                                blurRadius = 2f,
                            )
                            Text(
                                text = "Updog", // TODO persona name
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                            )
                            Text(
                                text = "Online", // TODO online status
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                            )
                        }
                    }
                }
                // Spacer(Modifier.height(8.dp))
                drawerDestinations.forEach { destination ->
                    OreoDrawerItem(
                        label = destination.label,
                        icon = destination.icon,
                        selected = currentKey == destination.key,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentKey != destination.key) {
                                backStack.clear()
                                backStack.add(NavKeyRoot.Home)
                                if (destination.key != NavKeyRoot.Home) {
                                    backStack.add(destination.key)
                                }
                            }
                        },
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                OreoDrawerItem(
                    label = R.string.logout,
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        /* TODO */
                    },
                )
            }
        },
    ) {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<NavKeyRoot.Home> {
                    HomeScreen(
                        onMenuClick = toggleDrawer,
                        onLoginClick = { /* TODO */ },
                        onStopSteam = { /* TODO */ },
                    )
                }
                entry<NavKeyRoot.Login> {
                    LoginScreen(
                        onBack = backStack::removeLastOrNull,
                        onLoggedIn = { /* TODO */ },
                    )
                }
                entry<NavKeyRoot.Games> {
                    GamesScreen(
                        onBack = backStack::removeLastOrNull,
                        onMenuClick = toggleDrawer,
                    )
                }
                entry<NavKeyRoot.About> {
                    AboutScreen(onBack = backStack::removeLastOrNull)
                }
                entry<NavKeyRoot.Settings> {
                    SettingsScreen(onBack = backStack::removeLastOrNull)
                }
            }
        )
    }
}

@Composable
private fun OreoDrawerItem(
    @StringRes label: Int,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = stringResource(label),
                modifier = Modifier.padding(start = 20.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        },
        icon = { Icon(icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        shape = RectangleShape,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@OptIn(ExperimentalCoilApi::class)
@Preview
@Composable
private fun Preview() {
    val backStack = rememberNavBackStack(NavKeyRoot.About)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Open)
    CompositionLocalProvider(
        LocalNavigationEventDispatcherOwner provides rememberNavigationEventDispatcherOwner(parent = null),
        LocalAsyncImagePreviewHandler provides AsyncImagePreviewHandler {
            ColorImage(Color.LightGray.toArgb())
        },
    ) {
        IdleTheme {
            MainScreen(
                backStack = backStack,
                drawerState = drawerState,
            )
        }
    }
}


//    private var title = ""
//
//    // Views
//    private var mainContainer: LinearLayout? = null
//    private lateinit var avatarView: ImageView
//    private lateinit var usernameView: TextView
//    private var drawerLayout: DrawerLayout? = null
//    private lateinit var drawerView: NavigationView
//    private var drawerToggle: ActionBarDrawerToggle? = null
//    private lateinit var logoutToggle: ImageView
//    private var logoutExpanded = false
//    private var drawerItemId = 0
//
//    private lateinit var prefs: SharedPreferences
//
//    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
//        override fun onReceive(context: Context?, intent: Intent) {
//            when (intent.action) {
//                SteamService.LOGIN_EVENT, SteamService.DISCONNECT_EVENT, SteamService.STOP_EVENT -> updateStatus()
//                SteamService.PERSONA_EVENT -> updateDrawerHeader(intent)
//            }
//        }
//    }
//
//    private fun doLogout() {
//        service!!.logoff()
//        closeDrawer()
//        avatarView.setImageResource(R.color.transparent)
//        usernameView.text = ""
//        logoutExpanded = false
//        logoutToggle.rotation = 0f
//        drawerView.menu.setGroupVisible(R.id.logout_group, false)
//        updateStatus()
//    }
//
//    /**
//     * Update drawer header with avatar and username
//     */
//    private fun updateDrawerHeader(intent: Intent?) {
//        val personaName: String?
//        val avatarHash: String?
//
//        if (intent != null) {
//            personaName = intent.getStringExtra(SteamService.PERSONA_NAME)
//            avatarHash = intent.getStringExtra(SteamService.AVATAR_HASH)
//            PrefsManager.writePersonaName(personaName)
//            PrefsManager.writeAvatarHash(avatarHash)
//        } else {
//            personaName = PrefsManager.getPersonaName()
//            avatarHash = PrefsManager.getAvatarHash()
//        }
//
//        if (!personaName.isNullOrEmpty()) {
//            usernameView.text = personaName
//        }
//
//        if (!PrefsManager.minimizeData() && !avatarHash.isNullOrEmpty() && (avatarHash != "0000000000000000000000000000000000000000")) {
//            Glide.with(this).load(Utils.avatar(avatarHash)).into(avatarView)
//        }
//    }
//
//    override fun onServiceConnected() {
//        Log.i(TAG, "Service connected")
//        updateStatus()
//        updateDrawerHeader(null)
//
//        // Check if a Steam key was sent to us from another app
//        if (Intent.ACTION_SEND == intent.action) {
//            handleKeyIntent(intent)
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//        WindowCompat.setDecorFitsSystemWindows(window, false)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
//            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
//        ) {
//            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
//        }
//
//        //val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        //setSupportActionBar(toolbar)
//
//        mainContainer = findViewById(R.id.main_container)
//
//        drawerLayout = findViewById(R.id.drawer_layout)
//        // On tablets we use the DrawerView but not the DrawerLayout
//        if (drawerLayout != null) {
//            drawerToggle = object : ActionBarDrawerToggle(
//                this,
//                drawerLayout,
//                R.string.open_drawer,
//                R.string.close_drawer
//            ) {
//                override fun onDrawerClosed(drawerView: View) {
//                    super.onDrawerClosed(drawerView)
//                    invalidateOptionsMenu()
//                }
//
//                override fun onDrawerOpened(drawerView: View) {
//                    super.onDrawerOpened(drawerView)
//                    invalidateOptionsMenu()
//                }
//            }
//            drawerLayout!!.addDrawerListener(drawerToggle!!)
//            supportActionBar?.setDisplayHomeAsUpEnabled(true)
//            supportActionBar?.setHomeButtonEnabled(true)
//        }
//        val contentFrame = findViewById<FrameLayout>(R.id.content_frame)
//        val insetRoot: View = drawerLayout ?: mainContainer!!
//        ViewCompat.setOnApplyWindowInsetsListener(insetRoot) { _, insetsCompat ->
//            val bars = insetsCompat.getInsets(
//                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
//            )
//            // toolbar.setPaddingRelative(
//            //     toolbar.paddingStart, bars.top,
//            //     toolbar.paddingEnd, toolbar.paddingBottom
//            // )
//            contentFrame.setPaddingRelative(
//                contentFrame.paddingStart, contentFrame.paddingTop,
//                contentFrame.paddingEnd, bars.bottom
//            )
//            insetsCompat
//        }
//
//        drawerView = findViewById(R.id.left_drawer)
//        // Disable shadow
//        drawerView.elevation = 0f
//        drawerView.setNavigationItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.logout -> {
//                    // No page for this
//                    doLogout()
//                }
//
//                R.id.about -> {
//                    supportFragmentManager.beginTransaction()
//                        .replace(R.id.content_frame, AboutFragment.newInstance())
//                        .addToBackStack(null)
//                        .commit()
//                    closeDrawer()
//                }
//
//                else -> {
//                    // Go to page
//                    selectItem(item.itemId, true)
//                }
//            }
//            true
//        }
//
//        // Get avatar and username views from drawer header
//        val headerView = drawerView.getHeaderView(0)
//        avatarView = headerView.findViewById(R.id.avatar)
//        usernameView = headerView.findViewById(R.id.username)
//        logoutToggle = headerView.findViewById(R.id.logout_toggle)
//        headerView.setOnClickListener {
//            logoutExpanded = !logoutExpanded
//            val rotation = if (logoutExpanded) 180 else 0
//            logoutToggle.animate().rotation(rotation.toFloat()).setDuration(250).start()
//            drawerView.menu.setGroupVisible(R.id.logout_group, logoutExpanded)
//        }
//
//        // Update the navigation drawer and title on backstack changes
//        supportFragmentManager.addOnBackStackChangedListener {
//            updateStatus()
//        }
//
//        if (savedInstanceState != null) {
//            drawerItemId = savedInstanceState.getInt(DRAWER_ITEM)
//            logoutExpanded = savedInstanceState.getBoolean(LOGOUT_EXPANDED)
//            setTitle(savedInstanceState.getString(TITLE)!!)
//            drawerView.menu.setGroupVisible(R.id.logout_group, logoutExpanded)
//            logoutToggle.rotation = (if (logoutExpanded) 180 else 0).toFloat()
//        } else {
//            logoutExpanded = false
//            selectItem(R.id.home, false)
//        }
//    }
//
//    public override fun onPostCreate(savedInstanceState: Bundle?) {
//        super.onPostCreate(savedInstanceState)
//        drawerToggle?.syncState()
//    }
//
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        drawerToggle?.onConfigurationChanged(newConfig)
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putInt(DRAWER_ITEM, drawerItemId)
//        outState.putString(TITLE, title)
//        outState.putBoolean(LOGOUT_EXPANDED, logoutExpanded)
//    }
//
//    /**
//     * Activate a Steam key sent from another app
//     */
//    private fun handleKeyIntent(intent: Intent) {
//        val key = intent.getStringExtra(Intent.EXTRA_TEXT)
//        if (PrefsManager.getRefreshToken().isNotEmpty() && key != null) {
//            service!!.redeemKey(key.trim())
//        } else {
//            Toast.makeText(applicationContext, R.string.error_not_logged_in, Toast.LENGTH_LONG)
//                .show()
//        }
//        finish()
//    }
//
//    private fun selectItem(id: Int, addToBackStack: Boolean) {
//        if (drawerItemId == id) {
//            // Already selected
//            closeDrawer()
//            return
//        }
//
//        val fragment: Fragment = when (id) {
//            R.id.home -> HomeFragment.newInstance()
//            R.id.games -> GamesFragment.newInstance()
//            R.id.settings -> SettingsFragment.newInstance()
//            else -> Fragment()
//        }
//        val ft = supportFragmentManager.beginTransaction()
//        ft.replace(R.id.content_frame, fragment)
//        if (addToBackStack) {
//            ft.addToBackStack(null)
//        }
//        ft.commit()
//        closeDrawer()
//    }
//
//    private val currentFragment: Fragment?
//        get() = supportFragmentManager.findFragmentById(R.id.content_frame)
//
//    private fun closeDrawer() {
//        drawerLayout?.closeDrawer(drawerView)
//    }
//
//    fun openDrawer() {
//        drawerLayout?.openDrawer(drawerView)
//    }
//
//    fun stopSteamService() {
//        stopSteam()
//    }
//
//    override fun setTitle(titleId: Int) {
//        title = getString(titleId)
//        super.setTitle(titleId)
//    }
//
//    override fun setTitle(title: CharSequence) {
//        this.title = title.toString()
//        super.setTitle(title)
//    }
//
//    override fun onResume() {
//        super.onResume()
//        val filter = IntentFilter()
//        filter.addAction(SteamService.LOGIN_EVENT)
//        filter.addAction(SteamService.DISCONNECT_EVENT)
//        filter.addAction(SteamService.STOP_EVENT)
//        filter.addAction(SteamService.PERSONA_EVENT)
//        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
//        // Listen for preference changes
//        prefs = PrefsManager.getPrefs()
//        prefs.registerOnSharedPreferenceChangeListener(this)
//    }
//
//    override fun onPause() {
//        super.onPause()
//        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
//        prefs.unregisterOnSharedPreferenceChangeListener(this)
//    }
//
//    /**
//     * Update the fragments
//     */
//    private fun updateStatus() {
//        drawerView.getHeaderView(0).isClickable = service != null && service!!.isLoggedIn
//        when (val fragment = this.currentFragment) {
//            is HomeFragment -> {
//                drawerItemId = R.id.home
//                setTitle(R.string.app_name)
//                drawerView.menu.findItem(R.id.home).isChecked = true
//            }
//
//            is GamesFragment -> {
//                drawerItemId = R.id.games
//                setTitle("")
//                drawerView.menu.findItem(R.id.games).isChecked = true
//                fragment.update(ArrayList(service!!.currentGames))
//            }
//
//            is SettingsFragment -> {
//                drawerItemId = R.id.settings
//                setTitle(R.string.settings)
//                drawerView.menu.findItem(R.id.settings).isChecked = true
//            }
//
//            is AboutFragment -> {
//                drawerItemId = R.id.about
//                setTitle(R.string.about)
//                drawerView.menu.findItem(R.id.about).isChecked = true
//            }
//        }
//    }
//
//
//    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
//        when (key) {
//            // Keep device awake or allow it to sleep
//            "stay_awake" -> service!!.setWakeLock(PrefsManager.stayAwake())
//
//            // Change status
//            "offline" -> service!!.changeStatus(if (PrefsManager.getOffline()) EPersonaState.Offline else EPersonaState.Online)
//
//            "language" -> Toast.makeText(this, R.string.language_changed, Toast.LENGTH_LONG).show()
//        }
//    }
//
//    companion object {
//        private val TAG: String = MainActivity::class.java.simpleName
//        private const val DRAWER_ITEM = "DRAWER_ITEM"
//        private const val TITLE = "TITLE"
//        private const val LOGOUT_EXPANDED = "LOGOUT_EXPANDED"
//    }
//}
