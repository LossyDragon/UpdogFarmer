package com.steevsapps.idledaddy.ui.component.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.theme.IdleTheme

@Composable
fun GameOptionsDialog(
    game: Game,
    blacklisted: Boolean,
    onToggleBlacklist: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = game.name) },
        text = { Text(text = stringResource(R.string.sum_blacklist)) },
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

private class BlacklistedPreview : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)

    override fun getDisplayName(index: Int) =
        if (values.elementAt(index)) "Blacklisted" else "Not blacklisted"
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(BlacklistedPreview::class) blacklisted: Boolean,
) {
    IdleTheme {
        Box(Modifier.fillMaxSize()) {
            GameOptionsDialog(
                game = Game(0, "Updog Farmer", "", 0f, 0),
                blacklisted = blacklisted,
                onToggleBlacklist = {},
                onDismiss = {}
            )
        }
    }
}