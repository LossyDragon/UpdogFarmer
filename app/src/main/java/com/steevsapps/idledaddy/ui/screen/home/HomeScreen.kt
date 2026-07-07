package com.steevsapps.idledaddy.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager.minimizeData
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.component.DropInfoCard
import com.steevsapps.idledaddy.ui.component.NowPlayingCard
import com.steevsapps.idledaddy.ui.component.StartCard
import com.steevsapps.idledaddy.ui.component.StatusCard
import com.steevsapps.idledaddy.ui.component.StopCard
import com.steevsapps.idledaddy.ui.theme.IdleTheme

private const val TYPE_APPID = 0
private const val TYPE_CUSTOM = 1

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onMenuClick: () -> Unit,
    onLoginClick: () -> Unit,
    onStopSteam: () -> Unit,
) {
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
                onDismiss = { customAppDialogVisible = false },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomAppDialog(
    onConfirm: (Game) -> Unit,
    onDismiss: () -> Unit,
) {
    val resources = LocalResources.current
    val typeOptions = stringArrayResource(R.array.custom_app_type_options)
    var typeIndex by rememberSaveable { mutableIntStateOf(TYPE_APPID) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.idle_custom_app)) },
        text = {
            Column {
                Box {
                    TextButton(onClick = { typeMenuExpanded = true }) {
                        Text(text = typeOptions[typeIndex])
                        Icon(
                            imageVector = if (typeMenuExpanded) {
                                Icons.Default.ArrowDropUp
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                            contentDescription = null,
                        )
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        typeOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    typeIndex = index
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text(text = stringResource(R.string.desc_custom_app)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (typeIndex == TYPE_APPID) KeyboardType.Number else KeyboardType.Text,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val text = input.trim()
                val game = if (text.isEmpty()) {
                    null
                } else {
                    when (typeIndex) {
                        TYPE_APPID -> text.toIntOrNull()?.let { appId ->
                            Game(appId, resources.getString(R.string.playing_unknown_app, appId), 0f, 0)
                        }

                        TYPE_CUSTOM -> Game(0, text, 0f, 0)
                        else -> null
                    }
                }
                if (game != null) {
                    onConfirm(game)
                } else {
                    onDismiss()
                }
            }) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
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
    )
}