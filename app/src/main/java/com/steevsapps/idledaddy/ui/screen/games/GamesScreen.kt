package com.steevsapps.idledaddy.ui.screen.games

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.component.GameItem
import com.steevsapps.idledaddy.ui.component.OreoTextField
import com.steevsapps.idledaddy.ui.component.dialog.GameOptionsDialog
import com.steevsapps.idledaddy.ui.component.scrollbar
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun GamesScreen(
    onMenuClick: () -> Unit,
) {
    val viewModel = koinViewModel<GamesViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.saveLastSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    GamesScreenContent(
        state = state,
        onMenuClick = onMenuClick,
        onQueryChange = viewModel::setQuery,
        onTabChange = viewModel::switchTab,
        onSortChange = viewModel::sort,
        onRefresh = viewModel::refresh,
        onGameClick = viewModel::toggleGame,
        onGameLongClick = viewModel::showOptions,
        onPlayAll = viewModel::playAll,
        onToggleBlacklist = viewModel::toggleBlacklist,
        onDismissOptions = viewModel::dismissOptions,
        onImageError = viewModel::onImageError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreenContent(
    state: GamesScreenState,
    onMenuClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTabChange: (Int) -> Unit,
    onSortChange: (Int) -> Unit,
    onRefresh: () -> Unit,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onPlayAll: () -> Unit,
    onToggleBlacklist: (Game) -> Unit,
    onDismissOptions: () -> Unit,
    onImageError: (Game) -> Unit,
) {
    IdleTheme {
        val gridState = rememberLazyGridState()
        var fabMenuExpanded by rememberSaveable { mutableStateOf(state.fabMenuExpanded) }

        LaunchedEffect(state.games) {
            if (state.games.isNotEmpty()) {
                gridState.scrollToItem(0)
            }
        }

        BackHandler(enabled = fabMenuExpanded) {
            fabMenuExpanded = false
        }

        Scaffold(
            topBar = {
                GamesTopBar(
                    tab = state.tab,
                    query = state.query,
                    onMenuClick = onMenuClick,
                    onQueryChange = onQueryChange,
                    onSortChange = onSortChange,
                )
            },
            floatingActionButton = {
                GamesFabMenu(
                    expanded = fabMenuExpanded,
                    onExpandedChange = { fabMenuExpanded = it },
                    onTabChange = onTabChange,
                )
            },
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                PullToRefreshBox(
                    isRefreshing = state.refreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .scrollbar(gridState),
                        state = gridState,
                        contentPadding = PaddingValues(
                            start = 8.dp,
                            top = 8.dp,
                            end = 8.dp,
                            bottom = 88.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.showPlayAll) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                PlayAllButton(onClick = onPlayAll)
                            }
                        }
                        items(items = state.games, key = { it.appId }) { game ->
                            GameItem(
                                game = game,
                                selected = state.selected.contains(game),
                                showIcon = state.showIcons,
                                iconUrl = state.iconOverrides[game.appId] ?: game.iconUrl,
                                onClick = { onGameClick(game) },
                                onLongClick = { onGameLongClick(game) },
                                onImageError = onImageError,
                            )
                        }
                    }
                    if (state.games.isEmpty() && !state.refreshing) {
                        Text(
                            text = stringResource(R.string.no_games_found),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(8.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                // Scrim: catch stray taps while the FAB menu is open
                AnimatedVisibility(
                    visible = fabMenuExpanded,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                            .pointerInput(Unit) {
                                detectTapGestures { fabMenuExpanded = false }
                            }
                    )
                }

                if (state.optionsGame != null) {
                    GameOptionsDialog(
                        game = state.optionsGame,
                        blacklisted = state.optionsBlacklisted,
                        onToggleBlacklist = { onToggleBlacklist(state.optionsGame) },
                        onDismiss = onDismissOptions,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GamesTopBar(
    tab: Int,
    query: String,
    onMenuClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSortChange: (Int) -> Unit,
) {
    var searching by rememberSaveable { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    fun closeSearch() {
        searching = false
        onQueryChange("")
    }

    BackHandler(enabled = searching, onBack = ::closeSearch)

    TopAppBar(
        title = {
            if (searching) {
                OreoTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = stringResource(R.string.search),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            } else {
                Text(text = stringArrayResource(R.array.spinner_nav_options)[tab])
            }
        },
        navigationIcon = {
            if (searching) {
                IconButton(onClick = ::closeSearch) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(android.R.string.cancel),
                    )
                }
            } else {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = stringResource(R.string.open_drawer),
                    )
                }
            }
        },
        actions = {
            if (searching) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(android.R.string.cancel),
                        )
                    }
                }
            } else {
                IconButton(onClick = { searching = true }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                    )
                }
                IconButton(onClick = { sortMenuOpen = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort),
                    )
                }
                DropdownMenu(
                    expanded = sortMenuOpen,
                    onDismissRequest = { sortMenuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.sort_alphabetically)) },
                        onClick = {
                            sortMenuOpen = false
                            onSortChange(GamesViewModel.SORT_ALPHABETICALLY)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.sort_hours_played)) },
                        onClick = {
                            sortMenuOpen = false
                            onSortChange(GamesViewModel.SORT_HOURS_PLAYED)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.sort_hours_played_reversed)) },
                        onClick = {
                            sortMenuOpen = false
                            onSortChange(GamesViewModel.SORT_HOURS_PLAYED_REVERSED)
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun GamesFabMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onTabChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabNames = stringArrayResource(R.array.spinner_nav_options)
    val tabIcons = listOf(
        Icons.Default.SportsEsports,
        Icons.Default.History,
        Icons.Default.Block,
    )

    FloatingActionButtonMenu(
        expanded = expanded,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = onExpandedChange,
                containerSize = { 56.dp },
                containerCornerRadius = { 2.dp },
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.Close else Icons.Default.Apps,
                    contentDescription = null,
                    tint = if (expanded) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
                )
            }
        },
        modifier = modifier,
    ) {
        tabNames.forEachIndexed { index, name ->
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    onTabChange(index)
                },
                text = { Text(text = name) },
                icon = { Icon(imageVector = tabIcons[index], contentDescription = null) },
            )
        }
    }
}

