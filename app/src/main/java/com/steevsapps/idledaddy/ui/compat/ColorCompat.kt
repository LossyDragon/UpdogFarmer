package com.steevsapps.idledaddy.ui.compat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** Darken by blending toward black; [fraction] 0f = unchanged, 1f = black. */
fun Color.darken(fraction: Float): Color = lerp(this, Color.Black, fraction)