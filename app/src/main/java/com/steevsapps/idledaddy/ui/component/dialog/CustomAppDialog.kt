package com.steevsapps.idledaddy.ui.component.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.component.OreoTextField
import com.steevsapps.idledaddy.ui.theme.IdleTheme

private const val TYPE_APPID_LIST = 0
private const val TYPE_CUSTOM = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppDialog(
    onConfirm: (List<Game>) -> Unit,
    onDismiss: () -> Unit,
    initialTypeIndex: Int = TYPE_APPID_LIST,
    initialAppIds: List<Int> = emptyList(),
) {
    val resources = LocalResources.current
    val typeOptions = stringArrayResource(R.array.custom_app_type_options)
    var typeIndex by rememberSaveable { mutableIntStateOf(initialTypeIndex) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var input by rememberSaveable {
        mutableStateOf(
            if (initialTypeIndex == TYPE_APPID_LIST) initialAppIds.joinToString(" ") else ""
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(typeIndex) {
        focusRequester.requestFocus()
    }

    fun unknownApp(appId: Int) =
        Game(appId, resources.getString(R.string.playing_unknown_app, appId), 0f, 0)

    fun parseAppIds(text: String) =
        text.trim().split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.distinct()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val text = if(initialTypeIndex == TYPE_CUSTOM) stringResource(R.string.idle_custom_app)
            else stringResource(R.string.idle_custom_app_ids)
            Text(text = text) },
        text = {
            Column {
                Box {
                    TextButton(onClick = { typeMenuExpanded = true }) {
                        Text(text = typeOptions[typeIndex])
                        Icon(
                            imageVector = if (typeMenuExpanded) {
                                Icons.Default.ArrowDropUp
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                            contentDescription = null,
                        )
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        typeOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    typeIndex = index
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                OreoTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.focusRequester(focusRequester),
                    placeholder = stringResource(
                        if (typeIndex == TYPE_APPID_LIST) {
                            R.string.enter_appids
                        } else {
                            R.string.desc_custom_app
                        }
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val text = input.trim()
                when (typeIndex) {
                    TYPE_CUSTOM ->
                        if (text.isNotEmpty()) onConfirm(listOf(Game(0, text, 0f, 0))) else onDismiss()

                    TYPE_APPID_LIST -> {
                        val appIds = parseAppIds(text)
                        if (appIds.isNotEmpty()) onConfirm(appIds.map(::unknownApp)) else onDismiss()
                    }

                    else -> onDismiss()
                }
            }) {
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

/**
 * Preview
 */

private class CustomAppTypePreview : PreviewParameterProvider<Int> {
    private val types = listOf(
        "App ID list" to TYPE_APPID_LIST,
        "Custom name" to TYPE_CUSTOM,
    )

    override val values = types.asSequence().map { it.second }

    override fun getDisplayName(index: Int) = types[index].first
}

@Preview
@Composable
private fun Preview(@PreviewParameter(CustomAppTypePreview::class) typeIndex: Int) {
    IdleTheme {
        Box(Modifier.fillMaxSize()) {
            CustomAppDialog(
                onConfirm = {},
                onDismiss = {},
                initialTypeIndex = typeIndex,
                initialAppIds = listOf(440, 570, 730),
            )
        }
    }
}