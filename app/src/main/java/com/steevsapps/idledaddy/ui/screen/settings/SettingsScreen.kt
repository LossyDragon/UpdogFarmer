package com.steevsapps.idledaddy.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.createDefaultPreferenceFlow
import me.zhanghai.compose.preference.createPreferenceFlow
import me.zhanghai.compose.preference.footerPreference
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference
import me.zhanghai.compose.preference.textFieldPreference
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current

    // entryValues + entries from arrays.xml, keyed so valueToText can look up the label
    val languageValues = stringArrayResource(R.array.language_option_values).toList()
    val languageLabels = languageValues.zip(stringArrayResource(R.array.language_options)).toMap()

    val preferenceFlow = if (LocalView.current.isInEditMode) {
        createDefaultPreferenceFlow()
    } else {
        remember {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            createPreferenceFlow(prefs)
        }
    }

    var showBlacklist by rememberSaveable { mutableStateOf(false) }

    IdleTheme {
        ProvidePreferenceLocals(flow = preferenceFlow) {
            if (showBlacklist) {
                BlacklistEditDialog(onDismiss = { showBlacklist = false })
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
                    item { HorizontalDivider(modifier = Modifier.fillMaxWidth()) }
                    footerPreference(
                        key = "cat_footer",
                        summary = {
                            Column {
                                Text(text = stringResource(R.string.app_name) + ", maintained by @LossyDragon")
                                Text(text = "Version Name: " + BuildConfig.VERSION_NAME)
                                Text(text = "Version Code: " + BuildConfig.VERSION_CODE)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlacklistEditDialog(onDismiss: () -> Unit) {
    var storedValue by rememberPreferenceState("blacklist", "")
    val ids = remember { storedValue.split(",").filter { it.isNotEmpty() }.toMutableStateList() }
    var input by rememberSaveable { mutableStateOf("") }

    fun addItem() {
        val text = input.trim()
        if (text.matches("\\d+".toRegex()) && text !in ids) {
            ids.add(0, text)
            input = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.pref_blacklist)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            // autoSize seems neat. 
                            Text(
                                text = stringResource(R.string.blacklist_hint),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 10.sp,
                                    stepSize = 1.sp,
                                ),
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { addItem() }),
                        singleLine = true,
                    )
                    IconButton(onClick = { addItem() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_to_blacklist),
                        )
                    }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                    items(items = ids, key = { it }) { id ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = id, modifier = Modifier.weight(1f))
                            IconButton(onClick = { ids.remove(id) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove_from_blacklist),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    storedValue = ids.joinToString(",")
                    onDismiss()
                }
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    SettingsScreen()
}