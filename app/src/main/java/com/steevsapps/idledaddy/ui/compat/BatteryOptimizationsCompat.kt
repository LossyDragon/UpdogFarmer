package com.steevsapps.idledaddy.ui.compat

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.steevsapps.idledaddy.R

private const val TAG = "BatteryOptimizationsCompat"

/** Show the system dialog to exempt the app from Doze so idling survives screen-off. */
@SuppressLint("BatteryLife") // Sideloaded app, Play's policy on this permission doesn't apply
fun Context.requestIgnoreBatteryOptimizations() {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    if (pm.isIgnoringBatteryOptimizations(packageName)) {
        Log.i(TAG, "Doze exemption already granted")
    } else {
        Log.i(TAG, "Requesting Doze exemption")
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData("package:$packageName".toUri())
        )
    }
}

/**
 * There's no API to un-exempt ourselves, so send the user to the system list to undo it manually.
 * No-op if the exemption was never granted.
 */
fun Context.restoreBatteryOptimizations() {
    val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
    if (pm.isIgnoringBatteryOptimizations(packageName)) {
        Log.i(TAG, "Doze exemption still granted, sending user to settings to remove it")
        Toast.makeText(
            this,
            getString(R.string.battery_optimization_restore_hint, getString(R.string.app_name)),
            Toast.LENGTH_LONG
        ).show()
        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}