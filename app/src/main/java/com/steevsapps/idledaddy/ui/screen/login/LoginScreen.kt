package com.steevsapps.idledaddy.ui.screen.login

import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.theme.IdleTheme

@Composable
fun LoginScreen(
    snackbarHostState: SnackbarHostState
) {
}

@Composable
private fun LoginScreenContent(
    snackbarHostState: SnackbarHostState
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                backgroundColor = Color.Transparent,
                elevation = 0.dp
            ) {
                Text(
                    text = stringResource(R.string.login_to_steam),
                    fontSize = 30.sp
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* TODO */ },
                text = { Text(text = "QR Sign in") },
                icon = { Icon(imageVector = Icons.Default.QrCode2, contentDescription = null) },
                shape = MaterialTheme.shapes.small
            )
        }
    ) { pv ->

    }
}

@Preview
@Composable
private fun Preview() {
    IdleTheme {
        LoginScreenContent(
            snackbarHostState = SnackbarHostState(),
        )
    }
}