package com.steevsapps.idledaddy.ui.component.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game

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