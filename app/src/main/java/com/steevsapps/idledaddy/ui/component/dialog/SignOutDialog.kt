package com.steevsapps.idledaddy.ui.component.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.theme.IdleTheme

/**
 * Confirms signing out. [onConfirm] receives whether the user also chose to erase the
 * saved login data.
 */
@Composable
fun SignOutDialog(
    onConfirm: (eraseSavedLogin: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var eraseSavedLogin by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.logout)) },
        text = {
            Column {
                Text(text = stringResource(R.string.sign_out_msg))
                // The whole row toggles, so the label is part of the touch target
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .toggleable(
                            value = eraseSavedLogin,
                            role = Role.Checkbox,
                            onValueChange = { eraseSavedLogin = it },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = eraseSavedLogin, onCheckedChange = null)
                    Text(
                        text = stringResource(R.string.sign_out_erase),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                Text(
                    text = stringResource(
                        if (eraseSavedLogin) R.string.sign_out_erase_on else R.string.sign_out_erase_off
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(eraseSavedLogin) }) {
                Text(text = stringResource(R.string.logout))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    IdleTheme {
        Box(Modifier.fillMaxSize()) {
            SignOutDialog(onConfirm = {}, onDismiss = {})
        }
    }
}
