package com.steevsapps.idledaddy.ui.component.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.SubcomposeAsyncImage
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import com.steevsapps.idledaddy.ui.theme.LocalAmoled
import kotlin.math.ceil

@Composable
fun NowPlayingCard(
    game: Game,
    isPaused: Boolean,
    showNext: Boolean,
    showIcon: Boolean,
    onStop: () -> Unit,
    onPauseResume: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amoled = LocalAmoled.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (amoled) Color(0xFF101010) else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier.size(100.dp),
                model = game.iconUrl.takeIf { showIcon },
                contentDescription = stringResource(R.string.desc_game_icon),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                },
                error = { PlaceholderIcon() },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = if (game.appId == 0) {
                        stringResource(R.string.playing_non_steam_game, game.name)
                    } else {
                        game.name
                    },
                    color = Color(0xFF90BA3C),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = if (isPaused) {
                        stringResource(R.string.paused)
                    } else if (game.dropsRemaining > 0) {
                        pluralStringResource(
                            R.plurals.card_drops_remaining,
                            game.dropsRemaining,
                            game.dropsRemaining,
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.hours_on_record,
                            if (game.hoursPlayed < 1) 0 else ceil(game.hoursPlayed.toDouble()).toInt(),
                            game.hoursPlayed,
                        )
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    IconButton(onClick = onStop) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = stringResource(R.string.desc_stop_button),
                            tint = if (amoled) Color(0xFF4A4A4A) else LocalContentColor.current,
                        )
                    }
                    IconButton(onClick = onPauseResume) {
                        Icon(
                            imageVector = if (isPaused) {
                                Icons.Default.PlayArrow
                            } else {
                                Icons.Default.Pause
                            },
                            contentDescription = stringResource(R.string.pause_button),
                            tint = if (amoled) Color(0xFF4A4A4A) else LocalContentColor.current,
                        )
                    }
                    if (showNext) {
                        IconButton(onClick = onNext) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = stringResource(R.string.next_button),
                                tint = if (amoled) Color(0xFF4A4A4A) else LocalContentColor.current,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceholderIcon() {
    val amoled = LocalAmoled.current
    Icon(
        imageVector = Icons.Default.Image,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        tint = if (amoled) Color(0xFF4A4A4A) else LocalContentColor.current,
    )
}

/**
 * Preview
 */

private class NowPlayingPreview : PreviewParameterProvider<Pair<Boolean, Game>> {
    override val values = sequenceOf(
        false to Game(440, "Team Fortress 2", 12.3f, 3),
        false to Game(0, "Idle Daddy", 0.5f, 0),
        true to Game(440, "Team Fortress 2", 12.3f, 3),
    )

    override fun getDisplayName(index: Int): String {
        val (amoled, game) = values.elementAt(index)
        return if (amoled) "AMOLED ${game.name}" else game.name
    }
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(NowPlayingPreview::class) values: Pair<Boolean, Game>,
) {
    val (amoled, game) = values
    IdleTheme(amoled = amoled) {
        NowPlayingCard(
            game = game,
            isPaused = game.appId == 0,
            showNext = game.appId != 0,
            showIcon = false,
            onStop = {},
            onPauseResume = {},
            onNext = {},
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Preview(name = "Icon loading")
@Composable
private fun LoadingPreview() {
    val previewHandler = AsyncImagePreviewHandler { _, _ ->
        AsyncImagePainter.State.Loading(null)
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        IdleTheme {
            NowPlayingCard(
                game = Game(440, "Team Fortress 2", 12.3f, 3),
                isPaused = false,
                showNext = true,
                showIcon = true,
                onStop = {},
                onPauseResume = {},
                onNext = {},
            )
        }
    }
}