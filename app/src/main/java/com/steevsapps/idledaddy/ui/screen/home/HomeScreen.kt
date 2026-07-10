package com.steevsapps.idledaddy.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager.minimizeData
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.component.cards.DropInfoCard
import com.steevsapps.idledaddy.ui.component.cards.NowPlayingCard
import com.steevsapps.idledaddy.ui.component.cards.StartCard
import com.steevsapps.idledaddy.ui.component.cards.StatusCard
import com.steevsapps.idledaddy.ui.component.cards.StopCard
import com.steevsapps.idledaddy.ui.component.dialog.CustomAppDialog
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onMenuClick: () -> Unit,
    onLoginClick: () -> Unit,
    onStopSteam: () -> Unit,
) {
    val viewModel = koinViewModel<HomeViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        state = state,
        showIcon = !minimizeData(),
        onMenuClick = onMenuClick,
        onStatusClick = onLoginClick,
        onStartFarming = viewModel::startFarming,
        onStopGame = viewModel::stopGame,
        onPauseResume = viewModel::pauseOrResume,
        onNextGame = viewModel::skipGame,
        onStopSteam = onStopSteam,
        onIdleCustomApp = viewModel::idleGame,
        onIdleCustomApps = viewModel::idleGames,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeUiState,
    showIcon: Boolean,
    onMenuClick: () -> Unit,
    onStatusClick: () -> Unit,
    onStartFarming: () -> Unit,
    onStopGame: () -> Unit,
    onPauseResume: () -> Unit,
    onNextGame: () -> Unit,
    onStopSteam: () -> Unit,
    onIdleCustomApp: (Game) -> Unit,
    onIdleCustomApps: (List<Game>) -> Unit,
) {
    // Currently just show the first game
    val game = state.currentGames.firstOrNull()
    val showNext = state.farming && state.currentGames.size == 1
    var customAppDialogVisible by rememberSaveable { mutableStateOf(state.customAppDialogVisible) }

    IdleTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = stringResource(R.string.open_drawer),
                            )
                        }
                    },
                    actions = {
                        if (state.loggedIn) {
                            IconButton(onClick = { customAppDialogVisible = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.idle_custom_app),
                                )
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                state = rememberLazyListState(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    StatusCard(
                        isLoggedIn = state.loggedIn,
                        isParentalControlled = state.parentalStatus,
                        isBlocked = state.blocked,
                        nextRetryAtMillis = state.nextRetryAtMillis,
                        onClick = onStatusClick,
                    )
                }
                if (game != null) {
                    item {
                        NowPlayingCard(
                            game = game,
                            isPaused = state.paused,
                            showNext = showNext,
                            showIcon = showIcon,
                            onStop = onStopGame,
                            onPauseResume = onPauseResume,
                            onNext = onNextGame,
                        )
                    }
                }
                if (state.showDropInfo) {
                    item {
                        DropInfoCard(
                            gameCount = state.gameCount,
                            cardCount = state.cardCount
                        )
                    }
                }
                item {
                    StartCard(
                        enabled = state.loggedIn && !state.farming,
                        onClick = onStartFarming,
                    )
                }
                item {
                    StopCard(
                        onClick = onStopSteam,
                    )
                }
            }
        }

        if (customAppDialogVisible) {
            CustomAppDialog(
                onConfirm = { customGame ->
                    onIdleCustomApp(customGame)
                    customAppDialogVisible = false
                },
                onConfirmList = { customGames ->
                    onIdleCustomApps(customGames)
                    customAppDialogVisible = false
                },
                onDismiss = { customAppDialogVisible = false },
            )
        }
    }
}


/**
 * Preview
 */

private class HomePreview : PreviewParameterProvider<HomeUiState> {
    private val states = listOf(
        "Logged out" to HomeUiState(),
        "Idling" to HomeUiState(
            loggedIn = true,
            currentGames = listOf(Game(440, "Team Fortress 2", 987654F, 42)),
            gameCount = 66,
            cardCount = 68,
            farming = true,
            showDropInfo = true,
        ),
        "Paused with parental" to HomeUiState(
            loggedIn = true,
            parentalStatus = true,
            currentGames = listOf(Game(440, "Team Fortress 2", 987654F, 42)),
            paused = true,
        ),
        "Custom app dialog open" to HomeUiState(
            loggedIn = true,
            customAppDialogVisible = true,
        ),
    )

    override val values = states.asSequence().map { it.second }

    override fun getDisplayName(index: Int) = states[index].first
}

@Preview
@Composable
private fun Preview(@PreviewParameter(HomePreview::class) state: HomeUiState) {
    HomeScreenContent(
        state = state,
        showIcon = true,
        onMenuClick = {},
        onStatusClick = {},
        onStartFarming = {},
        onStopGame = {},
        onPauseResume = {},
        onNextGame = {},
        onStopSteam = {},
        onIdleCustomApp = {},
        onIdleCustomApps = {},
    )
}