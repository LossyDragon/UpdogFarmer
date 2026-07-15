package com.steevsapps.idledaddy.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.SteamServiceConnection
import com.steevsapps.idledaddy.ui.compat.requestIgnoreBatteryOptimizations
import com.steevsapps.idledaddy.ui.compat.restoreBatteryOptimizations
import com.steevsapps.idledaddy.ui.component.IdleTopAppBar
import com.steevsapps.idledaddy.ui.component.dialog.BlacklistEditDialog
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.createDefaultPreferenceFlow
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@Composable
private fun <T> OnPreferenceChanged(value: T, onChanged: (T) -> Unit) {
    var isFirst by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (isFirst) isFirst = false else onChanged(value)
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val serviceConnection = koinInject<SteamServiceConnection>()
    val context = LocalContext.current
    SettingsScreenContent(
        onBack = onBack,
        onStayAwakeChanged = { enabled ->
            serviceConnection.service.value?.setWakeLock()
            // Doze ignores wake locks, so also ask for a battery-optimization exemption.
            // The exemption can't be revoked in code, so on disable point the user at settings.
            if (enabled) {
                context.requestIgnoreBatteryOptimizations()
            } else {
                context.restoreBatteryOptimizations()
            }
        },
        onOfflineChanged = { serviceConnection.service.value?.changeStatus() },
    )
}

@Composable
private fun SettingsScreenContent(
    onBack: () -> Unit = {},
    onStayAwakeChanged: (Boolean) -> Unit = {},
    onOfflineChanged: () -> Unit = {},
) {
    val context = LocalContext.current

    // entryValues + entries from arrays.xml, keyed so valueToText can look up the label
    val languageValues = stringArrayResource(R.array.language_option_values).toList()
    val languageLabels = languageValues.zip(stringArrayResource(R.array.language_options)).toMap()

    val preferenceFlow = if (LocalView.current.isInEditMode) {
        createDefaultPreferenceFlow()
    } else {
        PrefsManager.preferenceFlow
    }

    var showBlacklist by rememberSaveable { mutableStateOf(false) }

    IdleTheme {
        ProvidePreferenceLocals(flow = preferenceFlow) {
            if (showBlacklist) {
                BlacklistEditDialog(onDismiss = { showBlacklist = false })
            }

            val stayAwake by rememberPreferenceState("stay_awake", false)
            OnPreferenceChanged(stayAwake) { onStayAwakeChanged(it) }

            val offline by rememberPreferenceState("offline", false)
            OnPreferenceChanged(offline) { onOfflineChanged() }

            val language by rememberPreferenceState("language", "")
            OnPreferenceChanged(language) {
                Toast.makeText(context, R.string.language_changed, Toast.LENGTH_LONG).show()
            }

            Scaffold(
                topBar = {
                    IdleTopAppBar(
                        title = stringResource(R.string.settings),
                        onNavClick = onBack
                    )
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxSize(),
                ) {
                    preferenceCategory(
                        key = "cat_general",
                        title = { Text(text = stringResource(R.string.cat_general)) }
                    )
                    switchPreference(
                        key = "minimize_data",
                        defaultValue = false,
                        title = { Text(text = stringResource(R.string.pref_minimize_data)) },
                        summary = { Text(text = stringResource(R.string.sum_minimize_data)) },
                    )
                    switchPreference(
                        key = "stay_awake",
                        defaultValue = false,
                        title = { Text(text = stringResource(R.string.pref_stay_awake)) },
                        summary = { Text(text = stringResource(R.string.sum_stay_awake)) },
                    )
                    switchPreference(
                        key = "keep_screen_on",
                        defaultValue = false,
                        title = { Text(text = stringResource(R.string.pref_keep_screen_on)) },
                        summary = { Text(text = stringResource(R.string.sum_keep_screen_on)) },
                    )
                    switchPreference(
                        key = "include_free_games",
                        defaultValue = true,
                        title = { Text(text = stringResource(R.string.pref_include_free_games)) },
                        summary = { Text(text = stringResource(R.string.sum_include_free_games)) },
                    )
                    switchPreference(
                        key = "use_custom_loginid",
                        defaultValue = false,
                        title = { Text(text = stringResource(R.string.pref_use_custom_loginid)) },
                        summary = { Text(text = stringResource(R.string.sum_use_custom_loginid)) },
                    )
                    textFieldPreference(
                        key = "parental_pin",
                        defaultValue = "",
                        title = { Text(text = stringResource(R.string.pref_parental_pin)) },
                        summary = { Text(text = stringResource(R.string.sum_parental_pin)) },
                        textToValue = { it },
                    )
                    listPreference(
                        key = "language",
                        defaultValue = "",
                        values = languageValues,
                        title = { Text(text = stringResource(R.string.pref_language)) },
                        summary = { Text(text = stringResource(R.string.sum_language)) },
                        valueToText = { AnnotatedString(languageLabels[it] ?: it) },
                    )
                    preferenceCategory(
                        key = "cat_idle",
                        title = { Text(text = stringResource(R.string.cat_idle)) }
                    )
                    switchPreference(
                        key = "offline",
                        defaultValue = false,
                        title = { Text(text = stringResource(R.string.pref_offline)) },
                        summary = { Text(text = stringResource(R.string.sum_offline)) },
                    )
                    // The slider value is an INDEX into HOURS_UNTIL_DROPS_OPTIONS, not the hours.
                    val hoursOptions = PrefsManager.HOURS_UNTIL_DROPS_OPTIONS
                    sliderPreference(
                        key = "hours_until_drops",
                        defaultValue = PrefsManager.HOURS_UNTIL_DROPS_DEFAULT_INDEX.toFloat(),
                        title = { Text(text = stringResource(R.string.pref_hours_until)) },
                        summary = { Text(text = stringResource(R.string.sum_hours_until)) },
                        valueRange = 0f..(hoursOptions.size - 1).toFloat(),
                        valueSteps = hoursOptions.size - 2,
                        valueText = {
                            val hours = hoursOptions[it.roundToInt()]
                            Text(text = if (hours == Int.MAX_VALUE) "∞" else hours.toString())
                        },
                    )
                    preference(
                        key = "blacklist",
                        title = { Text(text = stringResource(R.string.pref_blacklist)) },
                        summary = { Text(text = stringResource(R.string.sum_blacklist)) },
                        onClick = { showBlacklist = true },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    SettingsScreenContent()
}