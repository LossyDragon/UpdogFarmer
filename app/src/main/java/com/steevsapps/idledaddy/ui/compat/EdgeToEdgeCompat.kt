package com.steevsapps.idledaddy.ui.compat

import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/***
 * From: https://github.com/android/nav3-recipes/blob/main/app/src/main/java/com/example/nav3recipes/ui/EdgeToEdgeCompat.kt
 */
fun ComponentActivity.setEdgeToEdgeConfig() {
    // App theme is always dark, so force light system bar icons regardless of system setting
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        // Force the 3-button navigation bar to be transparent
        // See: https://developer.android.com/develop/ui/views/layout/edge-to-edge#create-transparent
        window.isNavigationBarContrastEnforced = false
    }
}