package com.steevsapps.idledaddy.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.theme.IdleTheme

enum class NavIcon(val imageVector: ImageVector, @StringRes val desc: Int) {
    Back(Icons.AutoMirrored.Filled.ArrowBack, R.string.go_back),
    Menu(Icons.Filled.Menu, R.string.open_drawer),
}

@Composable
fun IdleTopAppBar(
    title: String,
    onNavClick: () -> Unit,
    navIcon: NavIcon = NavIcon.Back,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            IconButton(onClick = onNavClick) {
                Icon(
                    imageVector = navIcon.imageVector,
                    contentDescription = stringResource(navIcon.desc),
                )
            }
        },
        actions = actions,
    )
}

/**
 * Preview
 */

@Preview
@Composable
private fun PreviewBack() {
    IdleTheme {
        IdleTopAppBar(
            title = stringResource(R.string.app_name),
            navIcon = NavIcon.Back,
            onNavClick = {},
        )
    }
}

@Preview
@Composable
private fun PreviewMenu() {
    IdleTheme {
        IdleTopAppBar(
            title = stringResource(R.string.app_name),
            navIcon = NavIcon.Menu,
            onNavClick = {},
        )
    }
}