package com.steevsapps.idledaddy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.ui.compat.darken

// Legacy AppTheme palette (colors.xml/styles.xml), kept for the Oreo-era AppCompat dark look
private val GreyPrimary = Color(0xFF424242)
private val GreyPrimaryDark = Color(0xFF212121)
private val RedAccent = Color(0xFFEF5350)
private val WindowBackground = Color(0xFF303030)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xB3FFFFFF)
private val Divider = Color(0xFF4A4A4A)
private val RedAccentDark = RedAccent.darken(0.15f)

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

private val AmoledColorScheme = OreoColorScheme.copy(
    primary = RedAccentDark,
    primaryContainer = RedAccentDark,
    secondary = RedAccentDark,
    tertiary = RedAccentDark,
    background = Color.Black,
    surface = Color.Black,
    surfaceTint = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = GreyPrimaryDark,
    surfaceContainerHighest = Color(0xFF2C2C2C),
)

// Oreo-era Material used 2dp corners on cards, dialogs, and menus
private val OreoShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
)

val LocalAmoled = staticCompositionLocalOf { false }

@Composable
fun IdleTheme(
    amoled: Boolean = false,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalAmoled provides amoled) {
        MaterialTheme(
            colorScheme = if (amoled) AmoledColorScheme else OreoColorScheme,
            shapes = OreoShapes,
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                content = content,
            )
        }
    }
}
