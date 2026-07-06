package com.steevsapps.idledaddy.ui.screen.games

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.component.GameItem
import com.steevsapps.idledaddy.ui.theme.IdleTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesScreen(
    games: List<Game>,
    selected: List<Game>,
    refreshing: Boolean,
    showPlayAll: Boolean,
    showRedeem: Boolean,
    showIcons: Boolean,
    optionsGame: Game?,
    optionsBlacklisted: Boolean,
    onRefresh: () -> Unit,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onPlayAll: () -> Unit,
    onRedeem: () -> Unit,
    onToggleBlacklist: (Game) -> Unit,
    onDismissOptions: () -> Unit,
) {
    IdleTheme {
        val gridState = rememberLazyGridState()

        LaunchedEffect(games) {
            if (games.isNotEmpty()) {
                gridState.scrollToItem(0)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(integerResource(R.integer.game_columns)),
                    modifier = Modifier.fillMaxSize(),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 8.dp,
                        top = 8.dp,
                        end = 8.dp,
                        bottom = if (showRedeem) 88.dp else 8.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showPlayAll) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            PlayAllButton(onClick = onPlayAll)
                        }
                    }
                    items(items = games, key = { it.appId }) { game ->
                        GameItem(
                            game = game,
                            selected = selected.contains(game),
                            showIcon = showIcons,
                            onClick = { onGameClick(game) },
                            onLongClick = { onGameLongClick(game) },
                        )
                    }
                }
                if (games.isEmpty() && !refreshing) {
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

            optionsGame?.let { game ->
                GameOptionsDialog(
                    game = game,
                    blacklisted = optionsBlacklisted,
                    onToggleBlacklist = { onToggleBlacklist(game) },
                    onDismiss = onDismissOptions,
                )
            }

            if (showRedeem) {
                FloatingActionButton(
                    onClick = onRedeem,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.redeem),
                    )
                }
            }
        }
    }
}

@Composable
private fun GameOptionsDialog(
    game: Game,
    blacklisted: Boolean,
    onToggleBlacklist: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = game.name) },
        confirmButton = {
            TextButton(onClick = onToggleBlacklist) {
                Text(
                    text = stringResource(
                        if (blacklisted) {
                            R.string.remove_from_blacklist
                        } else {
                            R.string.add_to_blacklist
                        }
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
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

@Preview(name = "Games")
@Composable
private fun PreviewGames() {
    GamesPreview(
        games = listOf(
            Game(440, "Team Fortress 2", 12.3f, 3),
            Game(730, "Counter-Strike 2", 1502.7f, 0),
            Game(570, "Dota 2", 0.5f, 1),
        ),
    )
}

@Preview(name = "Empty")
@Composable
private fun PreviewEmpty() {
    GamesPreview(games = emptyList())
}

@Composable
private fun GamesPreview(games: List<Game>) {
    GamesScreen(
        games = games,
        selected = games.take(1),
        refreshing = false,
        showPlayAll = games.isNotEmpty(),
        showRedeem = true,
        showIcons = false,
        optionsGame = null,
        optionsBlacklisted = false,
        onRefresh = {},
        onGameClick = {},
        onGameLongClick = {},
        onPlayAll = {},
        onRedeem = {},
        onToggleBlacklist = {},
        onDismissOptions = {},
    )
}