package com.steevsapps.idledaddy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.steevsapps.idledaddy.steam.SteamService
import com.steevsapps.idledaddy.steam.SteamServiceConnection
import com.steevsapps.idledaddy.steam.SteamServiceState
import com.steevsapps.idledaddy.ui.compat.setEdgeToEdgeConfig
import com.steevsapps.idledaddy.ui.screen.about.AboutScreen
import com.steevsapps.idledaddy.ui.screen.games.GamesScreen
import com.steevsapps.idledaddy.ui.screen.home.HomeScreen
import com.steevsapps.idledaddy.ui.screen.login.LoginScreen
import com.steevsapps.idledaddy.ui.screen.settings.SettingsScreen
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import com.steevsapps.idledaddy.utils.LocaleManager
import com.steevsapps.idledaddy.utils.Utils
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.android.ext.android.inject

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

    private val serviceConnection: SteamServiceConnection by inject()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    private fun stopSteam() {
        serviceConnection.unbind()
        stopService(SteamService.createIntent(this))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.i(TAG, "onCreate")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        serviceConnection.bind()
        setEdgeToEdgeConfig()
        setContent {
            val backStack = rememberNavBackStack(NavKeyRoot.Home)
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val serviceState by serviceConnection.state.collectAsStateWithLifecycle()
            IdleTheme {
                MainScreen(
                    serviceState = serviceState,
                    backStack = backStack,
                    drawerState = drawerState,
                    onLogOff = { serviceConnection.service.value?.logoff() },
                    onStopSteam = ::stopSteam,
                )
            }
        }
    }
}

@Composable
private fun MainScreen(
    serviceState: SteamServiceState,
    backStack: NavBackStack<NavKey>,
    drawerState: DrawerState,
    onLogOff: () -> Unit,
    onStopSteam: () -> Unit,
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
                            model = Utils.avatar(serviceState.avatarHash),
                            contentDescription = null,
                            modifier = Modifier
                                .size(100.dp)
                                .dropShadow(
                                    RoundedCornerShape(4.dp),
                                    androidx.compose.ui.graphics.shadow.Shadow(
                                        radius = 10.dp,
                                        spread = 6.dp,
                                        color = Color(0x40000000),
                                        offset = DpOffset(x = 4.dp, 4.dp)
                                    )
                                ),
                        )
                        Column(Modifier.padding(start = 16.dp)) {
                            val textShadow = Shadow(
                                color = Color.Black.copy(alpha = 0.9f),
                                offset = Offset(0f, 2f),
                                blurRadius = 2f,
                            )
                            Text(
                                text = serviceState.personaName.trim(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(shadow = textShadow),
                            )
                            Text(
                                text = serviceState.personaState.name,
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
                        onLogOff()
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
                        onLoginClick = { backStack.add(NavKeyRoot.Login) },
                        onStopSteam = onStopSteam,
                    )
                }
                entry<NavKeyRoot.Login> {
                    LoginScreen(
                        onBack = backStack::removeLastOrNull,
                        onLoggedIn = { backStack.remove(NavKeyRoot.Login) },
                    )
                }
                entry<NavKeyRoot.Games> {
                    GamesScreen(onMenuClick = toggleDrawer)
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
                serviceState = SteamServiceState(
                    personaName = "Steev"
                ),
                backStack = backStack,
                drawerState = drawerState,
                onLogOff = {},
                onStopSteam = {},
            )
        }
    }
}
