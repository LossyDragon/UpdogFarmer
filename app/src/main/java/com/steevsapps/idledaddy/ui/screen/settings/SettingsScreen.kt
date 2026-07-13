package com.steevsapps.idledaddy.ui.screen.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.preferences.PrefsManager
import com.steevsapps.idledaddy.steam.SteamServiceConnection
import com.steevsapps.idledaddy.ui.component.dialog.BlacklistEditDialog
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.createDefaultPreferenceFlow
import me.zhanghai.compose.preference.footerPreference
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
    SettingsScreenContent(
        onBack = onBack,
        onStayAwakeChanged = { serviceConnection.service.value?.setWakeLock() },
        onOfflineChanged = { serviceConnection.service.value?.changeStatus() },
    )
}

@Composable
private fun SettingsScreenContent(
    onBack: () -> Unit = {},
    onStayAwakeChanged: () -> Unit = {},
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
            OnPreferenceChanged(stayAwake) { onStayAwakeChanged() }

            val offline by rememberPreferenceState("offline", false)
            OnPreferenceChanged(offline) { onOfflineChanged() }

            val language by rememberPreferenceState("language", "")
            OnPreferenceChanged(language) {
                Toast.makeText(context, R.string.language_changed, Toast.LENGTH_LONG).show()
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(text = stringResource(R.string.settings)) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                )
                            }
                        }
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
                    sliderPreference(
                        key = "hours_until_drops",
                        defaultValue = 3f,
                        title = { Text(text = stringResource(R.string.pref_hours_until)) },
                        summary = { Text(text = stringResource(R.string.sum_hours_until)) },
                        valueRange = 0f..5f,
                        valueSteps = 4,
                        valueText = { Text(text = it.roundToInt().toString()) },
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