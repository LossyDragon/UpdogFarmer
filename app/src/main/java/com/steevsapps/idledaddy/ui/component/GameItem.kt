package com.steevsapps.idledaddy.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil3.compose.SubcomposeAsyncImage
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import kotlin.math.ceil

@Composable
fun GameItem(
    game: Game,
    selected: Boolean,
    showIcon: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
            SubcomposeAsyncImage(
                model = game.iconUrl.takeIf { showIcon },
                contentDescription = stringResource(R.string.desc_game_icon),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(292f / 136f),
                contentScale = ContentScale.Fit,
                loading = { GameIconPlaceholder() },
                error = { GameIconPlaceholder() },
            )
            Spacer(modifier = Modifier.height(6.dp))
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

@Composable
private fun GameIconPlaceholder() {
    Icon(
        imageVector = Icons.Default.Image,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
    )
}

/**
 * Preview
 */

private class GameItemPreview : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

@Preview(widthDp = 180)
@Composable
private fun Preview(
    @PreviewParameter(GameItemPreview::class) selected: Boolean,
) {
    IdleTheme {
        GameItem(
            game = Game(440, "Team Fortress 2", 12.3f, 3),
            selected = selected,
            showIcon = false,
            onClick = {},
            onLongClick = {},
        )
    }
}