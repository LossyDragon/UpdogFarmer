package com.steevsapps.idledaddy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Legacy AppTheme palette (colors.xml/styles.xml), kept for the Oreo-era AppCompat dark look
private val GreyPrimary = Color(0xFF424242)
private val GreyPrimaryDark = Color(0xFF212121)
private val RedAccent = Color(0xFFEF5350)
private val WindowBackground = Color(0xFF303030)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xB3FFFFFF)
private val Divider = Color(0xFF4A4A4A)

/**
 * M3 scheme mapped to the old AppCompat roles: `primary` here does the job of `colorAccent`
 * (that's what M3 components tint their active states with), and the surface-container steps
 * are the flat greys AppCompat dark used instead of tonal elevation.
 */
private val OreoColorScheme = darkColorScheme(
    primary = RedAccent,
    onPrimary = Color.White,
    primaryContainer = RedAccent,
    onPrimaryContainer = Color.White,
    secondary = RedAccent,
    onSecondary = Color.White,
    secondaryContainer = GreyPrimary,
    onSecondaryContainer = TextPrimary,
    tertiary = RedAccent,
    onTertiary = Color.White,
    background = WindowBackground,
    onBackground = TextPrimary,
    surface = WindowBackground,
    onSurface = TextPrimary,
    surfaceVariant = GreyPrimary,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = GreyPrimaryDark,
    surfaceContainerLow = Color(0xFF383838),
    surfaceContainer = Color(0xFF3C3C3C),
    surfaceContainerHigh = GreyPrimary,
    surfaceContainerHighest = Color(0xFF4F4F4F),
    surfaceTint = WindowBackground,
    outline = Color(0xFF8A8A8A),
    outlineVariant = Divider,
)

// Oreo-era Material used 2dp corners on cards, dialogs, and menus
private val OreoShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
)

@Composable
fun IdleTheme(content: @Composable () -> Unit, ) {
    MaterialTheme(
        colorScheme = OreoColorScheme,
        shapes = OreoShapes,
        content = content,
    )
}