@Composable
private fun PlayAllButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Text(
            text = stringResource(R.string.play_all),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
        )
    }
}

/**
 * Preview
 */

private class GamesPreview : PreviewParameterProvider<GamesScreenState> {
    private val sampleGames = listOf(
        Game(440, "Team Fortress 2", 12.3f, 3),
        Game(730, "Counter-Strike 2", 1502.7f, 0),
        Game(570, "Dota 2", 0.5f, 1),
    )

    private val states = listOf(
        "Games" to GamesScreenState(
            games = sampleGames,
            selected = sampleGames.take(1),
        ),
        "Last session" to GamesScreenState(
            games = sampleGames,
            tab = TAB_LAST,
            showPlayAll = true,
        ),
        "Empty" to GamesScreenState(),
        "Fab menu open" to GamesScreenState(
            games = sampleGames,
            fabMenuExpanded = true,
        ),
        "Redeem dialog open" to GamesScreenState(
            games = sampleGames,
            redeemDialogVisible = true,
        ),
        "Options dialog open" to GamesScreenState(
            games = sampleGames,
            optionsGame = sampleGames.first(),
            optionsBlacklisted = false,
        ),
    )

    override val values = states.asSequence().map { it.second }

    override fun getDisplayName(index: Int) = states[index].first
}

@Preview
@Composable
private fun Preview(@PreviewParameter(GamesPreview::class) state: GamesScreenState) {
    GamesScreenContent(
        state = state,
        onMenuClick = {},
        onQueryChange = {},
        onTabChange = {},
        onSortChange = {},
        onRefresh = {},
        onGameClick = {},
        onGameLongClick = {},
        onPlayAll = {},
        onToggleBlacklist = {},
        onDismissOptions = {},
        onImageError = {},
    )
}