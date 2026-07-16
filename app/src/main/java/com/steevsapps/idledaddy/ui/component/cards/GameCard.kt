package com.steevsapps.idledaddy.ui.component.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
fun GameCard(
    game: Game,
    selected: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconUrl: String = game.iconUrl,
    onImageError: (Game) -> Unit,
) {
    val amoled = LocalAmoled.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (amoled) Color(0xFF101010) else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (selected) BorderStroke(2.dp, Color(0xFF90BA3C)) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(8.dp),
        ) {
            if (showIcon) {
                SubcomposeAsyncImage(
                    model = iconUrl,
                    contentDescription = stringResource(R.string.desc_game_icon),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(292f / 136f),
                    contentScale = ContentScale.Fit,
                    onError = { onImageError(game) },
                    loading = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .size(64.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(64.dp))
                        }
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = if (amoled) Color(0xFF4A4A4A) else LocalContentColor.current,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Text(
                text = game.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.hours_on_record,
                    if (game.hoursPlayed < 1) 0 else ceil(game.hoursPlayed.toDouble()).toInt(),
                    game.hoursPlayed,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * Preview
 */

private class GameItemPreview : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(GameItemPreview::class) selected: Boolean,
) {
    IdleTheme {
        GameCard(
            game = Game(440, "Team Fortress 2", 12.3f, 3),
            selected = selected,
            showIcon = true,
            onClick = {},
            onLongClick = {},
            onImageError = {},
        )
    }
}

@OptIn(ExperimentalCoilApi::class)
@Preview
@Composable
private fun LoadingPreview() {
    val previewHandler = AsyncImagePreviewHandler { _, _ ->
        AsyncImagePainter.State.Loading(null)
    }
    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        IdleTheme {
            GameCard(
                game = Game(440, "Portal 2", 12.3f, 3),
                selected = false,
                showIcon = true,
                onClick = {},
                onLongClick = {},
                onImageError = {},
            )
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Preview
@Composable
private fun LoadingDarkPreview() {
    IdleTheme(amoled = true) {
        GameCard(
            game = Game(440, "Portal 2", 12.3f, 3),
            selected = false,
            showIcon = true,
            onClick = {},
            onLongClick = {},
            onImageError = {},
        )
    }
}