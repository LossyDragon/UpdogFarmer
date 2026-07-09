package com.steevsapps.idledaddy.ui.component.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.component.OreoTextField
import com.steevsapps.idledaddy.ui.theme.IdleTheme

@Composable
fun RedeemDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var key by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.redeem)) },
        text = {
            OreoTextField(
                value = key,
                onValueChange = { key = it },
                placeholder = stringResource(R.string.redeem_msg),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(key) }) {
                Text(text = stringResource(android.R.string.ok))
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
            RedeemDialog(onConfirm = {}, onDismiss = {})
        }
    }
}