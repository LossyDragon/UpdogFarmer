package com.steevsapps.idledaddy.ui.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val darkColors = darkColors(primary = Color(0xFF424242))

@Composable
fun IdleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = darkColors,
        content = content,
        shapes = MaterialTheme.shapes.copy(
            small = MaterialTheme.shapes.small,
            medium = MaterialTheme.shapes.small,
            large = MaterialTheme.shapes.small,
        )
    )
}