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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.component.DropInfoCard
import com.steevsapps.idledaddy.ui.component.NowPlayingCard
import com.steevsapps.idledaddy.ui.component.StartCard
import com.steevsapps.idledaddy.ui.component.StatusCard
import com.steevsapps.idledaddy.ui.component.StopCard
import com.steevsapps.idledaddy.ui.theme.IdleTheme

@Immutable
data class HomeScreenState(
    val isLoggedIn: Boolean = false,
    val isParentalControlled: Boolean = false,
    val game: Game? = null,
    val isPaused: Boolean = false,
    val showNext: Boolean = false,
    val showIcon: Boolean = false,
    val gameCount: Int = 0,
    val cardCount: Int = 0,
    val isFarming: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val state = HomeScreenState() // TODO stub

    HomeScreenContent(state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(state: HomeScreenState) {
    IdleTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(
                            onClick = { /* TODO */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = null
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { /* TODO */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null
                            )
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
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    StatusCard(
                        isLoggedIn = state.isLoggedIn,
                        isParentalControlled = state.isParentalControlled,
                        onClick = { /* TODO */ }
                    )
                }
                if (state.game != null) {
                    item {
                        NowPlayingCard(
                            game = state.game,
                            isPaused = state.isPaused,
                            showNext = state.showNext,
                            showIcon = state.showIcon,
                            onStop = { /*TODO*/ },
                            onPauseResume = { /*TODO*/ },
                            onNext = { /*TODO*/ },
                        )
                    }
                }
                if (state.isFarming) {
                    item {
                        DropInfoCard(
                            gameCount = state.gameCount,
                            cardCount = state.cardCount
                        )
                    }
                }
                item {
                    StartCard(
                        enabled = state.isLoggedIn && !state.isFarming,
                        onClick = { /* TODO */ }
                    )
                }
                item {
                    StopCard(
                        onClick = { /* TODO */ }
                    )
                }
            }
        }
    }
}

/**
 * Preview
 */

private class HomePreview : PreviewParameterProvider<HomeScreenState> {
    private val states = listOf(
        "Logged out" to HomeScreenState(),
        "Idling" to HomeScreenState(
            isLoggedIn = true,
            game = Game(440, "Team Fortress 2", 987654F, 42),
            showNext = true,
            showIcon = true,
            gameCount = 66,
            cardCount = 68,
            isFarming = true,
        ),
        "Paused with parental" to HomeScreenState(
            isLoggedIn = true,
            isParentalControlled = true,
            game = Game(440, "Team Fortress 2", 987654F, 42),
            isPaused = true,
            showIcon = true,
        ),
    )

    override val values = states.asSequence().map { it.second }

    override fun getDisplayName(index: Int) = states[index].first
}

@Preview
@Composable
private fun Preview(@PreviewParameter(HomePreview::class) state: HomeScreenState) {
    HomeScreenContent(state)
}