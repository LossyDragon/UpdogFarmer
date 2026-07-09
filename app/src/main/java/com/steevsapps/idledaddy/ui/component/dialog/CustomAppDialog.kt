package com.steevsapps.idledaddy.ui.component.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.component.OreoTextField
import com.steevsapps.idledaddy.ui.theme.IdleTheme

private const val TYPE_APPID = 0
private const val TYPE_CUSTOM = 1
private const val TYPE_APPID_LIST = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppDialog(
    onConfirm: (Game) -> Unit,
    onConfirmList: (List<Game>) -> Unit,
    onDismiss: () -> Unit,
    initialTypeIndex: Int = TYPE_APPID,
) {
    val resources = LocalResources.current
    val typeOptions = stringArrayResource(R.array.custom_app_type_options)
    var typeIndex by rememberSaveable { mutableIntStateOf(initialTypeIndex) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }

    // Collected app IDs for TYPE_APPID_LIST, added one at a time
    val appIds = remember { mutableStateListOf<Int>() }

    fun unknownApp(appId: Int) =
        Game(appId, resources.getString(R.string.playing_unknown_app, appId), 0f, 0)

    fun addAppId() {
        val id = input.trim().toIntOrNull()
        if (id != null && id !in appIds) {
            appIds.add(0, id)
            input = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.idle_custom_app)) },
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
                if (typeIndex == TYPE_APPID_LIST) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OreoTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = stringResource(R.string.custom_app_list_hint),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { addAppId() }),
                        )
                        IconButton(onClick = { addAppId() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_app_id),
                            )
                        }
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(items = appIds, key = { it }) { id ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = id.toString(), modifier = Modifier.weight(1f))
                                IconButton(onClick = { appIds.remove(id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_app_id),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OreoTextField(
                        value = input,
                        onValueChange = { input = it },
                        placeholder = stringResource(R.string.desc_custom_app),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (typeIndex == TYPE_APPID) KeyboardType.Number else KeyboardType.Text,
                        ),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (typeIndex == TYPE_APPID_LIST) addAppId()
                val text = input.trim()
                when (typeIndex) {
                    TYPE_APPID ->
                        text.toIntOrNull()?.let { onConfirm(unknownApp(it)) } ?: onDismiss()

                    TYPE_CUSTOM ->
                        if (text.isNotEmpty()) onConfirm(Game(0, text, 0f, 0)) else onDismiss()

                    TYPE_APPID_LIST ->
                        if (appIds.isNotEmpty()) onConfirmList(appIds.map(::unknownApp)) else onDismiss()

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
        "App ID" to TYPE_APPID,
        "Custom name" to TYPE_CUSTOM,
        "App ID list" to TYPE_APPID_LIST,
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
                onConfirmList = {},
                onDismiss = {},
                initialTypeIndex = typeIndex,
            )
        }
    }
}